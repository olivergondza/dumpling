/*
 * The MIT License
 *
 * Copyright (c) 2015 Red Hat, Inc.
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
package com.github.olivergondza.dumpling.cli;

import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;

import org.junit.Rule;
import org.junit.Test;

import com.github.olivergondza.dumpling.DisposeRule;
import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.TestThread;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.StackTrace;
import com.github.olivergondza.dumpling.model.ThreadStatus;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;

public class SourceTest extends AbstractCliTest {

    @Rule public DisposeRule disposer = new DisposeRule();

    @Test
    public void cliNoSuchFile() {
        run("deadlocks", "--in", "threaddump", "/there_is_no_such_file");
        assertThat(exitValue, equalTo(-1));
        assertThat(out.toString(), equalTo(""));
        // Error message is platform specific
        assertThat(err.toString(), not(equalTo("")));
    }

    @Test
    public void jmxRemoteConnectViaCli() throws Exception {
        TestThread.JMXProcess process = disposer.register(TestThread.runJmxObservableProcess(false));
        stdin("runtime.threads.where(nameIs('remotely-observed-thread'))");
        run("groovy", "--in", "jmx", process.JMX_CONNECTION);

        assertThat(err.toString(), equalTo(""));
        // Reuse verification logic re-parsing the output as thread dump
        ThreadDumpRuntime reparsed = new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
        assertThreadState(reparsed);
    }

    @Test
    public void jmxLocalConnectViaCli() {
        disposer.register(TestThread.runThread());
        stdin("runtime.threads.where(nameIs('remotely-observed-thread'))");
        run("groovy", "--in", "jmx", Integer.toString(Util.currentPid()));

        assertThat(err.toString(), equalTo(""));
        // Reuse verification logic re-parsing the output as thread dump
        ThreadDumpRuntime reparsed = new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
        assertThreadState(reparsed);
    }

    private void assertThreadState(ProcessRuntime<?, ?, ?> runtime) {
        ProcessThread<?, ?, ?> actual = runtime.getThreads().where(nameIs("remotely-observed-thread")).onlyThread();
        StackTrace trace = actual.getStackTrace();

        assertThat(actual.getName(), equalTo("remotely-observed-thread"));
        assertThat(actual.getStatus(), equalTo(ThreadStatus.IN_OBJECT_WAIT));
        // TODO other attributes

        // Test class and method name only as MXBean way offer filename too while thread dump way does not
        final StackTraceElement innerFrame = trace.getElement(0);
        assertThat(innerFrame.getClassName(), equalTo("java.lang.Object"));
        assertThat(innerFrame.getMethodName(), equalTo("wait"));

        // Do not assert line number as it changes between JDK versions
        final StackTraceElement waitElement = trace.getElement(1);
        assertThat(waitElement.getClassName(), equalTo("java.lang.Object"));
        assertThat(waitElement.getMethodName(), equalTo("wait"));
        assertThat(waitElement.getFileName(), equalTo("Object.java"));

        final StackTraceElement testFrame = trace.getElement(2);
        assertThat(testFrame.getClassName(), equalTo("com.github.olivergondza.dumpling.TestThread$1"));
        assertThat(testFrame.getMethodName(), equalTo("run"));
        assertThat(testFrame.getFileName(), equalTo("TestThread.java"));
    }

    @Test
    public void invokeCommand() {
        disposer.register(TestThread.setupSleepingThreadWithLock());

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
        assertThat(err.toString(), containsString("Unable to parse 'not_a_pid' as process ID"));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void inferSourcePid() throws Exception {
        run("threaddump", "--in", Integer.toString(Util.currentPid()));
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));

        new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void inferSourceFile() throws Exception {
        String path = Util.asFile(Util.resource("jstack/producer-consumer.log")).getAbsolutePath();
        run("threaddump", "--in", path);
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));

        new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
    }
}
