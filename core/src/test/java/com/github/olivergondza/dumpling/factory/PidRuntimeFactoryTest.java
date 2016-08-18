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
import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;

import com.github.olivergondza.dumpling.model.ThreadLock;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import com.github.olivergondza.dumpling.DisposeRule;
import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.TestThread;
import com.github.olivergondza.dumpling.model.ThreadStatus;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread;

public class PidRuntimeFactoryTest {

    @Rule public DisposeRule disposer = new DisposeRule();

    @Test
    public void invokeFactory() throws Exception {
        disposer.register(TestThread.setupSleepingThreadWithLock());

        ThreadDumpRuntime pidRuntime = new PidRuntimeFactory().fromProcess(Util.currentPid());

        ThreadDumpThread thread = pidRuntime.getThreads().where(nameIs("sleepingThreadWithLock")).onlyThread();
        assertThat(thread.getStatus(), equalTo(ThreadStatus.SLEEPING));

        assertThat(thread.getAcquiredLocks(), not(Matchers.<ThreadLock>emptyIterable()));
        assertThat(
                only(thread.getAcquiredLocks()).getClassName(),
                equalTo("java.util.concurrent.locks.ReentrantLock$NonfairSync")
        );
    }

    @Test
    public void notAJavaProcess() throws Exception {
        try {
            new PidRuntimeFactory().fromProcess(299);
            fail("No exception thrown");
        } catch(IOException ex) {
            assertThat(ex.getMessage(), containsString("jstack failed with code "));
        }
    }

    @Test(expected = PidRuntimeFactory.UnsupportedJdk.class)
    public void jstackNotExecutable() throws Exception {
        PidRuntimeFactory factory = new PidRuntimeFactory(System.getProperty("java.home") + "/no_such_dir/");
        factory.fromProcess(Util.currentPid());
    }
}
