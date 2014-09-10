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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.InputStreamReader;
import java.util.Arrays;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Execute arbitrary groovy expression on runtime.
 *
 * @author ogondza
 */
public class GroovyCommand implements CliCommand {

    @Option(name = "-i", aliases = {"--in"}, required = true, usage = "Input for process runtime")
    private ProcessRuntime runtime;

    @Override
    public String getName() {
        return "groovy";
    }

    @Override
    public String getDescription() {
        return "Execute groovy script as a query";
    }

    @Override
    public int run(ProcessStream process) throws CmdLineException {
        Binding binding = new Binding();
        binding.setProperty("runtime", runtime);
        binding.setProperty("out", process.out());
        binding.setProperty("err", process.err());

        CompilerConfiguration cc = new CompilerConfiguration();
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStarImports("com.github.olivergondza.dumpling.cli");
        imports.addStarImports("com.github.olivergondza.dumpling.factory");
        imports.addStarImports("com.github.olivergondza.dumpling.query");
        imports.addStarImports("com.github.olivergondza.dumpling.model");
        imports.addStaticStars("com.github.olivergondza.dumpling.model.ProcessThread");
        cc.addCompilationCustomizers(imports);

        GroovyShell shell = new GroovyShell(binding, cc);
        Object exitVal = shell.run(new InputStreamReader(process.in()), "dumpling-script", Arrays.asList());
        if (exitVal != null) {
            process.out().println(exitVal);
        }

        if (exitVal instanceof Boolean) {
            return Boolean.TRUE.equals(exitVal) ? 0 : -1;
        } else if (exitVal instanceof Integer) {
            return ((Integer) exitVal);
        }

        return 0;
    }
}
