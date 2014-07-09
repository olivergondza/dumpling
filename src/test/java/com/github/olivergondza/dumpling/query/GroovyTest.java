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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URISyntaxException;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;

public class GroovyTest extends AbstractCliTest {

    @Test
    public void executeScript() throws URISyntaxException {
        stdin("print runtime.threads.size()");
        run("groovy", "--in", "threaddump", Util.resourceFile(getClass(), "jstack.log").getAbsolutePath());
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));
        assertThat(out.toString(), equalTo("8"));
    }

    @Test
    public void filter() throws URISyntaxException {
        stdin("print runtime.threads.onlyNamed('owning_thread').collect { it.name }");
        run("groovy", "--in", "threaddump", Util.resourceFile(getClass(), "jstack.log").getAbsolutePath());
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));
        assertThat(out.toString(), equalTo("[owning_thread]"));
    }

    @Test
    public void loadSymbolsFromOtherDumplingPackages() throws URISyntaxException {
        stdin("new DeadlockDetector(); new ThreadLock('', 0); new JvmRuntimeFactory(); new CommandFailedException('')");
        run("groovy", "--in", "threaddump", Util.resourceFile(getClass(), "jstack.log").getAbsolutePath());
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void failTheScript() throws URISyntaxException {
        stdin("new ThereIsNoSuchClass()");
        run("groovy", "--in", "threaddump", Util.resourceFile(getClass(), "jstack.log").getAbsolutePath());
        assertThat(err.toString(), containsString("dumpling-script: 1: unable to resolve class ThereIsNoSuchClass"));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
    }
}
