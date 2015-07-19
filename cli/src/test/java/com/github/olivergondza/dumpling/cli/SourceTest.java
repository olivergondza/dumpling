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

import static com.github.olivergondza.dumpling.TestThread.JMX_CONNECTION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;

public class SourceTest extends AbstractCliTest {
    @Test
    public void cliNoSuchFile() {
        run("deadlocks", "--in", "threaddump", "/there_is_no_such_file");
        assertThat(exitValue, equalTo(-1));
        assertThat(err.toString(), containsString("/there_is_no_such_file (No such file or directory)"));
        assertThat(out.toString(), equalTo(""));
    }

    @Test
    public void jmxRemoteConnectViaCli() throws Exception {
        runRemoteSut();
        stdin("runtime.threads.where(nameIs('remotely-observed-thread'))");
        run("groovy", "--in", "jmx", JMX_CONNECTION);

        // Reuse verification logic re-parsing the output as thread dump
        ThreadDumpRuntime reparsed = new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
        assertThreadState(reparsed);
    }

    @Test
    public void jmxLocalConnectViaCli() {
        runLocalSut();
        stdin("runtime.threads.where(nameIs('remotely-observed-thread'))");
        run("groovy", "--in", "jmx", Integer.toString(Util.currentPid()));

        // Reuse verification logic re-parsing the output as thread dump
        ThreadDumpRuntime reparsed = new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
        assertThreadState(reparsed);
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
}
