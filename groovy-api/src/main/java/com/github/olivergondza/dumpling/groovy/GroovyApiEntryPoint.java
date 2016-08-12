/*
 * The MIT License
 *
 * Copyright (c) Red Hat, Inc.
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

import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory;
import com.github.olivergondza.dumpling.factory.JvmRuntimeFactory;
import com.github.olivergondza.dumpling.factory.PidRuntimeFactory;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * The D Object exposed for Groovy scripting.
 *
 * This is considered to be an API to groovy clients, java clients can and will observe source and binary compatibility breakage.
 *
 * @author ogondza.
 */
@SuppressWarnings("unused")
public class GroovyApiEntryPoint extends GroovyApi {

    private final @Nullable ProcessRuntime<?, ?, ?> runtime;
    private final @Nonnull List<String> args;

    public GroovyApiEntryPoint(@Nonnull List<String> args, @Nullable ProcessRuntime<?, ?, ?> runtime, @Nonnull String property) {
        super(property + '.');
        this.runtime = runtime;
        this.args = Collections.unmodifiableList(args);
    }

    public @Nonnull LoadCommand getLoad() {
        return new LoadCommand(initIndent + "load.");
    }

    @ApiDoc(text = "CLI arguments passed to the script.")
    public @Nonnull List<String> getArgs() {
        return args;
    }

    @ApiDoc(text = "Current runtime passed via `--in` option. null if not provided.")
    public @Nullable ProcessRuntime<?, ?, ?> getRuntime() {
        return runtime;
    }

    @SuppressWarnings("unused")
    public static final class LoadCommand extends GroovyApi {

        private LoadCommand(@Nonnull String initIndent) {
            super(initIndent);
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
}
