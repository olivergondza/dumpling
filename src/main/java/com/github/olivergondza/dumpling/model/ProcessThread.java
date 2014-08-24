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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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

    /**
     * Java thread id.
     *
     * @return Null if not provided.
     */
    public Long getId() {
        return state.id;
    }

    /**
     * Native thread id.
     *
     * @return Null if not provided.
     */
    public Long getNid() {
        return state.nid;
    }

    public Long getTid() {
        return state.tid;
    }

    public ThreadStatus getThreadStatus() {
        return state.status;
    }

    public Thread.State getState() {
        return state.state;
    }

    public Integer getPriority() {
        return state.priority;
    }

    public boolean isDaemon() {
        return state.daemon;
    }

    public @Nonnull StackTrace getStackTrace() {
        return new StackTrace(state.stackTrace);
    }

    public @CheckForNull ThreadLock getWaitingOnLock() {
        return state.waitingOnLock;
    }

    public @Nonnull Set<ThreadLock> getAcquiredLocks() {
        // Convert to Set not to expose duplicates
        return new HashSet<ThreadLock>(state.acquiredLocks);
    }

    /**
     * Get threads that are waiting for lock held by this thread.
     */
    public @Nonnull ThreadSet getBlockedThreads() {
        Set<ProcessThread> blocked = new LinkedHashSet<ProcessThread>();
        for (ProcessThread thread: runtime.getThreads()) {
            if (thread == this) continue;
            if (state.acquiredLocks.contains(thread.state.waitingOnLock)) {
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
            if (thread == this) continue;
            if (thread.state.acquiredLocks.contains(state.waitingOnLock)) {
                return thread;
            }
        }

        return null;
    }

    public String getHeader() {
        return state.getHeader();
    }

    @Override
    public String toString() {
        return state.toString();
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs == null) return false;
        if (!rhs.getClass().equals(this.getClass())) return false;

        ProcessThread other = (ProcessThread) rhs;

        if (state.tid == null ? other.state.tid != null : !state.tid.equals(other.state.tid)) return false;
        if (state.nid == null ? other.state.nid != null : !state.nid.equals(other.state.nid)) return false;
        if (state.id == null ? other.state.id != null : !state.id.equals(other.state.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        Long tid = state.tid;
        if (tid == null) tid = 0L;

        Long nid = state.nid;
        if (nid == null) nid = 0L;

        Long id = state.id;
        if (id == null) id = 0L;

        return new Long(7 + 31 * tid + 17 * nid + 11 * id).hashCode();
    }

    public static class Builder implements Cloneable {

        private String name;
        private boolean daemon;
        private Integer priority;
        // https://gist.github.com/rednaxelafx/843622
        private Long id, nid, tid;
        private @Nonnull StackTraceElement[] stackTrace = new StackTraceElement[] {};
        private Thread.State state;
        private ThreadStatus status;
        private @CheckForNull ThreadLock waitingOnLock;
        // Preserve locks as List not to collapse identical entries
        private @Nonnull List<ThreadLock> acquiredLocks = Collections.emptyList();

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

        public @Nonnull Builder setId(long id) {
            this.id = id;
            return this;
        }

        public @Nonnull Builder setNid(long nid) {
            this.nid = nid;
            return this;
        }

        public @Nonnull Builder setTid(long tid) {
            this.tid = tid;
            return this;
        }

        public @Nonnull Builder setDaemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public @Nonnull Builder setPriority(Integer priority) {
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

        public ThreadStatus getStatus() {
            return status;
        }

        public @Nonnull Builder setLock(ThreadLock lock) {
            this.waitingOnLock = lock;
            return this;
        }

        public @Nonnull Builder setAcquiredLocks(List<ThreadLock> locks) {
            this.acquiredLocks = Collections.unmodifiableList(locks);
            return this;
        }

        public @Nonnull Builder setAcquiredLocks(ThreadLock... locks) {
            List<ThreadLock> data = new ArrayList<ThreadLock>(locks.length);
            Collections.addAll(data, locks);
            return setAcquiredLocks(data);
        }

        public String getHeader() {
            return headerBuilder().toString();
        }

        @Override
        public String toString() {
            StringBuilder sb = headerBuilder();

            if (status != null) {
                sb.append("\n   java.lang.Thread.State: ").append(status.getName());
            }

            int depth = 0;
            for (StackTraceElement traceLine: stackTrace) {
                sb.append("\n\tat ").append(traceLine);

                if (waitingOnLock != null && waitingOnLock.getStackDepth() == depth) {
                    assert depth == 0: "Waiting on lock should always relate to the innermost stack frame";

                    String verb = StackTrace.waitingVerb(traceLine);
                    sb.append("\n\t- ").append(verb).append(' ').append(waitingOnLock);
                }

                for (ThreadLock lock: acquiredLocks) {
                    if (lock.getStackDepth() == depth) {
                        sb.append("\n\t- locked " + lock);
                    }
                }

                depth++;
            }

            return sb.toString();
        }

        private StringBuilder headerBuilder() {
            StringBuilder sb = new StringBuilder();
            sb.append('"').append(name).append('"');
            if (daemon) sb.append(" daemon");
            sb.append(" prio=").append(priority);
            sb.append(" id=").append(id);
            sb.append(" tid=").append(tid);
            sb.append(" nid=").append(nid);
            return sb;
        }
    }
}
