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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;

/**
 * Create {@link ProcessRuntime} from running local process.
 *
 * Process ID is used as a locator.
 *
 * This implementations invokes jstack binary and delegates to {@link ThreadDumpFactory} so it shares its features
 * and limitations.
 *
 * @author ogondza
 */
public class PidRuntimeFactory {

    private final @Nonnull String javaHome;

    public PidRuntimeFactory() {
        this(System.getProperty("java.home"));
    }

    public PidRuntimeFactory(@Nonnull String javaHome) {
        this.javaHome = javaHome;
    }

    /**
     * @param pid Process id to examine.
     * @throws IOException When jstack invocation failed.
     * @throws InterruptedException When jstack invocation was interrupted.
     */
    public @Nonnull ThreadDumpRuntime fromProcess(long pid) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(jstackBinary(), "-l", Long.toString(pid));

        Process process = pb.start();

        // Start consuming the output without waiting for process completion not to block both processes.
        ThreadDumpRuntime runtime = null;
        RuntimeException runtimeEx = null;
        try {
            runtime = createRuntime(process);
        } catch (IllegalRuntimeStateException ex) {
            // Do not throw the exception right away so #validateResult can diagnose more severe problem first.
            runtimeEx = ex;
        }

        int ret = process.waitFor();
        validateResult(process, ret);

        if (runtimeEx != null) throw runtimeEx;

        return runtime;
    }

    // Kept for binary compatibility.
    public @Nonnull ThreadDumpRuntime fromProcess(int pid) throws IOException, InterruptedException {
        return fromProcess((long) pid);
    }

    /**
     * Extract runtime from running process.
     * @param process Jvm process.
     * @throws UnsupportedOperationException Dumpling is not able to extract needed information from Process instance.
     * @throws IllegalStateException Process has already terminated.
     */
    public @Nonnull ThreadDumpRuntime fromProcess(@Nonnull Process process) throws IOException, InterruptedException {
        try {
            int exitValue = process.exitValue();
            throw new IllegalStateException("Process terminated with " + exitValue);
        } catch (IllegalThreadStateException expected) {
            // Process alive
        }

        long pid;
        try {
            // Protected class
            Class<?>  clazz = Class.forName("java.lang.UNIXProcess");
            if (!clazz.isAssignableFrom(process.getClass())) throw new UnsupportedOperationException(
                    "Unknown java.lang.Process implementation: " + process.getClass().getName()
            );
            Field pidField = clazz.getDeclaredField("pid");
            pidField.setAccessible(true);
            pid = pidField.getLong(process);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Unable to find java.lang.UNIXProcess", e);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException("Unable to find java.lang.UNIXProcess.pid", e);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Unable to access java.lang.UNIXProcess.pid", e);
        }

        return fromProcess(pid);
    }

    /**
     * Extract runtime from current process.
     *
     * This approach is somewhat external and {@link JvmRuntimeFactory} should be preferred.
     */
    public @Nonnull ThreadDumpRuntime fromCurrentProcess() throws IOException, InterruptedException {
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) throw new IOException("Unable to extract PID from " + jvmName);

        long pid;
        try {
            pid = Long.parseLong(jvmName.substring(0, index));
        } catch (NumberFormatException e) {
            throw new IOException("Unable to extract PID from " + jvmName);
        }

        return fromProcess(pid);
    }

    protected ThreadDumpRuntime createRuntime(Process process) {
        return new ThreadDumpFactory().fromStream(process.getInputStream());
    }

    private void validateResult(Process process, int ret) throws IOException {
        if (ret == 0) return;

        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        while (process.getErrorStream().read(buffer) != -1) {
            sb.append(new String(buffer));
        }

        throw new IOException("jstack failed with code " + ret + ": " + sb.toString().trim());
    }

    private String jstackBinary() {
        String suffix = ";".equals(File.pathSeparator) ? ".exe" : "";

        File jstack = new File(javaHome + "/bin/jstack" + suffix);
        if (!jstack.exists()) {
            // This is the more common variant when java.home points to the jre/ (or other) subdirectory
            jstack = new File(javaHome + "/../bin/jstack" + suffix);
        }

        if (jstack.exists()) return jstack.getAbsolutePath();

        // Chances are there is 'jstack' on PATH that happens to be compatible
        try {
            Process p = new ProcessBuilder("jstack", "-h").start();
            if (p.waitFor() == 0) {
                return "jstack";
            }
        } catch (IOException e) {
            // Likely does not exist
        } catch (InterruptedException e) {
            // Likely does not exist
        }

        throw new UnsupportedJdk(javaHome);
    }

    public static final class UnsupportedJdk extends RuntimeException {
        private UnsupportedJdk(String jdk) {
            super("Unable to capture runtime as the JDK is missing jstack utility: " + jdk);
        }
    }
}
