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
package com.github.olivergondza.dumpling.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.PrintStream;
import java.util.HashSet;

import org.junit.Test;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

public class SingleThreadSetQueryTest {

    @Test
    public void noThreadsOutput() {
        String out = new NoThreads().toString();
        assertThat(out, equalTo(String.format("RESULT%n%n%nSUMMARY%n")));
    }

    @Test
    public void threadsOutput() {
        String out = new Threads().toString();
        assertThat(out, equalTo(String.format("RESULT%n%n%n\"THREAD_NAME\" #42%n   java.lang.Thread.State: UNKNOWN%n%n%nSUMMARY%n")));
    }

    private static final class NoThreads extends SingleThreadSetQuery.Result {

        @Override
        protected void printResult(PrintStream out) {
            out.println("RESULT");
        }

        @Override
        protected ThreadSet involvedThreads() {
            return null;
        }

        @Override
        protected void printSummary(PrintStream out) {
            out.println("SUMMARY");
        }
    }

    private static final class Threads extends SingleThreadSetQuery.Result {

        @Override
        protected void printResult(PrintStream out) {
            out.println("RESULT");
        }

        @Override
        protected ThreadSet involvedThreads() {
            HashSet<ProcessThread.Builder> threads = new HashSet<ProcessThread.Builder>();
            threads.add(ProcessThread.builder().setName("THREAD_NAME").setId(42));
            return new ProcessRuntime(threads).getThreads();
        }

        @Override
        protected void printSummary(PrintStream out) {
            out.println("SUMMARY");
        }
    }
}
