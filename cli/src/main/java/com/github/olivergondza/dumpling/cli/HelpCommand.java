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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import com.google.auto.service.AutoService;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

@AutoService(CliCommand.class)
public class HelpCommand implements CliCommand {

    @Argument(usage = "Print detailed usage", metaVar = "COMMAND")
    private String commandName;

    @Nonnull
    @Override
    public String getName() {
        return "help";
    }

    @Nonnull
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

    /*package*/ static void printUsage(CliCommand handler, PrintStream out, CmdLineException ex) {
        if (handler == null) {
            printUsage(out);
            return;
        }
        if (getCauseOf(ex, Main.ProcessRuntimeOptionHandler.UnknownRuntimeKind.class) != null) {
            printAvailableRuntimeSources(out);
            return;
        }

        // Nested commands can throw errors related to nested handler so report help for the one that failed and the wrapper
        HandlerCmdLineException he = getCauseOf(ex, HandlerCmdLineException.class);
        if (he != null) {
            handler = he.getHandler();
        }
        printUsage(handler, out);
    }

    private static <T extends Throwable> T getCauseOf(CmdLineException ex, Class<T> type) {
        for (Throwable x = ex; x != null; x = x.getCause()) {
            if (type.isInstance(x)) return (T) x;
        }
        return null;
    }

    /*package*/ static void printUsage(PrintStream out) {
        out.printf(usage("<COMMAND> [...]%n%n"));

        out.printf("Available commands:%n%n");
        for (CliCommand handler: sortedHandlers()) {
            CmdLineParser parser = new CmdLineParser(handler);
            out.print(handler.getName());
            parser.printSingleLineUsage(out);
            out.printf("%n\t%s%n", handler.getDescription());
        }

        printAvailableRuntimeSources(out);
    }

    private static void printAvailableRuntimeSources(PrintStream out) {
        out.printf("%nAvailable runtime source KINDs:%n%n");
        for (CliRuntimeFactory<?> factory: Main.ProcessRuntimeOptionHandler.getFactories()) {
            out.println(factory.getKind());
            out.printf("\t%s%n", factory.getDescription());
        }
    }

    /*package*/ static void printUsage(CliCommand handler, PrintStream out) {
        out.print(handler.getDescription());
        out.printf("%n%n");

        CmdLineParser parser = new CmdLineParser(handler);
        out.print(usage(handler.getName()));
        parser.printSingleLineUsage(out);
        out.printf("%n");
        parser.printUsage(out);
        printLongDescription(handler, out);
    }

    private static void printLongDescription(CliCommand handler, PrintStream out) {
        String name = "command."+ handler.getName() + ".usage";
        InputStream description = handler.getClass().getResourceAsStream(name);
        if (description == null) return;

        out.printf("%n");

        try {
            try {
                forward(description, out);
            } finally {
                description.close();
            }
        } catch (IOException ex) {
            throw new CommandFailedException("Unable to read detailed usage for " + handler.getName(), ex);
        }
    }

    private static void forward(InputStream description, PrintStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = description.read(buffer)) != -1) {
            out.write(buffer, 0, len);
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
