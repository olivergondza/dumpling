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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.factory.IllegalRuntimeStateException;
import com.github.olivergondza.dumpling.query.SingleThreadSetQuery;

/**
 * Snapshot of threads in JVM at given time.
 *
 * @author ogondza
 */
public abstract class ProcessRuntime<
        RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>,
        SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
        ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>
> extends ModelObject {

    private final @Nonnull SetType threads;
    private final @Nonnull SetType emptySet;

    public ProcessRuntime(@Nonnull Set<? extends ProcessThread.Builder<?>> builders) {
        this.threads = createThreads(builders);
        this.emptySet = createSet(Collections.<ThreadType>emptySet());
        checkSanity();
    }

    private @Nonnull SetType createThreads(@Nonnull Set<? extends ProcessThread.Builder<?>> builders) {
        Set<ThreadType> threads = new LinkedHashSet<ThreadType>(builders.size());
        for (ProcessThread.Builder<?> builder: builders) {
            threads.add(createThread(builder));
        }

        int buildersSize = builders.size();
        int threadsSize = threads.size();
        if (buildersSize != threadsSize) throw new IllegalRuntimeStateException(
                "%d builders produced %d threads", buildersSize, threadsSize
        );

        return createSet(Collections.unmodifiableSet(threads));
    }

    private void checkSanity() {
        // At most one thread should own the monitor/synchronizer
        HashMap<ThreadLock, ThreadType> monitors = new HashMap<ThreadLock, ThreadType>();
        HashMap<ThreadLock, ThreadType> synchronizers = new HashMap<ThreadLock, ThreadType>();
        for (ThreadType t: threads) {
            for (ThreadLock lock: t.getAcquiredMonitors()) {
                ThreadType existing = monitors.put(lock, t);
                if (existing != null) {
                    throw new IllegalRuntimeStateException(
                            "Multiple threads own the same monitor '%s':%n%s%n%nAND%n%n%s%n",
                            lock, existing, t
                    );
                }
            }

            for (ThreadLock lock: t.getAcquiredSynchronizers()) {
                ThreadType existing = synchronizers.put(lock, t);
                if (existing != null) {
                    throw new IllegalRuntimeStateException(
                            "Multiple threads own the same synchronizer '%s':%n%s%n%nAND%n%n%s%n",
                            lock, existing, t
                    );
                }
            }
        }
    }

    protected abstract @Nonnull SetType createSet(@Nonnull Set<ThreadType> threads);

    protected abstract @Nonnull ThreadType createThread(@Nonnull ProcessThread.Builder<?> builder);

    /**
     * All threads in current runtime.
     */
    public @Nonnull SetType getThreads() {
        return threads;
    }

    public @Nonnull SetType getEmptyThreadSet() {
        return emptySet;
    }

    /**
     * Instantiate {@link ThreadSet} scoped to this runtime.
     */
    public @Nonnull SetType getThreadSet(@Nonnull Collection<ThreadType> threads) {
        if (threads.isEmpty()) return emptySet;

        Set<ThreadType> threadSet = threads instanceof Set
                ? (Set<ThreadType>) threads
                : new LinkedHashSet<ThreadType>(threads)
        ;
        return createSet(threadSet);
    }

    /**
     * Run query against all threads in the runtime.
     *
     * @see ThreadSet#query(SingleThreadSetQuery)
     */
    public <T extends SingleThreadSetQuery.Result<SetType, RuntimeType, ThreadType>> T query(SingleThreadSetQuery<T> query) {
        return threads.query(query);
    }

    @Override
    public void toString(PrintStream stream, Mode mode) {
        threads.toString(stream, mode);
    }
}
