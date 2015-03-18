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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.cli.CliRuntimeFactory;
import com.github.olivergondza.dumpling.cli.CommandFailedException;
import com.github.olivergondza.dumpling.cli.ProcessStream;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.StackTrace;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadLock.Monitor;
import com.github.olivergondza.dumpling.model.ThreadStatus;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread.Builder;

/**
 * Instantiate {@link ProcessRuntime} from threaddump produced by <tt>jstack</tt> or similar tool.
 *
 * @author ogondza
 */
public class ThreadDumpFactory implements CliRuntimeFactory<ThreadDumpRuntime> {

    private static final StackTraceElement WAIT_TRACE_ELEMENT = StackTrace.nativeElement("java.lang.Object", "wait");

    private static final String NL = "(?:\\r\\n|\\n)";
    private static final String LOCK_SUBPATTERN = "<0x(\\w+)> \\(a ([^\\)]+)\\)";

    private static final Pattern THREAD_DELIMITER = Pattern.compile(NL + NL + "(?!\\s)");
    private static final Pattern STACK_TRACE_ELEMENT_LINE = Pattern.compile(" *at (\\S+)\\.(\\S+)\\(([^:]+?)(\\:\\d+)?\\)");
    private static final Pattern ACQUIRED_LINE = Pattern.compile("- locked " + LOCK_SUBPATTERN);
    // Oracle/OpenJdk puts unnecessary space after 'parking to wait for'
    private static final Pattern WAITING_ON_LINE = Pattern.compile("- (?:waiting on|parking to wait for ?) " + LOCK_SUBPATTERN);
    private static final Pattern WAITING_TO_LOCK_LINE = Pattern.compile("- waiting to lock " + LOCK_SUBPATTERN);
    private static final Pattern OWNABLE_SYNCHRONIZER_LINE = Pattern.compile("- " + LOCK_SUBPATTERN);
    private static final Pattern THREAD_HEADER = Pattern.compile(
            "^\"(.*)\" ([^\\n\\r]+)(?:" + NL + "\\s+java.lang.Thread.State: ([^\\n\\r]+)(?:" + NL + "(.+))?)?",
            Pattern.DOTALL
    );

    @Override
    public @Nonnull String getKind() {
        return "threaddump";
    }

    @Override
    public String getDescription() {
        return "Parse threaddrump from file, or standard input when '-' provided as a locator.";
    }

