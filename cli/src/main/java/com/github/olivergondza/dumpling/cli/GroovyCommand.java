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
package com.github.olivergondza.dumpling.cli;

import com.google.auto.service.AutoService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.groovy.InterpretterConfig;
import com.github.olivergondza.dumpling.model.ModelObject;
import com.github.olivergondza.dumpling.model.ModelObject.Mode;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Execute arbitrary groovy expression on runtime.
 *
 * @author ogondza
 */
@AutoService(CliCommand.class)
public class GroovyCommand implements CliCommand {

    private static final InterpretterConfig CONFIG = new InterpretterConfig();

    @Option(name = "-i", aliases = {"--in"}, usage = "Input for process runtime")
    private ProcessRuntime<?, ?, ?> runtime;

    @Option(name = "-s", aliases = {"--script"}, usage = "Script file to execute")
    private String script;

    @Option(name = "-e", aliases = {"--expression"}, usage = "Script expression to execute")
    private String expression;

    @Option(name = "-p", aliases = {"--porcelain"}, usage = "Show in a format designed for machine consumption")
    private boolean porcelain = false;

    @Argument(metaVar = "SCRIPT_ARGS", multiValued = true, usage = "Arguments to be passed to the script")
    private @Nonnull List<String> args = new ArrayList<String>();

    @Nonnull
    @Override
    public String getName() {
        return "groovy";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Execute groovy script as a query";
    }

    @Override
    public int run(@Nonnull ProcessStream process) throws CmdLineException {
        Binding binding = CONFIG.getDefaultBinding(process, args, runtime);
        if (runtime != null) {
            binding.setProperty("runtime", runtime); // Compatibility
        }

        GroovyShell shell = new GroovyShell(binding, CONFIG.getCompilerConfiguration());
        CONFIG.setupDecorateMethods();
        Object exitVal = shell.run(getScript(process), "dumpling-script", args.toArray(new String[args.size()]));
        if (exitVal != null) {
            if (exitVal instanceof ModelObject) {
                final ModelObject model = (ModelObject) exitVal;
                model.toString(process.out(), porcelain ? Mode.MACHINE : Mode.HUMAN);
            } else {
                process.out().println(exitVal);
            }
        }

        if (exitVal instanceof Boolean) {
            return Boolean.TRUE.equals(exitVal) ? 0 : -1;
        } else if (exitVal instanceof Integer) {
            return ((Integer) exitVal);
        }

        return 0;
    }

    private InputStreamReader getScript(ProcessStream process) {
        InputStream scriptStream;

        if (expression != null && !expression.isEmpty()) {
            scriptStream = new ByteArrayInputStream(expression.getBytes());
        } else if (script != null && !"-".equals(script)) {
            try {
                scriptStream = new FileInputStream(script);
            } catch (FileNotFoundException ex) {
                throw new CommandFailedException(ex.getMessage(), ex);
            }
        } else {
            scriptStream = process.in();
            try {
                if (process.in().available() == 0) {
                    process.err().println("Reading script from standard input...");
                }
            } catch (IOException e) {
            }
        }

        return new InputStreamReader(scriptStream);
    }
}
