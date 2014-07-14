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
package com.github.olivergondza.dumpling.query;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.cli.CliCommand;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

public class DeadlockDetector {

    public @Nonnull Set<ThreadSet> getAll(@Nonnull ProcessRuntime runtime) {
        final ThreadSet threads = runtime.getThreads();

        // No need to revisit threads more than once
        final Set<ProcessThread> analyzed = new HashSet<ProcessThread>(threads.size());
        final HashSet<ThreadSet> deadlocks = new HashSet<ThreadSet>(1);

        for (ProcessThread thread: threads) {

            Set<ProcessThread> cycleCandidate = new HashSet<ProcessThread>(2);
            for (ProcessThread blocking = thread.getBlockingThread(); blocking != null; blocking = blocking.getBlockingThread()) {
                if (analyzed.contains(thread)) break;

                cycleCandidate.add(blocking);

                if (thread == blocking) {
                    // Cycle detected - record deadlock and break the cycle traversing.
                    deadlocks.add(new ThreadSet(runtime, cycleCandidate));
                    analyzed.addAll(cycleCandidate);
                    break;
                }
            }

            analyzed.add(thread);
        }

        return deadlocks;
    }

    public static class Command implements CliCommand {

        @Option(name = "-i", aliases = {"--in"}, required = true, usage = "Input for process runtime")
        private ProcessRuntime runtime;

        public String getName() {
            return "detect-deadlocks";
        }

        public String getDescription() {
            return "Detect cycles of blocked threads";
        }

        public int run(InputStream in, PrintStream out, PrintStream err) throws CmdLineException {

            Set<ThreadSet> deadlocks = new DeadlockDetector().getAll(runtime);
            out.println(deadlocks.size() + " deadlocks detected");

            for(ThreadSet deadlock: deadlocks) {
                for(ProcessThread thread: deadlock) {
                    out.print(" - ");
                    out.print(thread.getName());
                }

                out.println();
            }

            return deadlocks.size();
        }
    }
}
