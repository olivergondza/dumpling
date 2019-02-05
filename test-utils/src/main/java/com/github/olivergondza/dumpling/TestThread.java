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
package com.github.olivergondza.dumpling;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.olivergondza.dumpling.Util.pause;
import static com.github.olivergondza.dumpling.Util.processBuilder;
import static com.github.olivergondza.dumpling.Util.processTerminatedPrematurely;

/**
 * SUT thread to be observed from tests.
 *
 * Can be run as local thread of remote process.
 */
public final class TestThread {

    public static final @Nonnull String JMX_HOST = "localhost";
    public static final @Nonnull String JMX_USER = "user";
    public static final @Nonnull String JMX_PASSWD = "secret_passwd";

    public static final String MARKER = "DUMPLING-SUT-IS-READY";

    // Observable process entry point - not to be invoked directly
    public static synchronized void main(String... args) throws InterruptedException {
        runThread();

        System.out.println(MARKER);

        // Block process forever
        TestThread.class.wait();
        throw new Error("Should never get here");
    }

    public static Thread runThread() {
        final CountDownLatch cdl = new CountDownLatch(1);
        Thread thread = new Thread("remotely-observed-thread") {
            @Override
            public synchronized void run() {
                try {
                    cdl.countDown();
                    this.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        };
        thread.setDaemon(true);
        thread.setPriority(7);
        thread.start();

        try {
            cdl.await();
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        }

        return thread;
    }

    public static Thread setupSleepingThreadWithLock() {
        final ReentrantLock lock = new ReentrantLock();
        Thread thread = new Thread("sleepingThreadWithLock") {
            @Override
            public void run() {
                lock.lock();
                pause(10000);
            }
        };
        thread.start();
        while(!lock.isLocked()) {
            pause(1000);
        }
        return thread;
    }

    private static int getFreePort() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        try {
            return serverSocket.getLocalPort();
        } finally {
            serverSocket.close();
        }
    }

    /* Client is expected to dispose the thread */
    public static JMXProcess runJmxObservableProcess(boolean auth) throws Exception {
        int jmxPort = getFreePort();

        String //cp = "target/test-classes:target/classes"; // Current module
        // Use file to convert URI to a path platform FS would understand,
        cp = new File(TestThread.class.getProtectionDomain().getCodeSource().getLocation().toURI().getSchemeSpecificPart()).getAbsolutePath();
        List<String> args = new ArrayList<String>();
        args.add("java");
        args.add("-cp");
        args.add(cp);
        args.add("-Djava.util.logging.config.file=" + Util.asFile(Util.resource(TestThread.class, "logging.properties")).getAbsolutePath());
        args.add("-Dcom.sun.management.jmxremote");
        args.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
        args.add("-Dcom.sun.management.jmxremote.local.only=false");
        args.add("-Dcom.sun.management.jmxremote.authenticate=" + auth);
        args.add("-Dcom.sun.management.jmxremote.ssl=false");
        args.add("-Djava.rmi.server.hostname=127.0.0.1");
        if (auth) {
            args.add("-Dcom.sun.management.jmxremote.password.file=" + getCredFile("jmxremote.password"));
            args.add("-Dcom.sun.management.jmxremote.access.file=" + getCredFile("jmxremote.access"));
        }
        args.add("com.github.olivergondza.dumpling.TestThread");
        ProcessBuilder pb = processBuilder().command(args);
        String processLine = pb.command().toString();
        final Process process = pb.start();

        BufferedInputStream bis = new BufferedInputStream(process.getInputStream());
        bis.mark(1024 * 1024 * 1);
        String out = "never_tried";
        for (int i = 0; i < 10; i++) {
            out = isUp(bis);
            if (out != null) return new JMXProcess(process, jmxPort);

            try {
                int exit = process.exitValue();
                throw processTerminatedPrematurely(process, exit, processLine);
            } catch (IllegalThreadStateException ex) {
                // Still running
            }

            Thread.sleep(500);
        }

        throw new AssertionError("Unable to bring to SUT up in time: " + out);
    }

    public static final class JMXProcess extends Process {

        private final Process p;
        public final int JMX_PORT;
        public final @Nonnull String JMX_CONNECTION;
        public final @Nonnull String JMX_AUTH_CONNECTION;

        public JMXProcess(Process p, int port) {
            this.p = p;
            JMX_PORT = port;
            JMX_CONNECTION = JMX_HOST + ":" + JMX_PORT;
            JMX_AUTH_CONNECTION = JMX_USER + ":" + JMX_PASSWD + "@" + JMX_CONNECTION;
        }

        @Override public OutputStream getOutputStream() {
            return p.getOutputStream();
        }

        @Override public InputStream getInputStream() {
            return p.getInputStream();
        }

        @Override public InputStream getErrorStream() {
            return p.getErrorStream();
        }

        @Override public int waitFor() throws InterruptedException {
            return p.waitFor();
        }

        @Override public int exitValue() {
            return p.exitValue();
        }

        @Override public void destroy() {
            p.destroy();
        }

        public Process getNativeProcess() {
            return p;
        }

        // Copy&paste from PidRuntimeFactory
        public long pid() {
            Throwable problem = null;
            try {
                Method pidMethod = Process.class.getMethod("pid");
                return (long) (Long) pidMethod.invoke(p);
            } catch (NoSuchMethodException e) {
                // Not Java 9 - Fallback
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof UnsupportedOperationException) {
                    // Process impl might not support management - Fallback
                    problem = cause;
                } else if (cause instanceof SecurityException) {
                    // Access to monitoring is rejected - Fallback
                    problem = cause;
                } else {
                    throw new AssertionError(e);
                }
            }

            // Fallback for Java6+ on unix. This is known not to work for Java8 on Windows.
            if (!"java.lang.UNIXProcess".equals(p.getClass().getName())) throw new UnsupportedOperationException(
                    "Unknown java.lang.Process implementation: " + p.getClass().getName(),
                    problem
            );

            try {
                // Protected class
                Class<?>  clazz = Class.forName("java.lang.UNIXProcess");
                Field pidField = clazz.getDeclaredField("pid");
                pidField.setAccessible(true);
                return pidField.getLong(p);
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException("Unable to find java.lang.UNIXProcess", e);
            } catch (NoSuchFieldException e) {
                throw new UnsupportedOperationException("Unable to find java.lang.UNIXProcess.pid", e);
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException("Unable to access java.lang.UNIXProcess.pid", e);
            }
        }
    }

    private static String isUp(BufferedInputStream is) throws IOException {
        is.reset();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            String line = new String(buffer, 0, length);
            if (line.contains(MARKER)) return line;
        }

        return null;
    }

    private static String getCredFile(String path) throws Exception {
        String file = Util.asFile(Util.resource(TestThread.class, path)).getAbsolutePath();
        // Workaround http://jira.codehaus.org/browse/MRESOURCES-132
        Process process = new ProcessBuilder("chmod", "600", file).start();
        if (process.waitFor() != 0) {
            throw new RuntimeException(
                    "Failed to adjust permissions: " + Util.asString(process.getErrorStream())
            );
        }
        return file;
    }
}
