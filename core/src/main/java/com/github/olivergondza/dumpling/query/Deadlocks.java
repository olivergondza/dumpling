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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadSet;

/**
 * Detect deadlocks in thread set.
 *
 * @author ogondza
 */
public final class Deadlocks implements SingleThreadSetQuery<Deadlocks.Result<?, ?, ?>> {

    private boolean showStackTraces = false;

    public Deadlocks showStackTraces() {
        this.showStackTraces = true;
        return this;
    }

    /**
     * @param threads Include only cycles that contain at least one of input threads.
     */
    @Override
    public @Nonnull <
            SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
            RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>,
            ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>
    > Result<SetType, RuntimeType, ThreadType> query(@Nonnull SetType threads) {
        return new Result<SetType, RuntimeType, ThreadType>(threads, showStackTraces);
    }

    /**
     * Deadlock detection result.
     *
     * A set of all deadlocks found. Involved threads are all threads that are part of any deadlock.
     *
     * @author ogondza
     */
    public final static class Result<
            SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
            RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>,
            ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>
    > extends SingleThreadSetQuery.Result<SetType, RuntimeType, ThreadType> {

        private final @Nonnull Set<SetType> deadlocks;
        private final @Nonnull SetType involved;

        private Result(@Nonnull SetType input, boolean showStackTraces) {
            super(showStackTraces);

            final HashSet<SetType> deadlocks = new HashSet<SetType>(1);
            final LinkedHashSet<ThreadType> involved = new LinkedHashSet<ThreadType>(2);
            // No need to visit threads more than once
            final Set<ThreadType> analyzed = new HashSet<ThreadType>(input.size());

            for (ThreadType thread: input) {

                ArrayList<ThreadType> cycleCandidate = new ArrayList<ThreadType>(2);
                for (ThreadType blocking = thread.getBlockingThread(); blocking != null; blocking = blocking.getBlockingThread()) {
                    if (analyzed.contains(thread)) break;

                    int beginning = cycleCandidate.indexOf(blocking);
                    if (beginning != -1) {
                        @SuppressWarnings("null")
                        @Nonnull List<ThreadType> cycle = cycleCandidate.subList(beginning, cycleCandidate.size());
                        deadlocks.add(input.derive(cycle));
                        involved.addAll(cycle);
                        analyzed.addAll(cycleCandidate);
                        break;
                    }

                    cycleCandidate.add(blocking);
                }

                analyzed.add(thread);
            }

            this.deadlocks = Collections.unmodifiableSet(deadlocks);
            this.involved = input.derive(involved);
        }

        /**
         * Get found deadlocks.
         *
         * @return {@link Set} of {@link ThreadSet}s representing found deadlocks.
         */
        public @Nonnull Set<SetType> getDeadlocks() {
            return deadlocks;
        }

        @Override
        protected void printResult(PrintStream out) {
            int i = 1;
            for(SetType deadlock: deadlocks) {
                HashSet<ThreadLock> involvedLocks = new HashSet<ThreadLock>(deadlock.size());
                for(ThreadType thread: deadlock) {
                    involvedLocks.add(thread.getWaitingToLock());
                    involvedLocks.add(thread.getWaitingOnLock());
                }

                out.printf("%nDeadlock #%d:%n", i++);
                for(ThreadType thread: deadlock) {
                    out.println(thread.getHeader());
                    if (thread.getWaitingToLock() != null) {
                        out.printf("\tWaiting to %s%n", thread.getWaitingToLock());
                    } else if (thread.getWaitingOnLock() != null) {
                        out.printf("\tWaiting on %s%n", thread.getWaitingOnLock());
                    } else {
                        assert false;
                    }

                    for (ThreadLock lock: thread.getAcquiredLocks()) {

                        char mark = involvedLocks.contains(lock) ? '*' : ' ';
                        out.printf("\tAcquired %c %s%n", mark, lock);
                    }
                }
            }
        }

        @Override
        protected SetType involvedThreads() {
            return involved;
        }

        @Override
        protected void printSummary(PrintStream out) {
            out.printf("Deadlocks: %d%n", deadlocks.size());
        }

        @Override
        public int exitCode() {
            return deadlocks.size();
        }
    }
}
