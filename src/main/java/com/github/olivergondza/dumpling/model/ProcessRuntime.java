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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.query.SingleRuntimeQuery;

/**
 * Snapshot of threads in JVM at given time.
 * @author ogondza
 */
public class ProcessRuntime {

    private @Nonnull final ThreadSet threads;
    private @Nonnull final ThreadSet emptySet;

    public ProcessRuntime(@Nonnull Set<ProcessThread.Builder> builders) {
        this.threads = createThreads(builders);
        this.emptySet = new ThreadSet(this, Collections.<ProcessThread>emptySet());

        int buildersSize = builders.size();
        int threadsSize = threads.size();
        if (buildersSize != threadsSize) throw new AssertionError(
                String.format("%d builders produced %d threads", buildersSize, threadsSize)
        );
    }

    private @Nonnull ThreadSet createThreads(@Nonnull Set<ProcessThread.Builder> builders) {
        Set<ProcessThread> threads = new HashSet<ProcessThread>(builders.size());
        for (ProcessThread.Builder builder: builders) {
            threads.add(builder.build(this));
        }
        return new ThreadSet(this, Collections.unmodifiableSet(threads));
    }

    /**
     * All threads in current runtime.
     */
    public @Nonnull ThreadSet getThreads() {
        return threads;
    }

    /*package*/ @Nonnull ThreadSet getEmptyThreadSet() {
        return emptySet;
    }

    public <T> T query(SingleRuntimeQuery<T> query) {
        return threads.query(query);
    }
}
