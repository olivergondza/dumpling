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

import static com.github.olivergondza.dumpling.Util.only;
import static com.github.olivergondza.dumpling.Util.pause;
import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.DisposeRule;
import com.github.olivergondza.dumpling.model.jvm.JvmRuntime;
import com.github.olivergondza.dumpling.model.jvm.JvmThread;
import com.github.olivergondza.dumpling.query.BlockingTree;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.model.ModelObject.Mode;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.StackTrace;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThreadSet;

public class ThreadDumpFactoryTest {

    public static final ThreadDumpFactory FACTORY = new ThreadDumpFactory();

    public @Rule DisposeRule cleaner = new DisposeRule();

    @Test
    public void openJdk7_60() throws Exception {

        ThreadDumpThreadSet threads = runtimeFrom("openjdk-1.7.0_60.log").getThreads();

        assertEquals(35, threads.size());

        ThreadDumpThread main = threads.where(nameIs("main")).onlyThread();
        assertEquals(ThreadStatus.RUNNABLE, main.getStatus());
        assertEquals(Thread.State.RUNNABLE, main.getState());
        assertThat(139675222183936L, equalTo(main.getTid()));
        assertThat(24597L, equalTo(main.getNid()));
        assertEquals(10, main.getPriority().intValue());

        StackTrace trace = main.getStackTrace();
        assertEquals(27, trace.size());

        assertEquals("org.eclipse.swt.internal.gtk.OS", trace.getElement(0).getClassName());
        assertEquals("Call", trace.getElement(0).getMethodName());
        assertEquals(null, trace.getElement(0).getFileName());
        assertEquals(-2, trace.getElement(0).getLineNumber());

        assertEquals("org.eclipse.swt.widgets.Display", trace.getElement(1).getClassName());
        assertEquals("sleep", trace.getElement(1).getMethodName());
        assertEquals("Display.java", trace.getElement(1).getFileName());
        assertEquals(4233, trace.getElement(1).getLineNumber());
    }

    @Test
    public void oracleJdk7_51() throws Exception {

        ThreadDumpThreadSet threads = runtimeFrom("oraclejdk-1.7.0_51.log").getThreads();

        assertEquals(143, threads.size());

        ThreadDumpThread thread = threads.where(nameIs("Channel reader thread: jenkins_slave_02")).onlyThread();
        assertEquals(ThreadStatus.RUNNABLE, thread.getStatus());
        StackTrace trace = thread.getStackTrace();
        assertEquals(13, trace.size());

        assertEquals("java.io.FileInputStream", trace.getElement(0).getClassName());
        assertEquals("readBytes", trace.getElement(0).getMethodName());
        assertEquals(null, trace.getElement(0).getFileName());
        assertEquals(-2, trace.getElement(0).getLineNumber());

        StackTraceElement lastTrace = trace.getElement(trace.size() - 1);
        assertEquals("hudson.remoting.SynchronousCommandTransport$ReaderThread", lastTrace.getClassName());
        assertEquals("run", lastTrace.getMethodName());
        assertEquals("SynchronousCommandTransport.java", lastTrace.getFileName());
        assertEquals(48, lastTrace.getLineNumber());
    }

    @Test @Ignore
    public void oracleJdk6() throws Exception {

        ThreadDumpThreadSet threads = runtimeFrom("oraclejdk-1.6.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test
    public void oracleJdk7() throws Exception {

        ThreadDumpRuntime expected = runtime(
                daemon("Attach Listener").setTid(194867200).setNid(18909).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                thread("GC task thread#0 (ParallelGC)").setTid(191416320).setNid(18882).setPriority(10),
                thread("GC task thread#1 (ParallelGC)").setTid(191424512).setNid(18883).setPriority(10),
                thread("GC task thread#2 (ParallelGC)").setTid(191430656).setNid(18884).setPriority(10),
                thread("GC task thread#3 (ParallelGC)").setTid(191438848).setNid(18885).setPriority(10),
                thread("VM Periodic Task Thread").setTid(192006144).setNid(18893).setPriority(10),
                daemon("Signal Dispatcher").setTid(191895552).setNid(18889).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("process reaper").setTid(47867348480000L).setNid(18895).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("C2 CompilerThread0").setTid(191905792).setNid(18890).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("C2 CompilerThread1").setTid(191952896).setNid(18891).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                thread("VM Thread").setTid(191717376).setNid(18886).setPriority(10),
                daemon("Service Thread").setTid(191963136).setNid(18892).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("Finalizer").setTid(191744000).setNid(18888).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.ref.ReferenceQueue$Lock", 33678346384L))
                ,
                daemon("Reference Handler").setTid(191727616).setNid(18887).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.ref.Reference$Lock", 33678167272L))
                ,
                thread("main").setTid(191326208).setNid(18881).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.UNIXProcess", 33649075520L))
        );

        ThreadDumpRuntime actual = runtimeFrom("oraclejdk-1.7.log");
        assertThat(actual, sameThreadsAs(expected));

        StackTrace expectedStackTrace = new StackTrace(
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
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "main", "GroovyStarter.java", 128)
        );

        assertThat(actual, stacktraceEquals(expectedStackTrace, "main"));
    }

    @Test
    public void oracleJdk8() throws Exception {

        ThreadDumpRuntime expected = runtime(
                daemon("Attach Listener").setTid(1716535296).setNid(8144).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(11),
                thread("GC task thread#0 (ParallelGC)").setTid(3059810304L).setNid(8115),
                thread("GC task thread#1 (ParallelGC)").setTid(3059815424L).setNid(8116),
                thread("GC task thread#2 (ParallelGC)").setTid(3059820544L).setNid(8117),
                thread("GC task thread#3 (ParallelGC)").setTid(3059825664L).setNid(8118),
                thread("VM Periodic Task Thread").setTid(1718347776).setNid(8127),
                daemon("Signal Dispatcher").setTid(1718240256).setNid(8122).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(4),
                daemon("process reaper").setTid(1697889280).setNid(8129).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10).setId(10),
                daemon("C2 CompilerThread0").setTid(1718247424).setNid(8123).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(5),
                daemon("C2 CompilerThread1").setTid(1718254592).setNid(8124).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(6),
                daemon("C1 CompilerThread2").setTid(1718260736).setNid(8125).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(7),
                thread("VM Thread").setTid(1718094848).setNid(8119),
                daemon("Service Thread").setTid(1718273024).setNid(8126).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(8),
                daemon("Finalizer").setTid(1718118400).setNid(8121).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(8).setId(3)
                        .setWaitingOnLock(lock("java.lang.ref.ReferenceQueue$Lock", 2495908272L))
                ,
                daemon("Reference Handler").setTid(1718108160).setNid(8120).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10).setId(2)
                        .setWaitingOnLock(lock("java.lang.ref.Reference$Lock", 2495922552L))
                ,
                thread("main").setTid(3059771392L).setNid(8114).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(5).setId(1)
                        .setWaitingOnLock(lock("java.lang.UNIXProcess", 2468857072L))
        );

