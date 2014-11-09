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
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory;
import com.github.olivergondza.dumpling.factory.PidRuntimeFactory;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

public class GroovyInterpretterConfig {

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
     * Properties prefixed with '$' are considered special and should not be overwritten by interpreters.
     */
    /*package*/ Binding getDefaultBinding(@Nonnull ProcessStream stream) {
        Binding binding = new Binding();
        binding.setProperty("out", stream.out());
        binding.setProperty("err", stream.err());

        binding.setProperty("load", new Load(stream)); // Compatibility
        binding.setProperty("$load", new LoadCommand());

        return binding;
    }

    /**
     * @deprecated Use <tt>load.threaddump(String)</tt> instead.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static final class Load {
        private ProcessStream stream;

        public Load(ProcessStream stream) {
            this.stream = stream;
        }

        public ProcessRuntime call(String filename) throws Exception {
            stream.err().println("load(String) command is deprecated. Use '$load.threaddump(String)' instead.");
            return new ThreadDumpFactory().fromFile(new File(filename));
        }
    }

    @SuppressWarnings("unused")
    private static final class LoadCommand {
        public ProcessRuntime threaddump(String filename) throws Exception {
            return new ThreadDumpFactory().fromFile(new File(filename));
        }

        public ProcessRuntime process(int pid ) throws Exception {
            return new PidRuntimeFactory().forProcess(pid);
        }

        public ProcessRuntime jmx(int pid) throws Exception {
            return new JmxRuntimeFactory().forLocalProcess(pid);
        }

        public ProcessRuntime jmx(String connection) throws Exception {
            return new JmxRuntimeFactory().createRuntime(connection, ProcessStream.system());
        }
    }
}
