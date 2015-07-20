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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class MainTest extends AbstractCliTest {

    @Test
    public void notEnoughArguments() {

        run();
        assertThat(err.toString(), containsString("Argument \"COMMAND\" is required"));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void noSuchCommand() {

        run("there_is_no_such_command");
        assertThat(err.toString(), containsString("Command \"there_is_no_such_command\" not found"));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void help() {

        run("help");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("Usage: "));
        assertThat(out.toString(), containsString("Available commands:"));
        assertThat(out.toString(), containsString("blocking-tree"));
        assertThat(this, succeeded());
    }

    @Test
    public void detailedHelp() {

        run("help", "deadlocks");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("dumpling.sh deadlocks"));
        assertThat(out.toString(), containsString("Detect cycles of blocked threads"));
        assertThat(this, succeeded());
    }

    @Test
    public void factoryWithoutKind() {

        run("deadlocks", "--in");
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
        assertThat(err.toString(), containsString("takes an operand KIND"));
    }

    @Test
    public void factoryWithoutLocator() {

        run("deadlocks", "--in", "threaddump");
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
        assertThat(err.toString(), containsString("takes an operand LOCATOR"));
    }
}
