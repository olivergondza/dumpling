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

import java.lang.Thread.State;
import java.util.concurrent.locks.LockSupport;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Thread status.
 *
 * More detailed version of {@link java.lang.Thread.State}.
 *
 * @author ogondza
 * @see <a href="http://hg.openjdk.java.net/jdk7/jdk7/hotspot/file/167b70ff3abc/src/share/vm/classfile/javaClasses.cpp#l922">javaClasses.cpp</a>
 */
public enum ThreadStatus {
    NEW                 ("NEW",                                 0,      State.NEW),
    RUNNABLE            ("RUNNABLE",                            5,      State.RUNNABLE),
    SLEEPING            ("TIMED_WAITING (sleeping)",            225,    State.TIMED_WAITING),

    /**
     * Thread in {@link Object#wait()}.
     *
     * @see java.lang.Thread.State#WAITING
     */
    IN_OBJECT_WAIT      ("WAITING (on object monitor)",         401,    State.WAITING),

    /**
     * Thread in {@link Object#wait(long)}.
     *
     * @see java.lang.Thread.State#TIMED_WAITING
     */
    IN_OBJECT_WAIT_TIMED("TIMED_WAITING (on object monitor)",   417,    State.TIMED_WAITING),

    /**
     * Thread in {@link LockSupport#park()}.
     *
     * @see java.lang.Thread.State#WAITING
     */
    PARKED              ("WAITING (parking)",                   657,    State.WAITING),

    /**
     * Thread in {@link LockSupport#parkNanos}.
     *
     * @see java.lang.Thread.State#TIMED_WAITING
     */
    PARKED_TIMED        ("TIMED_WAITING (parking)",             673,    State.TIMED_WAITING),

    BLOCKED             ("BLOCKED (on object monitor)",         1025,   State.BLOCKED),
    TERMINATED          ("TERMINATED",                          2,      State.TERMINATED),

    /**
     * Runtime factory was not able to determine thread state.
     *
     * This can happen for service threads in threaddump.
     */
    UNKNOWN             ("UNKNOWN",                             -1,     null);

    /**
     * Description used in thread dump.
     */
    private final @Nonnull String name;
    @Deprecated private final int code;

    /**
     * Matching java.lang.Thread.State.
     */
    private final State state;

    private ThreadStatus(@Nonnull String name, int code, State state) {
        this.name = name;
        this.code = code;
        this.state = state;
    }

    public @Nonnull String getName() {
        return name;
    }

    public int getCode() {
        return code;
    }

    public @CheckForNull State getState() {
        return state;
    }

    /**
     * Newly create thread
     *
     * @see java.lang.Thread.State#NEW
     */
    public boolean isNew() {
        return this == NEW;
    }

    /**
     * Thread that is runnable / running.
     *
     * @see java.lang.Thread.State#RUNNABLE
     */
    public boolean isRunnable() {
        return this == RUNNABLE;
    }

    /**
     * Thread in {@link Thread#sleep(long)} or {@link Thread#sleep(long, int)} waiting to be notified.
     *
     * @see java.lang.Thread.State#TIMED_WAITING
     */
    public boolean isSleeping() {
        return this == SLEEPING;
    }

    /**
     * Thread in {@link Object#wait()} or {@link Object#wait(long)}.
     *
     * @see java.lang.Thread.State#WAITING
     * @see java.lang.Thread.State#TIMED_WAITING
     */
    public boolean isWaiting() {
        return this == IN_OBJECT_WAIT || this == IN_OBJECT_WAIT_TIMED;
    }

    /**
     * Thread in {@link LockSupport#park()} or {@link LockSupport#parkNanos}.
     *
     * @see java.lang.Thread.State#WAITING
     * @see java.lang.Thread.State#TIMED_WAITING
     */
    public boolean isParked() {
        return this == PARKED || this == PARKED_TIMED;
    }

    /**
     * Thread (re-)entering a synchronization block.
     *
     * @see java.lang.Thread.State#BLOCKED
     */
    public boolean isBlocked() {
        return this == BLOCKED;
    }

    /**
     * Thread that terminated execution.
     *
     * @see java.lang.Thread.State#TERMINATED
     */
    public boolean isTerminated() {
        return this == TERMINATED;
    }


    @Deprecated public static @Nonnull ThreadStatus valueOf(int code) {
        for (ThreadStatus status: values()) {
            if (status.code == code) return status;
        }

        return UNKNOWN;
    }

    public static @Nonnull ThreadStatus fromString(String title) {
        try {

            return Enum.valueOf(ThreadStatus.class, title);
        } catch (IllegalArgumentException ex) {

            for (ThreadStatus value: values()) {
                if (value.getName().equals(title)) return value;
            }

            throw ex;
        }
    }

    @Deprecated public static @Nonnull ThreadStatus fromState(Thread.State state) {
        for (ThreadStatus value: values()) {
            if (value.state.equals(state)) return value;
        }

        throw new AssertionError("No matching ThreadState");
    }

    public static @Nonnull ThreadStatus fromState(@Nonnull Thread.State state, @Nonnull StackTraceElement head) {
        switch (state) {
            case NEW: return NEW;
            case RUNNABLE: return RUNNABLE;
            case BLOCKED: return BLOCKED;
            case WAITING: return waitingState(false, head); // Not exact
            case TIMED_WAITING: return waitingState(true, head); // Not exact
            case TERMINATED: return TERMINATED;
            default: return UNKNOWN;
        }
    }

    private static @Nonnull ThreadStatus waitingState(boolean timed, @Nonnull StackTraceElement head) {
        if ("sleep".equals(head.getMethodName()) && "java.lang.Thread".equals(head.getClassName())) return SLEEPING;
        if ("wait".equals(head.getMethodName()) && "java.lang.Object".equals(head.getClassName())) {
            return timed ? IN_OBJECT_WAIT_TIMED : IN_OBJECT_WAIT;
        }
        if ("park".equals(head.getMethodName()) && "sun.misc.Unsafe".equals(head.getClassName())) {
            return timed ? PARKED_TIMED : PARKED;
        }

        throw new AssertionError("Unable to infer ThreadStatus from WAITING state in " + head);
    }
}
