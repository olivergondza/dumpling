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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsEmptyCollection;
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

        ProcessThread main = threads.where(nameIs("main")).onlyThread();
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

        ThreadSet threads = runtimeFrom("oraclejdk-1.7.0_51.log").getThreads();

        assertEquals(143, threads.size());

        ProcessThread thread = threads.where(nameIs("Channel reader thread: jenkins_slave_02")).onlyThread();
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

        ThreadSet threads = runtimeFrom("oraclejdk-1.6.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test
    public void oracleJdk7() throws Exception {

        ProcessRuntime expected = runtime (
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

        ProcessRuntime actual = runtimeFrom("oraclejdk-1.7.log");
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

        ProcessRuntime expected = runtime (
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

        ProcessRuntime actual = runtimeFrom("oraclejdk-1.8.log");
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
    public void openjdk6() throws Exception {

        ProcessRuntime expected = runtime (
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

        ProcessRuntime actual = runtimeFrom("openjdk-1.6.log");
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

        ProcessRuntime expected = runtime (
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

        ProcessRuntime actual = runtimeFrom("openjdk-1.7.log");
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

        ProcessRuntime expected = runtime (
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

        ProcessRuntime actual = runtimeFrom("openjdk-1.8.log");
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

        ThreadSet threads = runtimeFrom("jrockit-1.6.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test @Ignore
    public void jrockit5() throws Exception {

        ThreadSet threads = runtimeFrom("jrockit-1.5.log").getThreads();
        assertEquals(15, threads.size());
    }

    @Test
    public void preserveThreadOrder() throws Exception {

        ThreadSet threads = new ThreadDumpFactory().fromFile(Util.resourceFile(getClass(), "self-lock.log")).getThreads();

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
        for (ProcessThread thread: threads) {
            actualNames.add(thread.getName());
        }

        assertThat(actualNames, equalTo(expectedNames));
    }

    @Test
    public void parseStacktraceContaining$() throws Exception {

        ThreadSet threads = runtimeFrom("oraclejdk-1.7.0_51.log").getThreads();

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

    @Test
    public void cliNoSuchFile() {
        run("deadlocks", "--in", "threaddump", "/there_is_no_such_file");
        assertThat(exitValue, equalTo(-1));
        assertThat(err.toString(), containsString("/there_is_no_such_file (No such file or directory)"));
        assertThat(out.toString(), equalTo(""));
    }

    // Thread state and locks might not be consistent with each other.
    // Can not assume that, for instance, thread waiting to acquire lock is not runnable
    @Test
    @Ignore // Dumpling now rejects inconsistent models not to fail later or provide confusing results
    public void inconsistentThreadStateAndLockInformation() throws Exception {
        ThreadSet threads = runtimeFrom("inconsistent-locks-and-state.log").getThreads();
        ProcessThread parking = threads.where(nameIs("runnable-parking-to-wait")).onlyThread();

        assertThat(parking.getStatus(), equalTo(ThreadStatus.RUNNABLE));
        assertThat(parking.getWaitingToLock(), equalTo(new ThreadLock(
                "java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject", 21032260640L
        )));
        // Based on stacktrace - not thread status
        assertThat(parking.toString(), containsString("parking to wait for"));

        ProcessThread blocked = threads.where(nameIs("blocked-without-monitor")).onlyThread();
    }

    @Test
    public void runnableInObjectWait() throws Exception {
        ProcessThread runnable = runtimeFrom("runnable-in-object-wait.log").getThreads().onlyThread();
        assertThat(runnable.getStatus(), equalTo(ThreadStatus.RUNNABLE));
        assertThat(only(runnable.getAcquiredLocks()), equalTo(new ThreadLock(
                "hudson.remoting.UserRequest", 22040315832L
        )));
    }

    @Test
    public void ownableSynchronizers() throws Exception {
        ProcessRuntime threads = runtimeFrom("ownable-synchronizers.log");
        checkOwnableSynchronizers(threads);
        checkOwnableSynchronizers(reparse(threads));
    }

    private void checkOwnableSynchronizers(ProcessRuntime runtime) {
        ThreadSet threads = runtime.getThreads();
        ProcessThread waiting = threads.where(nameIs("blockedThread")).onlyThread();
        ProcessThread owning = threads.where(nameIs("main")).onlyThread();

        final ThreadLock lock = new ThreadLock("java.util.concurrent.locks.ReentrantLock$NonfairSync", 32296902960L);
        final Set<ThreadLock> locks = new HashSet<ThreadLock>(Arrays.asList(lock));
        assertThat(owning.getAcquiredLocks(), equalTo(locks));
        assertThat(owning.getWaitingToLock(), equalTo(null));

        assertThat(waiting.getStatus(), equalTo(ThreadStatus.PARKED));
        assertThat(waiting.getWaitingToLock(), equalTo(lock));
        assertThat(waiting.getAcquiredLocks(), IsEmptyCollection.<ThreadLock>empty());

        assertThat(waiting.getBlockingThread(), equalTo(owning));
    }

    @Test
    public void crlf() throws Exception {
        ThreadSet threads = runtimeFrom("crlf.log").getThreads();
        assertThat(threads.size(), equalTo(2));
    }

    @Test
    public void parseOutputProducedByJvmRuntimeFactory() {
        new Thread("parseOutputProducedByJvmRuntimeFactory") {
            @Override
            public void run() {
                synchronized (ThreadDumpFactoryTest.this) {
                    pause(1000);
                }
            }
        }.start();

        pause(100);

        ProcessThread t = reparse(new JvmRuntimeFactory().currentRuntime()).getThreads().where(
                nameIs("parseOutputProducedByJvmRuntimeFactory")
        ).onlyThread();

        assertThat(only(t.getAcquiredLocks()), equalTo(ThreadLock.fromInstance(this)));
    }

    // Presumably this is a bug in certain java 6 versions from Oracle
    @Test
    public void parseThreadInObjectWaitThatDoesNotDeclareDesiredMonitor() throws Exception {
        ThreadSet threads = runtimeFrom("in_wait_without_monitor.log").getThreads();
        ProcessThread blocked = threads.where(nameIs("blocked_without_log")).onlyThread();
        ProcessThread waiting = threads.where(nameIs("waiting_without_log")).onlyThread();
        ProcessThread timedWaiting = threads.where(nameIs("timed_waiting_without_log")).onlyThread();

        assertThat(blocked.getWaitingToLock(), equalTo(new ThreadLock("hudson.model.Queue", 17233414264L)));
        assertThat(waiting.getWaitingToLock(), equalTo(null));
        assertThat(timedWaiting.getWaitingToLock(), equalTo(null));

        assertThat(blocked.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(waiting.getAcquiredLocks(), Matchers.<ThreadLock>empty());
        assertThat(timedWaiting.getAcquiredLocks(), Matchers.<ThreadLock>empty());
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
                // Preset unique id for purposes of the test as we can not rely
                // that SUT will correctly initialize IDs. Threads with the
                // same Ids will otherwise be collapsed into one by java.util.Set.
                // Correct factory implementation will always overwrite this.
                .setTid(syntheticId++)
        ;
    }

    private ProcessThread.Builder daemon(String name) {
        return thread(name).setDaemon(true);
    }

    private ThreadLock lock(@Nonnull String classname, long address) {
        return new ThreadLock(classname, address);
    }

    private TypeSafeMatcher<ProcessRuntime> stacktraceEquals(final StackTrace expected, final String threadName) {
        return new TypeSafeMatcher<ProcessRuntime>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Runtime with same threads");
            }

            @Override
            protected boolean matchesSafely(ProcessRuntime actual) {
                if (!doesMatch(expected, trace(actual, threadName))) return false;

                return doesMatch(expected, trace(reparse(actual), threadName));
            }

            @Override
            protected void describeMismatchSafely(ProcessRuntime actual, Description mismatch) {
                final StackTrace originalTrace = trace(actual, threadName);

                // report only the first failure
                if (!doesMatch(expected, originalTrace)) {
                    doDescribe(expected, originalTrace, mismatch);
                } else {
                    doDescribe(expected, trace(reparse(actual), threadName), mismatch);
                }
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

            private StackTrace trace(ProcessRuntime runtime, String threadName) {
                return runtime.getThreads().where(nameIs(threadName)).onlyThread().getStackTrace();
            }
        };
    }

    private TypeSafeMatcher<ProcessRuntime> sameThreadsAs(final ProcessRuntime expectedRuntime) {
        return new TypeSafeMatcher<ProcessRuntime>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Runtime with same threads");
            }

            @Override
            protected boolean matchesSafely(ProcessRuntime actual) {
                if (!doesMatch(expectedRuntime, actual)) return false;

                return doesMatch(expectedRuntime, reparse(actual));
            }

            private boolean doesMatch(ProcessRuntime expectedRuntime, ProcessRuntime actual) {
                if (expectedRuntime.getThreads().size() != actual.getThreads().size()) return false;

                for (ProcessThread actualThread: actual.getThreads()) {
                    final ThreadSet matching = expectedRuntime.getThreads().where(nameIs(actualThread.getName()));
                    if (matching.size() != 1) return false;

                    ProcessThread expectedThread = matching.onlyThread();

                    if (difference(expectedThread, actualThread) != null) return false;
                }

                return true;
            }

            @Override
            protected void describeMismatchSafely(ProcessRuntime actualRuntime, Description mismatch) {
                // report only first problem
                if (!doesMatch(expectedRuntime, actualRuntime)) {
                    doDescribe(expectedRuntime, actualRuntime, mismatch);
                } else {
                    doDescribe(expectedRuntime, reparse(actualRuntime), mismatch);
                }
            }

            private void doDescribe(
                    ProcessRuntime expectedRuntime, ProcessRuntime actualRuntime, Description mismatch
            ) throws AssertionError {
                final ThreadSet expectedThreads = expectedRuntime.getThreads();
                final ThreadSet actualThreads = actualRuntime.getThreads();

                for (ProcessThread actual: actualThreads) {
                    final ThreadSet named = expectedThreads.where(nameIs(actual.getName()));

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

                ThreadSet missing = expectedThreads.ignoring(actualThreads);
                mismatch.appendText("Missing Threads:\n").appendText(missing.toString());
            }
        };
    }

    private ProcessRuntime reparse(ProcessRuntime actual) {
        String output = actual.getThreads().toString();
        ByteArrayInputStream stream = new ByteArrayInputStream(output.getBytes(Charset.forName("UTF8")));
        ProcessRuntime reparsed = new ThreadDumpFactory().fromStream(stream);
        return reparsed;
    }

    // Deep equality for test purposes
    private String difference(ProcessThread lhs, ProcessThread rhs) {
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
