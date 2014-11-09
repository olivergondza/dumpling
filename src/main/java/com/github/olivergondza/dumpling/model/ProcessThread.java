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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ThreadLock.Monitor;

/**
 * Immutable representation of a thread.
 *
 * @author ogondza
 */
public class ProcessThread {

    private static final @Nonnull String NL = System.getProperty("line.separator", "\n");

    private final @Nonnull ProcessRuntime runtime;
    private final @Nonnull Builder state;

    public static @Nonnull Builder builder() {
        return new Builder();
    }

    private ProcessThread(@Nonnull ProcessRuntime runtime, @Nonnull Builder builder) {
        this.runtime = runtime;
        this.state = builder.clone();

        checkSanity();
    }

    private void checkSanity() {
        if (state.name == null || state.name.isEmpty()) throw new IllegalArgumentException("Thread name not set");
        if (state.status == null) throw new IllegalArgumentException("Thread status not set");

        if (state.id == null && state.tid == null && state.nid == null) {
            throw new IllegalArgumentException("No thread identifier set");
        }
    }

    public @Nonnull String getName() {
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

    /**
     * @deprecated Kept for backward compatibility, use {@link #getStatus()} instead.
     */
    @Deprecated
    public @Nonnull ThreadStatus getThreadStatus() {
        return getStatus();
    }

    public @Nonnull ThreadStatus getStatus() {
        return state.status;
    }

    /**
     * {@link Thread.State} of current thread.
     *
     * @return null if was not able to determine thread state.
     */
    public @CheckForNull Thread.State getState() {
        return state.status.getState();
    }

    public Integer getPriority() {
        return state.priority;
    }

    public boolean isDaemon() {
        return state.daemon;
    }

    public @Nonnull StackTrace getStackTrace() {
        return state.stackTrace;
    }

    public @CheckForNull ThreadLock getWaitingOnLock() {
        return state.waitingOnLock;
    }

    public @Nonnull Set<ThreadLock> getAcquiredLocks() {
        // Convert to Set not to expose duplicates
        LinkedHashSet<ThreadLock> locks = new LinkedHashSet<ThreadLock>(
                state.acquiredMonitors.size() + state.acquiredSynchronizers.size()
        );
        for (Monitor m: state.acquiredMonitors) {
            locks.add(m.getLock());
        }
        locks.addAll(state.acquiredSynchronizers);
        return locks;
    }

    /**
     * Get threads that are waiting for lock held by this thread.
     */
    public @Nonnull ThreadSet getBlockedThreads() {
        Set<ProcessThread> blocked = new LinkedHashSet<ProcessThread>();
        for (ProcessThread thread: runtime.getThreads()) {
            if (thread == this) continue;
            if (getAcquiredLocks().contains(thread.state.waitingOnLock)) {
                blocked.add(thread);
            }
        }
        return runtime.getThreadSet(blocked);
    }

    /**
     * Get threads holding lock this thread is trying to acquire.
     *
     * @return {@link ThreadSet} that contains blocked thread or empty set if this thread does not hold any lock.
     */
    public @Nonnull ThreadSet getBlockingThreads() {
        final ProcessThread blocking = getBlockingThread();
        if (blocking == null) return runtime.getEmptyThreadSet();

        return runtime.getThreadSet(Collections.singleton(blocking));
    }

    /**
     * Get thread blocking this threads execution.
     * @return Blocking thread or null if not block by a thread.
     */
    public @CheckForNull ProcessThread getBlockingThread() {
        for (ProcessThread thread: runtime.getThreads()) {
            if (thread == this) continue;
            if (thread.getAcquiredLocks().contains(state.waitingOnLock)) {
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
        // priority might not be present in threaddump
        private Integer priority;
        // https://gist.github.com/rednaxelafx/843622
        private Long id, nid, tid;
        private @Nonnull StackTrace stackTrace = new StackTrace();
        private @Nonnull ThreadStatus status = ThreadStatus.UNKNOWN;
        private @CheckForNull ThreadLock waitingOnLock;
        private @Nonnull List<ThreadLock.Monitor> acquiredMonitors = Collections.emptyList();
        private @Nonnull List<ThreadLock> acquiredSynchronizers = Collections.emptyList();

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
            this.stackTrace = new StackTrace(stackTrace);
            return this;
        }

        public @Nonnull StackTrace getStacktrace() {
            return stackTrace;
        }

        public @Nonnull Builder setThreadStatus(@Nonnull ThreadStatus status) {
            this.status = status;
            return this;
        }

        public ThreadStatus getThreadStatus() {
            return status;
        }

        public @Nonnull Builder setWaitingOnLock(ThreadLock lock) {
            this.waitingOnLock = lock;
            return this;
        }

        public @Nonnull Builder setAcquiredSynchronizers(List<ThreadLock> synchronizers) {
            this.acquiredSynchronizers = Collections.unmodifiableList(synchronizers);
            return this;
        }

        public @Nonnull Builder setAcquiredSynchronizers(ThreadLock... synchronizers) {
            List<ThreadLock> data = new ArrayList<ThreadLock>(synchronizers.length);
            Collections.addAll(data, synchronizers);
            return setAcquiredSynchronizers(data);
        }

        public @Nonnull Builder setAcquiredMonitors(List<ThreadLock.Monitor> monitors) {
            this.acquiredMonitors = Collections.unmodifiableList(monitors);
            return this;
        }

        private String getHeader() {
            return headerBuilder().toString();
        }

        private List<ThreadLock> getMonitorsByDepth(int depth) {
            List<ThreadLock> monitors = new ArrayList<ThreadLock>();

            for (Monitor monitor: acquiredMonitors) {
                if (monitor.getDepth() == depth) {
                    monitors.add(monitor.getLock());
                }
            }

            return monitors;
        }

        @Override
        public String toString() {
            StringBuilder sb = headerBuilder();

            sb.append(NL).append("   java.lang.Thread.State: ").append(status.getName());

            int depth = 0;
            for (StackTraceElement traceLine: stackTrace.getElements()) {
                sb.append(NL).append("\tat ").append(traceLine);

                if (waitingOnLock != null && depth == 0) {
                    String verb = StackTrace.waitingVerb(traceLine);
                    sb.append(NL).append("\t- ").append(verb).append(' ').append(waitingOnLock);
                }

                for (ThreadLock monitor: getMonitorsByDepth(depth)) {
                    sb.append(NL).append("\t- locked ").append(monitor.toString());
                }

                depth++;
            }

            if (!acquiredSynchronizers.isEmpty()) {
                sb.append(NL + NL).append("   Locked ownable synchronizers:").append(NL);
                for (ThreadLock synchronizer: acquiredSynchronizers) {
                    sb.append("\t- ").append(synchronizer.toString()).append(NL);
                }
            }

            return sb.toString();
        }

        private StringBuilder headerBuilder() {
            StringBuilder sb = new StringBuilder();
            sb.append('"').append(name).append('"');
            if (id != null) sb.append(" #").append(id);
            if (daemon) sb.append(" daemon");
            if (priority != null) sb.append(" prio=").append(priority);
            if (tid != null) sb.append(" tid=").append(tid);
            if (nid != null) sb.append(" nid=").append(nid);
            return sb;
        }
    }

    /**
     * {@link ProcessThread} predicate.
     *
     * @author ogondza
     * @see ThreadSet#where(ProcessThread.Predicate)
     */
    public static interface Predicate {
        boolean isValid(@Nonnull ProcessThread thread);
    }

    /**
     * Match thread by name.
     */
    public static Predicate nameIs(final String name) {
        return new Predicate() {
            @Override
            public boolean isValid(ProcessThread thread) {
                return thread.getName().equals(name);
            }
        };
    }

    /**
     * Match thread its name contains pattern.
     */
    public static Predicate nameContains(final Pattern pattern) {
        return new Predicate() {
            @Override
            public boolean isValid(ProcessThread thread) {
                return pattern.matcher(thread.getName()).find();
            }
        };
    }

    /**
     * Match thread that is waiting on lock identified by <tt>className</tt>.
     */
    public static Predicate waitingOnLock(final String className) {
        return new Predicate() {
            @Override
            public boolean isValid(ProcessThread thread) {
                final ThreadLock lock = thread.getWaitingOnLock();
                return lock != null && lock.getClassName().equals(className);
            }
        };
    }

    /**
     * Match thread that has acquired lock identified by <tt>className</tt>.
     */
    public static Predicate acquiredLock(final String className) {
        return new Predicate() {
            @Override
            public boolean isValid(@Nonnull ProcessThread thread) {
                for (ThreadLock lock: thread.getAcquiredLocks()) {
                    if (lock.getClassName().equals(className)) return true;
                }
                return false;
            }
        };
    }
}
