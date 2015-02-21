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

import static com.github.olivergondza.dumpling.model.ProcessThread.acquiredLock;
import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static com.github.olivergondza.dumpling.model.ProcessThread.waitingToLock;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.factory.IllegalRuntimeStateException;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactoryTest;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThreadSet;

public class ProcessTheadTest {

    private final ThreadDumpFactory factory = new ThreadDumpFactory();

    @Test
    public void printLocksOnCorrectPosionInStackTrace() throws Exception {
        String dump = factory.fromFile(Util.resourceFile("producer-consumer.log")).getThreads().toString();

        assertThat(dump, containsString(Util.formatTrace(
                "at hudson.model.Queue.getItem(Queue.java:719)",
                "- waiting to lock <0x4063a9378> (a hudson.model.Queue)",
                "at hudson.model.AbstractProject.getQueueItem(AbstractProject.java:927)"
        )));

        assertThat(dump, containsString(Util.formatTrace(
                "at hudson.model.Queue.maintain(Queue.java:1106)",
                "- locked <0x4063a9378> (a hudson.model.Queue)",
                "at hudson.model.Queue.pop(Queue.java:935)",
                "- locked <0x4063a9378> (a hudson.model.Queue)",
                "at hudson.model.Executor.grabJob(Executor.java:297)",
                "at hudson.model.Executor.run(Executor.java:211)",
                "- locked <0x4063a9378> (a hudson.model.Queue)"
        )));
    }

    @Test
    public void differentWaitingVerbs() throws Exception {
        ThreadDumpRuntime runtime = factory.fromFile(Util.resourceFile("deadlock.log"));
        assertThat(runtime.getThreads().toString(), containsString(
                "- waiting to lock <0x404325338> (a hudson.model.Hudson)"
        ));

        runtime = factory.fromFile(Util.resourceFile(ThreadDumpFactoryTest.class, "oraclejdk-1.7.0_51.log"));
        assertThat(runtime.getThreads().where(nameIs("MSC service thread 1-2")).toString(), containsString(
                "- parking to wait for <0x7007d87c8> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)"
        ));
    }

    @Test
    public void filterByLocks() throws Exception {
        ThreadDumpThreadSet threads = factory.fromFile(Util.resourceFile("producer-consumer.log")).getThreads();
        assertThat(threads.where(nameIs("blocked_thread")), equalTo(threads.where(waitingToLock("hudson.model.Queue"))));
        assertThat(threads.where(nameIs("owning_thread")), equalTo(threads.where(acquiredLock("hudson.model.Queue"))));
    }

    @Test
    public void failSanityCheck() {
        try {
            runtime(new ThreadDumpThread.Builder().setName(null));
            fail();
        } catch (IllegalRuntimeStateException ex) {
            assertThat(ex.getMessage(), equalTo("Thread name not set"));
        }

        try {
            runtime(new ThreadDumpThread.Builder().setName(""));
            fail();
        } catch (IllegalRuntimeStateException ex) {
            assertThat(ex.getMessage(), equalTo("Thread name not set"));
        }

        try {
            runtime(new ThreadDumpThread.Builder().setName("t"));
            fail();
        } catch (IllegalRuntimeStateException ex) {
            assertThat(ex.getMessage(), equalTo("No thread identifier set"));
        }

        try {
            runtime(new ThreadDumpThread.Builder().setName("t").setId(42).setThreadStatus(ThreadStatus.BLOCKED));
            fail();
        } catch (IllegalRuntimeStateException ex) {
            assertThat(ex.getMessage(), startsWith("Blocked thread does not declare monitor"));
        }
    }

    private ThreadDumpRuntime runtime(ThreadDumpThread.Builder... builders) {
        return new ThreadDumpRuntime(new HashSet<ThreadDumpThread.Builder>(Arrays.asList(builders)));
    }
}
