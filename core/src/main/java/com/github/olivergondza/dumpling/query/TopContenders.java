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

import com.github.olivergondza.dumpling.model.ModelObject;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

/**
 * Detect top-contenders, threads that block largest number of other threads.
 *
 * @author ogondza
 */
public final class TopContenders implements SingleThreadSetQuery<TopContenders.Result<?, ?, ?>> {

    private boolean showStackTraces = false;

    public TopContenders showStackTraces() {
        this.showStackTraces = true;
        return this;
    }

    /**
     * @param threads Thread subset to be considered as a potential contenders. All threads in runtime are considered as blocking threads.
     */
    @Override
    public @Nonnull <
            SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
            RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>,
            ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>
    > Result<SetType, RuntimeType, ThreadType> query(SetType threads) {
        return new Result<SetType, RuntimeType, ThreadType>(threads, showStackTraces);
    }

    public final static class Result<
            SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
            RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>,
            ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>
    > extends SingleThreadSetQuery.Result<SetType, RuntimeType, ThreadType> {

        private final @Nonnull Map<ThreadType, SetType> contenders;
        private final @Nonnull SetType involved;
        private final @Nonnegative int blocked;

        /*package*/ Result(SetType threads, boolean showStacktraces) {
            super(showStacktraces);
            final Set<ThreadType> involved = new LinkedHashSet<ThreadType>();
            final Map<ThreadType, SetType> contenders = new TreeMap<ThreadType, SetType>(new Comparator<ThreadType>() {
                @Override
                public int compare(ThreadType lhs, ThreadType rhs) {
                    int lhsSize = lhs.getBlockedThreads().size();
                    int rhsSize = rhs.getBlockedThreads().size();

                    if (lhsSize > rhsSize) return -1;
                    if (lhsSize < rhsSize) return 1;

                    return 0;
                }
            });

            for (ThreadType thread: threads) {
                SetType blocked = thread.getBlockedThreads();
                if (blocked.isEmpty()) continue;

                contenders.put(thread, blocked);
                involved.add(thread);
                for (ThreadType b: blocked) {
                    involved.add(b);
                }
            }

            this.contenders = Collections.unmodifiableMap(contenders);
            this.involved = threads.derive(involved);
            this.blocked = involved.size() - contenders.size();
        }

        public @Nonnull SetType getBlockers() {
            return involved.derive(contenders.keySet());
        }

        /**
         * Get threads blocked by a thread.
         *
         * @return null when there is none.
         */
        public @CheckForNull SetType blockedBy(ThreadType thread) {
            return contenders.get(thread);
        }

        @Override
        protected void printResult(PrintStream out) {
            for (Entry<ThreadType, SetType> contention: contenders.entrySet()) {

                out.print("* ");
                contention.getKey().printHeader(out, ModelObject.Mode.HUMAN);
                int i = 1;
                for (ProcessThread<?, ?, ?> blocked: contention.getValue()) {

                    out.printf("  (%d) ", i++);
                    blocked.printHeader(out, ModelObject.Mode.HUMAN);
                }
            }
        }

        @Override
        protected SetType involvedThreads() {
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
