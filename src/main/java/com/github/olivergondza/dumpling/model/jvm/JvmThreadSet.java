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
package com.github.olivergondza.dumpling.model.jvm;

import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.mxbean.MXBeanThreadSet;

/**
 * ThreadSet with convenient methods to take advantage from {@link Thread} availability.
 *
 * @author ogondza
 */
public final class JvmThreadSet extends MXBeanThreadSet<JvmThreadSet, JvmRuntime, JvmThread> {

    protected JvmThreadSet(@Nonnull JvmRuntime runtime, @Nonnull Set<JvmThread> threads) {
        super(runtime, threads);
    }

    /**
     * Get {@link ProcessThread} for given {@link Thread}.
     */
    public @CheckForNull JvmThread forThread(@CheckForNull Thread needle) {
        if (needle == null) return null;

        for (JvmThread candidate: this) {
            final JvmThread jvmThread = candidate;
            final Thread thread = jvmThread.getThread();
            if (needle.equals(thread)) return jvmThread;
        }
        return null;
    }

    /**
     * Get {@link ProcessThread} for given {@link Thread}.
     */
    public @CheckForNull JvmThread forCurrentThread() {
        return forThread(Thread.currentThread());
    }
}
