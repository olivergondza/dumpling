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
package com.github.olivergondza.dumpling.query;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.cli.CliCommand;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

public class TopContenders {

    /**
     * Get threads that block other threads.
     *
     * @return Mapping between blocking thread and a set of blocked threads.
     * Map is sorted by the number of blocked threads.
     */
    public @Nonnull Map<ProcessThread, ThreadSet> getAll(ProcessRuntime runtime) {
        Map<ProcessThread, ThreadSet> contenders = new TreeMap<ProcessThread, ThreadSet>(new Comparator<ProcessThread>() {
            public int compare(ProcessThread lhs, ProcessThread rhs) {
                int lhsSize = lhs.getBlockedThreads().size();
                int rhsSize = rhs.getBlockedThreads().size();

                if (lhsSize > rhsSize) return -1;
                if (lhsSize < rhsSize) return 1;

                return 0;
            }
        });

        for (ProcessThread thread: runtime.getThreads()) {
            ThreadSet blocked = thread.getBlockedThreads();
            if (blocked.isEmpty()) continue;

            contenders.put(thread, blocked);
        }
        return contenders;
    }

    public static class Command implements CliCommand {

        @Option(name = "-i", aliases = {"--in"}, required = true, usage = "Input for process runtime")
        private ProcessRuntime runtime;

        public String getName() {
            return "top-contenders";
        }

        public String getDescription() {
            return "Detect top-contenders, threads that block largest number of other threads";
        }

        public int run(InputStream in, PrintStream out, PrintStream err) throws CmdLineException {

            Map<ProcessThread, ThreadSet> contenders = new TopContenders().getAll(runtime);

            out.print(contenders.size());
            out.println(" blocking threads");
            out.println();

            Set<ProcessThread> engagedThreads = new LinkedHashSet<ProcessThread>();
            for (Entry<ProcessThread, ThreadSet> contention: contenders.entrySet()) {

                engagedThreads.add(contention.getKey());
                out.print("* ");
                out.println(contention.getKey().getHeader());
                int i = 1;
                for (ProcessThread blocked: contention.getValue()) {

                    engagedThreads.add(blocked);
                    out.printf("  (%d) ", i++);
                    out.println(blocked.getHeader());
                }
            }

            out.println();
            out.print(new ThreadSet(runtime, engagedThreads).toString());

            return contenders.size();
        }
    }
}
