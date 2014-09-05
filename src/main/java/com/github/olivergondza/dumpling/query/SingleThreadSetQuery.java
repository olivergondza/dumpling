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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ThreadSet;

/**
 * Query single {@link ThreadSet}.
 *
 * Run query against a subset of runtime threads passed in using
 * {@link #query(ThreadSet)}. Query can access whore runtime and all its threads
 * using <tt>initialSet.getProcessRuntime()</tt>. It is up to query implementation
 * to decide that an "initial set" mean in its context.
 *
 * For instance {@link Deadlocks} detect only those deadlocks where at least
 * one of initial threads are part of the cycle.
 *
 * @author ogondza
 * @see ThreadSet#query(SingleThreadSetQuery)
 * @see ProcessRuntime#query(SingleThreadSetQuery)
 */
public interface SingleThreadSetQuery<T extends SingleThreadSetQuery.Result> {

    /**
     * Get typed result of the query.
     */
    public @Nonnull T query(@Nonnull ThreadSet initialSet);

    /**
     * Query result that filter/arrange threads.
     *
     * Data holder to uniformly represent query results in both CLI and #toString
     * when used from groovy.
     *
     * Result consists of 3 parts:
     * - query result description ({@link #printResult(StringBuilder)}),
     * - involved thread listing (optional),
     * - query result summary ({@link #printSummary(StringBuilder)}).
     *
     * @author ogondza
     * @see SingleThreadSetQuery
     */
    public static abstract class Result {

        /**
         * Print query result.
         */
        protected abstract void printResult(@Nonnull PrintStream out);

        /**
         * Threads that are involved in result.
         *
         * @return null or empty set when there are no threads to be printed.
         */
        protected abstract @CheckForNull ThreadSet involvedThreads();

        /**
         * Print optional summary for a query.
         *
         * To be overriden when there is any summary to report.
         */
        protected void printSummary(@Nonnull PrintStream out) {}

        /**
         * Exit code to report when run from CLI.
         *
         * @return By default it is number of involved threads, implementation can
         * provide custom value.
         */
        public int exitCode() {
            final ThreadSet involvedThreads = involvedThreads();
            return involvedThreads == null ? 0 : involvedThreads.size();
        }

        @Override
        public final String toString() {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            printInto(new PrintStream(buffer));

            return buffer.toString();
        }

        /**
         * Print whole query result into stream.
         */
        protected final void printInto(@Nonnull PrintStream out) {
            printResult(out);

            final ThreadSet involvedThreads = involvedThreads();
            if (involvedThreads != null) {
                out.println("\n");
                out.print(involvedThreads);
            }

            printSummary(out);
        }
    }
}
