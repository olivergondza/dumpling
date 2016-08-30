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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import com.github.olivergondza.dumpling.groovy.InterpretterConfig;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Run groovysh with imported Dumpling to investigate threaddump interactively.
 *
 * @author ogondza
 */
public class GroovyshCommand implements CliCommand {

    private static final InterpretterConfig CONFIG = new InterpretterConfig();

    @Option(name = "-i", aliases = {"--in"}, usage = "Input for process runtime")
    private ProcessRuntime<?, ?, ?> runtime;

    @Argument(metaVar = "SCRIPT_ARGS", multiValued = true, usage = "Arguments to be passed to the script")
    private @Nonnull List<String> args = new ArrayList<String>();

    @Override
    public String getName() {
        return "groovysh";
    }

    @Override
    public String getDescription() {
        return "Open Groovy shell to inspect runtime";
    }

    @Override
    public int run(@Nonnull ProcessStream process) throws CmdLineException {

        IO io = new IO(new BufferedInputStream(process.in()), process.out(), process.err());

        // Do not use .inputrc as jline does not interpret it correctly: https://github.com/jline/jline2/issues/51
        System.setProperty("jline.inputrc", "~/.no.inputrc");

        CONFIG.setupDecorateMethods();
        Groovysh groovysh = new Groovysh(this.getClass().getClassLoader(), CONFIG.getDefaultBinding(process, args, runtime), io);

        groovysh.getImports().addAll(getImports());

        configureHistory(process, groovysh);

        return groovysh.run("");
    }

    private Collection<String> getImports() {
        ArrayList<String> imports = new ArrayList<String>();
        for (String starImport: CONFIG.getStarImports()) {
            imports.add(starImport + ".*");
        }
        for (String staticStar: CONFIG.getStaticStars()) {
            imports.add("static " + staticStar + ".*");
        }
        return imports;
    }

    private void configureHistory(final ProcessStream process, final Groovysh groovysh) {

        // Save history when shell ends
        Runtime.getRuntime().addShutdownHook(new Thread("Save history dumpling shutdown hook") {
            @Override
            public void run() {
                try {
                    groovysh.getHistory().flush();
                } catch (IOException ex) {
                    process.err().println("Unable to save shell history");
                    ex.printStackTrace(process.err());
                }
            }
        });
    }
}
