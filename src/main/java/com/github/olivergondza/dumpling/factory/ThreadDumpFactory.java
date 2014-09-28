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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.cli.CliRuntimeFactory;
import com.github.olivergondza.dumpling.cli.CommandFailedException;
import com.github.olivergondza.dumpling.cli.ProcessStream;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ProcessThread.Builder;
import com.github.olivergondza.dumpling.model.StackTrace;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;

/**
 * Instantiate {@link ProcessRuntime} from threaddump produced by <tt>jstack</tt> or similar tool.
 *
 * @author ogondza
 */
public class ThreadDumpFactory implements CliRuntimeFactory {

    private static final String NL = "(?:\\r\\n|\\n)";

    private static final Pattern THREAD_DELIMITER = Pattern.compile(NL + NL + "(?!\\s)");
    private static final Pattern STACK_TRACE_ELEMENT_LINE = Pattern.compile(" *at (\\S+)\\.(\\S+)\\(([^:]+?)(\\:\\d+)?\\)");
    private static final Pattern ACQUIRED_LINE = Pattern.compile("- locked <0x(\\w+)> \\(a ([^\\)]+)\\)");
    private static final Pattern WAITING_FOR_LINE = Pattern.compile("- (?:waiting on|waiting to lock|parking to wait for ) <0x(\\w+)> \\(a ([^\\)]+)\\)");
    private static final Pattern OWNABLE_SYNCHRONIZER_LINE = Pattern.compile("- <0x(\\w+)> \\(a ([^\\)]+)\\)");
    private static final Pattern THREAD_HEADER = Pattern.compile(
            "^\"(.*)\" ([^\\n\\r]+)(?:" + NL + "\\s+java.lang.Thread.State: ([^\\n\\r]+)(?:" + NL + "(.+))?)?",
            Pattern.DOTALL
    );

    @Override
    public @Nonnull String getKind() {
        return "threaddump";
    }

    @Override
    public @Nonnull ProcessRuntime createRuntime(@Nonnull String locator, @Nonnull ProcessStream process) throws CommandFailedException {
        if ("-".equals(locator)) {
            // Read stdin
            return fromStream(process.in());
        }

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
        FileInputStream fis = new FileInputStream(threadDump);
        try {
            return fromStream(fis);
        } finally {
            fis.close();
        }
    }

    /*package*/ @Nonnull ProcessRuntime fromStream(InputStream stream) {
        return new ProcessRuntime(threads(stream));
    }

    private @Nonnull Set<Builder> threads(InputStream stream) {
        Set<Builder> threads = new LinkedHashSet<Builder>();

        Scanner scanner = new Scanner(stream).useDelimiter(THREAD_DELIMITER);
        while (scanner.hasNext()) {
            String singleThread = scanner.next();
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

        String status = matcher.group(3);
        if (status != null) {
            builder.setThreadStatus(ThreadStatus.fromString(status));
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

        // Eliminate self lock that is presented in threaddumps when in Object.wait()
        if (
                (builder.getThreadStatus().isWaiting() || builder.getThreadStatus() == ThreadStatus.BLOCKED) &&
                builder.getStacktrace().getElemens().get(0).equals(StackTrace.nativeElement("java.lang.Object", "wait"))
        ) {
            // Sometimes there are threads that are in Object.wait() and
            // (TIMED_)WAITING, yet does not declare to wait on self monitor
            if (lock == null) {
                ThreadLock.WithAddress data = (ThreadLock.WithAddress) acquired.get(0);
                lock = new ThreadLock.WithAddress(0, data.getClassName(), data.getAddress());
            }

            acquired.removeAll(Collections.singleton(lock));
        }

        builder.setAcquiredLocks(acquired);
        builder.setWaitingOnLock(lock);

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
            else if (token.startsWith("tid=")) builder.setTid(parseLong(token.substring(4)));
            else if (token.startsWith("nid=")) builder.setNid(parseLong(token.substring(4)));
            else if (token.matches("#\\d+")) builder.setId(Integer.parseInt(token.substring(1)));
        }

        return builder;
    }

    private long parseLong(String value) {
        return value.startsWith("0x")
                ? Long.parseLong(value.substring(2), 16)
                : Long.parseLong(value, 10)
        ;
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
