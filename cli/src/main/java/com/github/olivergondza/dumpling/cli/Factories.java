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
package com.github.olivergondza.dumpling.cli;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.factory.IllegalRuntimeStateException;
import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory;
import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory.FailedToInitializeJmxConnection;
import com.github.olivergondza.dumpling.factory.PidRuntimeFactory;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;
import com.github.olivergondza.dumpling.model.jmx.JmxRuntime;

final /*package*/ class Factories {

    final /*package*/ static class ThreadDump implements CliRuntimeFactory<ThreadDumpRuntime> {
        @Override
        public @Nonnull String getKind() {
            return "threaddump";
        }

        @Override
        public String getDescription() {
            return "Parse threaddrump from file, or standard input when '-' provided as a locator.";
        }

        @Override
        public @Nonnull ThreadDumpRuntime createRuntime(
                @Nonnull String locator, @Nonnull ProcessStream process
        ) throws CommandFailedException {
            ThreadDumpFactory factory = new ThreadDumpFactory();

            if ("-".equals(locator)) {
                // Read stdin
                return factory.fromStream(process.in());
            }

            try {
                return factory.fromFile(new File(locator));
            } catch (IOException ex) {
                throw new CommandFailedException(ex);
            } catch (IllegalRuntimeStateException ex) {
                throw new CommandFailedException(ex);
            }
        }
    }

    final /*package*/ static class Jmx implements CliRuntimeFactory<JmxRuntime> {
        @Override
        public @Nonnull String getKind() {
            return "jmx";
        }

        @Override
        public String getDescription() {
            return "Create runtime from JMX process identified by PID or HOST:PORT combination. Credentials can be provided as USER:PASSWORD@HOST:PORT.";
        }

        @Override
        public @Nonnull JmxRuntime createRuntime(@Nonnull String locator, @Nonnull ProcessStream process) throws CommandFailedException {
            JmxRuntimeFactory factory = new JmxRuntimeFactory();

            try {
                return factory.forConnectionString(locator);
            } catch (FailedToInitializeJmxConnection ex) {
                throw new CommandFailedException(ex);
            } catch (IllegalRuntimeStateException ex) {
                throw new CommandFailedException(ex);
            }
        }
    }

    final /*package*/ static class Pid implements CliRuntimeFactory<ThreadDumpRuntime> {

        @Override
        public @Nonnull String getKind() {
            return "process";
        }

        @Override
        public String getDescription() {
            return "Create runtime from running process identified by PID.";
        }

        @Override
        public @Nonnull ThreadDumpRuntime createRuntime(String locator, ProcessStream streams) throws CommandFailedException {
            PidRuntimeFactory factory = new PidRuntimeFactory();
            try {
                return factory.fromProcess(pid(locator));
            } catch (IOException ex) {
                throw new CommandFailedException("Unable to invoke jstack: " + ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                throw new CommandFailedException("jstack invocation interrupted: " + ex.getMessage(), ex);
            } catch (IllegalRuntimeStateException ex) {
                throw new CommandFailedException(ex);
            }
        }

        private int pid(String locator) {
            try {
                return Integer.parseInt(locator.trim());
            } catch (NumberFormatException ex) {
                throw new CommandFailedException("Unable to parse '" + locator + "' as process ID", ex);
            }
        }
    }
}
