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

import java.io.PrintStream;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class HelpCommand implements CliCommand {

    @Argument(required = false, usage = "Print detailed usage", metaVar = "COMMAND")
    private String commandName;

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Print dumpling usage";
    }

    @Override
    public int run(@Nonnull ProcessStream process) throws CmdLineException {
        if (commandName == null) {
            printUsage(process.out());
        } else {
            CliCommand command = CliCommandOptionHandler.getHandler(commandName);
            if (command == null) throw new CmdLineException(null, "No such command " + commandName);

            printUsage(command, process.out());
        }

        return 0;
    }

    /*package*/ static void printUsage(CliCommand handler, PrintStream out) {
        CmdLineParser parser = new CmdLineParser(handler);
        out.print(usage(handler.getName()));
        parser.printSingleLineUsage(out);
        out.println("\n");
        out.println(handler.getDescription());
        parser.printUsage(out);
    }

    /*package*/ static void printUsage(PrintStream out) {
        out.printf(usage("<COMMAND> [...]%n%n"));
        out.println("Available commands:");
        for (CliCommand handler: sortedHandlers()) {
            CmdLineParser parser = new CmdLineParser(handler);
            out.print(handler.getName());
            parser.printSingleLineUsage(out);
            out.printf("%n\t");
            out.println(handler.getDescription());
        }
    }

    private static SortedSet<? extends CliCommand> sortedHandlers() {
        TreeSet<CliCommand> sorted = new TreeSet<CliCommand>(new Comparator<CliCommand>() {
            @Override
            public int compare(CliCommand lhs, CliCommand rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        sorted.addAll(CliCommandOptionHandler.getAllHandlers());
        return sorted;
    }

    private static String usage(String rest) {
        return "Usage: ./dumpling.sh " + rest;
    }
}
