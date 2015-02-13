/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
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
package com.github.olivergondza.dumpling.cli;

import groovy.lang.Binding;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory;
import com.github.olivergondza.dumpling.factory.JvmRuntimeFactory;
import com.github.olivergondza.dumpling.factory.PidRuntimeFactory;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

/*package*/ class GroovyInterpretterConfig {

    /*package*/ Collection<String> getStarImports() {
        return Arrays.asList(
                "com.github.olivergondza.dumpling.cli",
                "com.github.olivergondza.dumpling.factory",
                "com.github.olivergondza.dumpling.model",
                "com.github.olivergondza.dumpling.query"
        );
    }

    /*package*/ Collection<String> getStaticStars() {
        return Arrays.asList("com.github.olivergondza.dumpling.model.ProcessThread");
    }

    /**
     * Default binding to be used in groovy interpreters.
     *
     * Dumpling exposed API is available via <tt>D</tt> property.
     */
    /*package*/ Binding getDefaultBinding(@Nonnull ProcessStream stream) {
        Binding binding = new Binding();
        binding.setProperty("out", stream.out());
        binding.setProperty("err", stream.err());

        binding.setProperty("D", new CliApiEntryPoint(ProcessStream.system()));

        binding.setProperty("load", new Load(stream)); // Compatibility
        binding.setProperty("$load", new LoadCommand(stream)); // Compatibility

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

        public ProcessRuntime call(String filename) throws Exception {
            stream.err().println("load(String) command is deprecated. Use 'D.load.threaddump(String)' instead.");
            return new ThreadDumpFactory().fromFile(new File(filename));
        }
    }

    @SuppressWarnings("unused")
    /*package*/ static class CliApiEntryPoint {

        private final @Nonnull ProcessStream streams;

        public CliApiEntryPoint(@Nonnull ProcessStream streams) {
            this.streams = streams;
        }

        public @Nonnull LoadCommand getLoad() {
            return new LoadCommand(streams);
        }
    }

    @SuppressWarnings("unused")
    private static final class LoadCommand {

        private final @Nonnull ProcessStream streams;

        public LoadCommand(@Nonnull ProcessStream streams) {
            this.streams = streams;
        }

        public ProcessRuntime threaddump(@Nonnull String filename) throws IOException {
            return new ThreadDumpFactory().fromFile(new File(filename));
        }

        public ProcessRuntime process(int pid) throws IOException, InterruptedException {
            return new PidRuntimeFactory().fromProcess(pid);
        }

        public ProcessRuntime jmx(int pid) {
            return new JmxRuntimeFactory().forLocalProcess(pid);
        }

        public ProcessRuntime jmx(@Nonnull String connection) {
            return new JmxRuntimeFactory().createRuntime(connection, streams);
        }

        public ProcessRuntime getJvm() {
            return new JvmRuntimeFactory().currentRuntime();
        }
    }
}
