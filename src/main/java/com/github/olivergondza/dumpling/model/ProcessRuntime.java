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
package com.github.olivergondza.dumpling.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.factory.IllegalRuntimeStateException;
import com.github.olivergondza.dumpling.query.SingleThreadSetQuery;

/**
 * Snapshot of threads in JVM at given time.
 * @author ogondza
 */
public class ProcessRuntime {

    private final @Nonnull ThreadSet threads;
    private final @Nonnull ThreadSet emptySet;

    public ProcessRuntime(@Nonnull Set<ProcessThread.Builder> builders) {
        this.emptySet = new ThreadSet(this, Collections.<ProcessThread>emptySet());
        this.threads = createThreads(builders);

        int buildersSize = builders.size();
        int threadsSize = threads.size();
        if (buildersSize != threadsSize) throw new IllegalRuntimeStateException(
                "%d builders produced %d threads", buildersSize, threadsSize
        );
    }

    private @Nonnull ThreadSet createThreads(@Nonnull Set<ProcessThread.Builder> builders) {
        Set<ProcessThread> threads = new LinkedHashSet<ProcessThread>(builders.size());
        for (ProcessThread.Builder builder: builders) {
            threads.add(builder.build(this));
        }
        return getThreadSet(Collections.unmodifiableSet(threads));
    }

    /**
     * All threads in current runtime.
     */
    public @Nonnull ThreadSet getThreads() {
        return threads;
    }

    public @Nonnull ThreadSet getEmptyThreadSet() {
        return emptySet;
    }

    /**
     * Instantiate {@link ThreadSet} scoped to this runtime.
     */
    public @Nonnull ThreadSet getThreadSet(@Nonnull Collection<ProcessThread> threads) {
        if (threads.isEmpty()) return emptySet;

        Set<ProcessThread> threadSet = threads instanceof Set
                ? (Set<ProcessThread>) threads
                : new LinkedHashSet<ProcessThread>(threads)
        ;
        return new ThreadSet(this, threadSet);
    }

    /**
     * Run query against all threads in the runtime.
     *
     * @see ThreadSet#query(SingleThreadSetQuery)
     */
    public @Nonnull <T extends SingleThreadSetQuery.Result> T query(@Nonnull SingleThreadSetQuery<T> query) {
        return threads.query(query);
    }
}
