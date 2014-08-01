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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;

public class ProcessTheadTest {

    @Test
    public void printLocksOnCorrectPosionInStackTrace() throws Exception {
        ProcessRuntime runtime = new ThreadDumpFactory().fromFile(
                Util.resourceFile("producer-consumer.log")
        );

        String dump = runtime.getThreads().toString();

        assertThat(dump, containsString(slice(
                "at hudson.model.Queue.getItem(Queue.java:719)",
                "- waiting to lock <0x4063a9378> (a hudson.model.Queue)",
                "at hudson.model.AbstractProject.getQueueItem(AbstractProject.java:927)"
        )));

        assertThat(dump, containsString(slice(
                "at hudson.util.OneShotEvent.block(OneShotEvent.java:72)",
                "- locked <0x4063a9378> (a hudson.model.Queue)",
                "at hudson.model.Queue.pop(Queue.java:816)",
                "- locked <0x4063a9378> (a hudson.model.Queue)",
                "at hudson.model.Executor.grabJob(Executor.java:284)",
                "at hudson.model.Executor.run(Executor.java:205)",
                "- locked <0x4063a9378> (a hudson.model.Queue)"
        )));
    }

    private String slice(String... frames) {
        StringBuilder sb = new StringBuilder();
        for (String frame: frames) {
            sb.append('\t').append(frame).append('\n');
        }

        return sb.toString();
    }
}
