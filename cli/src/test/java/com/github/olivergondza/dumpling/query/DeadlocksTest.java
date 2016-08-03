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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;

public class DeadlocksTest extends AbstractCliTest {

    private final String logPath = Util.asFile(Util.resource("jstack/deadlock.log")).getAbsolutePath();

    @Test
    public void cliQuery() throws Exception {
        run("deadlocks", "--in", "threaddump", logPath);
        assertThat(err.toString(), equalTo(""));
        assertListing(out.toString());

        assertThat(exitValue, equalTo(1));
    }

    @Test
    public void toStringNoTraces() throws Exception {
        ThreadDumpRuntime runtime = new ThreadDumpFactory().fromStream(Util.resource("jstack/deadlock.log"));
        assertListing(new Deadlocks().query(runtime.getThreads()).toString());
    }

    private void assertListing(String out) {
        assertThat(out, startsWith(Util.multiline("",
                "Monitor Deadlock #1:",
                "\"Handling POST /hudson/job/some_other_job/doRename : ajp-127.0.0.1-8009-24\" daemon prio=10 tid=1481750528 nid=27336",
                "\tWaiting to <0x40dce6960> (a hudson.model.ListView)",
                "\tAcquired   <0x40dce0d68> (a hudson.plugins.nested_view.NestedView)",
                "\tAcquired   <0x49c5f7990> (a hudson.model.FreeStyleProject)",
                "\tAcquired * <0x404325338> (a hudson.model.Hudson)",
                "\"Handling POST /hudson/view/some_view/configSubmit : ajp-127.0.0.1-8009-107\" daemon prio=10 tid=47091108077568 nid=17982",
                "\tWaiting to <0x404325338> (a hudson.model.Hudson)",
                "\tAcquired * <0x40dce6960> (a hudson.model.ListView)"
        )));
        assertThat(out, containsString("%nDeadlocks: 1%n"));
    }

    @Test
    public void cliQueryTraces() throws Exception {
        run("deadlocks", "--show-stack-traces", "--in", "threaddump", logPath);
        assertThat(err.toString(), equalTo(""));
        assertLongListing(out.toString());

        assertThat(exitValue, equalTo(1));
    }

    @Test
    public void toStringWithTraces() throws Exception {
        ThreadDumpRuntime runtime = new ThreadDumpFactory().fromStream(Util.resource("jstack/deadlock.log"));
        assertLongListing(new Deadlocks().showStackTraces().query(runtime.getThreads()).toString());
    }

    private void assertLongListing(String out) {
        assertThat(out, startsWith(Util.multiline("",
                "Monitor Deadlock #1:",
                "\"Handling POST /hudson/job/some_other_job/doRename : ajp-127.0.0.1-8009-24\" daemon prio=10 tid=1481750528 nid=27336",
                "\tWaiting to <0x40dce6960> (a hudson.model.ListView)",
                "\tAcquired   <0x40dce0d68> (a hudson.plugins.nested_view.NestedView)",
                "\tAcquired   <0x49c5f7990> (a hudson.model.FreeStyleProject)",
                "\tAcquired * <0x404325338> (a hudson.model.Hudson)",
                "\"Handling POST /hudson/view/some_view/configSubmit : ajp-127.0.0.1-8009-107\" daemon prio=10 tid=47091108077568 nid=17982",
                "\tWaiting to <0x404325338> (a hudson.model.Hudson)",
                "\tAcquired * <0x40dce6960> (a hudson.model.ListView)"
        )));
        assertThat(out, containsString("%nDeadlocks: 1%n"));
    }
}
