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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.reflections.Reflections;

public class CliCommandOptionHandler extends OptionHandler<CliCommand> {

    public CliCommandOptionHandler(
            CmdLineParser parser, OptionDef option, Setter<? super CliCommand> setter
    ) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        String name = params.getParameter(0);
        CliCommand handler = getHandler(name);

        if (handler == null) throw new CmdLineException(
                owner, "Command \"" + name + "\" not found"
        );

        setter.addValue(handler);

        // Collect subcommand arguments
        int paramCount = params.size();
        String[] subCommandParams = new String[paramCount - 1];
        for (int i = 1; i < paramCount; i++) {
            subCommandParams[i - 1] = params.getParameter(i);
        }
        new CmdLineParser(handler).parseArgument(subCommandParams);

        return params.size(); // All arguments consumed
    }

    @Override
    public String getDefaultMetaVariable() {
        return "COMMAND";
    }

    public static @CheckForNull CliCommand getHandler(String name) {
        for (CliCommand handler: getAllHandlers()) {
            if (handler.getName().equals(name)) {
                return handler;
            }
        }

        return null;
    }

    public static @Nonnull Set<? extends CliCommand> getAllHandlers() {
        Reflections reflections = new Reflections("com.github.olivergondza.dumpling");
        final Set<Class<? extends CliCommand>> types = reflections.getSubTypesOf(CliCommand.class);

        final Set<CliCommand> handlers = new HashSet<CliCommand>();
        for (Class<? extends CliCommand> type: types) {
            try {

                handlers.add(type.newInstance());
            } catch (InstantiationException ex) {

                throw new AssertionError("Cli handler " + type.getName() + " does not declare default contructor", ex);
            } catch (IllegalAccessException ex) {

                throw new AssertionError("Cli handler " + type.getName() + " does not declare default contructor", ex);
            }
        }

        return handlers;
    }
}
