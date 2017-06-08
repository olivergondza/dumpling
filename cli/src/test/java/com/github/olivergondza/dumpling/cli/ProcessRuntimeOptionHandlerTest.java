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
package com.github.olivergondza.dumpling.cli;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import org.hamcrest.Matchers;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;

import java.io.ByteArrayInputStream;

public class ProcessRuntimeOptionHandlerTest extends AbstractCliTest {

    @Test
    public void readStdin() throws Exception {
        stdin(Util.asString(Util.resource("jstack/deadlock.log")));
        exitValue = run("deadlocks", "--in", "threaddump:-");

        assertThat(err.toString(), isEmptyString());
        assertThat(exitValue, equalTo(1)); // One deadlock
    }

    @Test
    public void inferSourcePid() throws Exception {
        run("threaddump", "--in", String.valueOf(Util.currentPid()));
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));

        new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void inferSourceFile() throws Exception {
        String path = Util.asFile(Util.resource("jstack/producer-consumer.log")).getAbsolutePath();
        run("threaddump", "--in", String.valueOf(path));
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));

        new ThreadDumpFactory().fromStream(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void inferNonsense() throws Exception {
        run("threaddump", "--in", "nonono");
        assertThat(err.toString(), startsWith("Unable to infer source from: nonono"));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void infer2ArgForm() throws Exception {
        run("threaddump", "--in", "threaddump", "value");
        assertThat(err.toString(), startsWith("Source format mismatch. Expected threaddump:LOCATOR"));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
    }
}
