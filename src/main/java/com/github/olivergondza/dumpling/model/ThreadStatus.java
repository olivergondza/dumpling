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
    RUNNABLE            ("RUNNABLE",                            5,      State.RUNNABLE), // runnable / running
    SLEEPING            ("TIMED_WAITING (sleeping)",            225,    State.TIMED_WAITING), // Thread.sleep()
    IN_OBJECT_WAIT      ("WAITING (on object monitor)",         401,    State.WAITING), // Object.wait()
    IN_OBJECT_WAIT_TIMED("TIMED_WAITING (on object monitor)",   417,    State.TIMED_WAITING), // Object.wait(long)
    PARKED              ("WAITING (parking)",                   657,    State.WAITING), // LockSupport.park()
    PARKED_TIMED        ("TIMED_WAITING (parking)",             673,    State.TIMED_WAITING), // LockSupport.park(long)
    BLOCKED             ("BLOCKED (on object monitor)",         1025,   State.BLOCKED), // (re-)entering a synchronization block
    TERMINATED          ("TERMINATED",                          2,      State.TERMINATED),

    /**
     * Runtime factory was not able to determine thread state.
     *
     * This can happen for service threads in threaddump.
     */
    UNKNOWN             ("UNKNOWN",                             -1,     null);

    private final @Nonnull String name;
    private final int code;
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

    public State getState() {
        return state;
    }

    public boolean isWaiting() {
        return this == IN_OBJECT_WAIT || this == IN_OBJECT_WAIT_TIMED;
    }

    public boolean isParked() {
        return this == PARKED || this == PARKED_TIMED;
    }

    public static @Nonnull ThreadStatus valueOf(int code) {
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
}
