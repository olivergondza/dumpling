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

    private static final Pattern STACK_TRACE_ELEMENT_LINE = Pattern.compile(" *at (\\S+)\\.(\\S+)\\(([^:]+?)(\\:\\d+)?\\)");
    private static final Pattern ACQUIRED_LINE = Pattern.compile("- locked <0x(\\w+)> \\(a ([^\\)]+)\\)");
    private static final Pattern WAITING_FOR_LINE = Pattern.compile("- (?:waiting on|waiting to lock|parking to wait for ) <0x(\\w+)> \\(a ([^\\)]+)\\)");
    private static final Pattern OWNABLE_SYNCHRONIZER_LINE = Pattern.compile("- <0x(\\w+)> \\(a ([^\\)]+)\\)");
    private static final Pattern THREAD_HEADER = Pattern.compile(
            "^\"(.*)\" ([^\\n]+)(?:\\n\\s+java.lang.Thread.State: ([^\\n]+)(?:\\n(.+))?)?",
            Pattern.DOTALL
    );

    @Override
    public @Nonnull String getKind() {
        return "threaddump";
    }

    @Override
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

        for (String singleThread: content.split("\n\n(?!\\s)")) {
            ProcessThread.Builder thread = thread(singleThread);
            if (thread == null) continue;
            threads.add(thread);
        }
        return threads;
    }

    private Builder thread(String singleThread) {

        Matcher matcher = THREAD_HEADER.matcher(singleThread);
        if (!matcher.find()) return null;

        Builder builder = ProcessThread.builder();
        builder.setName(matcher.group(1));
        builder = initHeader(builder, matcher.group(2));

        String s = matcher.group(3);
        if (s != null) {
            ThreadStatus status = ThreadStatus.fromString(s);
            builder.setStatus(status);
            builder.setState(status.getState());
        }

        final String trace = matcher.group(4);
        if (trace != null) {
            builder = initStacktrace(builder, trace);
            builder = initLocks(builder, trace);
        }

        return builder;
    }

    private Builder initLocks(Builder builder, String string) {
        ArrayList<ThreadLock> acquired = new ArrayList<ThreadLock>(2);
        ArrayList<ThreadLock> waitingFor = new ArrayList<ThreadLock>(1);
        int depth = -1;

        StringTokenizer tokenizer = new StringTokenizer(string, "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();

            Matcher acquiredMatcher = ACQUIRED_LINE.matcher(line);
            if (acquiredMatcher.find()) {
                acquired.add(createLock(acquiredMatcher, depth));
                continue;
            }

            Matcher waitingForMatcher = WAITING_FOR_LINE.matcher(line);
            if (waitingForMatcher.find()) {
                waitingFor.add(createLock(waitingForMatcher, depth));
                continue;
            }

            if (line.contains("Locked ownable synchronizers:")) {
                while (tokenizer.hasMoreTokens()) {
                    line = tokenizer.nextToken();

                    if (line.contains("- None")) break;
                    Matcher matcher = OWNABLE_SYNCHRONIZER_LINE.matcher(line);
                    matcher.find();
                    acquired.add(createLock(matcher, -1));
                }
            }

            // Count stack frames - not locks
            depth++;
        }

        ThreadLock lock = null;
        switch(waitingFor.size()) {
            case 0: // Noop
            break;
            case 1:
                lock = waitingFor.get(0);
            break;
            default: throw new AssertionError("Waiting for locks: " + waitingFor.size());
        }

        // Eliminated self lock that is presented in thread dump when in Object.wait()
        if (acquired.contains(lock)) {
            assert acquired.get(0).equals(lock);
            assert builder.getStatus().isWaiting();

            acquired.remove(lock);
        }

        builder.setAcquiredLocks(acquired);
        builder.setLock(lock);

        return builder;
    }

    private @Nonnull ThreadLock.WithAddress createLock(Matcher matcher, int depth) {
        return new ThreadLock.WithAddress(
                depth, matcher.group(2), Long.parseLong(matcher.group(1), 16)
        );
    }

    private Builder initHeader(Builder builder, String attrs) {

        StringTokenizer tknzr = new StringTokenizer(attrs, " ");
        while (tknzr.hasMoreTokens()) {
            String token = tknzr.nextToken();
            if ("daemon".equals(token)) builder.setDaemon(true);
            else if (token.startsWith("prio=")) builder.setPriority(Integer.parseInt(token.substring(5)));
            else if (token.startsWith("tid=")) builder.setTid(Long.parseLong(token.substring(6), 16));
            else if (token.startsWith("nid=")) builder.setNid(Long.parseLong(token.substring(6), 16));
            else if (token.matches("#\\d+")) builder.setId(Integer.parseInt(token.substring(1)));
        }

        return builder;
    }

    private Builder initStacktrace(Builder builder, String trace) {
        Matcher match = STACK_TRACE_ELEMENT_LINE.matcher(trace);
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
