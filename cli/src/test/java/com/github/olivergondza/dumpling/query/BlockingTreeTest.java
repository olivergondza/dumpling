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

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;

public class BlockingTreeTest extends AbstractCliTest {

    private String logPath;
    private ThreadDumpRuntime runtime;

    @Before
    public void setUp() {
        logPath = Util.asFile(Util.resource("jstack/blocking-tree.log")).getAbsolutePath();
        runtime = new ThreadDumpFactory().fromStream(Util.resource("jstack/blocking-tree.log"));
    }

    @Test
    public void cliQuery() {
        run("blocking-tree", "--in", "threaddump:" + logPath);
        assertThat(err.toString(), equalTo(""));

        assertQueryListing(out.toString());
    }

    @Test
    public void toStringNoTraces() {
        assertQueryListing(new BlockingTree().query(runtime.getThreads()).toString());
    }

    private void assertQueryListing(String out) {
        // Roots
        assertThat(out, containsString("\"a\""));
        assertThat(out, containsString("%n\"b\""));

        // Blocked by roots
        assertThat(out, containsString("%n\t\"aa\""));
        assertThat(out, containsString("%n\t\"ab\""));
        assertThat(out, containsString("%n\t\"ba\""));

        // Deeply nested
        assertThat(out, containsString("%n\t\t\"aaa\""));

        assertThat(out, not(containsString("%n\"aaa\" prio=10 tid=0x7f416030e800 nid=31957%n")));
    }

    @Test
    public void cliQueryTraces() {
        run("blocking-tree", "--show-stack-traces", "--in", "threaddump:" + logPath);
        assertThat(err.toString(), equalTo(""));

        final String stdout = out.toString();
        assertLongQueryListing(stdout);
    }

    @Test
    public void toStringTraces() {
        assertLongQueryListing(new BlockingTree().showStackTraces().query(runtime.getThreads()).toString());
    }

    private void assertLongQueryListing(final String out) {
        // Roots
        assertThat(out, containsString("\"a\""));
        assertThat(out, containsString("%n\"b\""));

        // Blocked by roots
        assertThat(out, containsString("%n\t\"aa\""));
        assertThat(out, containsString("%n\t\"ab\""));
        assertThat(out, containsString("%n\t\"ba\""));

        // Deeply nested
        assertThat(out, containsString("%n\t\t\"aaa\""));

        assertThat(out, containsString("%n\"aaa\" prio=10 tid=0x7f416030e800 nid=31957%n"));
    }
}
