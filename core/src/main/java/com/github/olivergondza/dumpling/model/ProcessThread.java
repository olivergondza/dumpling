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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.factory.IllegalRuntimeStateException;
import com.github.olivergondza.dumpling.model.ThreadLock.Monitor;

/**
 * Immutable representation of a thread.
 *
 * @author ogondza
 */
public class ProcessThread<
        ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>,
        SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
        RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>
> extends ModelObject {

    private final @Nonnull RuntimeType runtime;
    private final @Nonnull Builder<?> state;

    protected ProcessThread(@Nonnull RuntimeType runtime, @Nonnull Builder<?> builder) {
        this.runtime = runtime;
        this.state = builder.clone();

        checkSanity();
    }

    private void checkSanity() {
        if (state.name == null || state.name.isEmpty()) throw new IllegalRuntimeStateException("Thread name not set");
        if (state.status == null) throw new IllegalRuntimeStateException("Thread status not set");

        if (state.id == null && state.tid == null && state.nid == null) {
            throw new IllegalRuntimeStateException("No thread identifier set");
        }

        if (state.status.isBlocked() && state.waitingToLock == null) {
            throw new IllegalRuntimeStateException(
                    "Blocked thread does not declare monitor: >>>\n%s\n<<<\n", state
            );
        }
    }

    public @Nonnull RuntimeType getRuntime() {
        return runtime;
    }

    public @Nonnull String getName() {
        return state.name;
    }

    /**
     * Java thread id.
     *
     * @return <tt>null</tt> when not available in threaddump.
     */
    public @CheckForNull Long getId() {
        return state.id;
    }

    /**
     * Native thread id.
     *
     * @return <tt>null</tt> when not available in threaddump.
     */
    public @CheckForNull Long getNid() {
        return state.nid;
    }

    /**
     * @return <tt>null</tt> when not available in threaddump.
     */
    public @CheckForNull Long getTid() {
        return state.tid;
    }

    public @Nonnull ThreadStatus getStatus() {
        return state.status;
    }

    /**
     * {@link java.lang.Thread.State} of current thread.
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

    /**
     * Monitor thread is waiting to be notified.
     *
     * @return null is the thread is not in {@link Object#wait()}.
     */
    public @CheckForNull ThreadLock getWaitingOnLock() {
        return state.waitingOnLock;
    }

    /**
     * Monitor thread is waiting to acquire.
     *
     * @return null then the thread is not <tt>BLOCKED</tt> acquiring the monitor.
     */
    public @CheckForNull ThreadLock getWaitingToLock() {
        return state.waitingToLock;
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

    public @Nonnull Set<ThreadLock> getAcquiredMonitors() {
        LinkedHashSet<ThreadLock> locks = new LinkedHashSet<ThreadLock>(state.acquiredMonitors.size());
        for (Monitor m: state.acquiredMonitors) {
            locks.add(m.getLock());
        }
        return locks;
    }

    public @Nonnull Set<ThreadLock> getAcquiredSynchronizers() {
        return new LinkedHashSet<ThreadLock>(state.acquiredSynchronizers);
    }

    /**
     * Get threads that are waiting for lock held by this thread.
     */
    public @Nonnull SetType getBlockedThreads() {
        Set<ThreadType> blocked = new LinkedHashSet<ThreadType>();
        for (ThreadType thread: runtime.getThreads()) {
            if (thread == this) continue;
            if (getAcquiredLocks().contains(thread.getWaitingToLock())) {
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
    public @Nonnull SetType getBlockingThreads() {
        final ThreadType blocking = getBlockingThread();
        if (blocking == null) return runtime.getEmptyThreadSet();

        return runtime.getThreadSet(Collections.singleton(blocking));
    }

    /**
     * Get thread blocking this threads execution.
     * @return Blocking thread or null if not block by a thread.
     */
    public @CheckForNull ThreadType getBlockingThread() {
        if (state.waitingToLock == null && state.waitingOnLock == null) {
            return null;
        }

        for (ThreadType thread: runtime.getThreads()) {
            if (thread == this) continue;
            Set<ThreadLock> acquired = thread.getAcquiredLocks();
            if (acquired.isEmpty()) continue;
            if (acquired.contains(state.waitingToLock) || acquired.contains(state.waitingOnLock)) {
                return thread;
            }
        }

        return null;
    }

    public String getHeader() {
        return state.getHeader();
    }

    @Override
    public void toString(PrintStream stream, Mode mode) {
        state.toString(stream, mode);
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs == null) return false;
        if (!rhs.getClass().equals(this.getClass())) return false;

        ProcessThread<?, ?, ?> other = (ProcessThread<?, ?, ?>) rhs;

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

    public static class Builder<
            BuilderType extends Builder<BuilderType>
    > extends ModelObject implements Cloneable {

        private @Nonnull String name = "";
        private boolean daemon;
        // priority might not be present in threaddump
        private Integer priority;
        // https://gist.github.com/rednaxelafx/843622
        private Long id, nid, tid;
        private @Nonnull StackTrace stackTrace = new StackTrace();
        private @Nonnull ThreadStatus status = ThreadStatus.UNKNOWN;
        private @CheckForNull ThreadLock waitingToLock;
        private @CheckForNull ThreadLock waitingOnLock;
        private @Nonnull List<ThreadLock.Monitor> acquiredMonitors = Collections.emptyList();
        private @Nonnull List<ThreadLock> acquiredSynchronizers = Collections.emptyList();

        @Override
        protected @Nonnull BuilderType clone() {
            try {
                return (BuilderType) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }

        public @Nonnull BuilderType setName(@Nonnull String name) {
            this.name = name;
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setId(long id) {
            this.id = id;
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setNid(long nid) {
            this.nid = nid;
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setTid(long tid) {
            this.tid = tid;
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setDaemon(boolean daemon) {
            this.daemon = daemon;
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setPriority(Integer priority) {
            this.priority = priority;
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setStacktrace(@Nonnull StackTraceElement... stackTrace) {
            this.stackTrace = new StackTrace(stackTrace);
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setStacktrace(@Nonnull StackTrace stackTrace) {
            this.stackTrace = stackTrace;
            return (BuilderType) this;
        }

        public @Nonnull StackTrace getStacktrace() {
            return stackTrace;
        }

        public @Nonnull BuilderType setThreadStatus(@Nonnull ThreadStatus status) {
            this.status = status;
            return (BuilderType) this;
        }

        public ThreadStatus getThreadStatus() {
            return status;
        }

        public @Nonnull BuilderType setWaitingOnLock(ThreadLock lock) {
            this.waitingOnLock = lock;
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setWaitingToLock(ThreadLock lock) {
            this.waitingToLock = lock;
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setAcquiredSynchronizers(List<ThreadLock> synchronizers) {
            this.acquiredSynchronizers = Collections.unmodifiableList(synchronizers);
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setAcquiredSynchronizers(ThreadLock... synchronizers) {
            List<ThreadLock> data = new ArrayList<ThreadLock>(synchronizers.length);
            Collections.addAll(data, synchronizers);
            return setAcquiredSynchronizers(data);
        }

        public @Nonnull BuilderType setAcquiredMonitors(List<ThreadLock.Monitor> monitors) {
            this.acquiredMonitors = Collections.unmodifiableList(monitors);
            return (BuilderType) this;
        }

        public @Nonnull BuilderType setAcquiredMonitors(ThreadLock.Monitor... monitors) {
            ArrayList<Monitor> data = new ArrayList<ThreadLock.Monitor>(monitors.length);
            Collections.addAll(data, monitors);
            return setAcquiredMonitors(data);
        }

        public @Nonnull List<Monitor> getAcquiredMonitors() {
            return new ArrayList<Monitor>(acquiredMonitors);
        }

        private String getHeader() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            printHeader(new PrintStream(baos), Mode.HUMAN);
            return baos.toString();
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
        public void toString(PrintStream stream, Mode mode) {
            printHeader(stream, mode);
            stream.format("%n   java.lang.Thread.State: %s", status.getName());

            int depth = 0;
            for (StackTraceElement traceLine: stackTrace.getElements()) {
                stream.format("%n\tat %s", traceLine);

                if (depth == 0) {
                    if (waitingToLock != null) {
                        stream.println();
                        stream.append("\t- ").append(waitingVerb()).append(' ');
                        waitingToLock.toString(stream, mode);
                    }
                    if (waitingOnLock != null) {
                        stream.println();
                        stream.append("\t- ").append(waitingVerb()).append(' ');
                        waitingOnLock.toString(stream, mode);
                    }
                }

                for (ThreadLock monitor: getMonitorsByDepth(depth)) {
                    stream.println();
                    stream.append("\t- locked ");
                    monitor.toString(stream, mode);
                }

                depth++;
            }

            if (!acquiredSynchronizers.isEmpty()) {
                stream.format("%n%n   Locked ownable synchronizers:%n");
                for (ThreadLock synchronizer: acquiredSynchronizers) {
                    stream.append("\t- ");
                    synchronizer.toString(stream, mode);
                    stream.println();
                }
            }
        }

        private final String waitingVerb() {
            if (status.isParked()) return "parking to wait for";
            if (status.isWaiting()) return "waiting on";
            if (status.isBlocked()) return "waiting to lock";

            throw new AssertionError(status + " thread can not declare a lock: " + name);
        }

        private void printHeader(PrintStream stream, Mode mode) {
            stream.append('"').append(name).append('"');
            if (id != null) stream.append(" #").append(id.toString());
            if (daemon) stream.append(" daemon");
            if (priority != null) stream.append(" prio=").append(priority.toString());

            if (tid != null) {
                String format = !mode.isHuman() ? "0x%016x": "0x%x";
                stream.append(" tid=").format(format, tid);
            }
            if (nid != null) {
                String format = mode.isHuman() ? "%d" : "0x%x";
                stream.append(" nid=").format(format, nid);
            }
        }
    }

    /**
     * {@link ProcessThread} predicate.
     *
     * @author ogondza
     * @see ThreadSet#where(ProcessThread.Predicate)
     */
    public interface Predicate {
        boolean isValid(@Nonnull ProcessThread<?, ?, ?> thread);
    }

    /**
     * Match thread by name.
     */
    public static @Nonnull Predicate nameIs(final @Nonnull String name) {
        return new Predicate() {
            @Override
            public boolean isValid(ProcessThread<?, ?, ?> thread) {
                return thread.getName().equals(name);
            }
        };
    }

    /**
     * Match thread its name contains pattern.
     */
    public static @Nonnull Predicate nameContains(final @Nonnull Pattern pattern) {
        return new Predicate() {
            @Override
            public boolean isValid(ProcessThread<?, ?, ?> thread) {
                return pattern.matcher(thread.getName()).find();
            }
        };
    }

    /**
     * Match thread its name contains string.
     */
    public static @Nonnull Predicate nameContains(final @Nonnull String pattern) {
        return new Predicate() {
            @Override
            public boolean isValid(ProcessThread<?, ?, ?> thread) {
                return thread.getName().contains(pattern);
            }
        };
    }

    /**
     * Match waiting thread waiting for given thread to be notified.
     */
    public static @Nonnull Predicate waitingOnLock(final @Nonnull String className) {
        return new Predicate() {
            @Override
            public boolean isValid(ProcessThread<?, ?, ?> thread) {
                final ThreadLock lock = thread.getWaitingOnLock();
                return lock != null && lock.getClassName().equals(className);
            }
        };
    }

    /**
     * Match thread that is waiting on lock identified by <tt>className</tt>.
     */
    public static @Nonnull Predicate waitingToLock(final @Nonnull String className) {
        return new Predicate() {
            @Override
            public boolean isValid(ProcessThread<?, ?, ?> thread) {
                final ThreadLock lock = thread.getWaitingToLock();
                return lock != null && lock.getClassName().equals(className);
            }
        };
    }

    /**
     * Match thread that has acquired lock identified by <tt>className</tt>.
     */
    public static @Nonnull Predicate acquiredLock(final @Nonnull String className) {
        return new Predicate() {
            @Override
            public boolean isValid(@Nonnull ProcessThread<?, ?, ?> thread) {
                for (ThreadLock lock: thread.getAcquiredLocks()) {
                    if (lock.getClassName().equals(className)) return true;
                }
                return false;
            }
        };
    }
}
