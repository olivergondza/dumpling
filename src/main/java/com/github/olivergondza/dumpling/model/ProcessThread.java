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

public class ProcessThread {

    private final ProcessRuntime runtime;
    private final Builder state;

    private ProcessThread(ProcessRuntime runtime, Builder builder) {
        this.runtime = runtime;
        this.state = builder.clone();
    }

    public static Builder builder() {
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

    /**
     * Get threads that are waiting for lock held by this thread.
     */
    public ThreadSet getBlockedThreads() {
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
    public ThreadSet getBlockingThreads() {
        return new ThreadSet(runtime, Collections.singleton(getBlockingThread()));
    }

    public ProcessThread getBlockingThread() {
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
        private StackTraceElement[] stackTrace;
        private Thread.State state;
        private ThreadStatus status;
        private ThreadLock waitingOnLock;
        private Set<ThreadLock> acquiredLocks;

        public ProcessThread build(ProcessRuntime runtime) {
            return new ProcessThread(runtime, this);
        }

        @Override
        public Builder clone() {
            try {
                return (Builder) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setId(long tid) {
            this.tid = tid;
            return this;
        }

        public Builder setDaemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder setStacktrace(StackTraceElement[] stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder setState(Thread.State state) {
            this.state = state;
            return this;
        }

        public Builder setStatus(ThreadStatus status) {
            this.status = status;
            return this;
        }

        public Builder setLock(ThreadLock lock) {
            this.waitingOnLock = lock;
            return this;
        }

        public Builder setAcquiredLocks(Set<ThreadLock> locks) {
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

            sb.append("\n   java.lang.Thread.State: ").append(status.getName());

            for (StackTraceElement traceLine: stackTrace) {
                sb.append("\n\tat ").append(traceLine);
            }
            return sb.toString();
        }
    }
}
