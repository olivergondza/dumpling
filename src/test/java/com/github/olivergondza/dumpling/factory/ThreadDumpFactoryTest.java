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
package com.github.olivergondza.dumpling.factory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;

import javax.annotation.Nonnull;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.StackTrace;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadSet;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class ThreadDumpFactoryTest extends AbstractCliTest {

    @Test
    public void openJdk7_60() throws Exception {

        ThreadSet threads = runtimeFrom("openjdk-1.7.0_60.log").getThreads();

        assertEquals(35, threads.size());

        ProcessThread main = threads.onlyNamed("main").onlyThread();
        assertEquals(ThreadStatus.RUNNABLE, main.getThreadStatus());
        assertEquals(Thread.State.RUNNABLE, main.getState());
        assertThat(139675222183936L, equalTo(main.getTid()));
        assertThat(24597L, equalTo(main.getNid()));
        assertEquals(10, main.getPriority());

        StackTraceElement[] trace = main.getStackTrace();
        assertEquals(27, trace.length);

        assertEquals("org.eclipse.swt.internal.gtk.OS", trace[0].getClassName());
        assertEquals("Call", trace[0].getMethodName());
        assertEquals(null, trace[0].getFileName());
        assertEquals(-2, trace[0].getLineNumber());

        assertEquals("org.eclipse.swt.widgets.Display", trace[1].getClassName());
        assertEquals("sleep", trace[1].getMethodName());
        assertEquals("Display.java", trace[1].getFileName());
        assertEquals(4233, trace[1].getLineNumber());
    }

    @Test
    public void oracleJdk7_51() throws Exception {

        ThreadSet threads = runtimeFrom("oraclejdk-1.7.0_51.log").getThreads();

        assertEquals(143, threads.size());

        ProcessThread thread = threads.onlyNamed("Channel reader thread: jenkins_slave_02").onlyThread();
        assertEquals(ThreadStatus.RUNNABLE, thread.getThreadStatus());
        StackTraceElement[] trace = thread.getStackTrace();
        assertEquals(13, trace.length);

        assertEquals("java.io.FileInputStream", trace[0].getClassName());
        assertEquals("readBytes", trace[0].getMethodName());
        assertEquals(null, trace[0].getFileName());
        assertEquals(-2, trace[0].getLineNumber());

        StackTraceElement lastTrace = trace[trace.length - 1];
        assertEquals("hudson.remoting.SynchronousCommandTransport$ReaderThread", lastTrace.getClassName());
        assertEquals("run", lastTrace.getMethodName());
        assertEquals("SynchronousCommandTransport.java", lastTrace.getFileName());
        assertEquals(48, lastTrace.getLineNumber());
    }

    @Test
    public void oracleJdk7() throws Exception {

        ProcessRuntime expected = runtime (
                daemon("Attach Listener").setTid(194867200).setNid(18909).setStatus(ThreadStatus.RUNNABLE),
                thread("GC task thread#0 (ParallelGC)").setTid(191416320).setNid(18882),
                thread("GC task thread#1 (ParallelGC)").setTid(191424512).setNid(18883),
                thread("GC task thread#2 (ParallelGC)").setTid(191430656).setNid(18884),
                thread("GC task thread#3 (ParallelGC)").setTid(191438848).setNid(18885),
                thread("VM Periodic Task Thread").setTid(192006144).setNid(18893),
                daemon("Signal Dispatcher").setTid(191895552).setNid(18889).setStatus(ThreadStatus.RUNNABLE),
                daemon("process reaper").setTid(47867348480000L).setNid(18895).setStatus(ThreadStatus.RUNNABLE),
                daemon("C2 CompilerThread0").setTid(191905792).setNid(18890).setStatus(ThreadStatus.RUNNABLE),
                daemon("C2 CompilerThread1").setTid(191952896).setNid(18891).setStatus(ThreadStatus.RUNNABLE),
                thread("VM Thread").setTid(191717376).setNid(18886),
                daemon("Service Thread").setTid(191963136).setNid(18892).setStatus(ThreadStatus.RUNNABLE),
                daemon("Finalizer").setTid(191744000).setNid(18888).setStatus(ThreadStatus.IN_OBJECT_WAIT)
                        .setLock(lock("java.lang.ref.ReferenceQueue$Lock", 33678346384L))
                        .setAcquiredLocks(lock("java.lang.ref.ReferenceQueue$Lock", 33678346384L))
                ,
                daemon("Reference Handler").setTid(191727616).setNid(18887).setStatus(ThreadStatus.IN_OBJECT_WAIT)
                        .setLock(lock("java.lang.ref.Reference$Lock", 33678167272L))
                        .setAcquiredLocks(lock("java.lang.ref.Reference$Lock", 33678167272L))
                ,
                thread("main").setTid(191326208).setNid(18881).setStatus(ThreadStatus.IN_OBJECT_WAIT)
                        .setLock(lock("java.lang.UNIXProcess", 33649075520L))
                        .setAcquiredLocks(lock("java.lang.UNIXProcess", 33649075520L))
        );

        ProcessRuntime actual = runtimeFrom("oraclejdk-1.7.log");
        assertThat(actual, sameThreadsAs(expected));

        ProcessThread mainThread = actual.getThreads().onlyNamed("main").onlyThread();
        StackTraceElement[] expectedStackTrace = new StackTraceElement[] {
                StackTrace.nativeElement("java.lang.Object", "wait"),
                StackTrace.element("java.lang.Object", "wait", "Object.java", 503),
                StackTrace.element("java.lang.UNIXProcess", "waitFor", "UNIXProcess.java", 210),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 606),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite$PojoCachedMethodSiteNoUnwrapNoCoerce", "invoke", "PojoMetaMethodSite.java", 230),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite", "call", "PojoMetaMethodSite.java", 53),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.CallSiteArray", "defaultCall", "CallSiteArray.java", 45),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.AbstractCallSite", "call", "AbstractCallSite.java", 108),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.AbstractCallSite", "call", "AbstractCallSite.java", 112),
                StackTrace.element("hudson5336955934972498423", "run", "hudson5336955934972498423.groovy", 9),
                StackTrace.element("groovy.lang.GroovyShell", "runScriptOrMainOrTestOrRunnable", "GroovyShell.java", 257),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 220),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 150),
                StackTrace.element("groovy.ui.GroovyMain", "processOnce", "GroovyMain.java", 588),
                StackTrace.element("groovy.ui.GroovyMain", "run", "GroovyMain.java", 375),
                StackTrace.element("groovy.ui.GroovyMain", "process", "GroovyMain.java", 361),
                StackTrace.element("groovy.ui.GroovyMain", "processArgs", "GroovyMain.java", 120),
                StackTrace.element("groovy.ui.GroovyMain", "main", "GroovyMain.java", 100),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 606),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "rootLoader", "GroovyStarter.java", 106),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "main", "GroovyStarter.java", 128),
        };

        assertArrayEquals(expectedStackTrace, mainThread.getStackTrace());
    }

    @Test @Ignore
    public void oracleJdk8() throws Exception {

        ThreadSet threads = runtimeFrom("oraclejdk-1.8.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test @Ignore
    public void openjdk6() throws Exception {

        ThreadSet threads = runtimeFrom("openjdk-1.6.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test @Ignore
    public void openjdk7() throws Exception {

        ThreadSet threads = runtimeFrom("openjdk-1.7.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test @Ignore
    public void jrockit6() throws Exception {

        ThreadSet threads = runtimeFrom("jrockit-1.6.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test @Ignore
    public void jrockit5() throws Exception {

        ThreadSet threads = runtimeFrom("jrockit-1.5.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test
    public void lockRelationshipsShouldBePreserved() throws Exception {

        ThreadSet threads = new ThreadDumpFactory().fromFile(Util.resourceFile("producer-consumer.log")).getThreads();

        ProcessThread blocked = threads.onlyNamed("blocked_thread").onlyThread();
        ProcessThread owning = threads.onlyNamed("owning_thread").onlyThread();

        assertTrue(owning.getBlockingThreads().isEmpty());
        assertEquals(threads.onlyNamed("blocked_thread"), owning.getBlockedThreads());

        assertEquals(threads.onlyNamed("owning_thread"), blocked.getBlockingThreads());
        assertTrue(blocked.getBlockedThreads().isEmpty());
    }

    @Test
    public void doNotIncludeSelfToBlockedOrBlockingThreads() throws Exception {

        ThreadSet threads = new ThreadDumpFactory().fromFile(Util.resourceFile(getClass(), "self-lock.log")).getThreads();

        ProcessThread handler = threads.onlyNamed("Reference Handler").onlyThread();
        ProcessThread finalizer = threads.onlyNamed("Finalizer").onlyThread();

        assertTrue(handler.getBlockedThreads().isEmpty());
        assertTrue(handler.getBlockingThreads().isEmpty());
        assertTrue(finalizer.getBlockedThreads().isEmpty());
        assertTrue(finalizer.getBlockingThreads().isEmpty());
    }

    @Test
    public void cliNoSuchFile() {
        run("detect-deadlocks", "--in", "threaddump", "/there_is_no_such_file");
        assertThat(exitValue, equalTo(-1));
        assertThat(err.toString(), containsString("/there_is_no_such_file (No such file or directory)"));
        assertThat(out.toString(), equalTo(""));
    }

    private ProcessRuntime runtimeFrom(String resource) throws IOException, URISyntaxException {
        return new ThreadDumpFactory().fromFile(Util.resourceFile(getClass(), resource));
    }

    private ProcessRuntime runtime(ProcessThread.Builder... builders) {
        return new ProcessRuntime(new LinkedHashSet<ProcessThread.Builder>(Arrays.asList(builders)));
    }

    private static volatile int syntheticId = 42;
    private ProcessThread.Builder thread(String name) {
        return ProcessThread.builder().setName(name)
                .setPriority(10) // Not really any default value, just most common in thread dumps.
                // Preset unique id for purposes of the test as we can not rely
                // that SUT will correctly initialize IDs. Threads
                // with the same Ids will be collapsed into one by Set.
                // Correct factory implementation will always overwrite this.
                .setTid(syntheticId++)
        ;
    }

    private ProcessThread.Builder daemon(String name) {
        return thread(name).setDaemon(true);
    }

    private ThreadLock lock(@Nonnull String classname, long id) {
        return new ThreadLock(classname, id);
    }

    private TypeSafeMatcher<ProcessRuntime> sameThreadsAs(final ProcessRuntime expectedRuntime) {
        return new TypeSafeMatcher<ProcessRuntime>() {

            public void describeTo(Description description) {
                description.appendText("Runtime with same threads");
            }

            @Override
            protected boolean matchesSafely(ProcessRuntime actual) {
                if (expectedRuntime.getThreads().size() != actual.getThreads().size()) return false;

                for (ProcessThread actualThread: actual.getThreads()) {
                    ProcessThread expectedThread = expectedRuntime.getThreads()
                            .onlyNamed(actualThread.getName()).onlyThread()
                    ;

                    if (difference(expectedThread, actualThread) != null) return false;
                }

                return true;
            }

            @Override
            protected void describeMismatchSafely(ProcessRuntime actualRuntime, Description mismatch) {
                final ThreadSet expectedThreads = expectedRuntime.getThreads();
                final ThreadSet actualThreads = actualRuntime.getThreads();

                for (ProcessThread actual: actualThreads) {
                    final ThreadSet named = expectedThreads.onlyNamed(actual.getName());

                    if (named.size() > 1) throw new AssertionError("Several threads named: " + actual.getName());
                    if (named.size() == 0) {
                        mismatch.appendText("Unexpected Thread:\n").appendText(actual.toString());
                        return;
                    }

                    ProcessThread expected = named.onlyThread();
                    String difference = difference(expected, actual);
                    if (difference == null) continue; // Equal

                    mismatch.appendText(expected.toString())
                            .appendText("\nDiffers in: ").appendText(difference).appendText("\n")
                            .appendText(actual.toString())
                    ;
                    return;
                }

                if (expectedThreads.size() == actualThreads.size()) return;

                mismatch.appendText("UNKNOWN MISSMATCH");
            }
        };
    }

    // Deep equality for test purposes
    private String difference(ProcessThread lhs, ProcessThread rhs) {
        if (!equals(lhs.getId(), rhs.getId())) return "id";
        if (!equals(lhs.getTid(), rhs.getTid())) return "tid";
        if (!equals(lhs.getNid(), rhs.getNid())) return "nid";
        if (!equals(lhs.getName(), rhs.getName())) return "name";
        if (lhs.getPriority() != rhs.getPriority()) return "priority";
        if (lhs.isDaemon() != rhs.isDaemon()) return "daemon";
        if (!equals(lhs.getThreadStatus(), rhs.getThreadStatus())) return "thread status";
        if (!equals(lhs.getWaitingOnLock(), rhs.getWaitingOnLock())) return "waiting on lock";
        if (!lhs.getAcquiredLocks().equals(rhs.getAcquiredLocks())) return "acquired locks";
        // if (!Arrays.equals(lhs.getStackTrace(), rhs.getStackTrace())) return "stack trace";

        return null;
    }

    private boolean equals(Object lhs, Object rhs) {
        if (lhs == null) return rhs == null;

        return lhs.equals(rhs);
    }
}