    @Override
    public @Nonnull ThreadDumpRuntime createRuntime(@Nonnull String locator, @Nonnull ProcessStream process) throws CommandFailedException {
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
    public @Nonnull ThreadDumpRuntime fromFile(File threadDump) throws IOException {
        FileInputStream fis = new FileInputStream(threadDump);
        try {
            return fromStream(fis);
        } finally {
            fis.close();
        }
    }

    /*package*/ @Nonnull ThreadDumpRuntime fromStream(InputStream stream) {
        return new ThreadDumpRuntime(threads(stream));
    }

    private @Nonnull Set<ThreadDumpThread.Builder> threads(InputStream stream) {
        Set<ThreadDumpThread.Builder> threads = new LinkedHashSet<ThreadDumpThread.Builder>();

        Scanner scanner = new Scanner(stream);
        scanner.useDelimiter(THREAD_DELIMITER);
        try {
            while (scanner.hasNext()) {
                String singleThread = scanner.next();
                ThreadDumpThread.Builder thread = thread(singleThread);
                if (thread == null) continue;
                threads.add(thread);
            }

            return threads;
        } finally {
            scanner.close();
        }
    }

    private ThreadDumpThread.Builder thread(String singleThread) {

        Matcher matcher = THREAD_HEADER.matcher(singleThread);
        if (!matcher.find()) return null;

        ThreadDumpThread.Builder builder = new ThreadDumpThread.Builder();
        builder.setName(matcher.group(1));
        initHeader(builder, matcher.group(2));

        String status = matcher.group(3);
        if (status != null) {
            builder.setThreadStatus(ThreadStatus.fromString(status));
        }

        final String trace = matcher.group(4);
        if (trace != null) {
            builder = initStacktrace(builder, trace);
        }

        return builder;
    }

    private Builder initStacktrace(Builder builder, String trace) {
        ArrayList<StackTraceElement> traceElements = new ArrayList<StackTraceElement>();

        List<ThreadLock.Monitor> monitors = new ArrayList<ThreadLock.Monitor>();
        List<ThreadLock> synchronizers = new ArrayList<ThreadLock>();
        ThreadLock waitingToLock = null; // Block waiting on monitor
        ThreadLock waitingOnLock = null; // in Object.wait()
        int depth = -1;

        StringTokenizer tokenizer = new StringTokenizer(trace, "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();

            StackTraceElement elem = traceElement(line);
            if (elem != null) {
                traceElements.add(elem);
                depth++;
                continue;
            }

            Matcher acquiredMatcher = ACQUIRED_LINE.matcher(line);
            if (acquiredMatcher.find()) {
                monitors.add(new ThreadLock.Monitor(createLock(acquiredMatcher), depth));
                continue;
            }

            Matcher waitingToMatcher = WAITING_TO_LOCK_LINE.matcher(line);
            if (waitingToMatcher.find()) {
                if (waitingToLock != null) throw new IllegalRuntimeStateException(
                        "Waiting to lock reported several times per single thread >>>%n%s%n<<<%n", trace
                );
                waitingToLock = createLock(waitingToMatcher);
                continue;
            }

            Matcher waitingOnMatcher = WAITING_ON_LINE.matcher(line);
            if (waitingOnMatcher.find()) {
                if (waitingOnLock != null) throw new IllegalRuntimeStateException(
                        "Waiting on lock reported several times per single thread >>>%n%s%n<<<%n", trace
                );
                waitingOnLock = createLock(waitingOnMatcher);
                continue;
            }

            if (line.contains("Locked ownable synchronizers:")) {
                while (tokenizer.hasMoreTokens()) {
                    line = tokenizer.nextToken();

                    if (line.contains("- None")) break;
                    Matcher matcher = OWNABLE_SYNCHRONIZER_LINE.matcher(line);
                    matcher.find();
                    synchronizers.add(createLock(matcher));
                }
            }
        }

        builder.setStacktrace(new StackTrace(traceElements));

        ThreadStatus status = builder.getThreadStatus();
        StackTraceElement innerFrame = builder.getStacktrace().getElement(0);

        // Probably a bug in JVM/jstack but let's see what we can do
        if (waitingOnLock == null && !status.isRunnable() && WAIT_TRACE_ELEMENT.equals(innerFrame)) {
            HashSet<ThreadLock> acquiredLocks = new HashSet<ThreadLock>(monitors.size());
            for (Monitor m: monitors) {
                acquiredLocks.add(m.getLock());
            }
            if (acquiredLocks.size() == 1) {
                waitingOnLock = acquiredLocks.iterator().next();
            }
        }

        if (waitingOnLock != null) {
            // Eliminate self lock that is presented in threaddumps when in Object.wait().
            filterMonitors(monitors, waitingOnLock);

            // 'waiting on' is reported even when blocked re-entering the monitor. Convert it from waitingOn to waitingTo
            if (builder.getThreadStatus().isBlocked()) {

                waitingToLock = waitingOnLock;
                waitingOnLock = null;
            }
        }

        // https://github.com/olivergondza/dumpling/issues/43
        if (waitingOnLock != null && status.isRunnable()) {
            // Presumably when entering or leaving the parked state.
            // Remove the lock instead of fixing the thread status as there is
            // no general way to tell PARKED and PARKED_TIMED apart.
            waitingOnLock = null;
        }

        if (waitingToLock != null && !status.isBlocked()) throw new IllegalRuntimeStateException(
                "%s thread declares waitingTo lock: >>>%n%s%n<<<%n", status, trace
        );
        if (waitingOnLock != null && !status.isWaiting() && !status.isParked()) throw new IllegalRuntimeStateException(
                "%s thread declares waitingOn lock: >>>%n%s%n<<<%n", status, trace
        );

        builder.setAcquiredMonitors(monitors);
        builder.setAcquiredSynchronizers(synchronizers);
        builder.setWaitingToLock(waitingToLock);
        builder.setWaitingOnLock(waitingOnLock);

        return builder;
    }

    private static final WeakHashMap<String, StackTraceElement> traceElementCache = new WeakHashMap<String, StackTraceElement>();
    private StackTraceElement traceElement(String line) {
        if (!line.startsWith("\tat ")) return null;

        StackTraceElement cached = traceElementCache.get(line);
        if (cached != null) return cached;

        Matcher match = STACK_TRACE_ELEMENT_LINE.matcher(line);
        if (!match.find()) return null;

        String sourceFile = match.group(3);
        int sourceLine = match.group(4) == null
                ? -1
                : Integer.parseInt(match.group(4).substring(1))
        ;

        if (sourceLine == -1 && "Native Method".equals(match.group(3))) {
            sourceFile = null;
            sourceLine = -2; // Magic value for native methods
        }

        StackTraceElement element = StackTrace.element(
                match.group(1), match.group(2), sourceFile, sourceLine
        );
        traceElementCache.put(line, element);
        return element;
    }

    private void filterMonitors(List<ThreadLock.Monitor> monitors, ThreadLock lock) {
        for (Iterator<Monitor> it = monitors.iterator(); it.hasNext();) {
            Monitor m = it.next();

            if (m.getLock().equals(lock)) {
                it.remove();
            }
        }
    }

    private @Nonnull ThreadLock createLock(Matcher matcher) {
        return new ThreadLock(matcher.group(2), Long.parseLong(matcher.group(1), 16));
    }

    private void initHeader(ThreadDumpThread.Builder builder, String attrs) {
        StringTokenizer tknzr = new StringTokenizer(attrs, " ");
        while (tknzr.hasMoreTokens()) {
            String token = tknzr.nextToken();
            if ("daemon".equals(token)) builder.setDaemon(true);
            else if (token.startsWith("prio=")) builder.setPriority(Integer.parseInt(token.substring(5)));
            else if (token.startsWith("tid=")) builder.setTid(parseLong(token.substring(4)));
            else if (token.startsWith("nid=")) builder.setNid(parseLong(token.substring(4)));
            else if (token.matches("#\\d+")) builder.setId(Integer.parseInt(token.substring(1)));
        }
    }

    private long parseLong(String value) {
        return value.startsWith("0x")
                ? Long.parseLong(value.substring(2), 16)
                : Long.parseLong(value, 10)
        ;
    }
}
