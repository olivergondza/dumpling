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
package com.github.olivergondza.dumpling.factory;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ThreadLock;

/**
 * Useful functions to convert MXBean structures to Dumpling ones.
 *
 * @author ogondza
 */
/*package*/ class MXBeanFactoryUtils {

    /*package*/ static @Nonnull List<ThreadLock.Monitor> getMonitors(final ThreadInfo threadInfo) {
        final MonitorInfo[] monitors = threadInfo.getLockedMonitors();

        final List<ThreadLock.Monitor> locks = new ArrayList<ThreadLock.Monitor>(monitors.length);

        for (MonitorInfo info: monitors) {
            locks.add(getMonitor(info));
        }

        return locks;
    }

    /*package*/ static @Nonnull List<ThreadLock> getSynchronizers(final ThreadInfo threadInfo) {
        final LockInfo[] synchronizers = threadInfo.getLockedSynchronizers();

        final List<ThreadLock> locks = new ArrayList<ThreadLock>(synchronizers.length);

        for (LockInfo info: synchronizers) {
            locks.add(getSynchronizer(info));
        }

        return locks;
    }

    /*package*/ static @Nonnull ThreadLock.Monitor getMonitor(final MonitorInfo info) {
        return new ThreadLock.Monitor(getSynchronizer(info), info.getLockedStackDepth());
    }

    /*package*/ static @Nonnull ThreadLock getSynchronizer(final LockInfo info) {
        return new ThreadLock(info.getClassName(), info.getIdentityHashCode());
    }
}
