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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.PrintWriter;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;

public class GroovyCommandTest extends AbstractCliTest {

    @Test
    public void runScriptThatDoesNotExist() {
        run("groovy", "--script", "no_such_file");
        assertThat(out.toString(), equalTo(""));
        assertThat(err.toString(), equalTo("no_such_file (No such file or directory)\n"));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void runScriptThatDoesExist() throws Exception {
        File file = File.createTempFile("dumpling-GroovyCommandTest", "script");
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.println("println 'groovy out'; return 42;");
            writer.close();

            run("groovy", "--script", file.getAbsolutePath());
            assertThat(out.toString(), equalTo("groovy out\n42\n"));
            assertThat(err.toString(), equalTo(""));
            assertThat(exitValue, equalTo(42));
        } finally {
            file.delete();
        }
    }

    @Test
    public void porcelain() throws Exception {
        final String log = Util.resourceFile("producer-consumer.log").getAbsolutePath();

        stdin("D.runtime.threads%n");
        run("groovy", "--in", "threaddump", log);
        assertThat(this, succeeded());
        assertThat(out.toString(), containsString("\"blocked_thread\" prio=10 tid=47088345200640 nid=32297"));
        assertThat(out.toString(), containsString("- waiting to lock <0x4063a9378> (a hudson.model.Queue)"));

        stdin("D.runtime.threads%n");
        run("groovy", "--in", "threaddump", log, "--porcelain");
        assertThat(this, succeeded());
        assertThat(out.toString(), containsString("\"blocked_thread\" prio=10 tid=0x00002ad39c16b800 nid=0x7e29"));
        assertThat(out.toString(), containsString("- waiting to lock <0x00000004063a9378> (a hudson.model.Queue)"));
    }
}
