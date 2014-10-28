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

import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static com.github.olivergondza.dumpling.model.StackTrace.element;
import static com.github.olivergondza.dumpling.model.StackTrace.nativeElement;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.junit.After;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory.RemoteConnector;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.StackTrace;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class JmxRuntimeFactoryTest {

    private Process process;

    @After
    public void after() {
        if (process != null) process.destroy();
    }

    @Test
    public void parseRemoteLogin() {
        RemoteConnector login = new RemoteConnector("localhost:8080");
        assertThat(login.host, equalTo("localhost"));
        assertThat(login.port, equalTo(8080));
        assertThat(login.username, equalTo(null));
        assertThat(login.password, equalTo(null));

        login = new RemoteConnector("user:passwd@localhost:8080");
        assertThat(login.host, equalTo("localhost"));
        assertThat(login.port, equalTo(8080));
        assertThat(login.username, equalTo("user"));
        assertThat(login.password, equalTo("passwd"));
    }

    @Test
    public void jmxRemoteConnect() throws IOException {
        runRemoteSut();
        ProcessRuntime runtime = new JmxRuntimeFactory().forRemoteProcess("localhost", 9876);
        ProcessThread actual = runtime.getThreads().where(nameIs("remotely-observed-thread")).onlyThread();
        StackTrace trace = actual.getStackTrace();

        assertThat(actual.getName(), equalTo("remotely-observed-thread"));
        assertThat(actual.getStatus(), equalTo(ThreadStatus.IN_OBJECT_WAIT));
        // TODO priority
        // TODO daemon
        assertThat(trace.getElement(0), equalTo(nativeElement("java.lang.Object", "wait", "Object.java")));
        assertThat(trace.getElement(1), equalTo(element("java.lang.Object", "wait", "Object.java", 503)));
        assertThat(trace.getElement(2), equalTo(element(
                "com.github.olivergondza.dumpling.factory.JmxTestProcess$1", "run", "JmxTestProcess.java", 42
        )));
    }

    @Test
    public void jmxLocalConnect() throws IOException {
        runLocalSut();
        ProcessRuntime runtime = new JmxRuntimeFactory().forLocalProcess(Util.currentPid());
        ProcessThread actual = runtime.getThreads().where(nameIs("remotely-observed-thread")).onlyThread();
        StackTrace trace = actual.getStackTrace();

        assertThat(actual.getName(), equalTo("remotely-observed-thread"));
        assertThat(actual.getStatus(), equalTo(ThreadStatus.IN_OBJECT_WAIT));
        // TODO priority
        // TODO daemon
        assertThat(trace.getElement(0), equalTo(nativeElement("java.lang.Object", "wait", "Object.java")));
        assertThat(trace.getElement(1), equalTo(element("java.lang.Object", "wait", "Object.java", 503)));
        assertThat(trace.getElement(2), equalTo(element(
                "com.github.olivergondza.dumpling.factory.JmxTestProcess$1", "run", "JmxTestProcess.java", 42
        )));
    }

    private void runLocalSut() {
        JmxTestProcess.runThread();
    }

    private void runRemoteSut() throws IOException {
        String[] args = {
                "java", "-cp", "target/test-classes:target/classes",
                "-Dcom.sun.management.jmxremote",
                "-Dcom.sun.management.jmxremote.port=9876",
                "-Dcom.sun.management.jmxremote.local.only=false",
                "-Dcom.sun.management.jmxremote.authenticate=false",
                "-Dcom.sun.management.jmxremote.ssl=false",
                "com.github.olivergondza.dumpling.factory.JmxTestProcess"
        };

        this.process = new ProcessBuilder(args)
                .redirectError(Redirect.INHERIT)
                .redirectOutput(Redirect.INHERIT)
                .start()
        ;

        Util.pause(1000);
    }
}
