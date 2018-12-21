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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.github.olivergondza.dumpling.DisposeRule;
import com.github.olivergondza.dumpling.TestThread;
import com.github.olivergondza.dumpling.Util;

import javax.annotation.Nonnull;

/**
 * Test interoperability between groovy and groovysh command and main method invocation with uberjar invocation.
 *
 * @author ogondza
 */
@RunWith(Theories.class)
public class GroovyRuntimeTest {

    @Rule public DisposeRule disposer = new DisposeRule();

    @DataPoints
    public static String[] commands() {
        return new String[] { "groovy", "groovysh" };
    }

    @DataPoints
    public static List<AbstractCliTest> invokers() {
        ArrayList<AbstractCliTest> callables = new ArrayList<AbstractCliTest>();
        callables.add(new AbstractCliTest() {}); // Run command within the JVM
        callables.add(new AbstractCliTest() { // Run in sibling JDK testing the uberjar
            @Override protected int run(@Nonnull String... args) {
                ArrayList<String> procArgs = new ArrayList<String>();
                procArgs.add("java");
                procArgs.add("-jar");
                procArgs.add(getUberjar());
                procArgs.addAll(Arrays.asList(args));

                try {
                    Process process = new ProcessBuilder(procArgs).start();
                    PrintStream inStream = new PrintStream(process.getOutputStream());
                    Util.forwardStream(in, inStream);
                    inStream.close();
                    exitValue = process.waitFor();
                    this.out = new ByteArrayOutputStream();
                    err = new ByteArrayOutputStream();
                    Util.forwardStream(process.getInputStream(), new PrintStream(this.out));
                    Util.forwardStream(process.getErrorStream(), new PrintStream(err));

                    return exitValue;
                } catch (IOException e) {
                    throw new AssertionError(e);
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
        });
        return callables;
    }

    private static String getUberjar() {
        File file = new File("./target");

        try {
            if (!file.exists()) throw new AssertionError(file.getCanonicalPath()+ " does not exist");
            List<File> files = Arrays.asList(file.listFiles(new FilenameFilter() {
                @Override public boolean accept(File dir, String name) {
                    return name.startsWith("dumpling-cli") && name.endsWith("-shaded.jar");
                }
            }));

            if (files.size() != 1) throw new AssertionError("One uberjar expected: " + files);

            return files.get(0).getCanonicalPath();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Theory
    public void executeScript(String command, AbstractCliTest i) {
        i.stdin("println D.load.jvm.threads.size() instanceof Integer;%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString().trim(), i.containsString("true"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void filter(String command, AbstractCliTest i) throws Exception {
        i.stdin("D.load.threaddump(D.args[0]).threads.where(nameIs('owning_thread')).collect { it.name };%n");
        i.run(command, Util.asFile(Util.resource("jstack/producer-consumer.log")).getAbsolutePath());

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString().trim(), i.containsString("[owning_thread]"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void loadTreaddump(String command, AbstractCliTest i) throws Exception {
        assertLoadThreaddump(command, i, "D.load.threaddump(D.args[0]).threads.where(nameIs('blocked_thread'));%n");
    }

    private void assertLoadThreaddump(String command, AbstractCliTest i, String script) throws Exception {
        File file = Util.asFile(Util.resource("jstack/producer-consumer.log"));
        i.stdin(script);
        i.run(command, file.getAbsolutePath());

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("\"blocked_thread\""));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void loadPid(String command, AbstractCliTest i) {
        assertLoadPid(command, i, "D.load.process(%d).threads.where(nameIs('remotely-observed-thread'));%n");
    }

    private void assertLoadPid(String command, AbstractCliTest i, String script) {
        disposer.register(TestThread.runThread());
        i.stdin(String.format(script, Util.currentPid()));
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("\"remotely-observed-thread\""));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void loadPidOverJmx(String command, AbstractCliTest i) throws Exception {
        assertLoadPidOverJmx(command, i, "D.load.jmx(%d).threads.where(nameIs('remotely-observed-thread'));%n");
    }

    private void assertLoadPidOverJmx(String command, AbstractCliTest i, String script) throws Exception {
        TestThread.JMXProcess process = disposer.register(TestThread.runJmxObservableProcess(false));
        i.stdin(String.format(script, process.pid()));
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("\"remotely-observed-thread\""));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void loadJmx(String command, AbstractCliTest i) throws Exception {
        assertLoadJmx(command, i, "println D.load.jmx(D.args[0]).threads.where(nameIs('remotely-observed-thread'));%n");
    }

    private void assertLoadJmx(String command, AbstractCliTest i, String script) throws Exception {
        TestThread.JMXProcess process = disposer.register(TestThread.runJmxObservableProcess(false));
        i.stdin(script);
        i.run(command, process.JMX_CONNECTION);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("\"remotely-observed-thread\""));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void loadSymbolsFromOtherDumplingPackages(String command, AbstractCliTest i) {
        i.stdin("new Deadlocks(); ModelObject.Mode.HUMAN; new JvmRuntimeFactory(); new CommandFailedException('');%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i, i.succeeded());
    }

    @Theory
    public void failTheScript(String command, AbstractCliTest i) {
        i.stdin("new ThereIsNoSuchClass();%n");
        i.run(command);

        assertThat(i.err.toString(), i.containsString("unable to resolve class ThereIsNoSuchClass"));
    }

    @Theory
    public void printToOutAndErr(String command, AbstractCliTest i) {
        i.stdin("out.println('stdout content'); err.println('stderr content');%n");
        i.run(command);

        assertThat(i.err.toString(), i.containsString("stderr content"));
        assertThat(i.out.toString(), i.containsString("stdout content"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void groovyGrep(String command, AbstractCliTest i) {
        i.stdin("def threads = D.load.jvm.threads; assert threads == threads.grep(); println threads.class;%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void groovyGrepWithArg(String command, AbstractCliTest i) {
        final String name = Thread.currentThread().getName();
        i.stdin("def threads = D.load.jvm.threads.grep { it.name == '" + name + "' }; assert threads.size() == 1; println threads.class%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void groovyFindAll(String command, AbstractCliTest i) {
        i.stdin("def threads = D.load.jvm.threads; assert threads == threads.findAll(); println threads.getClass()%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void groovyFindAllWithArg(String command, AbstractCliTest i) {
        final String name = Thread.currentThread().getName();
        i.stdin("def threads = D.load.jvm.threads.findAll { it.name == '" + name + "' }; assert threads.size() == 1; println threads.getClass()%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void groovyAsImmutable(String command, AbstractCliTest i) {
        i.stdin("def threads = D.load.jvm.threads; assert threads.asImmutable() == threads; print threads.getClass()%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void groovyIntersect(String command, AbstractCliTest i) {
        i.stdin("def threads = D.load.jvm.threads; def intersected = threads.intersect(threads); assert threads == intersected; print intersected.getClass()%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void groovyIntersectDifferentRuntime(String command, AbstractCliTest i) {
        i.stdin("D.load.jvm.threads.intersect(D.load.jvm.threads)%n");
        i.run(command);

        assertThat(i.err.toString(), i.containsString("java.lang.IllegalArgumentException"));
        assertThat(i.err.toString(), i.containsString("Arguments bound to different ProcessRuntimes"));
    }

    @Theory
    public void groovyPlus(String command, AbstractCliTest i) {
        i.stdin("rt = D.load.jvm; threadSum = rt.threads + rt.threads; print threadSum.getClass()%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void groovyPlusDifferentRuntime(String command, AbstractCliTest i) {
        i.stdin("D.load.jvm.threads + D.load.jvm.threads%n");
        i.run(command);

        assertThat(i.err.toString(), i.containsString("java.lang.IllegalArgumentException"));
        assertThat(i.err.toString(), i.containsString("Arguments bound to different ProcessRuntimes"));
    }

    @Theory
    public void groovyToSet(String command, AbstractCliTest i) {
        i.stdin("print D.load.jvm.threads.toSet().getClass()%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("class com.github.olivergondza.dumpling.model.jvm.JvmThreadSet"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void stateFilter(String command, AbstractCliTest i) {
        String choices = "it.status.new || it.status.runnable || it.status.sleeping || it.status.waiting || it.status.parked || it.status.blocked || it.status.terminated";
        i.stdin("print D.load.jvm.threads.grep { " + choices + " }.empty%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("false"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void cliArguments(String command, AbstractCliTest i) {
        i.stdin("print \"${D.args[1]} ${D.args[0]}!\"%n");
        i.run(command, "World", "Hello");

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("Hello World!"));
        assertThat(i, i.succeeded());
    }

    @Theory
    public void help(String command, AbstractCliTest i) {
        i.stdin("print D%n");
        i.run(command);

        assertThat(i, i.reportedNoError());
        assertThat(i.out.toString(), i.containsString("D.args: java.util.List%n  CLI arguments passed to the script"));
        assertThat(i.out.toString(), i.containsString("D.load.threaddump(String): com.github.olivergondza.dumpling.model.ProcessRuntime"));
        assertThat(i, i.succeeded());
    }
}
