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

import static com.github.olivergondza.dumpling.DumplingMatchers.*;
import static com.github.olivergondza.dumpling.TestThread.JMX_HOST;
import static com.github.olivergondza.dumpling.TestThread.JMX_PASSWD;
import static com.github.olivergondza.dumpling.TestThread.JMX_USER;
import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.fail;

import com.github.olivergondza.dumpling.DumplingMatchers;
import org.junit.Rule;
import org.junit.Test;

import com.github.olivergondza.dumpling.DisposeRule;
import com.github.olivergondza.dumpling.TestThread;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.StackTrace;
import com.github.olivergondza.dumpling.model.ThreadStatus;
import com.github.olivergondza.dumpling.model.jmx.JmxRuntime;

public class JmxRuntimeFactoryTest {

    @Rule public DisposeRule disposer = new DisposeRule();

    @Test
    public void jmxRemoteConnect() throws Exception {
        TestThread.JMXProcess process = runRemoteSut();
        JmxRuntime runtime = new JmxRuntimeFactory().forRemoteProcess(JMX_HOST, process.JMX_PORT);
        assertThreadState(runtime);
    }

    @Test
    public void jmxRemoteConnectWithPasswd() throws Exception {
        TestThread.JMXProcess process = runRemoteSut(true);
        assertThreadState(new JmxRuntimeFactory().forRemoteProcess(JMX_HOST, process.JMX_PORT, JMX_USER, JMX_PASSWD));
        assertThreadState(new JmxRuntimeFactory().forConnectionString(process.JMX_AUTH_CONNECTION));
    }

    @Test(timeout = 10000) // Not using Timeout rule as that would not clean the test resources
    public void jmxRemoteConnectMissingPasswd() throws Exception {
        TestThread.JMXProcess process = runRemoteSut(true);
        try {
            new JmxRuntimeFactory().forRemoteProcess(JMX_HOST, process.JMX_PORT);
            fail();
        } catch (JmxRuntimeFactory.FailedToInitializeJmxConnection ex) {
            assertThat(ex.getMessage(), containsString("Credentials required"));
        }
    }

    @Test
    public void jmxRemoteConnectWithIncorrectPasswd() throws Exception {
        TestThread.JMXProcess process = runRemoteSut(true);
        try {
            new JmxRuntimeFactory().forRemoteProcess(JMX_HOST, process.JMX_PORT, JMX_USER, "incorrect_passwd");
            fail();
        } catch (JmxRuntimeFactory.FailedToInitializeJmxConnection ex) {
            assertThat(ex.getMessage(), containsString("Invalid username or password"));
        }
    }

    @Test(expected = JmxRuntimeFactory.FailedToInitializeJmxConnection.class)
    public void connectToNonexistingLocalProcess() {
        new JmxRuntimeFactory().forLocalProcess(299);
    }

    @Test(expected = JmxRuntimeFactory.FailedToInitializeJmxConnection.class)
    public void connectToNonexistingRemoteProcess() {
        new JmxRuntimeFactory().forRemoteProcess("localhost", 0);
    }

    private void assertThreadState(ProcessRuntime<?, ?, ?> runtime) {
        ProcessThread<?, ?, ?> actual = runtime.getThreads().where(nameIs("remotely-observed-thread")).onlyThread();
        StackTrace trace = actual.getStackTrace();

        assertThat(actual.getName(), equalTo("remotely-observed-thread"));
        assertThat(actual.getStatus(), equalTo(ThreadStatus.IN_OBJECT_WAIT));

        // Cannot do an exact match as different JDK versions have a slightly different #wait() call hierarchy
        assertThat(trace.getElements(), containsInRelativeOrder(
                frameOf("java.lang.Object", "wait", "Object.java"),
                frameOf("com.github.olivergondza.dumpling.TestThread$1", "run", "TestThread.java")
        ));
    }

    private TestThread.JMXProcess runRemoteSut() throws Exception {
        return runRemoteSut(false);
    }

    private TestThread.JMXProcess runRemoteSut(boolean auth) throws Exception {
        return disposer.register(TestThread.runJmxObservableProcess(auth));
    }
}
