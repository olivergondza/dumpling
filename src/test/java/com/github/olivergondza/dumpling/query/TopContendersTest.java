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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

public class TopContendersTest extends AbstractCliTest {

    @Test
    public void trivial() throws Exception {
        ProcessRuntime runtime = new ThreadDumpFactory().fromFile(Util.resourceFile("producer-consumer.log"));

        Map<ProcessThread, ThreadSet> contenders = new TopContenders().getAll(runtime);

        Map<ProcessThread, ThreadSet> expected = new HashMap<ProcessThread, ThreadSet>();
        expected.put(
                runtime.getThreads().onlyNamed("owning_thread").onlyThread(),
                runtime.getThreads().onlyNamed("blocked_thread")
        );

        assertThat(contenders, equalTo(expected));
    }

    @Test
    public void contenders() throws Exception {
        ProcessRuntime runtime = new ThreadDumpFactory().fromFile(Util.resourceFile(getClass(), "contention.log"));

        Map<ProcessThread, ThreadSet> contenders = new TopContenders().getAll(runtime);

        ThreadSet ts = runtime.getThreads();
        Map<ProcessThread, ThreadSet> expected = new HashMap<ProcessThread, ThreadSet>();
        final ProcessThread producerProcessThread = ts.onlyNamed("producer").onlyThread();
        expected.put(
                producerProcessThread,
                ts.onlyNamed(Pattern.compile("consumer."))
        );

        assertThat(contenders, equalTo(expected));
        assertThat(contenders.size(), equalTo(1));
        assertThat(contenders.get(producerProcessThread).size(), equalTo(3));
    }
}
