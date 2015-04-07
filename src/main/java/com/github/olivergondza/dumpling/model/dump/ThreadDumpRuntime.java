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
package com.github.olivergondza.dumpling.model.dump;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread.Builder;

public final class ThreadDumpRuntime extends ProcessRuntime<ThreadDumpRuntime, ThreadDumpThreadSet, ThreadDumpThread> {

    /**
     * Threaddump header, either empty or terminated wit blank line so it can be prepended to the ThreadSet.
     */
    private final @Nonnull List<String> header;

    public ThreadDumpRuntime(@Nonnull Set<ThreadDumpThread.Builder> builders, @Nonnull List<String> header) {
        super(builders);
        this.header = new ArrayList<String>(header);
    }

    @Override
    protected ThreadDumpThreadSet createSet(Set<ThreadDumpThread> threads) {
        return new ThreadDumpThreadSet(this, threads);
    }

    @Override
    protected ThreadDumpThread createThread(Builder<?> builder) {
        return new ThreadDumpThread(this, (ThreadDumpThread.Builder) builder);
    }

    @Override
    public void toString(PrintStream stream, Mode mode) {
        if (!header.isEmpty()) {
            for (String line: header) {
                stream.println(line);
            }
            stream.println();
        }

        super.toString(stream, mode);
    }
}
