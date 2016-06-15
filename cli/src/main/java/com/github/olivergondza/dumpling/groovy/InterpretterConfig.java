/*
 * The MIT License
 *
 * Copyright (c) 2015 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.olivergondza.dumpling.groovy;

import groovy.lang.Binding;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.olivergondza.dumpling.cli.ProcessStream;
import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory;
import com.github.olivergondza.dumpling.factory.JvmRuntimeFactory;
import com.github.olivergondza.dumpling.factory.PidRuntimeFactory;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Specific config for CLI clients.
 *
 * @author ogondza
 */
public class InterpretterConfig extends GroovyInterpretterConfig {

    /**
     * Default binding to be used in groovy interpreters.
     *
     * Dumpling exposed API is available via <tt>D</tt> property.
     */
    public Binding getDefaultBinding(@Nonnull ProcessStream stream, @Nonnull List<String> args, @Nullable ProcessRuntime<?, ?, ?> runtime) {
        Binding binding = new Binding();
        binding.setProperty("out", stream.out());
        binding.setProperty("err", stream.err());

        binding.setProperty("D", new CliApiEntryPoint(ProcessStream.system(), args, runtime, "D"));

        return binding;
    }

    /**
     * @deprecated Use <tt>D.load.threaddump(String)</tt> instead.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static final class Load {
        private ProcessStream stream;

        public Load(ProcessStream stream) {
            this.stream = stream;
        }

        public ProcessRuntime<?, ?, ?> call(String filename) throws Exception {
            stream.err().println("load(String) command is deprecated. Use 'D.load.threaddump(String)' instead.");
            return new ThreadDumpFactory().fromFile(new File(filename));
        }
    }

    /*package*/ static class CliApi {
        private static final String NL = System.getProperty("line.separator");

        protected final @Nonnull String initIndent;

        /*package*/ public CliApi(@Nonnull String initIndent) {
            this.initIndent = initIndent;
        }

        /**
         * Generate refdoc.
         */
        @Override
        public final String toString() {
            StringBuilder sb = new StringBuilder();
            print(sb, getClass(), initIndent);
            return sb.toString();
        }

        private void print(StringBuilder sb, Class<?> base, String indent) {
            for (Method m: base.getMethods()) {

                ApiDoc doc = m.getAnnotation(ApiDoc.class);

                // Is declared in API?
                if (!isApi(m.getDeclaringClass())) continue;

                String header = groovyHeader(m);

                if (doc != null) {
                    sb.append(indent).append(header).append(": ").append(type(m.getReturnType())).append(NL);
                    final char[] padding = new char[indent.length()];
                    Arrays.fill(padding, ' ');
                    sb.append(padding).append(doc.text()).append(NL).append(NL);
                }

                Class<?> ret = m.getReturnType();
                if (isApi(ret)) {
                    print(sb, ret, indent + header + '.');
                }
            }
        }

        private boolean isApi(Class<?> type) {
            return CliApi.class.isAssignableFrom(type);
        }

        private String groovyHeader(Method m) {
            String name = m.getName();
            Class<?>[] args = m.getParameterTypes();

            StringBuilder sb = new StringBuilder();
            if (args.length == 0) {
                if (!name.startsWith("get") || name.length() < 4) return String.format("%s()", name);

                // Property getter
                return String.format("%s%s", Character.toLowerCase(name.charAt(3)), name.substring(4));
            } else {
                sb.append(name).append('(');
                boolean first = true;
                for (Class<?> a: args) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(type(a));
                    first = false;
                }
                sb.append(')');
            }

            return sb.toString();
        }

        private String type(Class<?> cls) {
            final Package pkg = cls.getPackage();

            return pkg != null && "java.lang".equals(pkg.getName())
                    ? cls.getSimpleName()
                    : cls.getCanonicalName()
            ;
        }
    }

    @SuppressWarnings("unused")
    /*package*/ static class CliApiEntryPoint extends CliApi {

        private final @Nonnull ProcessStream streams;
        private final @Nullable ProcessRuntime<?, ?, ?> runtime;
        private final @Nonnull List<String> args;

        /*package*/ CliApiEntryPoint(@Nonnull ProcessStream streams, @Nonnull List<String> args, @Nullable ProcessRuntime<?, ?, ?> runtime, @Nonnull String property) {
            super(property + '.');
            this.streams = streams;
            this.runtime = runtime;
            this.args = Collections.unmodifiableList(args);
        }

        public @Nonnull LoadCommand getLoad() {
            return new LoadCommand(streams, initIndent + "load.");
        }

        @ApiDoc(text = "CLI arguments passed to the script")
        public @Nonnull List<String> getArgs() {
            return args;
        }

        @ApiDoc(text = "Current runtime passed via `--in` option. null if not provided.")
        public @Nullable ProcessRuntime<?, ?, ?> getRuntime() {
            return runtime;
        }
    }

    @SuppressWarnings("unused")
    private static final class LoadCommand extends CliApi {

        private final @Nonnull ProcessStream streams;

        /*public*/ LoadCommand(@Nonnull ProcessStream streams, @Nonnull String initIndent) {
            super(initIndent);
            this.streams = streams;
        }

        @ApiDoc(text = "Load runtime from threaddump.")
        public ProcessRuntime<?, ?, ?> threaddump(@Nonnull String filename) throws IOException {
            return new ThreadDumpFactory().fromFile(new File(filename));
        }

        @ApiDoc(text = "Load runtime from process identified by PID.")
        public ProcessRuntime<?, ?, ?> process(int pid) throws IOException, InterruptedException {
            return new PidRuntimeFactory().fromProcess(pid);
        }

        @ApiDoc(text = "Load runtime via JMX from process identified by PID.")
        public ProcessRuntime<?, ?, ?> jmx(int pid) {
            return new JmxRuntimeFactory().forLocalProcess(pid);
        }

        @ApiDoc(text = "Load runtime via JMX using JMX connection string.")
        public ProcessRuntime<?, ?, ?> jmx(@Nonnull String connection) {
            return new JmxRuntimeFactory().forConnectionString(connection);
        }

        @ApiDoc(text = "Capture runtime of current JVM.")
        public ProcessRuntime<?, ?, ?> getJvm() {
            return new JvmRuntimeFactory().currentRuntime();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private static @interface ApiDoc {
        String text();
    }
}
