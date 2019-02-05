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
package com.github.olivergondza.dumpling.cli;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.Arrays;
import java.util.Collections;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.groovy.InterpretterConfig;
import com.github.olivergondza.dumpling.model.ModelObject.Mode;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ThreadSet;

public class GrepCommand implements CliCommand {

    private static final InterpretterConfig CONFIG = new InterpretterConfig();
    private static final String SCRIPT_STUB = "D.runtime.threads.grep { thread -> %s }";

    @Option(name = "-i", aliases = {"--in"}, usage = "Input for process runtime")
    private ProcessRuntime<?, ?, ?> runtime;

    @Option(name = "-p", aliases = {"--porcelain"}, usage = "Show in a format designed for machine consumption")
    private boolean porcelain = false;

    @Argument(metaVar = "PREDICATE", usage = "Groovy expression used as a filtering criteria", required = true)
    private String predicate;

    @Override
    public String getName() {
        return "grep";
    }

    @Override
    public String getDescription() {
        return "Filter threads using groovy expression";
    }

    @Override
    public int run(ProcessStream process) throws CmdLineException {
        CONFIG.setupDecorateMethods();
        Binding binding = CONFIG.getDefaultBinding(process, Collections.<String>emptyList(), runtime);
        GroovyShell shell = new GroovyShell(binding, CONFIG.getCompilerConfiguration());

        CONFIG.setupDecorateMethods();
        String script = String.format(SCRIPT_STUB, predicate);
        ThreadSet<?, ?, ?> set = (ThreadSet<?, ?, ?>) shell.run(script, "dumpling-script", Collections.emptyList());

        set.toString(process.out(), porcelain ? Mode.MACHINE : Mode.HUMAN);
        process.err().printf("Threads: %d%n", set.size());

        return set.isEmpty() ? 1 : 0;
    }
}
