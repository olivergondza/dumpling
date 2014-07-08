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

import java.io.InputStream;
import java.io.PrintStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Cli entry point.
 *
 * @author ogondza
 */
public class Main {

    // TODO metaVar is not extracted from handler in: Argument "COMMAND" is required
    @Argument(required = true, index = 0, metaVar = "COMMAND")
    private CliCommand handler;

    public static void main(String[] args) {
        int exitCode = new Main().run(args, System.in, System.out, System.err);
        System.exit(exitCode);
    }

    /*package*/ int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            return handler.run(in, out, err);
        } catch (CmdLineException ex) {

            err.println(ex.getMessage());
            if (handler == null) {
                HelpCommand.printUsage(err);
            } else {
                HelpCommand.printUsage(handler, err);
            }
        } catch (CommandFailedException ex) {
            err.println(ex.getMessage());
        }

        return -1;
    }

    static {
        CmdLineParser.registerHandler(CliCommand.class, CliCommandOptionHandler.class);
        CmdLineParser.registerHandler(ProcessRuntime.class, ProcessRuntimeOptionHandler.class);
    }
}
