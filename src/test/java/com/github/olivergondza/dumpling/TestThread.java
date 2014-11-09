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

import java.util.ArrayList;
import java.util.List;

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

    // Observable process entry point - not to be invoked directly
    public static synchronized void main(String... args) throws InterruptedException {
        runThread();

        // Block process forever
        TestThread.class.wait();
    }

    public static Thread runThread() {
        Thread thread = new Thread("remotely-observed-thread") {
            @Override
            public synchronized void run() {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            };
        };
        thread.start();

        Util.pause(1000);

        return thread;
    }

    public static Process runJmxObservableProcess(boolean auth) throws Exception {
        List<String> args = new ArrayList<String>();
        args.add("java");
        args.add("-cp");
        args.add("target/test-classes:target/classes");
        args.add("-Dcom.sun.management.jmxremote");
        args.add("-Dcom.sun.management.jmxremote.port=" + JMX_PORT);
        args.add("-Dcom.sun.management.jmxremote.local.only=false");
        args.add("-Dcom.sun.management.jmxremote.authenticate=" + auth);
        args.add("-Dcom.sun.management.jmxremote.ssl=false");
        if (auth) {
            args.add("-Dcom.sun.management.jmxremote.password.file=" + getCredFile("jmxremote.password"));
            args.add("-Dcom.sun.management.jmxremote.access.file=" + getCredFile("jmxremote.access"));
        }
        args.add("com.github.olivergondza.dumpling.TestThread");

        final Process process = new ProcessBuilder(args).inheritIO().start();

        Util.pause(1000);

        try {
            process.exitValue();
            throw new AssertionError("Test process process terminated prematurelly");
        } catch (IllegalThreadStateException ex) {
            Runtime.getRuntime().addShutdownHook(new DestroyProcess(process));
            return process;
        }
    }

    private static String getCredFile(String path) throws Exception {
        String file = Util.resourceFile(TestThread.class, path).getAbsolutePath();
        // Workaround http://jira.codehaus.org/browse/MRESOURCES-132
        new ProcessBuilder("chmod", "600", file).start().waitFor();
        return file;
    }

    private static final class DestroyProcess extends Thread {
        private final Process process;

        private DestroyProcess(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            process.destroy();
        }
    }
}