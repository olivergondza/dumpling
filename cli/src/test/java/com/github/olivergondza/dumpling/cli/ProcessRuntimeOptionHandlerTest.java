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

import org.hamcrest.Matchers;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;

public class ProcessRuntimeOptionHandlerTest extends AbstractCliTest {

    @Test
    public void readStdin() throws Exception {
        stdin(Util.asString(Util.resource("jstack/deadlock.log")));
        exitValue = run("deadlocks", "--in", "threaddump", "-");

        assertThat(err.toString(), isEmptyString());
        assertThat(exitValue, equalTo(1)); // One deadlock
    }

    @Test
    public void compatibility() throws Exception {
        String path = Util.asFile(Util.resource("jstack/blocking-chain.log")).getAbsolutePath();
        exitValue = run("threaddump", "--in", "threaddump", path, "-p");
        assertThat(this, succeeded());
        String expected = out.toString();

        exitValue = run("threaddump", "--in", "threaddump:" + path, "-p");
        assertThat(this, succeeded());
        String actual = out.toString();

        assertThat(expected, equalTo(actual));
    }
}
