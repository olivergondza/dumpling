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

import groovy.swing.factory.TDFactory;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.cli.CliRuntimeFactory;
import com.github.olivergondza.dumpling.cli.CommandFailedException;
import com.github.olivergondza.dumpling.cli.ProcessStream;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Create {@link ProcessRuntime} from running local process.
 *
 * Process ID is used as a locator.
 *
 * This implementations delegates to {@link TDFactory} so it shares its features
 * and limitations.
 *
 * @author ogondza
 */
public class PidRuntimeFactory implements CliRuntimeFactory {

    private final @Nonnull String javaHome;

    public PidRuntimeFactory() {
        this(System.getProperty("java.home"));
    }

    public PidRuntimeFactory(@Nonnull String javaHome) {
        this.javaHome = javaHome;
    }

    @Override
    public @Nonnull String getKind() {
        return "process";
    }

    public @Nonnull ProcessRuntime forProcess(int pid) {
        ProcessBuilder pb = new ProcessBuilder(jstackBinary(), "-l", Integer.toString(pid));
        try {
            Process process = pb.start();

            int ret = process.waitFor();
            validateResult(process, ret);

            return new ThreadDumpFactory().fromStream(process.getInputStream());
        } catch (IOException ex) {

            throw new CommandFailedException("Unable to invoke jstack: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {

            throw new CommandFailedException("jstack invocation interrupted: " + ex.getMessage(), ex);
        }
    }

    @Override
    public @Nonnull ProcessRuntime createRuntime(String locator, ProcessStream streams) throws CommandFailedException {
        return forProcess(pid(locator));
    }

    private void validateResult(Process process, int ret) throws IOException {
        if (ret == 0) return;

        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        while (process.getErrorStream().read(buffer) != -1) {
            sb.append(new String(buffer));
        }

        throw new CommandFailedException("jstack failed with code " + ret + ": " + sb.toString().trim());
    }

    private int pid(String locator) {
        try {
            return Integer.parseInt(locator.trim());
        } catch (NumberFormatException ex) {
            throw new CommandFailedException("Unable to parse '" + locator + "' as process ID", ex);
        }
    }

    private String jstackBinary() {
        return javaHome + "/../bin/jstack";
    }
}
