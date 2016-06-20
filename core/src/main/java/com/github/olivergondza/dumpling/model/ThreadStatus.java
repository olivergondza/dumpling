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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Thread status.
 *
 * More detailed version of {@link java.lang.Thread.State}.
 *
 * @author ogondza
 */
public enum ThreadStatus {
    NEW                 ("NEW",                                 State.NEW),
    RUNNABLE            ("RUNNABLE",                            State.RUNNABLE),
    SLEEPING            ("TIMED_WAITING (sleeping)",            State.TIMED_WAITING),

    /**
     * Thread in {@link Object#wait()}.
     *
     * @see java.lang.Thread.State#WAITING
     */
    IN_OBJECT_WAIT      ("WAITING (on object monitor)",         State.WAITING),

    /**
     * Thread in {@link Object#wait(long)}.
     *
     * @see java.lang.Thread.State#TIMED_WAITING
     */
    IN_OBJECT_WAIT_TIMED("TIMED_WAITING (on object monitor)",   State.TIMED_WAITING),

    /**
     * Thread in {@link LockSupport#park()}.
     *
     * @see java.lang.Thread.State#WAITING
     */
    PARKED              ("WAITING (parking)",                   State.WAITING),

    /**
     * Thread in {@link LockSupport#parkNanos}.
     *
     * @see java.lang.Thread.State#TIMED_WAITING
     */
    PARKED_TIMED        ("TIMED_WAITING (parking)",             State.TIMED_WAITING),

    BLOCKED             ("BLOCKED (on object monitor)",         State.BLOCKED),
    TERMINATED          ("TERMINATED",                          State.TERMINATED),

    /**
     * Runtime factory was not able to determine thread state.
     *
     * This can happen for service threads in threaddump.
     */
    UNKNOWN             ("UNKNOWN",                             null);

    /**
     * Description used in thread dump.
     */
    private final @Nonnull String name;

    /**
     * Matching java.lang.Thread.State.
     */
    private final State state;

    ThreadStatus(@Nonnull String name, State state) {
        this.name = name;
        this.state = state;
    }

    public @Nonnull String getName() {
        return name;
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

    public static @Nonnull ThreadStatus fromString(String title) {
        try {

            @SuppressWarnings("null")
            final @Nonnull ThreadStatus value = Enum.valueOf(ThreadStatus.class, title);
            return value;
        } catch (IllegalArgumentException ex) {

            for (ThreadStatus value: values()) {
                if (value.getName().equals(title)) return value;
            }

            throw ex;
        }
    }

    public static @Nonnull ThreadStatus fromState(@Nonnull Thread.State state, @CheckForNull StackTraceElement head) {
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

    private static @Nonnull ThreadStatus waitingState(boolean timed, @CheckForNull StackTraceElement head) {
        if (head == null) return ThreadStatus.UNKNOWN;
        if ("sleep".equals(head.getMethodName()) && "java.lang.Thread".equals(head.getClassName())) return SLEEPING;
        if ("wait".equals(head.getMethodName()) && "java.lang.Object".equals(head.getClassName())) {
            return timed ? IN_OBJECT_WAIT_TIMED : IN_OBJECT_WAIT;
        }

        if ("park".equals(head.getMethodName()) && UNSAFE.contains(head.getClassName())) {
            return timed ? PARKED_TIMED : PARKED;
        }

        throw new AssertionError("Unable to infer ThreadStatus from WAITING state in " + head);
    }

    public static final List<String> UNSAFE = Arrays.asList(
            "sun.misc.Unsafe", // hotspot JDK 8 and older
            "jdk.internal.misc.Unsafe" // hotspot JDK 9
    );
}
