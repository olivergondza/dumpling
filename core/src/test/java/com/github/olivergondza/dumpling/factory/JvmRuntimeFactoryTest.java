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

import static com.github.olivergondza.dumpling.Util.only;
import static com.github.olivergondza.dumpling.Util.pause;
import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import com.github.olivergondza.dumpling.DisposeRule;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Rule;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;
import com.github.olivergondza.dumpling.model.jvm.JvmRuntime;
import com.github.olivergondza.dumpling.model.jvm.JvmThread;
import com.github.olivergondza.dumpling.model.jvm.JvmThreadSet;
import org.junit.rules.Timeout;

public class JvmRuntimeFactoryTest {

    // Abort after 10 seconds - gets stuck on strange architectures
    public @Rule Timeout timeout = new Timeout(100000);

    public @Rule DisposeRule clean = new DisposeRule();

    @Test
    public synchronized void newThreadShouldNotBeAPartOfReportedRuntime() {
        Thread thread = clean.register(new Thread(getClass().getName() + " not run"));
        assertNull("Not started thread should not be a part fo runtime", forThread(thread));
    }

    @Test
    public synchronized void runnableThreadStatus() {
        Thread thread = clean.register(new Thread(getClass().getName() + " running") {
            long a;

            @Override
            public void run() {
                while (true) {
                    a = a + hashCode();
                }
            }
        });
        thread.start();

        assertStatusIs(ThreadStatus.RUNNABLE, thread);
        assertStateIs(Thread.State.RUNNABLE, thread);
    }

    @Test
    public void sleepingThreadStatus() {
        Thread thread = clean.register(new Thread(getClass().getName() + " sleeping") {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        });
        thread.start();

        assertStatusIs(ThreadStatus.SLEEPING, thread);
        assertStateIs(Thread.State.TIMED_WAITING, thread);
    }

