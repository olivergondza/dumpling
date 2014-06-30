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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class ThreadDumpFactoryTest {

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

    private ProcessRuntime runtimeFrom(String resource) throws IOException, URISyntaxException {
        return new ThreadDumpFactory().fromFile(traceFile("ThreadDumpFactoryTest/" + resource));
    }

    private File traceFile(String resource) throws URISyntaxException {
        URL res = getClass().getResource(resource);
        return new File(res.toURI());
    }
}
