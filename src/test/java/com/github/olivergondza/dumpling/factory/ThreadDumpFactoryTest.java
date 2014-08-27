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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.hamcrest.Description;
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

        ProcessThread main = threads.onlyNamed("main").onlyThread();
        assertEquals(ThreadStatus.RUNNABLE, main.getThreadStatus());
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

        ProcessThread thread = threads.onlyNamed("Channel reader thread: jenkins_slave_02").onlyThread();
        assertEquals(ThreadStatus.RUNNABLE, thread.getThreadStatus());
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
                        .setWaitingOnLock(lock(0, "java.lang.ref.ReferenceQueue$Lock", 33678346384L))
                ,
                daemon("Reference Handler").setTid(191727616).setNid(18887).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock(0, "java.lang.ref.Reference$Lock", 33678167272L))
                ,
                thread("main").setTid(191326208).setNid(18881).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock(0, "java.lang.UNIXProcess", 33649075520L))
        );

        ProcessRuntime actual = runtimeFrom("oraclejdk-1.7.log");
        assertThat(actual, sameThreadsAs(expected));

        ProcessThread mainThread = actual.getThreads().onlyNamed("main").onlyThread();
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

        assertEquals(expectedStackTrace, mainThread.getStackTrace());
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
                        .setWaitingOnLock(lock(0, "java.lang.ref.ReferenceQueue$Lock", 2495908272L))
                ,
                daemon("Reference Handler").setTid(1718108160).setNid(8120).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10).setId(2)
                        .setWaitingOnLock(lock(0, "java.lang.ref.Reference$Lock", 2495922552L))
                ,
                thread("main").setTid(3059771392L).setNid(8114).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(5).setId(1)
                        .setWaitingOnLock(lock(0, "java.lang.UNIXProcess", 2468857072L))
        );

        ProcessRuntime actual = runtimeFrom("oraclejdk-1.8.log");
        assertThat(actual, sameThreadsAs(expected));

        ProcessThread mainThread = actual.getThreads().onlyNamed("main").onlyThread();
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

        assertEquals(expectedStackTrace, mainThread.getStackTrace());
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
                        .setWaitingOnLock(lock(0, "java.lang.ref.ReferenceQueue$Lock", 3773522592L))
                ,
                daemon("Reference Handler").setTid(505221120).setNid(28573).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock(0, "java.lang.ref.Reference$Lock", 3773521872L))
                ,
                thread("main").setTid(504590336).setNid(28567).setThreadStatus(ThreadStatus.IN_OBJECT_WAIT).setPriority(10)
                        .setWaitingOnLock(lock(0, "java.lang.UNIXProcess", 3791474536L))
        );

        ProcessRuntime actual = runtimeFrom("openjdk-1.6.log");
        assertThat(actual, sameThreadsAs(expected));

        ProcessThread mainThread = actual.getThreads().onlyNamed("main").onlyThread();
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

        assertEquals(expectedStackTrace, mainThread.getStackTrace());
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
    public void parseParkedLocks() throws Exception {

        ThreadSet threads = runtimeFrom("oraclejdk-1.7.0_51.log").getThreads();

        ProcessThread parking = threads.onlyNamed("ConnectionValidator").onlyThread();

        assertThat(parking.getThreadStatus(), equalTo(ThreadStatus.PARKED_TIMED));
        assertThat(parking.getWaitingOnLock().getClassName(), equalTo(
                "java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject"
        ));
    }

    @Test
    public void parseStacktraceContaining$() throws Exception {

        ThreadSet threads = runtimeFrom("oraclejdk-1.7.0_51.log").getThreads();

        StackTrace actual = threads.onlyNamed("process reaper").iterator().next().getStackTrace();
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
        run("detect-deadlocks", "--in", "threaddump", "/there_is_no_such_file");
        assertThat(exitValue, equalTo(-1));
        assertThat(err.toString(), containsString("/there_is_no_such_file (No such file or directory)"));
        assertThat(out.toString(), equalTo(""));
    }

    // Thread state and locks might not be consistent with each other.
    // Can not assume that, for instance, thread waiting to acquire lock is not runnable
    @Test
    public void inconsistentThreadStateAndLockInformation() throws Exception {
        ProcessThread thread = runtimeFrom("inconsistent-locks-and-state.log").getThreads().onlyThread();

        assertThat(thread.getThreadStatus(), equalTo(ThreadStatus.RUNNABLE));
        ThreadLock expectedLock = new ThreadLock.WithAddress(
                1, "java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject", 21032260640L
        );
        assertThat(thread.getWaitingOnLock(), equalTo(expectedLock));
        // Based on stacktrace - not thread status
        assertThat(thread.toString(), containsString("parking to wait for"));
    }

    @Test
    public void monitorOwnerInObjectWait() throws Exception {
        ThreadSet threads = runtimeFrom("inObjectWait.log").getThreads();
        ProcessThread waiting = threads.onlyNamed("monitorOwnerInObjectWait").onlyThread();
        ProcessThread owning = threads.onlyNamed("main").onlyThread();

        final ThreadLock lock = new ThreadLock.WithAddress(0, "java.lang.Object", 33677473560L);
        final Set<ThreadLock> locks = new HashSet<ThreadLock>(Arrays.asList(lock));
        assertThat(owning.getAcquiredLocks(), equalTo(locks));
        assertThat(owning.getWaitingOnLock(), equalTo(null));

        assertThat(waiting.getThreadStatus(), equalTo(ThreadStatus.IN_OBJECT_WAIT));
        assertThat(waiting.getWaitingOnLock(), equalTo(lock));
        assertThat(waiting.getAcquiredLocks(), IsEmptyCollection.<ThreadLock>empty());
    }

    @Test
    public void ownableSynchronizers() throws Exception {
        ThreadSet threads = runtimeFrom("ownable-synchronizers.log").getThreads();
        ProcessThread waiting = threads.onlyNamed("blockedThread").onlyThread();
        ProcessThread owning = threads.onlyNamed("main").onlyThread();

        final ThreadLock lock = new ThreadLock.WithAddress(0, "java.util.concurrent.locks.ReentrantLock$NonfairSync", 32296902960L);
        final Set<ThreadLock> locks = new HashSet<ThreadLock>(Arrays.asList(lock));
        assertThat(owning.getAcquiredLocks(), equalTo(locks));
        assertThat(owning.getWaitingOnLock(), equalTo(null));

        assertThat(waiting.getThreadStatus(), equalTo(ThreadStatus.PARKED));
        assertThat(waiting.getWaitingOnLock(), equalTo(lock));
        assertThat(waiting.getAcquiredLocks(), IsEmptyCollection.<ThreadLock>empty());

        assertThat(waiting.getBlockingThread(), equalTo(owning));
    }

    @Test
    public void threadNameWithQuotes() throws Exception {
        ProcessThread quotes = runtimeFrom("quoted.log").getThreads().onlyThread();
        assertThat(quotes.getName(), equalTo("\"o\""));
        assertThat(quotes.getThreadStatus(), equalTo(ThreadStatus.SLEEPING));
        assertThat(quotes.getPriority(), equalTo(10));
        assertThat(quotes.getTid(), equalTo(139651066363904L));
        assertThat(quotes.getNid(), equalTo(19257L));
    }

    @Test
    public void threadNameWithLinebreak() throws Exception {
        ProcessThread quotes = runtimeFrom("linebreaked.log").getThreads().onlyThread();
        assertThat(quotes.getName(), equalTo("Thread\nName\nWith\nLinebreaks"));
        assertThat(quotes.getThreadStatus(), equalTo(ThreadStatus.SLEEPING));
        assertThat(quotes.getPriority(), equalTo(10));
        assertThat(quotes.getTid(), equalTo(139680862656512L));
        assertThat(quotes.getNid(), equalTo(18973L));
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

    private ThreadLock lock(int depth, @Nonnull String classname, long address) {
        return new ThreadLock.WithAddress(depth, classname, address);
    }

    private TypeSafeMatcher<ProcessRuntime> sameThreadsAs(final ProcessRuntime expectedRuntime) {
        return new TypeSafeMatcher<ProcessRuntime>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Runtime with same threads");
            }

            @Override
            protected boolean matchesSafely(ProcessRuntime actual) {
                if (expectedRuntime.getThreads().size() != actual.getThreads().size()) return false;

                for (ProcessThread actualThread: actual.getThreads()) {
                    final ThreadSet matching = expectedRuntime.getThreads().onlyNamed(actualThread.getName());
                    if (matching.size() != 1) return false;

                    ProcessThread expectedThread = matching.onlyThread();

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

                ThreadSet missing = expectedThreads.ignoring(actualThreads);
                mismatch.appendText("Missing Threads:\n").appendText(missing.toString());
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