    @Test
    public void waitingThreadStatus() {
        WaitingThreadStatus thread = new WaitingThreadStatus();
        thread.start();
        clean.register(thread);

        assertStatusIs(ThreadStatus.IN_OBJECT_WAIT, thread);
        assertStateIs(Thread.State.WAITING, thread);
        assertVerbIs("waiting on", thread);
        JvmThread pt = forThread(thread);
        assertThat(pt.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(pt.getWaitingOnLock().getClassName(), equalTo(getClass().getCanonicalName() + "$WaitingThreadStatus"));
        assertThat(pt.getWaitingToLock(), nullValue());
    }

    private static final class WaitingThreadStatus extends Thread {
        private WaitingThreadStatus() {
            super("JvmRuntimeFactoryTest#waitingThreadStatus");
        }

        @Override
        public synchronized void run() {
            try {
                wait();
            } catch (InterruptedException ex) {
                // Ignore
            }
        }
    }

    @Test
    public void timedWaitingThreadStatus() {
        TimedWaiting thread = new TimedWaiting();
        thread.start();
        clean.register(thread);

        assertStatusIs(ThreadStatus.IN_OBJECT_WAIT_TIMED, thread);
        assertStateIs(Thread.State.TIMED_WAITING, thread);
        JvmThread pt = forThread(thread);
        assertThat(pt.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(pt.getWaitingOnLock().getClassName(), equalTo(getClass().getCanonicalName() + "$TimedWaiting"));
        assertThat(pt.getWaitingToLock(), nullValue());
    }

    private static final class TimedWaiting extends Thread {
        private TimedWaiting() {
            super("JvmRuntimeFactoryTest#timedWaitingThreadStatus");
        }

        @Override
        public synchronized void run() {
            try {
                wait(10000);
            } catch (InterruptedException ex) {
                // Ignore
            }
        }
    }

    @Test
    public void parkedThreadStatus() {
        Thread thread = new Thread(getClass().getName() + " parked") {
            @Override
            public void run() {
                for (; ; ) {
                    LockSupport.park();
                }
            }
        };
        thread.start();
        clean.register(thread);

        assertStatusIs(ThreadStatus.PARKED, thread);
        assertStateIs(Thread.State.WAITING, thread);
        JvmThread pt = forThread(thread);
        assertThat(pt.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(pt.getWaitingOnLock(), nullValue());
        assertThat(pt.getWaitingToLock(), nullValue());
    }

    @Test
    public void parkedTimedThreadStatus() {
        Thread thread = new Thread(getClass().getName() + " parked timed") {
            @Override
            public void run() {
                for (; ; ) {
                    LockSupport.parkNanos(1000000000L);
                }
            }
        };
        thread.start();
        clean.register(thread);

        assertStatusIs(ThreadStatus.PARKED_TIMED, thread);
        assertStateIs(Thread.State.TIMED_WAITING, thread);
        JvmThread pt = forThread(thread);
        assertThat(pt.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(pt.getWaitingOnLock(), nullValue());
        assertThat(pt.getWaitingToLock(), nullValue());
    }

    @Test
    public void parkedThreadWithBlockerStatus() {
        final Object blocker = new Object();
        Thread thread = new Thread(getClass().getName() + " parked") {
            @Override
            public void run() {
                for (; ; ) {
                    LockSupport.park(blocker);
                }
            }
        };
        thread.start();
        clean.register(thread);

        assertStatusIs(ThreadStatus.PARKED, thread);
        assertStateIs(Thread.State.WAITING, thread);
        JvmThread pt = forThread(thread);
        assertThat(pt.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(pt.getWaitingOnLock(), equalTo(ThreadLock.fromInstance(blocker)));
        assertThat(pt.getWaitingToLock(), nullValue());
    }

    @Test
    public void parkedTimedThreadWithBlockerStatus() {
        final Object blocker = new Object();
        Thread thread = new Thread(getClass().getName() + " parked timed") {
            @Override
            public void run() {
                for (; ; ) {
                    LockSupport.parkNanos(blocker, 1000000000L);
                }
            }
        };
        thread.start();
        clean.register(thread);

        assertStatusIs(ThreadStatus.PARKED_TIMED, thread);
        assertStateIs(Thread.State.TIMED_WAITING, thread);
        JvmThread pt = forThread(thread);
        assertThat(pt.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(pt.getWaitingOnLock(), equalTo(ThreadLock.fromInstance(blocker)));
        assertThat(pt.getWaitingToLock(), nullValue());
    }

    @Test
    public synchronized void blockedOnMonitorThreadStatus() {
        Thread thread = new Thread(getClass().getName() + " waiting on monitor") {
            @Override
            public void run() {
                synchronized (JvmRuntimeFactoryTest.this) {
                    hashCode();
                }
            }
        };
        thread.start();
        clean.register(thread);

        assertStatusIs(ThreadStatus.BLOCKED, thread);
        assertStateIs(Thread.State.BLOCKED, thread);
        assertVerbIs("waiting to lock", thread);
    }

    @Test
    public void creatingAndTerminatingThreadsShouldBeHandledGracefully() {
        final ThreadGroup group = new ThreadGroup("creatingAndTerminatingThreadsShouldBeHandledGracefully");
        class Thrd extends Thread {
            private int countdown;
            public Thrd(int countdown) {
                super(group, "creatingAndTerminatingThreadsShouldBeHandledGracefully thread " + countdown);
                this.countdown = countdown;
            }

            @Override
            public void run() {
                synchronized (JvmRuntimeFactoryTest.class) {
                    if (countdown >= 0) {
                        new Thrd(countdown - 1).start();
                    }
                }
            }
        }

        int originalCount = runtime().getThreads().size();

        new Thrd(100).start();
        new Thrd(100).start();
        new Thrd(100).start();
        new Thrd(100).start();
        new Thrd(100).start();
        new Thrd(100).start();
        new Thrd(100).start();
        new Thrd(100).start();


        JvmRuntime runtime;
        do {
            runtime = runtime();

        } while (runtime.getThreads().size() > originalCount);

        group.interrupt();
    }

    @Test
    public void testThreadAttributes() {
        Thread expected = Thread.currentThread();

        JvmThread actual = runtime().getThreads().where(nameIs(expected.getName())).onlyThread();

        assertThat(expected.getName(), equalTo(actual.getName()));
        assertThat(expected.getState(), equalTo(actual.getState()));
        assertThat(expected.getPriority(), equalTo(actual.getPriority()));
        assertThat(expected.getId(), equalTo(actual.getId()));
    }

    @Test
    public void monitorOwnerInObjectWait() throws Exception {
        final Object lock = new Object();

        Thread thread = new Thread("monitorOwnerOnObjectWait") {
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
        clean.register(thread);

        Thread.sleep(100); // Wait until sleeping

        synchronized(lock) {

            // Waiting thread is not supposed to own the thread
            JvmRuntime runtime = runtime();
            JvmThread waiting = runtime.getThreads().where(nameIs("monitorOwnerOnObjectWait")).onlyThread();
            assertThat(waiting.getAcquiredLocks(), IsEmptyCollection.<ThreadLock>empty());
            assertThat(waiting.getStatus(), equalTo(ThreadStatus.IN_OBJECT_WAIT));

            // Current thread is
            JvmThread current = runtime.getThreads().forCurrentThread();
            final Set<ThreadLock> expected = new HashSet<ThreadLock>(Collections.singletonList(
                    ThreadLock.fromInstance(lock)
            ));
            assertThat(current.getAcquiredLocks(), equalTo(expected));
        }
    }

    @Test
    public void ownableSynchronizers() throws Exception {
        final Lock lock = new ReentrantLock();
        lock.lockInterruptibly();
        Thread thread = null;
        try {

            thread = new Thread("ownableSynchronizers") {
                @Override
                public void run() {
                    try {
                        lock.lockInterruptibly(); // Block here
                    } catch (InterruptedException ex) {
                    }
                }
            };
            thread.start();
            clean.register(thread);
            Thread.sleep(100); // Wait until blocked

            JvmRuntime runtime = runtime();
            JvmThread owner = runtime.getThreads().forCurrentThread();
            JvmThread blocked = runtime.getThreads().where(nameIs("ownableSynchronizers")).onlyThread();

            assertThat(owner.getStatus(), equalTo(ThreadStatus.RUNNABLE));
            assertThat(blocked.getStatus(), equalTo(ThreadStatus.PARKED));

            assertThat(only(owner.getAcquiredLocks()), equalTo(blocked.getWaitingOnLock()));
            assertThat(blocked.getBlockingThread(), equalTo(owner));
        } finally {
            thread.interrupt();
            lock.unlock();
        }
    }

    @Test
    public void multipleMonitorsOnSameStackFrame() throws Exception {
        final Lock lock = new ReentrantLock();
        final Object obj = new Object();
        final String str = new String();
        new Thread("multipleMonitors") {
            @Override
            public void run() {
                synchronized (lock) {
                    synchronized (obj) {
                        synchronized (str) {
                            synchronized (str) {
                                pause(1000);
                            }
                        }
                    }
                }
            }
        }.start();

        pause(100);

        JvmThreadSet monitors = runtime().getThreads().where(nameIs("multipleMonitors"));

        // All locks on single frame should be reported. Outermost lock should
        // be at the bottom (first), innermost last.
        assertThat(monitors.toString(), containsString(Util.formatTrace(
                "- locked " + ThreadLock.fromInstance(str),
                "- locked " + ThreadLock.fromInstance(str),
                "- locked " + ThreadLock.fromInstance(obj),
                "- locked " + ThreadLock.fromInstance(lock)
        )));

        assertThat(monitors.onlyThread().getAcquiredLocks().size(), equalTo(3));
    }

    @Test
    public void extractThread() {
        JvmThreadSet threads = new JvmRuntimeFactory().currentRuntime().getThreads();
        Thread currentThread = Thread.currentThread();

        assertThat(currentThread.getId(), equalTo(threads.forCurrentThread().getId()));
        assertThat(currentThread.getId(), equalTo(threads.forThread(currentThread).getId()));
        assertNull(threads.forThread(null));
        assertNull(threads.forThread(new Thread()));
    }

    @Test
    public void groupName() throws Exception {
        JvmThreadSet threads = new JvmRuntimeFactory().currentRuntime().getThreads();
        Thread actual = Thread.currentThread();

        JvmThread extracted = threads.forCurrentThread();
        String actualName = actual.getThreadGroup().getName();
        assertThat(actualName, equalTo(extracted.getGroupName()));
        assertThat(extracted.toString(), containsString("groupName=\"" + actualName + "\""));
    }

    private void assertStatusIs(ThreadStatus expected, Thread thread) {
        assertEquals("Reported state: " + thread.getState(), expected, statusOf(thread));
    }

    private void assertStateIs(Thread.State expected, Thread thread) {
        assertEquals("Reported state: " + thread.getState(), expected, statusOf(thread).getState());
    }

    private void assertVerbIs(String verb, Thread thread) {
        pause(100);
        assertThat(forThread(thread).toString(), containsString("- " + verb));
    }

    private ThreadStatus statusOf(Thread thread) {
        pause(100);
        final JvmThread processThread = forThread(thread);
        if (processThread == null) throw new AssertionError(
                "No process thread in runtime for " + thread.getName()
        );
        return processThread.getStatus();
    }

    private JvmRuntime runtime() {
        return new JvmRuntimeFactory().currentRuntime();
    }

    private JvmThread forThread(Thread candidate) {
        return runtime().getThreads().forThread(candidate);
    }
}
