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
package com.github.olivergondza.dumpling.query;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;

public class TopContendersTest extends AbstractCliTest {

    private final String logPath = Util.asFile(Util.resource("jstack/contention.log")).getAbsolutePath();

    @Test
    public void cliQuery() throws Exception {
        run("top-contenders", "--in", "threaddump", logPath);
        assertThat(err.toString(), equalTo(""));
        assertListing(out.toString());
        assertThat(exitValue, equalTo(1)); // Number of blocking threads
    }

    @Test
    public void toStringNoTraces() throws Exception {
        ThreadDumpRuntime runtime = new ThreadDumpFactory().fromStream(Util.resource("jstack/contention.log"));
        assertListing(new TopContenders().query(runtime.getThreads()).toString());
    }

    private void assertListing(String out) {
        assertThat(out, containsString("%nBlocking threads: 1; Blocked threads: 3%n"));

        // Header
        assertThat(out, containsString("* \"producer\" prio=10 tid=0x7ff5a02e2000 nid=4567%n"));
        assertThat(out, containsString("%n  (1) \"consumerC\" prio=10 tid=0x7ff5a02ef000 nid=4570%n"));
        assertThat(out, containsString("%n  (2) \"consumerB\" prio=10 tid=0x7ff5a02ee000 nid=4569%n"));
        assertThat(out, containsString("%n  (3) \"consumerA\" prio=10 tid=0x7ff5a02e2800 nid=4568%n"));

        // No tread listing
        assertThat(out, not(containsString("%n\"producer\" prio=10 tid=0x7ff5a02e2000 nid=4567%n")));
        assertThat(out, not(containsString("%n\"consumerA\" prio=10 tid=0x7ff5a02e2800 nid=4568%n")));
        assertThat(out, not(containsString("%n\"consumerB\" prio=10 tid=0x7ff5a02ee000 nid=4569%n")));
        assertThat(out, not(containsString("%n\"consumerC\" prio=10 tid=0x7ff5a02ef000 nid=4570%n")));
    }

    @Test
    public void cliQueryTraces() throws Exception {
        run("top-contenders", "--show-stack-traces", "--in", "threaddump", logPath);
        assertThat(err.toString(), equalTo(""));
        assertLongListing(out.toString());
        assertThat(exitValue, equalTo(1)); // Number of blocking threads
    }

    @Test
    public void toStringWithTraces() throws Exception {
        ThreadDumpRuntime runtime = new ThreadDumpFactory().fromStream(Util.resource("jstack/contention.log"));
        assertLongListing(new TopContenders().showStackTraces().query(runtime.getThreads()).toString());
    }

    private void assertLongListing(String out) {
        assertThat(out, containsString("%nBlocking threads: 1; Blocked threads: 3%n"));

        // Header
        assertThat(out, containsString("* \"producer\" prio=10 tid=0x7ff5a02e2000 nid=4567%n"));
        assertThat(out, containsString("%n  (1) \"consumerC\" prio=10 tid=0x7ff5a02ef000 nid=4570%n"));
        assertThat(out, containsString("%n  (2) \"consumerB\" prio=10 tid=0x7ff5a02ee000 nid=4569%n"));
        assertThat(out, containsString("%n  (3) \"consumerA\" prio=10 tid=0x7ff5a02e2800 nid=4568%n"));

        // Thread listing
        assertThat(out, containsString("%n\"producer\" prio=10 tid=0x7ff5a02e2000 nid=4567%n"));
        assertThat(out, containsString("%n\"consumerA\" prio=10 tid=0x7ff5a02e2800 nid=4568%n"));
        assertThat(out, containsString("%n\"consumerB\" prio=10 tid=0x7ff5a02ee000 nid=4569%n"));
        assertThat(out, containsString("%n\"consumerC\" prio=10 tid=0x7ff5a02ef000 nid=4570%n"));
    }
}
