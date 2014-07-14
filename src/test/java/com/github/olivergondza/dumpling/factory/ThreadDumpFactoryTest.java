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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class ThreadDumpFactoryTest extends AbstractCliTest {

    @Test
    public void openJdk7() throws Exception {

        ThreadSet threads = runtimeFrom("openjdk-1.7.0_60.log").getThreads();

        assertEquals(35, threads.size());

        ProcessThread main = threads.onlyNamed("main").onlyThread();
        assertEquals(ThreadStatus.RUNNABLE, main.getThreadStatus());
        StackTraceElement[] trace = main.getStackTrace();
        assertEquals(27, trace.length);

        assertEquals("org.eclipse.swt.internal.gtk.OS", trace[0].getClassName());
        assertEquals("Call", trace[0].getMethodName());
        assertEquals(null, trace[0].getFileName());
        assertEquals(-2, trace[0].getLineNumber());

        assertEquals("org.eclipse.swt.widgets.Display", trace[1].getClassName());
        assertEquals("sleep", trace[1].getMethodName());
        assertEquals("Display.java", trace[1].getFileName());
        assertEquals(4233, trace[1].getLineNumber());
    }

    @Test
    public void oracleJdk7() throws Exception {

        ThreadSet threads = runtimeFrom("oraclejdk-1.7.0_51.log").getThreads();

        assertEquals(143, threads.size());

        ProcessThread thread = threads.onlyNamed("Channel reader thread: jenkins_slave_02").onlyThread();
        assertEquals(ThreadStatus.RUNNABLE, thread.getThreadStatus());
        StackTraceElement[] trace = thread.getStackTrace();
        assertEquals(13, trace.length);

        assertEquals("java.io.FileInputStream", trace[0].getClassName());
        assertEquals("readBytes", trace[0].getMethodName());
        assertEquals(null, trace[0].getFileName());
        assertEquals(-2, trace[0].getLineNumber());

        StackTraceElement lastTrace = trace[trace.length - 1];
        assertEquals("hudson.remoting.SynchronousCommandTransport$ReaderThread", lastTrace.getClassName());
        assertEquals("run", lastTrace.getMethodName());
        assertEquals("SynchronousCommandTransport.java", lastTrace.getFileName());
        assertEquals(48, lastTrace.getLineNumber());
    }

    @Test
    public void lockRelationshipsShouldBePreserved() throws Exception {

        ThreadSet threads = new ThreadDumpFactory().fromFile(Util.resourceFile("producer-consumer.log")).getThreads();

        ProcessThread blocked = threads.onlyNamed("blocked_thread").onlyThread();
        ProcessThread owning = threads.onlyNamed("owning_thread").onlyThread();

        assertTrue(owning.getBlockingThreads().isEmpty());
        assertEquals(threads.onlyNamed("blocked_thread"), owning.getBlockedThreads());

        assertEquals(threads.onlyNamed("owning_thread"), blocked.getBlockingThreads());
        assertTrue(blocked.getBlockedThreads().isEmpty());
    }

    @Test
    public void cliNoSuchFile() {
        run("detect-deadlocks", "--in", "threaddump", "/there_is_no_such_file");
        assertThat(exitValue, equalTo(-1));
        assertThat(err.toString(), containsString("/there_is_no_such_file (No such file or directory)"));
        assertThat(out.toString(), equalTo(""));
    }

    private ProcessRuntime runtimeFrom(String resource) throws IOException, URISyntaxException {
        return new ThreadDumpFactory().fromFile(Util.resourceFile(getClass(), resource));
    }
}
