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

import java.io.File;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.TestThread;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactoryTest;

/**
 * Test interoperability between groovy and groovysh command.
 *
 * @author ogondza
 */
@RunWith(Theories.class)
public class GroovyRuntimeTest extends AbstractCliTest {

    @DataPoints
    public static String[] commands() {
        return new String[] {"groovy", "groovysh"};
    }

    @Theory
    public void loadTreaddump(String command) throws Exception {
        File file = Util.resourceFile(ThreadDumpFactoryTest.class, "openjdk-1.7.0_60.log");
        stdin("$load.threaddump('" + file.getAbsolutePath() + "').threads.where(nameIs('main'));" + Util.NL);
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), isEmptyString());
        assertThat(out.toString(), containsString("\"main\""));
    }

    @Theory
    public void loadPid(String command) {
        thread = TestThread.runThread();
        stdin("$load.process(" + Util.currentPid() + ").threads.where(nameIs('remotely-observed-thread'));" + Util.NL);
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), isEmptyString());
        assertThat(out.toString(), containsString("\"remotely-observed-thread\""));
    }

    @Theory
    public void loadPidOverJmx(String command) {
        thread = TestThread.runThread();
        stdin("$load.jmx(" + Util.currentPid() + ").threads.where(nameIs('remotely-observed-thread'));" + Util.NL);
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), isEmptyString());
        assertThat(out.toString(), containsString("\"remotely-observed-thread\""));
    }

    @Theory
    public void loadJmx(String command) throws Exception {
        process = TestThread.runJmxObservableProcess(false);
        stdin("println $load.jmx('" + TestThread.JMX_CONNECTION + "').threads.where(nameIs('remotely-observed-thread'));" + Util.NL);
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), isEmptyString());
        assertThat(out.toString(), containsString("\"remotely-observed-thread\""));
    }

    @Theory
    public void loadSymbolsFromOtherDumplingPackages(String command) {
        stdin("new Deadlocks(); ThreadStatus.valueOf(0); new JvmRuntimeFactory(); new CommandFailedException('');" + Util.NL);
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(this, succeeded());
    }

    @Theory
    public void failTheScript(String command) {
        stdin("new ThereIsNoSuchClass();" + Util.NL);
        run(command);

        assertThat(err.toString(), containsString("unable to resolve class ThereIsNoSuchClass"));
    }

    @Theory
    public void printToOutAndErr(String command) {
        stdin("out.println('stdout content'); err.println('stderr content');" + Util.NL);
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), containsString("stderr content"));
        assertThat(out.toString(), containsString("stdout content"));
    }
}