        ThreadDumpRuntime actual = runtimeFrom("oraclejdk-1.8.log");
        assertThat(actual, sameThreadsAs(expected));

        StackTrace expectedStackTrace = new StackTrace(
                StackTrace.nativeElement("java.lang.Object", "wait"),
                StackTrace.element("java.lang.Object", "wait", "Object.java", 502),
                StackTrace.element("java.lang.UNIXProcess", "waitFor", "UNIXProcess.java", 264),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 483),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite$PojoCachedMethodSiteNoUnwrapNoCoerce", "invoke", "PojoMetaMethodSite.java", 230),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite", "call", "PojoMetaMethodSite.java", 53),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.CallSiteArray", "defaultCall", "CallSiteArray.java", 45),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.AbstractCallSite", "call", "AbstractCallSite.java", 108),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.AbstractCallSite", "call", "AbstractCallSite.java", 112),
                StackTrace.element("hudson8109898462652879487", "run", "hudson8109898462652879487.groovy", 9),
                StackTrace.element("groovy.lang.GroovyShell", "runScriptOrMainOrTestOrRunnable", "GroovyShell.java", 257),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 220),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 150),
                StackTrace.element("groovy.ui.GroovyMain", "processOnce", "GroovyMain.java", 588),
                StackTrace.element("groovy.ui.GroovyMain", "run", "GroovyMain.java", 375),
                StackTrace.element("groovy.ui.GroovyMain", "process", "GroovyMain.java", 361),
                StackTrace.element("groovy.ui.GroovyMain", "processArgs", "GroovyMain.java", 120),
                StackTrace.element("groovy.ui.GroovyMain", "main", "GroovyMain.java", 100),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 483),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "rootLoader", "GroovyStarter.java", 106),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "main", "GroovyStarter.java", 128)
        );

        assertThat(actual, stacktraceEquals(expectedStackTrace, "main"));
    }

    @Test
    public void oracleJdk9() throws Exception {

        ThreadDumpRuntime expected = runtime(
                daemon("Attach Listener").setTid(0x7fb8f8001000L).setNid(12525).setId(14).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                thread("GC Thread#0").setTid(0x7fb980028000L).setNid(12396),
                thread("GC Thread#1").setTid(0x7fb980029800L).setNid(12397),
                thread("GC Thread#2").setTid(0x7fb98002b000L).setNid(12398),
                thread("GC Thread#3").setTid(0x7fb98002c800L).setNid(12399),
                thread("GC Thread#4").setTid(0x7fb98002e800L).setNid(12400),
                thread("GC Thread#5").setTid(0x7fb980030000L).setNid(12401),
                thread("GC Thread#6").setTid(0x7fb980032000L).setNid(12402),
                thread("GC Thread#7").setTid(0x7fb980033800L).setNid(12403),
                thread("G1 Main Marker").setTid(0x7fb980089000L).setNid(12413),
                thread("G1 Marker#0").setTid(0x7fb98008b000L).setNid(12414),
                thread("G1 Marker#1").setTid(0x7fb98008c800L).setNid(12415),
                thread("G1 Refine#0").setTid(0x7fb980042000L).setNid(12411),
                thread("G1 Refine#1").setTid(0x7fb980040800L).setNid(12410),
                thread("G1 Refine#2").setTid(0x7fb98003e800L).setNid(12409),
                thread("G1 Refine#3").setTid(0x7fb98003d000L).setNid(12408),
                thread("G1 Refine#4").setTid(0x7fb98003b000L).setNid(12407),
                thread("G1 Refine#5").setTid(0x7fb980039800L).setNid(12406),
                thread("G1 Refine#6").setTid(0x7fb980037800L).setNid(12405),
                thread("G1 Refine#7").setTid(0x7fb980036000L).setNid(12404),
                thread("G1 Young RemSet Sampling").setTid(0x7fb980044000L).setNid(0x307c),
                thread("Common-Cleaner").setTid(0x7fb98022c800L).setNid(12426).setId(11).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT_TIMED).setPriority(8).setDaemon(true)
                        .setWaitingOnLock(lock("java.lang.ref.ReferenceQueue$Lock", 0x6d8b47ea0L))
                ,
                thread("VM Periodic Task Thread").setTid(0x7fb9802d2000L).setNid(12428),
                daemon("Signal Dispatcher").setTid(0x7fb9801ff000L).setNid(12420).setId(5).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                daemon("C2 CompilerThread0").setTid(0x7fb980201800L).setNid(12421).setId(6).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                daemon("C2 CompilerThread1").setTid(0x7fb980203800L).setNid(12422).setId(7).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                daemon("C2 CompilerThread2").setTid(0x7fb980205800L).setNid(12423).setId(8).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                //daemon("C2 CompilerThread3").setTid(0x7fb980207800L).setNid(12424).setId(9).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                daemon("C1 CompilerThread3").setTid(0x7fb980207800L).setNid(12424).setId(9).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                thread("VM Thread").setTid(0x7fb9801c7000L).setNid(12416),
                daemon("Service Thread").setTid(0x7fb9802cf000L).setNid(12427).setId(12).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                daemon("Sweeper thread").setTid(0x7fb980211000L).setNid(12425).setId(10).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                daemon("Reference Pending List Locker").setTid(0x7fb9801fd800L).setNid(12419).setId(4).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9),
                daemon("Finalizer").setTid(0x7fb9801d7000L).setNid(12418).setId(3).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(8)
                        .setWaitingOnLock(lock("java.lang.ref.ReferenceQueue$Lock", 0x6d8b47e90L))
                ,
                daemon("Reference Handler").setTid(0x7fb9801d3000L).setNid(12417).setId(2).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.ref.Reference$Lock", 0x6d8b47e80L))
                ,
                thread("main").setTid(0x7fb980010800L).setNid(12395).setId(1).setThreadStatus(ThreadStatus.SLEEPING).setPriority(5)
        );

        ThreadDumpRuntime actual = runtimeFrom("oraclejdk-1.9.log");
        assertThat(actual, sameThreadsAs(expected));

        StackTrace expectedStackTrace = new StackTrace(
                StackTrace.nativeElement("java.lang.Thread", "sleep"),
                StackTrace.nativeElement("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("jdk.internal.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 535),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 93),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 325),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.StaticMetaMethodSite$StaticMetaMethodSiteNoUnwrap", "invoke", "StaticMetaMethodSite.java", 133),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.StaticMetaMethodSite", "call", "StaticMetaMethodSite.java", 91),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.CallSiteArray", "defaultCall", "CallSiteArray.java", 48),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.AbstractCallSite", "call", "AbstractCallSite.java", 113),
                StackTrace.element("org.codehaus.groovy.runtime.callsite.AbstractCallSite", "call", "AbstractCallSite.java", 125),
                StackTrace.element("script_from_command_line", "run", "script_from_command_line", 1),
                StackTrace.element("groovy.lang.GroovyShell", "runScriptOrMainOrTestOrRunnable", "GroovyShell.java", 263),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 518),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 507),
                StackTrace.element("groovy.ui.GroovyMain", "processOnce", "GroovyMain.java", 653),
                StackTrace.element("groovy.ui.GroovyMain", "run", "GroovyMain.java", 384),
                StackTrace.element("groovy.ui.GroovyMain", "process", "GroovyMain.java", 370),
                StackTrace.element("groovy.ui.GroovyMain", "processArgs", "GroovyMain.java", 129),
                StackTrace.element("groovy.ui.GroovyMain", "main", "GroovyMain.java", 109),
                StackTrace.nativeElement("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("jdk.internal.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 535),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "rootLoader", "GroovyStarter.java", 109),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "main", "GroovyStarter.java", 131)
        );

        assertThat(actual, stacktraceEquals(expectedStackTrace, "main"));
    }

    @Test
    public void openjdk6() throws Exception {

        ThreadDumpRuntime expected = runtime(
                daemon("Attach Listener").setTid(507363328).setNid(28597).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                thread("GC task thread#0 (ParallelGC)").setTid(504647680).setNid(28568).setPriority(10),
                thread("GC task thread#1 (ParallelGC)").setTid(504655872).setNid(28569).setPriority(10),
                thread("GC task thread#2 (ParallelGC)").setTid(504662016).setNid(28570).setPriority(10),
                thread("GC task thread#3 (ParallelGC)").setTid(504670208).setNid(28571).setPriority(10),
                thread("VM Periodic Task Thread").setTid(505618432).setNid(28579).setPriority(10),
                daemon("Signal Dispatcher").setTid(505575424).setNid(28575).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("process reaper").setTid(47702392721408L).setNid(28582).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("C2 CompilerThread0").setTid(505583616).setNid(28576).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("C2 CompilerThread1").setTid(505595904).setNid(28577).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                thread("VM Thread").setTid(505159680).setNid(28572).setPriority(10),
                daemon("Low Memory Detector").setTid(505606144).setNid(28578).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("Finalizer").setTid(505229312).setNid(28574).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.ref.ReferenceQueue$Lock", 3773522592L))
                ,
                daemon("Reference Handler").setTid(505221120).setNid(28573).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.ref.Reference$Lock", 3773521872L))
                ,
                thread("main").setTid(504590336).setNid(28567).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.UNIXProcess", 3791474536L))
        );

        ThreadDumpRuntime actual = runtimeFrom("openjdk-1.6.log");
        assertThat(actual, sameThreadsAs(expected));

        StackTrace expectedStackTrace = new StackTrace(
                StackTrace.nativeElement("java.lang.Object", "wait"),
                StackTrace.element("java.lang.Object", "wait", "Object.java", 502),
                StackTrace.element("java.lang.UNIXProcess", "waitFor", "UNIXProcess.java", 181),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 622),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 899),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 740),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokePojoMethod", "InvokerHelper.java", 765),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 753),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethodN", "ScriptBytecodeAdapter.java", 167),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethod0", "ScriptBytecodeAdapter.java", 195),
                StackTrace.element("hudson2995743014782811370", "run", "hudson2995743014782811370.groovy", 11),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 622),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 899),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 740),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokePogoMethod", "InvokerHelper.java", 777),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 757),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "runScript", "InvokerHelper.java", 402),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 622),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeStaticMethod", "MetaClassImpl.java", 1094),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 748),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethodN", "ScriptBytecodeAdapter.java", 167),
                StackTrace.element("hudson2995743014782811370", "main", "hudson2995743014782811370.groovy"),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 622),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeStaticMethod", "MetaClassImpl.java", 1094),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 748),
                StackTrace.element("groovy.lang.GroovyShell", "runMainOrTestOrRunnable", "GroovyShell.java", 244),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 218),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 147),
                StackTrace.element("groovy.ui.GroovyMain", "processOnce", "GroovyMain.java", 493),
                StackTrace.element("groovy.ui.GroovyMain", "run", "GroovyMain.java", 308),
                StackTrace.element("groovy.ui.GroovyMain", "process", "GroovyMain.java", 294),
                StackTrace.element("groovy.ui.GroovyMain", "processArgs", "GroovyMain.java", 111),
                StackTrace.element("groovy.ui.GroovyMain", "main", "GroovyMain.java", 92),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 622),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "rootLoader", "GroovyStarter.java", 101),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "main", "GroovyStarter.java", 130)
        );

        assertThat(actual, stacktraceEquals(expectedStackTrace, "main"));
    }

    @Test
    public void openjdk7() throws Exception {

        ThreadDumpRuntime expected = runtime(
                daemon("Attach Listener").setTid(1740638208).setNid(32404).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                thread("GC task thread#0 (ParallelGC)").setTid(3075542016L).setNid(32366).setPriority(10),
                thread("GC task thread#1 (ParallelGC)").setTid(3075547136L).setNid(32367).setPriority(10),
                thread("GC task thread#2 (ParallelGC)").setTid(3075553280L).setNid(32368).setPriority(10),
                thread("GC task thread#3 (ParallelGC)").setTid(3075559424L).setNid(32369).setPriority(10),
                thread("GC task thread#4 (ParallelGC)").setTid(3075564544L).setNid(32370).setPriority(10),
                thread("GC task thread#5 (ParallelGC)").setTid(3075570688L).setNid(32371).setPriority(10),
                thread("GC task thread#6 (ParallelGC)").setTid(3075576832L).setNid(32372).setPriority(10),
                thread("GC task thread#7 (ParallelGC)").setTid(3075581952L).setNid(32373).setPriority(10),
                thread("VM Periodic Task Thread").setTid(1748556800).setNid(32381).setPriority(10),
                daemon("Signal Dispatcher").setTid(1748527104).setNid(32377).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("process reaper").setTid(1734433792).setNid(32385).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("C2 CompilerThread0").setTid(1748534272).setNid(32378).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("C2 CompilerThread1").setTid(1748542464).setNid(32379).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                thread("VM Thread").setTid(1748436992).setNid(32374).setPriority(10),
                daemon("Service Thread").setTid(1748549632).setNid(32380).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10),
                daemon("Finalizer").setTid(1748454400).setNid(32376).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.ref.ReferenceQueue$Lock", 2683571272L))
                ,
                daemon("Reference Handler").setTid(1748448256).setNid(32375).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.ref.Reference$Lock", 2683601000L))
                ,
                thread("main").setTid(3075500032L).setNid(32365).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock("java.lang.UNIXProcess", 2672107728L))
        );

        ThreadDumpRuntime actual = runtimeFrom("openjdk-1.7.log");
        assertThat(actual, sameThreadsAs(expected));

        StackTrace expectedStackTrace = new StackTrace(
                StackTrace.nativeElement("java.lang.Object", "wait"),
                StackTrace.element("java.lang.Object", "wait", "Object.java", 503),
                StackTrace.element("java.lang.UNIXProcess", "waitFor", "UNIXProcess.java", 210),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 606),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 899),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 740),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokePojoMethod", "InvokerHelper.java", 765),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 753),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethodN", "ScriptBytecodeAdapter.java", 167),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethod0", "ScriptBytecodeAdapter.java", 195),
                StackTrace.element("hudson3357812930655452714", "run", "hudson3357812930655452714.groovy", 11),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 606),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 899),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 740),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokePogoMethod", "InvokerHelper.java", 777),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 757),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "runScript", "InvokerHelper.java", 402),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 606),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeStaticMethod", "MetaClassImpl.java", 1094),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 748),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethodN", "ScriptBytecodeAdapter.java", 167),
                StackTrace.element("hudson3357812930655452714", "main", "hudson3357812930655452714.groovy"),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 606),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeStaticMethod", "MetaClassImpl.java", 1094),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 748),
                StackTrace.element("groovy.lang.GroovyShell", "runMainOrTestOrRunnable", "GroovyShell.java", 244),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 218),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 147),
                StackTrace.element("groovy.ui.GroovyMain", "processOnce", "GroovyMain.java", 493),
                StackTrace.element("groovy.ui.GroovyMain", "run", "GroovyMain.java", 308),
                StackTrace.element("groovy.ui.GroovyMain", "process", "GroovyMain.java", 294),
                StackTrace.element("groovy.ui.GroovyMain", "processArgs", "GroovyMain.java", 111),
                StackTrace.element("groovy.ui.GroovyMain", "main", "GroovyMain.java", 92),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 57),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 606),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "rootLoader", "GroovyStarter.java", 101),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "main", "GroovyStarter.java", 130)
        );

        assertThat(actual, stacktraceEquals(expectedStackTrace, "main"));
    }

    @Test
    public void openjdk8() throws Exception {

        ThreadDumpRuntime expected = runtime(
                daemon("Attach Listener").setTid(1733312512).setNid(7829).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(11),
                thread("GC task thread#0 (ParallelGC)").setTid(3076587520L).setNid(7798),
                thread("GC task thread#1 (ParallelGC)").setTid(3076592640L).setNid(7799),
                thread("GC task thread#2 (ParallelGC)").setTid(3076597760L).setNid(7800),
                thread("GC task thread#3 (ParallelGC)").setTid(3076602880L).setNid(7801),
                thread("VM Periodic Task Thread").setTid(1735156736).setNid(7811),
                daemon("Signal Dispatcher").setTid(1735017472).setNid(7806).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(4),
                daemon("process reaper").setTid(1724858368).setNid(7813).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(10).setId(10),
                daemon("C2 CompilerThread0").setTid(1735023616).setNid(7807).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(5),
                daemon("C2 CompilerThread1").setTid(1735031808).setNid(7808).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(6),
                daemon("C1 CompilerThread2").setTid(1735036928).setNid(7809).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(7),
                thread("VM Thread").setTid(1734872064).setNid(7802),
                daemon("Service Thread").setTid(1735050240).setNid(7810).setThreadStatus(ThreadStatus.RUNNABLE).setPriority(9).setId(8),
                daemon("Finalizer").setTid(1734894592).setNid(7804).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(8).setId(3)
                        .setWaitingOnLock(lock("java.lang.ref.ReferenceQueue$Lock", 2493779712L))
                ,
                daemon("Reference Handler").setTid(1734884352).setNid(7803).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10).setId(2)
                        .setWaitingOnLock(lock("java.lang.ref.Reference$Lock", 2493780128L))
                ,
                thread("main").setTid(3076548608L).setNid(7797).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(5).setId(1)
                        .setWaitingOnLock(lock("java.lang.UNIXProcess", 2485805968L))
        );

        ThreadDumpRuntime actual = runtimeFrom("openjdk-1.8.log");
        assertThat(actual, sameThreadsAs(expected));

        StackTrace expectedStackTrace = new StackTrace(
                StackTrace.nativeElement("java.lang.Object", "wait"),
                StackTrace.element("java.lang.Object", "wait", "Object.java", 502),
                StackTrace.element("java.lang.UNIXProcess", "waitFor", "UNIXProcess.java", 264),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 483),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 899),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 740),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokePojoMethod", "InvokerHelper.java", 765),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 753),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethodN", "ScriptBytecodeAdapter.java", 167),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethod0", "ScriptBytecodeAdapter.java", 195),
                StackTrace.element("hudson2530543046334227738", "run", "hudson2530543046334227738.groovy", 11),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 483),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 899),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeMethod", "MetaClassImpl.java", 740),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokePogoMethod", "InvokerHelper.java", 777),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 757),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "runScript", "InvokerHelper.java", 402),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 483),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeStaticMethod", "MetaClassImpl.java", 1094),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 748),
                StackTrace.element("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "invokeMethodN", "ScriptBytecodeAdapter.java", 167),
                StackTrace.element("hudson2530543046334227738", "main", "hudson2530543046334227738.groovy"),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 483),
                StackTrace.element("org.codehaus.groovy.reflection.CachedMethod", "invoke", "CachedMethod.java", 86),
                StackTrace.element("groovy.lang.MetaMethod", "doMethodInvoke", "MetaMethod.java", 226),
                StackTrace.element("groovy.lang.MetaClassImpl", "invokeStaticMethod", "MetaClassImpl.java", 1094),
                StackTrace.element("org.codehaus.groovy.runtime.InvokerHelper", "invokeMethod", "InvokerHelper.java", 748),
                StackTrace.element("groovy.lang.GroovyShell", "runMainOrTestOrRunnable", "GroovyShell.java", 244),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 218),
                StackTrace.element("groovy.lang.GroovyShell", "run", "GroovyShell.java", 147),
                StackTrace.element("groovy.ui.GroovyMain", "processOnce", "GroovyMain.java", 493),
                StackTrace.element("groovy.ui.GroovyMain", "run", "GroovyMain.java", 308),
                StackTrace.element("groovy.ui.GroovyMain", "process", "GroovyMain.java", 294),
                StackTrace.element("groovy.ui.GroovyMain", "processArgs", "GroovyMain.java", 111),
                StackTrace.element("groovy.ui.GroovyMain", "main", "GroovyMain.java", 92),
                StackTrace.nativeElement("sun.reflect.NativeMethodAccessorImpl", "invoke0"),
                StackTrace.element("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62),
                StackTrace.element("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43),
                StackTrace.element("java.lang.reflect.Method", "invoke", "Method.java", 483),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "rootLoader", "GroovyStarter.java", 101),
                StackTrace.element("org.codehaus.groovy.tools.GroovyStarter", "main", "GroovyStarter.java", 130)
        );

        assertThat(actual, stacktraceEquals(expectedStackTrace, "main"));
    }

    @Test @Ignore
    public void jrockit6() throws Exception {

        ThreadDumpThreadSet threads = runtimeFrom("jrockit-1.6.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test @Ignore
    public void jrockit5() throws Exception {

        ThreadDumpThreadSet threads = runtimeFrom("jrockit-1.5.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test
    public void preserveThreadOrder() throws Exception {

        ThreadDumpThreadSet threads = FACTORY.fromStream(Util.resource(getClass(), "self-lock.log")).getThreads();

        List<String> expectedNames = Arrays.asList(
                "Service Thread",
                "C2 CompilerThread1",
                "C2 CompilerThread0",
                "Signal Dispatcher",
                "Finalizer",
                "Reference Handler",
                "VM Thread",
                "GC task thread#0 (ParallelGC)",
                "GC task thread#1 (ParallelGC)",
                "GC task thread#2 (ParallelGC)",
                "GC task thread#3 (ParallelGC)",
                "VM Periodic Task Thread"
        );

        List<String> actualNames = new ArrayList<String>();
        for (ThreadDumpThread thread: threads) {
            actualNames.add(thread.getName());
        }

        assertThat(actualNames, equalTo(expectedNames));
    }

    @Test
    public void parseStacktraceContaining$() throws Exception {

        ThreadDumpThreadSet threads = runtimeFrom("oraclejdk-1.7.0_51.log").getThreads();

        StackTrace actual = threads.where(nameIs("process reaper")).iterator().next().getStackTrace();
        StackTrace expected = new StackTrace(
                StackTrace.nativeElement("java.lang.UNIXProcess", "waitForProcessExit"),
                StackTrace.element("java.lang.UNIXProcess", "access$200", "UNIXProcess.java", 54),
                StackTrace.element("java.lang.UNIXProcess$3", "run", "UNIXProcess.java", 174),
                StackTrace.element("java.util.concurrent.ThreadPoolExecutor", "runWorker", "ThreadPoolExecutor.java", 1145),
                StackTrace.element("java.util.concurrent.ThreadPoolExecutor$Worker", "run", "ThreadPoolExecutor.java", 615),
                StackTrace.element("java.lang.Thread", "run", "Thread.java", 744)
        );

        assertEquals(actual, expected);
    }

    // Thread state and locks might not be consistent with each other.
    // Can not assume that, for instance, thread waiting to acquire lock is not runnable
    @Test
    @Ignore // Dumpling now rejects inconsistent models not to fail later or provide confusing results
    public void inconsistentThreadStateAndLockInformation() throws Exception {

        ThreadDumpThreadSet threads = runtimeFrom("inconsistent-locks-and-state.log").getThreads();
        ThreadDumpThread parking = threads.where(nameIs("runnable-parking-to-wait")).onlyThread();

        assertThat(parking.getStatus(), equalTo(ThreadStatus.RUNNABLE));
        assertThat(parking.getWaitingToLock(), equalTo(new ThreadLock(
                "java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject", 21032260640L
        )));
        // Based on stacktrace - not thread status
        assertThat(parking.toString(), containsString("parking to wait for"));

        threads.where(nameIs("blocked-without-monitor")).onlyThread();
    }

    @Test
    public void runnableInObjectWait() throws Exception {
        ThreadDumpThread runnable = runtimeFrom("runnable-in-object-wait.log").getThreads().onlyThread();
        assertThat(runnable.getStatus(), equalTo(ThreadStatus.RUNNABLE));
        assertThat(only(runnable.getAcquiredLocks()), equalTo(new ThreadLock(
                "hudson.remoting.UserRequest", 22040315832L
        )));
    }

    @Test
    public void ownableSynchronizers() throws Exception {
        ThreadDumpRuntime threads = runtimeFrom("ownable-synchronizers.log");
        checkOwnableSynchronizers(threads);
        checkOwnableSynchronizers(reparse(threads, Mode.MACHINE));
        checkOwnableSynchronizers(reparse(threads, Mode.HUMAN));
    }

    private void checkOwnableSynchronizers(ThreadDumpRuntime runtime) {
        ThreadDumpThreadSet threads = runtime.getThreads();
        ThreadDumpThread waiting = threads.where(nameIs("blockedThread")).onlyThread();
        ThreadDumpThread owning = threads.where(nameIs("main")).onlyThread();

        final ThreadLock lock = new ThreadLock("java.util.concurrent.locks.ReentrantLock$NonfairSync", 32296902960L);
        final Set<ThreadLock> locks = new HashSet<ThreadLock>(Arrays.asList(lock));
        assertThat(owning.getAcquiredLocks(), equalTo(locks));
        assertThat(owning.getWaitingToLock(), equalTo(null));

        assertThat(waiting.getStatus(), equalTo(ThreadStatus.PARKED));
        assertThat(waiting.getWaitingOnLock(), equalTo(lock));
        assertThat(waiting.getWaitingToLock(), nullValue());
        assertThat(waiting.getAcquiredLocks(), IsEmptyCollection.<ThreadLock>empty());
    }

    @Test
    public void crlf() throws Exception {
        ThreadDumpThreadSet threads = runtimeFrom("crlf.log").getThreads();
        assertThat(threads.size(), equalTo(2));
    }

    @Test
    public void parseOutputProducedByJvmRuntimeFactory() {
        cleaner.register(new Thread("parseOutputProducedByJvmRuntimeFactory") {
            @Override
            public void run() {
                synchronized (ThreadDumpFactoryTest.this) {
                    pause(1000);
                }
            }
        }).start();

        pause(100);

        JvmRuntime current = new JvmRuntimeFactory().currentRuntime();

        ThreadDumpThread t = reparse(current, Mode.MACHINE).getThreads().where(
                nameIs("parseOutputProducedByJvmRuntimeFactory")
        ).onlyThread();
        assertThat(only(t.getAcquiredLocks()), equalTo(ThreadLock.fromInstance(this)));

        t = reparse(current, Mode.HUMAN).getThreads().where(
                nameIs("parseOutputProducedByJvmRuntimeFactory")
        ).onlyThread();

        assertThat(only(t.getAcquiredLocks()), equalTo(ThreadLock.fromInstance(this)));
    }

    // Presumably this is a bug in certain java 6 versions from Oracle
    @Test
    public void parseThreadInObjectWaitThatDoesNotDeclareDesiredMonitor() throws Exception {
        ThreadDumpThreadSet threads = runtimeFrom("in_wait_without_monitor.log").getThreads();
        ThreadDumpThread blocked = threads.where(nameIs("blocked_without_lock")).onlyThread();
        ThreadDumpThread waiting = threads.where(nameIs("waiting_without_lock")).onlyThread();
        ThreadDumpThread timedWaiting = threads.where(nameIs("timed_waiting_without_lock")).onlyThread();

        assertThat(blocked.getWaitingToLock(), equalTo(new ThreadLock("hudson.model.Queue", 17233414264L)));
        assertThat(waiting.getWaitingToLock(), equalTo(null));
        assertThat(timedWaiting.getWaitingToLock(), equalTo(null));

        assertThat(blocked.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(waiting.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(timedWaiting.getAcquiredLocks(), Matchers.<ThreadLock>empty());
    }

    // Presumably this is a bug in certain java 6 versions from Oracle
    @Test
    public void parseBlockedThreadWithoutMonitor() throws Exception {
        ThreadDumpThread blocked = runtimeFrom("blocked-without-monitor.log").getThreads().onlyThread();
        assertThat(blocked.getAcquiredMonitors(), Matchers.<ThreadLock>empty());
        assertThat(blocked.getStatus(), equalTo(ThreadStatus.RUNNABLE));
    }

    // Presumably this is a bug in certain java 6 versions from Oracle
    @Test
    public void runnableThreadInUnsafePark() throws Exception {
        ThreadDumpThreadSet threads = runtimeFrom("runnable_in_unsafe_park.log").getThreads();
        ThreadDumpThread runnable = threads.where(nameIs("runnable")).onlyThread();

        assertThat(runnable.getStatus(), equalTo(ThreadStatus.RUNNABLE));
        assertThat(runnable.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(runnable.getWaitingOnLock(), nullValue());
        assertThat(runnable.getWaitingToLock(), nullValue());
    }

    @Test
    public void runtimeHeader() throws Exception {
        ThreadDumpRuntime runtime = runtimeFrom("crlf.log");
        String expected = String.format("%s%n%s%n",
                "2014-08-23 15:51:50", "Full thread dump OpenJDK 64-Bit Server VM (24.65-b04 mixed mode):"
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        runtime.toString(new PrintStream(out), Mode.HUMAN);
        assertThat(out.toString(), startsWith(expected));

        runtime.toString(new PrintStream(out), Mode.MACHINE);
        assertThat(out.toString(), startsWith(expected));
    }

    @Test // HotSpot seems to produce threaddump without blank lines between threads in case of deadlock
    public void noBlankLines() throws Exception {
        ThreadDumpRuntime runtime = runtimeFrom("no_blank_lines.log");
        String expected = String.format("%s%n%s%n",
                "2015-05-13 03:27:18", "Full thread dump Java HotSpot(TM) 64-Bit Server VM (24.65-b04 mixed mode):"
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        runtime.toString(new PrintStream(out), Mode.HUMAN);
        assertThat(out.toString(), startsWith(expected));

        assertThat(runtime.getThreads().size(), equalTo(9));
    }

    @Test
    public void inObjectWait() throws Exception {
        ThreadDumpRuntime runtime = runtimeFrom("in-object-wait.log");
        ThreadDumpThread blockedWaitingOn = runtime.getThreads().where(nameIs("blockedReacquiringWaitingOn")).onlyThread();
        ThreadDumpThread blockedLocked = runtime.getThreads().where(nameIs("blockedReacquiringLocked")).onlyThread();

        ThreadLock expected = new ThreadLock("java.lang.Object", 33677620560L);

        assertThat(blockedWaitingOn.getStatus(), equalTo(ThreadStatus.BLOCKED));
        assertTrue(blockedWaitingOn.getAcquiredLocks().isEmpty());
        assertThat(blockedWaitingOn.getWaitingToLock(), equalTo(expected));

        assertThat(blockedLocked.getStatus(), equalTo(ThreadStatus.BLOCKED));
        assertTrue(blockedLocked.getAcquiredLocks().isEmpty());
        assertThat(blockedLocked.getWaitingToLock(), equalTo(expected));
    }

    @Test // Do not require tab indented stacktraces
    public void doNotRequireTabs() throws Exception {
        ThreadDumpRuntime runtime = runtimeFrom("no-tabs.log");
        ThreadDumpThread thread = runtime.getThreads().where(nameIs("main")).onlyThread();

        assertThat(thread.getStackTrace().getElements().size(), equalTo(3));
    }

    @Test
    public void isStreamClosed() throws Exception {
        InputStream stream = Util.resource(getClass(), "in-object-wait.log");
        InputStream mock = spy(stream);

        FACTORY.fromStream(mock);

        Mockito.verify(mock).close();
    }

    @Test
    public void fixupHotspotUpdatingThreadStateInNonAtomicWay() throws Exception {
        ThreadDumpRuntime runtime = runtimeFrom("issue-46.log");
        ThreadDumpThread thread = runtime.getThreads().where(nameIs("Jenkins-cron-thread-8")).onlyThread();

        assertTrue(thread.getStatus().isBlocked());
        assertEquals(null, thread.getWaitingOnLock());
        assertEquals("hudson.model.Queue", thread.getWaitingToLock().getClassName());
        assertEquals(0, thread.getAcquiredMonitors().size());
        assertEquals(1, thread.getAcquiredSynchronizers().size());
    }

    @Test
    public void hexadecimalThreadIdsMightNotHavePrefix() throws Exception {
        ThreadDumpRuntime runtime = runtimeFrom("issue-59.log");
        ThreadDumpThread thread = runtime.getThreads().where(nameIs("process reaper")).onlyThread();

        assertEquals(140685595015168L, (long) thread.getTid());
        assertEquals(5199638528L, (long) thread.getNid());
    }

    @Test
    public void failToParseWhatIsNotAThreaddump() throws IOException, URISyntaxException {
        try {
            runtimeFrom("not-a-threaddump.log");
            fail();
        } catch (IllegalRuntimeStateException e) {
            // Expected
        }
    }

    @Test
    public void numberInUpperLongRange() throws Exception {
        ThreadDumpRuntime runtime = runtimeFrom("issue-71.log");
        ThreadDumpThread sut = runtime.getThreads().where(nameIs("SUT")).onlyThread();
        assertEquals(-494445558, (long) sut.getTid());
        assertEquals(-494445557, sut.getWaitingToLock().getId());
        assertEquals(-494445556, only(sut.getAcquiredSynchronizers()).getId());
        assertEquals(-494445555, (long) sut.getNid());

        // Decadic NID
        String human = new JvmThread.Builder(Thread.currentThread()).setName("Fake").setId(42).setTid(42).setNid(Short.MIN_VALUE).toString();
        assertThat(human, containsString("nid=-32768"));
        sut = FACTORY.fromString(human).getThreads().where(nameIs("Fake")).onlyThread();
        assertEquals(Short.MIN_VALUE, (long) sut.getNid());
    }

    @Test
    public void parseLong() throws Exception {
        String top = "0xffffffffffffffff";
        assertEquals(Long.toHexString(ThreadDumpFactory.parseLong(top)), -1, ThreadDumpFactory.parseLong(top));

        top = "0xfffffffffffffffe";
        assertEquals(Long.toHexString(ThreadDumpFactory.parseLong(top)), -2, ThreadDumpFactory.parseLong(top));

        top = "0x00000000000000f";
        assertEquals(Long.toHexString(ThreadDumpFactory.parseLong(top)), 15, ThreadDumpFactory.parseLong(top));

        top = "0xf0000000000000";
        assertEquals(Long.toHexString(ThreadDumpFactory.parseLong(top)), 67553994410557440L, ThreadDumpFactory.parseLong(top));

        top = "0xe0000000000000";
        assertEquals(Long.toHexString(ThreadDumpFactory.parseLong(top)), 63050394783186944L, ThreadDumpFactory.parseLong(top));

        top = "0x7fffffffffffffff";
        assertEquals(Long.toHexString(ThreadDumpFactory.parseLong(top)), 9223372036854775807L, ThreadDumpFactory.parseLong(top));

        top = "0x8000000000000000";
        assertEquals(Long.toHexString(ThreadDumpFactory.parseLong(top)), -9223372036854775808L, ThreadDumpFactory.parseLong(top));

        top = "ffffffffe2875c0a";
        assertEquals(Long.toHexString(ThreadDumpFactory.parseLong(top)), -494445558, ThreadDumpFactory.parseLong(top));
    }

    private ThreadDumpRuntime runtimeFrom(String resource) throws IOException, URISyntaxException {
        return FACTORY.fromStream(Util.resource(getClass(), resource));
    }

    private ThreadDumpRuntime runtime(ThreadDumpThread.Builder... builders) {
        return new ThreadDumpRuntime(
                new LinkedHashSet<ThreadDumpThread.Builder>(Arrays.asList(builders)),
                Arrays.asList("Expected threaddump")
        );
    }

    private static volatile int syntheticId = 42;
    private ThreadDumpThread.Builder thread(@Nonnull String name) {
        return new ThreadDumpThread.Builder().setName(name)
                // Preset unique id for purposes of the test as we can not rely
                // that SUT will correctly initialize IDs. Threads with the
                // same Ids will otherwise be collapsed into one by java.util.Set.
                // Correct factory implementation will always overwrite this.
                .setTid(syntheticId++)
        ;
    }

    private ThreadDumpThread.Builder daemon(@Nonnull String name) {
        return thread(name).setDaemon(true);
    }

    private ThreadLock lock(@Nonnull String classname, long address) {
        return new ThreadLock(classname, address);
    }

    private TypeSafeMatcher<ThreadDumpRuntime> stacktraceEquals(final StackTrace expected, final @Nonnull String threadName) {
        return new TypeSafeMatcher<ThreadDumpRuntime>() {

            // The first StackTrace variant that failed
            private StackTrace failed;

            @Override
            public void describeTo(Description description) {
                description.appendText("Runtime with same threads");
            }

            @Override
            protected boolean matchesSafely(ThreadDumpRuntime actual) {
                if (!doesMatch(expected, failed = trace(actual, threadName))) return false;
                if (!doesMatch(expected, failed = trace(reparse(actual, Mode.MACHINE), threadName))) return false;
                if (!doesMatch(expected, failed = trace(reparse(actual, Mode.HUMAN), threadName))) return false;

                return true;
            }

            @Override
            protected void describeMismatchSafely(ThreadDumpRuntime actual, Description mismatch) {
                doDescribe(expected, failed, mismatch);
            }

            private boolean doesMatch(StackTrace expected, StackTrace actual) {
                return expected.equals(actual);
            }

            private void doDescribe(StackTrace expected, StackTrace actual, Description mismatch) {
                int length = expected.size();
                if (actual.size() != length) mismatch.appendText(String.format(
                        "Stack depth differes, %d != %d", length, actual.size()
                ));

                for (int i = 0; i < length; i++) {
                    StackTraceElement exp = expected.getElement(i);
                    StackTraceElement act = actual.getElement(i);

                    if (!exp.equals(act)) {
                        mismatch.appendText(String.format("%s != %s", exp, act));
                        return;
                    }
                }
            }

            private StackTrace trace(ThreadDumpRuntime runtime, @Nonnull String threadName) {
                return runtime.getThreads().where(nameIs(threadName)).onlyThread().getStackTrace();
            }
        };
    }

    private TypeSafeMatcher<ThreadDumpRuntime> sameThreadsAs(final ThreadDumpRuntime expectedRuntime) {
        return new TypeSafeMatcher<ThreadDumpRuntime>() {

            // The first threaddump variant that failed
            private ThreadDumpRuntime failed;

            @Override
            public void describeTo(Description description) {
                description.appendText("Runtime with same threads");
            }

            @Override
            protected boolean matchesSafely(ThreadDumpRuntime actual) {
                if (!doesMatch(expectedRuntime, failed = actual)) return false;
                if (!doesMatch(expectedRuntime, failed = reparse(actual, Mode.MACHINE))) return false;
                if (!doesMatch(expectedRuntime, failed = reparse(actual, Mode.HUMAN))) return false;

                failed = null;
                return true;
            }

            private boolean doesMatch(ThreadDumpRuntime expectedRuntime, ThreadDumpRuntime actual) {
                if (expectedRuntime.getThreads().size() != actual.getThreads().size()) return false;

                for (ThreadDumpThread actualThread: actual.getThreads()) {
                    final ThreadDumpThreadSet matching = expectedRuntime.getThreads().where(nameIs(actualThread.getName()));
                    if (matching.size() != 1) return false;

                    ThreadDumpThread expectedThread = matching.onlyThread();

                    if (difference(expectedThread, actualThread) != null) return false;
                }

                return true;
            }

            @Override
            protected void describeMismatchSafely(ThreadDumpRuntime actualRuntime, Description mismatch) {
                doDescribe(expectedRuntime, failed, mismatch);
            }

            private void doDescribe(
                    ThreadDumpRuntime expectedRuntime, ThreadDumpRuntime actualRuntime, Description mismatch
            ) throws AssertionError {
                final ThreadDumpThreadSet expectedThreads = expectedRuntime.getThreads();
                final ThreadDumpThreadSet actualThreads = actualRuntime.getThreads();

                for (ThreadDumpThread actual: actualThreads) {
                    final ThreadDumpThreadSet named = expectedThreads.where(nameIs(actual.getName()));

                    if (named.size() > 1) throw new AssertionError("Several threads named: " + actual.getName());
                    if (named.size() == 0) {
                        mismatch.appendText("Unexpected Thread:\n").appendText(actual.toString());
                        return;
                    }

                    ThreadDumpThread expected = named.onlyThread();
                    String difference = difference(expected, actual);
                    if (difference == null) continue; // Equal

                    mismatch.appendText(expected.toString())
                            .appendText("\nDiffers in: ").appendText(difference).appendText("\n")
                            .appendText(actual.toString())
                    ;
                    return;
                }

                if (expectedThreads.size() == actualThreads.size()) return;

                ThreadDumpThreadSet missing = expectedThreads.ignoring(actualThreads);
                mismatch.appendText("Missing Threads:\n").appendText(missing.toString());
            }
        };
    }

    private ThreadDumpRuntime reparse(ProcessRuntime<?, ?, ?> actual) {
        return reparse(actual, Mode.MACHINE);
    }

    private ThreadDumpRuntime reparse(ProcessRuntime<?, ?, ?> actual, Mode mode) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        actual.getThreads().toString(new PrintStream(baos), mode);
        ByteArrayInputStream stream = new ByteArrayInputStream(baos.toByteArray());
        ThreadDumpRuntime reparsed = FACTORY.fromStream(stream);
        return reparsed;
    }

    // Deep equality for test purposes
    private String difference(ThreadDumpThread lhs, ThreadDumpThread rhs) {
        if (!equals(lhs.getId(), rhs.getId())) return "id";
        if (!equals(lhs.getTid(), rhs.getTid())) return "tid";
        if (!equals(lhs.getNid(), rhs.getNid())) return "nid";
        if (!equals(lhs.getName(), rhs.getName())) return "name";
        if (lhs.getPriority() != rhs.getPriority()) return "priority";
        if (lhs.isDaemon() != rhs.isDaemon()) return "daemon";
        if (!equals(lhs.getStatus(), rhs.getStatus())) return "thread status";
        if (!equals(lhs.getWaitingToLock(), rhs.getWaitingToLock())) return String.format(
                "waiting to lock (%s!=%s)", lhs.getWaitingToLock(), rhs.getWaitingToLock()
        );
        if (!equals(lhs.getWaitingOnLock(), rhs.getWaitingOnLock())) return String.format(
                "waiting on lock (%s!=%s)", lhs.getWaitingOnLock(), rhs.getWaitingOnLock()
        );
        if (!lhs.getAcquiredLocks().equals(rhs.getAcquiredLocks())) return "acquired locks";
        // if (!Arrays.equals(lhs.getStackTrace(), rhs.getStackTrace())) return "stack trace";

        return null;
    }

    private boolean equals(Object lhs, Object rhs) {
        if (lhs == null) return rhs == null;

        return lhs.equals(rhs);
    }
}
