/*
 * The MIT License
 *
 * Copyright (c) 2015 Red Hat, Inc.
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

import javax.annotation.Nonnull;

import com.google.auto.service.AutoService;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.cli.CliCommand;
import com.github.olivergondza.dumpling.cli.ProcessStream;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.query.BlockingTree.Result;

@AutoService(CliCommand.class)
public final class BlockingTreeCommand implements CliCommand {

    @Option(name = "-i", aliases = {"--in"}, required = true, usage = "Input for process runtime")
    private ProcessRuntime<?, ?, ?> runtime;

    @Option(name = "--show-stack-traces", usage = "List stack traces of all threads involved")
    private boolean showStackTraces = false;

    @Nonnull
    @Override
    public String getName() {
        return "blocking-tree";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Print trees of blocking threads";
    }

    @Override
    public int run(@Nonnull ProcessStream process) throws CmdLineException {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Result<?, ?, ?> result = new Result(runtime.getThreads(), showStackTraces);
        result.printInto(process.out());
        return result.exitCode();
    }
}
