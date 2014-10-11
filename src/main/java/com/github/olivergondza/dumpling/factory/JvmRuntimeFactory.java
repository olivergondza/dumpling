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
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadSet;
import com.github.olivergondza.dumpling.model.ThreadStatus;

/**
 * Create {@link ProcessRuntime} from state of current process.
 *
 * @author ogondza
 */
public class JvmRuntimeFactory {

    public @Nonnull ProcessRuntime currentRuntime() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        Map<Long, ThreadInfo> infos = infos();

        HashSet<JvmThread.Builder> state = new HashSet<JvmThread.Builder>(threads.size());

        for (@Nonnull Thread thread: threads) {
            JvmThread.Builder builder = (JvmThread.Builder) new JvmThread.Builder(thread)
                    .setName(thread.getName())
                    .setId(thread.getId())
                    .setDaemon(thread.isDaemon())
                    .setPriority(thread.getPriority())
                    .setStacktrace(thread.getStackTrace())
                    .setThreadStatus(status(thread))
            ;

            ThreadInfo info = infos.get(thread.getId());
            // The thread was terminated between Thread.getAllStackTraces() and ThreadMXBean.getThreadInfo()
            if (info == null) continue;

            builder.setAcquiredMonitors(monitors(info));
            builder.setAcquiredSynchronizers(locks(info));
            LockInfo lock = info.getLockInfo();
            if (lock != null) builder.setWaitingOnLock(lock(lock));

            state.add(builder);
        }

        return new JvmRuntime(state);
    }

    private Map<Long, ThreadInfo> infos() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<Long, ThreadInfo> infos= new HashMap<Long, ThreadInfo>();
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

    private @Nonnull ThreadStatus status(Thread thread) {

        try {
            int code = threadStatus.getInt(thread);
            return ThreadStatus.valueOf(code);
        } catch (IllegalArgumentException ex) {

            throw new UnsupportedJreException(ex);
        } catch (IllegalAccessException ex) {

            throw new UnsupportedJreException(ex);
        } catch (NullPointerException ex) {

            throw new UnsupportedJreException(ex);
        }
    }

    private static Field threadStatus;
    static {
        try {
            threadStatus = Thread.class.getDeclaredField("threadStatus");
            threadStatus.setAccessible(true);
        } catch (NoSuchFieldException ex) {
           // Ignore in initialization. NullPointerException will be thrown when accessing.
        } catch (SecurityException ex) {
           // Ignore in initialization. IllegalAccessException will be thrown when accessing.
        }
    }

    private final static class UnsupportedJreException extends RuntimeException {
        public UnsupportedJreException(Throwable cause) {
            super(
                    "Dumpling was unable to extract necessary information from running JVM. Report this as Dumpling feature request with JRE vendor and version attached.",
                    cause
            );
        }
    }

    /**
     * Subclass handing JVM-aware {@link ThreadSet} implementation.
     *
     * @author ogondza
     */
    private final static class JvmRuntime extends ProcessRuntime {

        private @Nonnull JvmThreadSet emptySet;

        public JvmRuntime(@Nonnull Set<JvmThread.Builder> builders) {
            super(builders);
            this.emptySet = new JvmThreadSet(this, Collections.<ProcessThread>emptySet());
        }

        @Override
        public ThreadSet getEmptyThreadSet() {
            return emptySet;
        }

        @Override
        public ThreadSet getThreads() {
            return new JvmThreadSet(this, super.getThreads().toSet());
        }
    }

    /**
     * ThreadSet with convenient methods to take advantage from {@link Thread} availability.
     *
     * @author ogondza
     */
    private final static class JvmThreadSet extends ThreadSet {

        public JvmThreadSet(@Nonnull JvmRuntime runtime, @Nonnull Set<ProcessThread> threads) {
            super(runtime, threads);
        }

        /**
         * Get {@link ProcessThread} for given {@link Thread}.
         */
        public @CheckForNull JvmThread forThread(@Nonnull Thread needle) {
            for (ProcessThread candidate: this) {
                final JvmThread jvmThread = (JvmThread) candidate;
                final Thread thread = jvmThread.state.thread.get();
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

    /**
     * {@link ProcessThread} with {@link Thread} reference.
     *
     * @author ogondza
     */
    private final static class JvmThread extends ProcessThread {

        private final @Nonnull Builder state;

        public JvmThread(@Nonnull ProcessRuntime runtime, @Nonnull Builder builder) {
            super(runtime, builder);
            this.state = builder;
        }

        /**
         * Get {@link Thread} represented by this {@link ProcessThread}.
         *
         * @return <tt>null</tt> in case the thread does not longer exist.
         */
        public @CheckForNull Thread getThread() {
            return state.thread.get();
        }

        private final static class Builder extends ProcessThread.Builder {

            // Using weak reference not to keep the thread in memory once terminated
            private final @Nonnull WeakReference<Thread> thread;

            public Builder(@Nonnull Thread thread) {
                this.thread = new WeakReference<Thread>(thread);
            }

            @Override
            public ProcessThread build(@Nonnull ProcessRuntime runtime) {
                return new JvmThread(runtime, this);
            }
        }
    }
}
