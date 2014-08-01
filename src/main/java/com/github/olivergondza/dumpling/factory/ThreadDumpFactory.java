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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

import com.github.olivergondza.dumpling.cli.CliRuntimeFactory;
import com.github.olivergondza.dumpling.cli.CommandFailedException;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ProcessThread.Builder;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class ThreadDumpFactory implements CliRuntimeFactory {

    public @Nonnull String getKind() {
        return "threaddump";
    }

    public @Nonnull ProcessRuntime createRuntime(String locator) throws CommandFailedException {
        try {
            return fromFile(new File(locator));
        } catch (IOException ex) {
            throw new CommandFailedException(ex);
        }
    }

    /**
     * Create runtime from thread dump.
     *
     * @throws IOException File could not be loaded.
     */
    public @Nonnull ProcessRuntime fromFile(File threadDump) throws IOException {
        String content = IOUtils.toString(threadDump.toURI());

        return new ProcessRuntime(threads(content));
    }

    private @Nonnull Set<Builder> threads(String content) {
        Set<Builder> threads = new LinkedHashSet<Builder>();

        for (String singleThread: content.split("\n\n")) {
            ProcessThread.Builder thread = thread(singleThread);
            if (thread == null) continue;
            threads.add(thread);
        }
        return threads;
    }

    private Builder thread(String singleThread) {

        String[] chunks = singleThread.split("\n", 3);

        Builder builder = ProcessThread.builder();
        builder = initHeader(builder, chunks[0]);
        if (builder == null) return null;

        if (chunks.length > 1) {
            builder = initStatus(builder, chunks[1]);
        }

        if (chunks.length > 2) {
            builder = initStacktrace(builder, chunks[2]);
            builder = initLocks(builder, chunks[2]);
        }

        return builder;
    }

    private Builder initLocks(Builder builder, String string) {
        Matcher acquiredLine = Pattern.compile("- locked <0x(\\w+)> \\(a ([^\\)]+)\\)").matcher(string);
        Matcher waitingForLine = Pattern.compile("- (?:waiting on|waiting to lock) <0x(\\w+)> \\(a ([^\\)]+)\\)").matcher(string);

        HashSet<ThreadLock> acquired = new HashSet<ThreadLock>(2);
        HashSet<ThreadLock> waitingFor = new HashSet<ThreadLock>(1);
        while (acquiredLine.find()) {
            acquired.add(new ThreadLock.WithAddress(
                    acquiredLine.group(2), Long.parseLong(acquiredLine.group(1), 16)
            ));
        }

        while (waitingForLine.find()) {
            waitingFor.add(new ThreadLock.WithAddress(
                    waitingForLine.group(2), Long.parseLong(waitingForLine.group(1), 16)
            ));
        }

        builder.setAcquiredLocks(acquired);

        switch(waitingFor.size()) {
            case 0: // Noop
            break;
            case 1:
                builder.setLock(waitingFor.iterator().next());
            break;
            default: throw new AssertionError("Waiting for locks: " + waitingFor.size());
        }

        return builder;
    }

    private Builder initHeader(Builder builder, String headerLine) {
        if (!headerLine.startsWith("\"")) return null;

        int endOfName = headerLine.indexOf('"', 1);
        builder.setName(headerLine.substring(1, endOfName));

        StringTokenizer tknzr = new StringTokenizer(headerLine.substring(endOfName + 1), " ");
        while (tknzr.hasMoreTokens()) {
            String token = tknzr.nextToken();
            if ("daemon".equals(token)) builder.setDaemon(true);
            else if (token.startsWith("prio=")) builder.setPriority(Integer.parseInt(token.substring(5)));
            else if (token.startsWith("tid=")) builder.setTid(Long.parseLong(token.substring(6), 16));
            else if (token.startsWith("nid=")) builder.setNid(Long.parseLong(token.substring(6), 16));
        }

        return builder;
    }

    private Builder initStatus(Builder builder, String statusLine) {
        Matcher matcher = Pattern.compile("\\s*java.lang.Thread.State: (.*)").matcher(statusLine);
        matcher.find();

        final ThreadStatus status = ThreadStatus.fromString(matcher.group(1));
        builder.setStatus(status);
        builder.setState(status.getState());

        return builder;
    }

    private Builder initStacktrace(Builder builder, String trace) {

        Matcher match = Pattern.compile(" *at (\\S+)\\.(\\w+)\\(([^:]+?)(\\:\\d+)?\\)").matcher(trace);
        ArrayList<StackTraceElement> traceElements = new ArrayList<StackTraceElement>();

        while (match.find()) {

            String sourceFile = match.group(3);
            int sourceLine = match.group(4) == null
                    ? -1
                    : Integer.parseInt(match.group(4).substring(1))
            ;

            if (sourceLine == -1 && "Native Method".equals(match.group(3))) {
                sourceFile = null;
                sourceLine = -2; // Magic value for native methods
            }

            traceElements.add(new StackTraceElement(
                    match.group(1), match.group(2), sourceFile, sourceLine
            ));
        }

        return builder.setStacktrace(
                traceElements.toArray(new StackTraceElement[traceElements.size()])
        );
    }
}
