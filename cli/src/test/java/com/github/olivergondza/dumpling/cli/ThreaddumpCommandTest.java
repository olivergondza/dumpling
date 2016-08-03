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

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;

public class ThreaddumpCommandTest extends AbstractCliTest {

    @Test
    public void cli() throws Exception {
        run("threaddump", "--in", "threaddump", Util.asFile(Util.resource("jstack/deadlock.log")).getAbsolutePath());
        assertThat(this, succeeded());
        assertThat(err.toString(), equalTo(""));

        String parsed = out.toString();
        stdin(parsed);
        run("threaddump", "--in", "threaddump", "-");
        assertThat(this, succeeded());
        String reparsed = out.toString();

        assertThat(parsed, equalTo(reparsed));
        assertThat(parsed, containsString("\"Handling GET /hudson/job/some_job/ : ajp-127.0.0.1-8009-133\" daemon prio=10 tid=47091150252032 nid=18245"));
    }

    @Test
    public void porcelain() throws Exception {
        final String log = Util.asFile(Util.resource("jstack/producer-consumer.log")).getAbsolutePath();

        run("threaddump", "--in", "threaddump", log);
        assertThat(this, succeeded());
        assertThat(out.toString(), containsString("\"blocked_thread\" prio=10 tid=47088345200640 nid=32297"));
        assertThat(out.toString(), containsString("- waiting to lock <0x4063a9378> (a hudson.model.Queue)"));

        run("threaddump", "--in", "threaddump", log, "--porcelain");
        assertThat(this, succeeded());
        assertThat(out.toString(), containsString("\"blocked_thread\" prio=10 tid=0x00002ad39c16b800 nid=0x7e29"));
        assertThat(out.toString(), containsString("- waiting to lock <0x00000004063a9378> (a hudson.model.Queue)"));
    }

    @Test
    public void  hasHelp() {
        run("help", "threaddump");
        assertThat(out.toString(), containsString("Allows to capture threaddump from factory such as jmx or process for later analysis"));
    }
}
