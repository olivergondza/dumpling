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
package com.github.olivergondza.dumpling.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.factory.JvmRuntimeFactory;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThreadSet;
import com.github.olivergondza.dumpling.model.jvm.JvmRuntime;
import com.github.olivergondza.dumpling.model.jvm.JvmThread;
import com.github.olivergondza.dumpling.model.jvm.JvmThreadSet;
import com.github.olivergondza.dumpling.query.Deadlocks.Result;

public class DeadlocksTest {

    volatile boolean running = false;
    @Test
    public void noDeadlockShouldBePresentNormally() {

        Thread thread = new Thread("Running thread") {
            @Override
            public void run() {
                running = true;
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                }
                running = false;
            }
        };
        thread.start();

        Util.pause(100);

        assertTrue("No deadlock should be present", deadlocks(runtime()).isEmpty());

        assertTrue(running);
    }

    @Test
    public void discoverActualDeadlock() {
        final Object lockA = new Object();
        final Object lockB = new Object();

        new Thread("Deadlock thread A") {
            @Override
            public void run() {
                while (true) {
                    synchronized (lockA) {
                        Util.pause(100);
                        synchronized (lockB) {
                            hashCode();
                        }
                    }
                }
            }
        }.start();

        new Thread("Deadlock thread B") {
            @Override
            public void run() {
                while (true) {
                    synchronized (lockB) {
                        Util.pause(100);
                        synchronized (lockA) {
                            hashCode();
                        }
                    }
                }
            }
        }.start();

        Util.pause(1000);

        JvmRuntime runtime = runtime();
        final Set<JvmThreadSet> deadlocks = deadlocks(runtime);

        assertEquals("One deadlock should be present\n\n" + runtime.getThreads(), 1, deadlocks.size());
        for (JvmThreadSet deadlock: deadlocks) {
            assertEquals("Deadlock should contain of 2 threads", 2, deadlock.size());
            for (JvmThread thread: deadlock) {
                assertTrue(thread.getName().matches("Deadlock thread [AB]"));
            }
        }
    }

    @Test
    public void doNotReportThreadsNotPartOfTheCycle() throws Exception {
        ThreadDumpRuntime runtime = new ThreadDumpFactory().fromStream(Util.resource("jstack/deadlock-and-friends.log"));
        Result<ThreadDumpThreadSet, ThreadDumpRuntime, ThreadDumpThread> result = new Deadlocks().query(runtime.getThreads());
        Set<ThreadDumpThreadSet> deadlocks = result.getDeadlocks();
        assertThat("Deadlock count", deadlocks.size(), equalTo(1));

        ThreadDumpThreadSet deadlock = deadlocks.iterator().next();
        assertThat(deadlock.size(), equalTo(2));

        assertThat("Involved thread count", result.involvedThreads().size(), equalTo(2));
    }

    @Test
    public void synchronizerDeadlock() throws Exception {
        ThreadDumpRuntime runtime = new ThreadDumpFactory().fromStream(Util.resource(getClass(), "synchronizer_deadlock.log"));
        String report = new Deadlocks().query(runtime.getThreads()).toString();
        assertThat(report, containsString(Util.multiline(
                "Deadlock #1:",
                "\"Executing labels(hudson.slaves.NodeProvisionerTest)\" prio=10 tid=139758839826432 nid=27543",
                "\tWaiting on <0xf1cf3470> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)",
                "\tAcquired * <0xf253d388> (a java.util.concurrent.locks.ReentrantReadWriteLock$NonfairSync)",
                "\"AtmostOneTaskExecutor[hudson.model.Queue$1@2f485891] [#15]\" daemon prio=10 tid=139757837897728 nid=28180",
                "\tWaiting on <0xf253d388> (a java.util.concurrent.locks.ReentrantReadWriteLock$NonfairSync)",
                "\tAcquired * <0xf1cf3470> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)"
        )));
    }

    private Set<JvmThreadSet> deadlocks(JvmRuntime runtime) {
        return new Deadlocks().query(runtime.getThreads()).getDeadlocks();
    }

    private JvmRuntime runtime() {
        return new JvmRuntimeFactory().currentRuntime();
    }
}
