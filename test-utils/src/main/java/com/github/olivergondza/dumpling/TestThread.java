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

import static com.github.olivergondza.dumpling.Util.pause;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * SUT thread to be observed from tests.
 *
 * Can be run as local thread of remote process.
 */
public final class TestThread {

    public static final int JMX_PORT = 9876;
    public static final @Nonnull String JMX_HOST = "localhost";
    public static final @Nonnull String JMX_USER = "user";
    public static final @Nonnull String JMX_PASSWD = "secret_passwd";
    public static final @Nonnull String JMX_CONNECTION = JMX_HOST + ":" + JMX_PORT;
    public static final @Nonnull String JMX_AUTH_CONNECTION = JMX_USER + ":" + JMX_PASSWD + "@" + JMX_CONNECTION;
    public static final String MARKER = "DUMPLING-SUT-IS-READY";

    // Observable process entry point - not to be invoked directly
    public static synchronized void main(String... args) throws InterruptedException {
        runThread();

        System.out.println(MARKER);

        // Block process forever
        TestThread.class.wait();
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

    /* Client is expected to dispose the thread */
    public static Process runJmxObservableProcess(boolean auth) throws Exception {
        String //cp = "target/test-classes:target/classes"; // Current module
        cp = TestThread.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

        List<String> args = new ArrayList<String>();
        args.add("java");
        args.add("-cp");
        args.add(cp);
        args.add("-Djava.util.logging.config.file=" + Util.asFile(Util.resource(TestThread.class, "logging.properties")).getAbsolutePath());
        args.add("-Dcom.sun.management.jmxremote");
        args.add("-Dcom.sun.management.jmxremote.port=" + JMX_PORT);
        args.add("-Dcom.sun.management.jmxremote.local.only=false");
        args.add("-Dcom.sun.management.jmxremote.authenticate=" + auth);
        args.add("-Dcom.sun.management.jmxremote.ssl=false");
        args.add("-Djava.rmi.server.hostname=127.0.0.1");
        if (auth) {
            args.add("-Dcom.sun.management.jmxremote.password.file=" + getCredFile("jmxremote.password"));
            args.add("-Dcom.sun.management.jmxremote.access.file=" + getCredFile("jmxremote.access"));
        }
        args.add("com.github.olivergondza.dumpling.TestThread");
        ProcessBuilder pb = new ProcessBuilder(args).redirectErrorStream(true);
        final Process process = pb.start();

        BufferedInputStream bis = new BufferedInputStream(process.getInputStream());
        bis.mark(1024 * 1024 * 1);
        while (!isUp(bis)) {
            try {
                int exit = process.exitValue();
                throw new AssertionError("Test process terminated prematurely: " + exit);
            } catch (IllegalThreadStateException ex) {
                // Still running
            }
        }

        return process;
    }

    private static boolean isUp(BufferedInputStream is) throws IOException {
        is.reset();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            String line = new String(buffer, 0, length);
            if (line.contains(MARKER)) return true;
        }

        return false;
    }

    private static String getCredFile(String path) throws Exception {
        String file = Util.asFile(Util.resource(TestThread.class, path)).getAbsolutePath();
        // Workaround http://jira.codehaus.org/browse/MRESOURCES-132
        new ProcessBuilder("chmod", "600", file).start().waitFor();
        return file;
    }
}
