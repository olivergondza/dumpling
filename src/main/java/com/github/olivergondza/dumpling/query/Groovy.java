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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.cli.CliCommand;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Execute arbitrary groovy expression on runtime.
 *
 * @author ogondza
 */
public class Groovy implements CliCommand {

    @Option(name = "-i", aliases = {"--in"}, required = true, usage = "Input for process runtime")
    private ProcessRuntime runtime;

    public String getName() {
        return "groovy";
    }

    public String getDescription() {
        return "Execute groovy script as a query";
    }

    public int run(InputStream in, PrintStream out, PrintStream err) throws CmdLineException {
        Binding binding = new Binding();
        binding.setProperty("runtime", runtime);
        binding.setProperty("out", out);
        binding.setProperty("err", err);

        CompilerConfiguration cc = new CompilerConfiguration();
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStarImports("com.github.olivergondza.dumpling.cli");
        imports.addStarImports("com.github.olivergondza.dumpling.factory");
        imports.addStarImports("com.github.olivergondza.dumpling.model");
        imports.addStarImports("com.github.olivergondza.dumpling.query");
        cc.addCompilationCustomizers(imports);

        GroovyShell shell = new GroovyShell(binding, cc);
        Object exitVal = shell.run(script(in), "dumpling-script", Arrays.asList());
        if (exitVal instanceof Boolean) {
            return Boolean.TRUE.equals(exitVal) ? 0 : -1;
        } else if (exitVal instanceof Integer) {
            return ((Integer) exitVal);
        }

        return 0;
    }

    private String script(InputStream in) {
        try {
            return IOUtils.toString(in);
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }
}
