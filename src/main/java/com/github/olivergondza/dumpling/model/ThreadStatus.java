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

/**
 *
 * @author ogondza
 * @see http://hg.openjdk.java.net/jdk7/jdk7/hotspot/file/167b70ff3abc/src/share/vm/classfile/javaClasses.cpp#l922
 */
public enum ThreadStatus {
    NEW                         ("NEW", 0),
    RUNNABLE                    ("RUNNABLE", 5), // runnable / running
    SLEEPING                    ("TIMED_WAITING (sleeping)", /*121*/ 225), // Thread.sleep()
    IN_OBJECT_WAIT              ("WAITING (on object monitor)", /*281*/ 401), // Object.wait()
    IN_OBJECT_WAIT_TIMED        ("TIMED_WAITING (on object monitor)", /*297*/ 417),  // Object.wait(long)
    PARKED                      ("WAITING (parking)", /*537*/ 657), // LockSupport.park()
    PARKED_TIMED                ("TIMED_WAITING (parking)", /*553*/ 673), // LockSupport.park(long)
    BLOCKED_ON_MONITOR_ENTER    ("BLOCKED (on object monitor)", 1025), // (re-)entering a synchronization block
    TERMINATED                  ("TERMINATED", 2),
    UNKNOWN                     ("UNKNOWN", -1);

    private String name;
    private int code;

    private ThreadStatus(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public int getCode() {
        return code;
    }

    public static ThreadStatus valueOf(int code) {
        for (ThreadStatus status: values()) {
            if (status.code == code) return status;
        }

        return UNKNOWN;
    }
}
