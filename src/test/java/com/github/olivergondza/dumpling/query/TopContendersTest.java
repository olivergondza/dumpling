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

import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;
import com.github.olivergondza.dumpling.query.TopContenders.Result;

public class TopContendersTest extends AbstractCliTest {

    @Test
    public void trivial() throws Exception {
        ProcessRuntime runtime = new ThreadDumpFactory().fromFile(Util.resourceFile("producer-consumer.log"));

        Result contenders = runtime.query(new TopContenders());

        final ProcessThread owning = runtime.getThreads().where(nameIs("owning_thread")).onlyThread();

        assertThat(contenders.getBlockers().size(), equalTo(1));
        assertThat(contenders.blockedBy(owning), equalTo(runtime.getThreads().where(nameIs("blocked_thread"))));
    }

    @Test
    public void contenders() throws Exception {
        ProcessRuntime runtime = new ThreadDumpFactory().fromFile(Util.resourceFile(getClass(), "contention.log"));

        Result contenders = runtime.query(new TopContenders());

        ThreadSet ts = runtime.getThreads();
        final ProcessThread producerProcessThread = ts.where(nameIs("producer")).onlyThread();

        assertThat(contenders.getBlockers().size(), equalTo(1));
        assertThat(contenders.blockedBy(producerProcessThread).size(), equalTo(3));
    }

    @Test
    public void cliQuery() throws Exception {
        run("top-contenders", "--in", "threaddump", Util.resourceFile(getClass(), "contention.log").getAbsolutePath());
        assertThat(err.toString(), equalTo(""));
        assertListing(out.toString());
        assertThat(exitValue, equalTo(1)); // Number of blocking threads
    }

    @Test
    public void toStringNoTraces() throws Exception {
        ProcessRuntime runtime = new ThreadDumpFactory().fromFile(Util.resourceFile(getClass(), "contention.log"));
        assertListing(runtime.query(new TopContenders()).toString());
    }

    private void assertListing(String out) {
        assertThat(out, containsString("\nBlocking threads: 1; Blocked threads: 3\n"));

        // Header
        assertThat(out, containsString("* \"producer\" prio=10 tid=140692931092480 nid=4567\n"));
        assertThat(out, containsString("\n  (1) \"consumerC\" prio=10 tid=140692931145728 nid=4570\n"));
        assertThat(out, containsString("\n  (2) \"consumerB\" prio=10 tid=140692931141632 nid=4569\n"));
        assertThat(out, containsString("\n  (3) \"consumerA\" prio=10 tid=140692931094528 nid=4568\n"));

        // No tread listing
        assertThat(out, not(containsString("\n\"producer\" prio=10 tid=140692931092480 nid=4567\n")));
        assertThat(out, not(containsString("\n\"consumerA\" prio=10 tid=140692931094528 nid=4568\n")));
        assertThat(out, not(containsString("\n\"consumerB\" prio=10 tid=140692931141632 nid=4569\n")));
        assertThat(out, not(containsString("\n\"consumerC\" prio=10 tid=140692931145728 nid=4570\n")));
    }

    @Test
    public void cliQueryTraces() throws Exception {
        run("top-contenders", "--show-stack-traces", "--in", "threaddump", Util.resourceFile(getClass(), "contention.log").getAbsolutePath());
        assertThat(err.toString(), equalTo(""));
        assertLongListing(out.toString());
        assertThat(exitValue, equalTo(1)); // Number of blocking threads
    }

    @Test
    public void toStringWithTraces() throws Exception {
        ProcessRuntime runtime = new ThreadDumpFactory().fromFile(Util.resourceFile(getClass(), "contention.log"));
        assertLongListing(runtime.query(new TopContenders().showStackTraces()).toString());
    }

    private void assertLongListing(String out) {
        assertThat(out, containsString("\nBlocking threads: 1; Blocked threads: 3\n"));

        // Header
        assertThat(out, containsString("* \"producer\" prio=10 tid=140692931092480 nid=4567\n"));
        assertThat(out, containsString("\n  (1) \"consumerC\" prio=10 tid=140692931145728 nid=4570\n"));
        assertThat(out, containsString("\n  (2) \"consumerB\" prio=10 tid=140692931141632 nid=4569\n"));
        assertThat(out, containsString("\n  (3) \"consumerA\" prio=10 tid=140692931094528 nid=4568\n"));

        // Thread listing
        assertThat(out, containsString("\n\"producer\" prio=10 tid=140692931092480 nid=4567\n"));
        assertThat(out, containsString("\n\"consumerA\" prio=10 tid=140692931094528 nid=4568\n"));
        assertThat(out, containsString("\n\"consumerB\" prio=10 tid=140692931141632 nid=4569\n"));
        assertThat(out, containsString("\n\"consumerC\" prio=10 tid=140692931145728 nid=4570\n"));
    }
}
