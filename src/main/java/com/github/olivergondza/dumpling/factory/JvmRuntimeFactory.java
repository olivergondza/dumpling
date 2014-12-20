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
package com.github.olivergondza.dumpling.factory;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ProcessThread.Builder;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;

/**
 * Create {@link ProcessRuntime} from state of current JVM process.
 *
 * @author ogondza
 */
public class JvmRuntimeFactory {

    private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public @Nonnull ProcessRuntime currentRuntime() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        Map<Long, ThreadInfo> infos = infos();

        HashSet<ProcessThread.Builder> state = new HashSet<ProcessThread.Builder>(threads.size());

        for (Thread thread: threads) {
            ThreadInfo info = infos.get(thread.getId());
            // The thread was terminated between Thread.getAllStackTraces() and ThreadMXBean.getThreadInfo()
            if (info == null) continue;

            Builder builder = ProcessThread.builder()
                    .setName(info.getThreadName())
                    .setId(info.getThreadId())
                    .setDaemon(thread.isDaemon())
                    .setPriority(thread.getPriority())
                    .setStacktrace(info.getStackTrace())
            ;

            final ThreadStatus status = ThreadStatus.fromState(
                    info.getThreadState(), builder.getStacktrace().head()
            );

            builder.setThreadStatus(status);

            builder.setAcquiredMonitors(monitors(info));
            builder.setAcquiredSynchronizers(locks(info));
            LockInfo lockInfo = info.getLockInfo();
            if (lockInfo != null) {
                ThreadLock lock = lock(lockInfo);
                if (status.isBlocked() || status.isParked()) {
                    builder.setWaitingToLock(lock);
                } else if (status.isWaiting()) {
                    builder.setWaitingOnLock(lock);
                } else {
                    throw new AssertionError(
                            String.format("Thread declares lock while %s: %n%s%n", status, info)
                    );
                }
            }

            state.add(builder);
        }

        return new ProcessRuntime(state);
    }

    private Map<Long, ThreadInfo> infos() {
        Map<Long, ThreadInfo> infos = new HashMap<Long, ThreadInfo>();
        for (ThreadInfo info: threadMXBean.dumpAllThreads(true, true)) {
            infos.put(info.getThreadId(), info);
        }

        return infos;
    }

    private List<ThreadLock.Monitor> monitors(final ThreadInfo threadInfo) {
        final MonitorInfo[] monitors = threadInfo.getLockedMonitors();

        final List<ThreadLock.Monitor> locks = new ArrayList<ThreadLock.Monitor>(monitors.length);

        for (MonitorInfo info: monitors) {
            locks.add(monitor(info));
        }

        return locks;
    }

    private List<ThreadLock> locks(final ThreadInfo threadInfo) {
        final LockInfo[] synchronizers = threadInfo.getLockedSynchronizers();

        final List<ThreadLock> locks = new ArrayList<ThreadLock>(synchronizers.length);

        for (LockInfo info: synchronizers) {
            locks.add(lock(info));
        }

        return locks;
    }

    private ThreadLock.Monitor monitor(final MonitorInfo info) {
        return new ThreadLock.Monitor(lock(info), info.getLockedStackDepth());
    }

    private @Nonnull ThreadLock lock(final LockInfo info) {
        return new ThreadLock(info.getClassName(), info.getIdentityHashCode());
    }

    // Not used for now
    private final static class UnsupportedJreException extends RuntimeException {
        public UnsupportedJreException(Throwable cause) {
            super(
                    "Dumpling was unable to extract necessary information from running JVM. Report this as Dumpling feature request with JRE vendor and version attached.",
                    cause
            );
        }
    }
}
