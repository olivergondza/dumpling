/*
 * The MIT License
 *
 * Copyright (c) Red Hat, Inc.
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

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kohsuke.args4j.CmdLineException;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

/**
 * @author ogondza.
 */
public class SampleCommandTest extends AbstractCliTest {

    public @Rule TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void once() throws Exception {
        run("sample", "-n" , "1", "threaddump", "--in", "process:" + Util.currentPid());

        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));

        new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void outToFiles() throws Exception {
        File destination = tmp.newFolder();
        run(
                "sample", "--number" , "3", "--out", destination.getAbsolutePath() + "/jstack",
                "threaddump", "--in", "process:" + Util.currentPid()
        );

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));

        String[] outs = destination.list();
        assertThat(outs, arrayWithSize(3));
        for (String j : outs) {
            assertThat(j, startsWith("jstack"));
            new ThreadDumpFactory().fromFile(new File(destination, j));
        }
    }

    @Test
    public void brokenCommand() throws Exception {
        run("sample", "-n" , "1", "Whoa?");
        assertThat(err.toString(), containsString("Command \"Whoa?\" not found"));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void brokenNumber() throws Exception {
        run("sample", "-n", "0", "help");
        assertThat(err.toString(), containsString("Number of invocations must be positive. 0 given."));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void parseInterval() throws Exception {
        SampleCommand sc = new SampleCommand();
        sc.setInterval("50");
        assertThat(sc.interval, equalTo(50000L));

        sc.setInterval("1s");
        assertThat(sc.interval, equalTo(1000L));

        sc.setInterval("100ms");
        assertThat(sc.interval, equalTo(100L));

        sc.setInterval("5m");
        assertThat(sc.interval, equalTo(1000L * 60 * 5));

        sc.setInterval("1h");
        assertThat(sc.interval, equalTo(1000L * 60 * 60));

        try {
            sc.setInterval("1d");
        } catch (CmdLineException ex) {

        }
    }

    @Test
    public void mixedArgsShouldReportProblemsForCorrectCommand() throws Exception {
        run("sample", "-n" , "1", "threaddump", "-o", "asdf");
        assertThat(out.toString(), equalTo(""));
        assertThat(err.toString(), not(containsString("Run dumpling command periodically")));
        assertThat(err.toString(), containsString("Print runtime as string"));
    }
}
