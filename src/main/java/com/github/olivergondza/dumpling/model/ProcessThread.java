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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class ProcessThread {

    private @Nonnull final ProcessRuntime runtime;
    private @Nonnull final Builder state;

    private ProcessThread(@Nonnull ProcessRuntime runtime, @Nonnull Builder builder) {
        this.runtime = runtime;
        this.state = builder.clone();
    }

    public static @Nonnull Builder builder() {
        return new Builder();
    }

    public String getName() {
        return state.name;
    }

    public long getId() {
        return state.tid;
    }

    public ThreadStatus getThreadStatus() {
        return state.status;
    }

    public Thread.State getState() {
        return state.state;
    }

    public @Nonnull StackTraceElement[] getStackTrace() {
        return state.stackTrace.clone();
    }

    /**
     * Get threads that are waiting for lock held by this thread.
     */
    public @Nonnull ThreadSet getBlockedThreads() {
        HashSet<ProcessThread> blocked = new HashSet<ProcessThread>();
        for (ProcessThread thread: runtime.getThreads()) {
            if (thread.state.acquiredLocks.contains(state.waitingOnLock)) {
                blocked.add(thread);
            }
        }
        return new ThreadSet(runtime, blocked);
    }

    /**
     * Get threads holding lock this thread is trying to acquire.
     *
     * @return {@link ThreadSet} that contains blocked thread or empty set if this thread does not hold any lock.
     */
    public @Nonnull ThreadSet getBlockingThreads() {
        final ProcessThread blocking = getBlockingThread();
        if (blocking == null) return runtime.getEmptyThreadSet();

        return new ThreadSet(runtime, Collections.singleton(blocking));
    }

    /**
     * Get thread blocking this threads execution.
     * @return Blocking thread or null if not block by a thread.
     */
    public @CheckForNull ProcessThread getBlockingThread() {
        for (ProcessThread thread: runtime.getThreads()) {
            if (state.acquiredLocks.contains(thread.state.waitingOnLock)) {
                return thread;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return state.toString();
    }

    @Override
    public boolean equals(Object lhs) {
        if (lhs == null) return false;
        if (!lhs.getClass().equals(this.getClass())) return false;

        ProcessThread other = (ProcessThread) lhs;
        return state.tid == other.state.tid;
    }

    @Override
    public int hashCode() {
        return new Long(state.tid * 31).hashCode();
    }

    public static class Builder implements Cloneable {

        private String name;
        private boolean daemon;
        private int priority;
        private long tid;
        private @Nonnull StackTraceElement[] stackTrace = new StackTraceElement[] {};
        private Thread.State state;
        private ThreadStatus status;
        private @CheckForNull ThreadLock waitingOnLock;
        private @Nonnull Set<ThreadLock> acquiredLocks = Collections.emptySet();

        public ProcessThread build(@Nonnull ProcessRuntime runtime) {
            return new ProcessThread(runtime, this);
        }

        @Override
        public @Nonnull Builder clone() {
            try {
                return (Builder) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }

        public @Nonnull Builder setName(String name) {
            this.name = name;
            return this;
        }

        public @Nonnull Builder setId(long tid) {
            this.tid = tid;
            return this;
        }

        public @Nonnull Builder setDaemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public @Nonnull Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public @Nonnull Builder setStacktrace(@Nonnull StackTraceElement[] stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public @Nonnull Builder setState(Thread.State state) {
            this.state = state;
            return this;
        }

        public @Nonnull Builder setStatus(ThreadStatus status) {
            this.status = status;
            return this;
        }

        public @Nonnull Builder setLock(ThreadLock lock) {
            this.waitingOnLock = lock;
            return this;
        }

        public @Nonnull Builder setAcquiredLocks(Set<ThreadLock> locks) {
            this.acquiredLocks = Collections.unmodifiableSet(locks);
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('"').append(name).append('"');
            if (daemon) sb.append(" daemon");
            sb.append(" prio=").append(priority);
            sb.append(" tid=").append(tid);

            if (status != null) {
                sb.append("\n   java.lang.Thread.State: ").append(status.getName());
            }

            for (StackTraceElement traceLine: stackTrace) {
                sb.append("\n\tat ").append(traceLine);
            }

            if (waitingOnLock != null) {
                sb.append("\n\t - waiting to lock " + waitingOnLock);
            }

            for (ThreadLock lock: acquiredLocks) {
                sb.append("\n\t - locking " + lock);
            }

            return sb.toString();
        }
    }
}
