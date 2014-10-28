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
import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Test;

import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.cli.CommandFailedException;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class PidRuntimeFactoryTest extends AbstractCliTest {

    private Thread t;

    @Test
    public void invokeFactory() {
        setupFixture();

        ProcessRuntime pidRuntime = new PidRuntimeFactory().forProcess(
                Integer.parseInt(currentPid())
        );

        ProcessThread thread = pidRuntime.getThreads().where(nameIs("process-thread")).onlyThread();
        assertThat(
                thread.getAcquiredLocks().iterator().next().getClassName(),
                equalTo("java.util.concurrent.locks.ReentrantLock$NonfairSync")
        );

        assertThat(thread.getStatus(), equalTo(ThreadStatus.SLEEPING));
    }

    @Test
    public void invokeCommand() {
        setupFixture();

        stdin("print runtime.threads.where(nameIs('process-thread')).onlyThread().status");
        run("groovy", "--in", "process", currentPid());

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("SLEEPING"));
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
            factory.forProcess(Integer.parseInt(currentPid()));
            fail("No exception thrown");
        } catch(CommandFailedException ex) {
            assertThat(ex.getMessage(), containsString("Unable to invoke jstack: Cannot run program"));
        }
    }

    private String currentPid() {
        return ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
    }

    private Thread setupFixture() {
        this.t = new Thread("process-thread") {
            @Override
            public void run() {
                new ReentrantLock().lock();
                pause(10000);
            }
        };
        this.t.start();
        return this.t;
    }

    @After
    public void tearDown() {
        if (t != null) {
            t.stop();
        }
    }
}
