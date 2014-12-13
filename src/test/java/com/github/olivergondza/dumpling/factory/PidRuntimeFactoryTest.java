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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.cli.CommandFailedException;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class PidRuntimeFactoryTest extends AbstractCliTest {

    private Thread t;

    @Test
    public void invokeFactory() {
        setupSleepingThreadWithLock();

        ProcessRuntime pidRuntime = new PidRuntimeFactory().forProcess(Util.currentPid());

        ProcessThread thread = pidRuntime.getThreads().where(nameIs("sleepingThreadWithLock")).onlyThread();
        assertThat(thread.getStatus(), equalTo(ThreadStatus.SLEEPING));

        assertFalse(thread.toString(), thread.getAcquiredLocks().isEmpty());
        assertThat(
                only(thread.getAcquiredLocks()).getClassName(),
                equalTo("java.util.concurrent.locks.ReentrantLock$NonfairSync")
        );
    }

    @Test
    public void invokeCommand() {
        setupSleepingThreadWithLock();

        stdin("t = runtime.threads.where(nameIs('sleepingThreadWithLock')).onlyThread(); print \"${t.status}:${t.acquiredLocks.collect{it.className}}\"");
        run("groovy", "--in", "process", Integer.toString(Util.currentPid()));

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("SLEEPING:[java.util.concurrent.locks.ReentrantLock$NonfairSync]"));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void illegalPid() {
        run("groovy", "--in", "process", "not_a_pid");
        assertThat(out.toString(), equalTo(""));
        assertThat(err.toString(), equalTo("Unable to parse 'not_a_pid' as process ID\n"));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void notAJavaProcess() {
        try {
            new PidRuntimeFactory().forProcess(299);
            fail("No exception thrown");
        } catch(CommandFailedException ex) {
            assertThat(ex.getMessage(), containsString("jstack failed with code "));
            assertThat(ex.getMessage(), containsString("299: No such process"));
        }
    }

    @Test
    public void jstackNotExecutable() {
        PidRuntimeFactory factory = new PidRuntimeFactory(System.getProperty("java.home") + "/no_such_dir/");
        try {
            factory.forProcess(Util.currentPid());
            fail("No exception thrown");
        } catch(CommandFailedException ex) {
            assertThat(ex.getMessage(), containsString("Unable to invoke jstack: Cannot run program"));
        }
    }

    private Thread setupSleepingThreadWithLock() {
        final ReentrantLock lock = new ReentrantLock();
        this.t = new Thread("sleepingThreadWithLock") {
            @Override
            public void run() {
                lock.lock();
                pause(10000);
            }
        };
        this.t.start();
        while(!lock.isLocked()) {
            pause(1000);
        }
        return this.t;
    }

    @After
    @SuppressWarnings("deprecation")
    public void tearDown() {
        if (t != null) {
            t.stop();
        }
    }
}
