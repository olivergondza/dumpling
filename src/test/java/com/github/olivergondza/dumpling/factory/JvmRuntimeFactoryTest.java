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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import org.junit.Test;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class JvmRuntimeFactoryTest {

    @Test
    public void test() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        assertEquals(threads.size(), runtime().getThreads().size());
    }

    @Test
    public synchronized void newThreadShouldNotBeAPartOfReportedRuntime() {
        Thread notRun = new Thread(getClass().getName() + " not run");
        assertNull("Not started thread should not be a part fo runtime", forThread(runtime(), notRun));
    }

    @Test
    public synchronized void runnableThreadStatus() {
        Thread running = new Thread(getClass().getName() + " running") {
            long a;
            @Override
            public void run() {
                while(true) {
                    a = a + hashCode();
                }
            }
        };
        running.start();

        assertStatusIs(ThreadStatus.RUNNABLE, running);
    }

    @Test
    public void sleepingThreadStatus() {
        Thread sleeping = new Thread(getClass().getName() + " sleeping") {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        };
        sleeping.start();

        assertStatusIs(ThreadStatus.SLEEPING, sleeping);
    }

    @Test
    public void waitingThredStatus() {
        Thread waiting = new Thread(getClass().getName() + " in object wait") {
            @Override
            public synchronized void run() {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        };
        waiting.start();

        assertStatusIs(ThreadStatus.IN_OBJECT_WAIT, waiting);
    }

    @Test
    public void timedWaitingThredStatus() {
        Thread timedWaiting = new Thread(getClass().getName() + " in timed object wait") {
            @Override
            public synchronized void run() {
                try {
                    wait(10000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        };
        timedWaiting.start();

        assertStatusIs(ThreadStatus.IN_OBJECT_WAIT_TIMED, timedWaiting);
    }

    @Test
    public void parkedThreadStatus() {
        Thread parked = new Thread(getClass().getName() + " parked") {
            @Override
            public void run() {
                LockSupport.park();
            }
        };
        parked.start();

        assertStatusIs(ThreadStatus.PARKED, parked);
    }

    @Test
    public void parkedTimedThreadStatus() {
        Thread parkedTimed = new Thread(getClass().getName() + " parked timed") {
            @Override
            public void run() {
                LockSupport.parkNanos(1000000000);
            }
        };
        parkedTimed.start();

        assertStatusIs(ThreadStatus.PARKED_TIMED, parkedTimed);
    }

    @Test
    public synchronized void blockedOnMonitorThreadStatus() {
        Thread waitingOnMonitor = new Thread(getClass().getName() + " waiting on monitor") {
            @Override
            public void run() {
                synchronized (JvmRuntimeFactoryTest.this) {
                    hashCode();
                }
            }
        };
        waitingOnMonitor.start();

        assertStatusIs(ThreadStatus.BLOCKED_ON_MONITOR_ENTER, waitingOnMonitor);
    }

    private void assertStatusIs(ThreadStatus expected, Thread thread) {
        assertEquals("Reported state: " + thread.getState(), expected, statusOf(thread));
    }

    private ThreadStatus statusOf(Thread thread) {
        return forThread(runtime(), thread).getThreadStatus();
    }

    private ProcessRuntime runtime() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            throw new AssertionError();
        }
        return new JvmRuntimeFactory().currentRuntime();
    }

    private ProcessThread forThread(ProcessRuntime runtime, Thread candidate) {
        for(ProcessThread thread: runtime.getThreads()) {
            if (thread.getId() == candidate.getId()) return thread;
        }

        return null;
    }
}
