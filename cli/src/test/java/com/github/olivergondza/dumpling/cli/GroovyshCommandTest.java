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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;

public class GroovyshCommandTest extends AbstractCliTest {

    private final String logPath = Util.asFile(Util.resource("jstack/deadlock.log")).getAbsolutePath();

    @Test
    public void load() {
        stdin("D.load.threaddump(D.args[0]).threads.size();%n");
        run("groovysh", logPath);

        assertThat(out.toString(), containsString(" 18%n"));
        assertThat(this, succeeded());
    }

    @Test
    public void useInputRuntime() {
        stdin("D.runtime.threads.size()%n");
        run("groovysh", "--in", "threaddump", logPath);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString(" 18%n"));
        assertThat(this, succeeded());
    }

    @Test
    public void  hasHelp() {
        run("help", "groovysh");
        assertThat(out.toString(), containsString("All Dumpling DSL classes and methods are imported"));
    }
}
