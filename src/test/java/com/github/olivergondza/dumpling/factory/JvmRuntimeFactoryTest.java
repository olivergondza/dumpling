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

import static com.github.olivergondza.dumpling.Util.pause;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Test;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class JvmRuntimeFactoryTest {

    private Thread thread;

    @Test
    public synchronized void newThreadShouldNotBeAPartOfReportedRuntime() {
        thread = new Thread(getClass().getName() + " not run");
        assertNull("Not started thread should not be a part fo runtime", forThread(runtime(), thread));
    }

    @Test
    public synchronized void runnableThreadStatus() {
        thread = new Thread(getClass().getName() + " running") {
            long a;
            @Override
            public void run() {
                while(true) {
                    a = a + hashCode();
                }
            }
        };
        thread.start();

        assertStatusIs(ThreadStatus.RUNNABLE, thread);
        assertStateIs(Thread.State.RUNNABLE, thread);
    }

    @Test
    public void sleepingThreadStatus() {
        thread = new Thread(getClass().getName() + " sleeping") {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        };
        thread.start();

        assertStatusIs(ThreadStatus.SLEEPING, thread);
        assertStateIs(Thread.State.TIMED_WAITING, thread);
    }

    @Test
    public void waitingThredStatus() {
        thread = new Thread(getClass().getName() + " in object wait") {
            @Override
            public synchronized void run() {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        };
        thread.start();

        assertStatusIs(ThreadStatus.IN_OBJECT_WAIT, thread);
        assertStateIs(Thread.State.WAITING, thread);
        assertVerbIs("waiting on", thread);
    }

    @Test
    public void timedWaitingThredStatus() {
        thread = new Thread(getClass().getName() + " in timed object wait") {
            @Override
            public synchronized void run() {
                try {
                    wait(10000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        };
        thread.start();

        assertStatusIs(ThreadStatus.IN_OBJECT_WAIT_TIMED, thread);
        assertStateIs(Thread.State.TIMED_WAITING, thread);
    }

    @Test
    public void parkedThreadStatus() {
        thread = new Thread(getClass().getName() + " parked") {
            @Override
            public void run() {
                LockSupport.park();
            }
        };
        thread.start();

        assertStatusIs(ThreadStatus.PARKED, thread);
        assertStateIs(Thread.State.WAITING, thread);
    }

    @Test
    public void parkedTimedThreadStatus() {
        thread = new Thread(getClass().getName() + " parked timed") {
            @Override
            public void run() {
                LockSupport.parkNanos(1000000000L);
            }
        };
        thread.start();

        assertStatusIs(ThreadStatus.PARKED_TIMED, thread);
        assertStateIs(Thread.State.TIMED_WAITING, thread);
    }

    @Test
    public synchronized void blockedOnMonitorThreadStatus() {
        thread = new Thread(getClass().getName() + " waiting on monitor") {
            @Override
            public void run() {
                synchronized (JvmRuntimeFactoryTest.this) {
                    hashCode();
                }
            }
        };
        thread.start();

        assertStatusIs(ThreadStatus.BLOCKED_ON_MONITOR_ENTER, thread);
        assertStateIs(Thread.State.BLOCKED, thread);
        assertVerbIs("waiting to lock", thread);
    }

    @Test
    public void creatingAndTerminatingThreadsShouldBeHandledGracefully() {
        class Thrd extends Thread {
            int countdown;
            public Thrd(int countdown) {
                this.countdown = countdown;
            }

            @Override
            public void run() {
                pause(1);
                new Thrd(countdown - 1).start();
            }
        }

        int originalCount = new JvmRuntimeFactory().currentRuntime().getThreads().size();

        new Thrd(10).start();
        new Thrd(10).start();
        new Thrd(10).start();
        new Thrd(10).start();
        new Thrd(10).start();

        ProcessRuntime runtime;
        do {
            runtime = new JvmRuntimeFactory().currentRuntime();

        } while (runtime.getThreads().size() > originalCount);
    }

    @Test
    public void testThreadAttributes() {
        Thread expected = Thread.currentThread();

        ProcessThread actual = new JvmRuntimeFactory().currentRuntime()
                .getThreads().onlyNamed(expected.getName()).onlyThread()
        ;

        assertThat(expected.getName(), equalTo(actual.getName()));
        assertThat(expected.getState(), equalTo(actual.getState()));
        assertThat(expected.getPriority(), equalTo(actual.getPriority()));
        assertThat(expected.getId(), equalTo(actual.getId()));
    }

    @Test
    public void monitorOwnerInObjectWait() throws Exception {
        final Object lock = new Object();

        thread = new Thread("monitorOwnerOnObjectWait") {
            @Override
            public void run() {
                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (InterruptedException ex) {
                }
            }
        };
        thread.start();

        Thread.sleep(100); // Wait until sleeping

        synchronized(lock) {

            // Waiting thread is not supposed to own the thread
            ProcessRuntime runtime = new JvmRuntimeFactory().currentRuntime();
            ProcessThread waiting = runtime.getThreads().onlyNamed("monitorOwnerOnObjectWait").onlyThread();
            assertThat(waiting.getAcquiredLocks(), IsEmptyCollection.<ThreadLock>empty());
            assertThat(waiting.getThreadStatus(), equalTo(ThreadStatus.IN_OBJECT_WAIT));

            // Current thread is
            ProcessThread current = runtime.getThreads().onlyNamed(Thread.currentThread().getName()).onlyThread();
            final Set<ThreadLock> expected = new HashSet<ThreadLock>(Arrays.asList(
                    new ThreadLock.WithHashCode(0, lock.getClass().getCanonicalName(), lock.hashCode())
            ));
            assertThat(current.getAcquiredLocks(), equalTo(expected));
        }
    }

    private void assertStatusIs(ThreadStatus expected, Thread thread) {
        assertEquals("Reported state: " + thread.getState(), expected, statusOf(thread));
    }

    private void assertStateIs(Thread.State expected, Thread thread) {
        assertEquals("Reported state: " + thread.getState(), expected, statusOf(thread).getState());
    }

    private void assertVerbIs(String verb, Thread thread) {
        assertThat(forThread(runtime(), thread).toString(), containsString("- " + verb));
    }

    private ThreadStatus statusOf(Thread thread) {
        final ProcessThread processThread = forThread(runtime(), thread);
        if (processThread == null) throw new AssertionError(
                "No process thread in runtime for " + thread.getName()
        );
        return processThread.getThreadStatus();
    }

    private ProcessRuntime runtime() {
        pause(100);
        return new JvmRuntimeFactory().currentRuntime();
    }

    private ProcessThread forThread(ProcessRuntime runtime, Thread candidate) {
        for(ProcessThread thread: runtime.getThreads()) {
            if (thread.getId() == candidate.getId()) return thread;
        }

        return null;
    }
}
