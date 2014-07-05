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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

public class MainTest {

    private ByteArrayOutputStream err = new ByteArrayOutputStream();
    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private int exitValue;

    @Test
    public void notEnoughArguments() {

        run();
        assertThat(exitValue, not(equalTo(0)));
        assertThat(out.toString(), equalTo(""));
        assertThat(err.toString(), containsString("Argument \"COMMAND\" is required"));
    }

    @Test
    public void noSuchCommand() {

        run("there_is_no_such_command");
        assertThat(exitValue, not(equalTo(0)));
        assertThat(out.toString(), equalTo(""));
        assertThat(err.toString(), containsString("Command \"there_is_no_such_command\" not found"));
    }

    @Test
    public void help() {

        run("help");
        assertThat(exitValue, equalTo(0));
        assertThat(out.toString(), containsString("Usage: "));
        assertThat(out.toString(), containsString("Available commands:"));
        assertThat(out.toString(), containsString("detect-deadlocks"));
        assertThat(err.toString(), equalTo(""));
    }

    @Test
    public void detailedHelp() {

        run("help", "detect-deadlocks");
        assertThat(exitValue, equalTo(0));
        assertThat(out.toString(), containsString("java -jar dumpling.jar detect-deadlocks"));
        assertThat(out.toString(), containsString("Detect cycles of blocked threads"));
        assertThat(err.toString(), equalTo(""));
    }

    private int run(String... args) {
        return exitValue = new Main().run(args, null, new PrintStream(out), new PrintStream(err));
    }
}
