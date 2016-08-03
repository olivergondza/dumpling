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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;

public class GrepCommandTest extends AbstractCliTest {

    @Test
    public void cli() throws Exception {
        final String log = Util.asFile(Util.resource("jstack/producer-consumer.log")).getAbsolutePath();

        run("grep", "thread.name == 'blocked_thread'", "--in", "threaddump:" + log);
        assertThat(this, succeeded());
        assertThat(err.toString(), equalTo(String.format("Threads: 1%n")));

        assertThat(out.toString(), containsString("\"blocked_thread\""));
        assertThat(out.toString(), not(containsString("\"owning_thread\"")));

        run("grep", "false", "--in", "threaddump:" + log);
        assertThat(exitValue, equalTo(1));
        assertThat(err.toString(), equalTo(String.format("Threads: 0%n")));

        assertThat(out.toString(), not(containsString("\"blocked_thread\"")));
        assertThat(out.toString(), not(containsString("\"owning_thread\"")));

        run("grep", "--in", "threaddump:" + log);
        assertThat(exitValue, equalTo(-1));
    }

    @Test
    public void porcelain() throws Exception {
        final String log = Util.asFile(Util.resource("jstack/producer-consumer.log")).getAbsolutePath();

        run("grep", "thread.status.blocked", "--in", "threaddump", log);
        assertThat(out.toString(), containsString("\"blocked_thread\" prio=10 tid=47088345200640 nid=32297"));
        assertThat(out.toString(), containsString("- waiting to lock <0x4063a9378> (a hudson.model.Queue)"));

        run("grep", "thread.status.blocked", "--in", "threaddump", log, "--porcelain");
        assertThat(out.toString(), containsString("\"blocked_thread\" prio=10 tid=0x00002ad39c16b800 nid=0x7e29"));
        assertThat(out.toString(), containsString("- waiting to lock <0x00000004063a9378> (a hudson.model.Queue)"));
    }

    @Test
    public void  hasHelp() {
        run("help", "grep");
        assertThat(out.toString(), containsString("Thread to be examined in the predicate is named 'thread'"));
    }
}
