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

import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.github.olivergondza.dumpling.DisposeRule;
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

    @Rule public DisposeRule disposer = new DisposeRule();

    @DataPoints
    public static String[] commands() {
        return new String[] {"groovy", "groovysh"};
    }

    @Theory
    public void executeScript(String command) {
        stdin("println D.load.jvm.threads.size() instanceof Integer;%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(this, succeeded());
        assertThat(out.toString().trim(), containsString("true"));
    }

    @Theory
    public void filter(String command) throws Exception {
        stdin("D.load.threaddump('" + Util.resourceFile("producer-consumer.log") + "').threads.where(nameIs('owning_thread')).collect { it.name };%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(this, succeeded());
        assertThat(out.toString().trim(), containsString("[owning_thread]"));
    }

    @Theory
    public void loadTreaddump(String command) throws Exception {
        assertLoadThreaddump(command, "D.load.threaddump('%s').threads.where(nameIs('main'));%n");
    }

    @Theory
    public void loadTreaddumpDolar(String command) throws Exception {
        assertLoadThreaddump(command, "$load.threaddump('%s').threads.where(nameIs('main'));%n");
    }

    private void assertLoadThreaddump(String command, String script) throws Exception {
        File file = Util.resourceFile(ThreadDumpFactoryTest.class, "openjdk-1.7.0_60.log");
        stdin(String.format(script, file.getAbsolutePath()));
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), isEmptyString());
        assertThat(out.toString(), containsString("\"main\""));
    }

    @Theory
    public void loadPid(String command) {
        assertLoadPid(command, "D.load.process(%d).threads.where(nameIs('remotely-observed-thread'));%n");
    }

    @Theory
    public void loadPidDolar(String command) {
        assertLoadPid(command, "$load.process(%d).threads.where(nameIs('remotely-observed-thread'));%n");
    }

    private void assertLoadPid(String command, String script) {
        disposer.register(TestThread.runThread());
        stdin(String.format(script, Util.currentPid()));
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), isEmptyString());
        assertThat(out.toString(), containsString("\"remotely-observed-thread\""));
    }

    @Theory
    public void loadPidOverJmx(String command) {
        assertLoadPidOverJmx(command, "D.load.jmx(%d).threads.where(nameIs('remotely-observed-thread'));%n");
    }

    @Theory
    public void loadPidOverJmxDolar(String command) {
        assertLoadPidOverJmx(command, "$load.jmx(%d).threads.where(nameIs('remotely-observed-thread'));%n");
    }

    private void assertLoadPidOverJmx(String command, String script) {
        disposer.register(TestThread.runThread());
        stdin(String.format(script, Util.currentPid()));
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), isEmptyString());
        assertThat(out.toString(), containsString("\"remotely-observed-thread\""));
    }

    @Theory
    public void loadJmx(String command) throws Exception {
        assertLoadJmx(command, "println D.load.jmx('%s').threads.where(nameIs('remotely-observed-thread'));%n");
    }

    @Theory
    public void loadJmxDolar(String command) throws Exception {
        assertLoadJmx(command, "println $load.jmx('%s').threads.where(nameIs('remotely-observed-thread'));%n");
    }

    private void assertLoadJmx(String command, String script) throws Exception {
        disposer.register(TestThread.runJmxObservableProcess(false));
        stdin(String.format(script, TestThread.JMX_CONNECTION));
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), isEmptyString());
        assertThat(out.toString(), containsString("\"remotely-observed-thread\""));
    }

    @Theory
    public void loadSymbolsFromOtherDumplingPackages(String command) {
        stdin("new Deadlocks(); ThreadStatus.valueOf(0); new JvmRuntimeFactory(); new CommandFailedException('');%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(this, succeeded());
    }

    @Theory
    public void failTheScript(String command) {
        stdin("new ThereIsNoSuchClass();%n");
        run(command);

        assertThat(err.toString(), containsString("unable to resolve class ThereIsNoSuchClass"));
    }

    @Theory
    public void printToOutAndErr(String command) {
        stdin("out.println('stdout content'); err.println('stderr content');%n");
        run(command);

        assertThat(exitValue, equalTo(0));
        assertThat(err.toString(), containsString("stderr content"));
        assertThat(out.toString(), containsString("stdout content"));
    }

    @Theory
    public void groovyGrep(String command) {
        stdin("print D.load.jvm.threads.grep().getClass();%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(this, succeeded());
    }

    @Theory
    public void groovyGrepWithArg(String command) {
        stdin("print D.load.jvm.threads.grep { it.name == 'blocked_thread' }.getClass()%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(this, succeeded());
    }

    @Theory
    public void groovyFindAll(String command) {
        stdin("print D.load.jvm.threads.findAll().getClass()%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(this, succeeded());
    }

    @Theory
    public void groovyFindAllWithArg(String command) {
        stdin("print D.load.jvm.threads.findAll { it.name == 'blocked_thread' }.getClass()%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(this, succeeded());
    }

    @Theory
    public void groovyAsImmutable(String command) {
        stdin("print D.load.jvm.threads.asImmutable().getClass()%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(this, succeeded());
    }

    @Theory
    public void groovyIntersect(String command) {
        stdin("rt = D.load.jvm; print rt.threads.intersect(rt.threads).getClass()%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(this, succeeded());
    }

    @Theory
    public void groovyPlus(String command) {
        stdin("rt = D.load.jvm; threadSum = rt.threads + rt.threads; print threadSum.getClass()%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(this, succeeded());
    }

    @Theory
    public void groovyToSet(String command) {
        stdin("print D.load.jvm.threads.toSet().getClass()%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(this, succeeded());
    }

    @Theory
    public void stateFilter(String command) {
        String choices = "it.status.new || it.status.runnable || it.status.sleeping || it.status.waiting || it.status.parked || it.status.blocked || it.status.terminated";
        stdin("print D.load.jvm.threads.grep { " + choices + " }.empty%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("false"));
        assertThat(this, succeeded());
    }

    @Theory
    public void cliArguments(String command) {
        stdin("print \"${D.args[1]} ${D.args[0]}!\"%n");
        run(command, "World", "Hello");

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("Hello World!"));
        assertThat(this, succeeded());
    }

    @Theory
    public void help(String command) {
        stdin("print D%n");
        run(command);

        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), containsString("D.args: java.util.List%n  CLI arguments passed to the script"));
        assertThat(out.toString(), containsString("D.load.threaddump(String): com.github.olivergondza.dumpling.model.ProcessRuntime"));
        assertThat(this, succeeded());
    }
}
