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

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.cli.CliCommand;
import com.github.olivergondza.dumpling.cli.ProcessStream;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

/**
 * Detect top-contenders, threads that block largest number of other threads.
 *
 * @author ogondza
 */
public final class TopContenders implements SingleThreadSetQuery<TopContenders.Result> {

    private boolean showStackTraces = false;

    public TopContenders showStackTraces() {
        this.showStackTraces = true;
        return this;
    }

    /**
     * @param threads Thread subset to be considered as a potential contenders. All threads in runtime are considered as blocking threads.
     */
    @Override
    public Result query(ThreadSet threads) {
        return new Result(threads, showStackTraces);
    }

    public final static class Command implements CliCommand {

        @Option(name = "-i", aliases = {"--in"}, required = true, usage = "Input for process runtime")
        private ProcessRuntime runtime;

        @Option(name = "--show-stack-traces", usage = "List stack traces of all threads involved")
        private boolean showStackTraces = false;

        @Override
        public String getName() {
            return "top-contenders";
        }

        @Override
        public String getDescription() {
            return "Detect top-contenders, threads that block largest number of other threads";
        }

        @Override
        public int run(@Nonnull ProcessStream process) throws CmdLineException {
            Result result = new Result(runtime.getThreads(), showStackTraces);
            result.printInto(process.out());
            return result.exitCode();
        }
    }

    public final static class Result extends SingleThreadSetQuery.Result {

        private final @Nonnull Map<ProcessThread, ThreadSet> contenders;
        private final @Nonnull ThreadSet involved;
        private final @Nonnegative int blocked;

        private Result(ThreadSet threads, boolean showStacktraces) {
            final Set<ProcessThread> involved = new LinkedHashSet<ProcessThread>();
            final Map<ProcessThread, ThreadSet> contenders = new TreeMap<ProcessThread, ThreadSet>(new Comparator<ProcessThread>() {
                @Override
                public int compare(ProcessThread lhs, ProcessThread rhs) {
                    int lhsSize = lhs.getBlockedThreads().size();
                    int rhsSize = rhs.getBlockedThreads().size();

                    if (lhsSize > rhsSize) return -1;
                    if (lhsSize < rhsSize) return 1;

                    return 0;
                }
            });

            for (ProcessThread thread: threads) {
                ThreadSet blocked = thread.getBlockedThreads();
                if (blocked.isEmpty()) continue;

                contenders.put(thread, blocked);
                involved.add(thread);
                for (ProcessThread b: blocked) {
                    involved.add(b);
                }
            }

            this.contenders = Collections.unmodifiableMap(contenders);
            this.involved = showStacktraces
                    ? threads.derive(involved)
                    : threads.getProcessRuntime().getEmptyThreadSet()
            ;
            this.blocked = involved.size() - contenders.size();
        }

        public @Nonnull ThreadSet getBlockers() {
            return involved.derive(contenders.keySet());
        }

        /**
         * Get threads blocked by a thread.
         *
         * @return null when there is none.
         */
        public @CheckForNull ThreadSet blockedBy(ProcessThread thread) {
            return contenders.get(thread);
        }

        @Override
        protected void printResult(PrintStream out) {
            for (Entry<ProcessThread, ThreadSet> contention: contenders.entrySet()) {

                out.print("* ");
                out.println(contention.getKey().getHeader());
                int i = 1;
                for (ProcessThread blocked: contention.getValue()) {

                    out.printf("  (%d) ", i++);
                    out.println(blocked.getHeader());
                }
            }
        }

        @Override
        protected ThreadSet involvedThreads() {
            return involved;
        }

        @Override
        protected void printSummary(PrintStream out) {
            int blocking = contenders.size();

            out.printf("Blocking threads: %d; Blocked threads: %d%n", blocking, blocked);
        }

        @Override
        public int exitCode() {
            return contenders.size();
        }
    }
}
