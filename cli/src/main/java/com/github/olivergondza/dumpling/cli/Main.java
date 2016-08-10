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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.OptionHandlerRegistry;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.reflections.Reflections;

import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Cli entry point.
 *
 * @author ogondza
 */
public class Main {

    // TODO metaVar is not extracted from handler in: Argument "COMMAND" is required
    @Argument(required = true, metaVar = "COMMAND")
    private CliCommand handler;

    public static void main(@Nonnull String[] args) {
        int exitCode = new Main().run(args, ProcessStream.system());
        System.exit(exitCode);
    }

    /*package*/ int run(@Nonnull String[] args, @Nonnull final ProcessStream system) {
        OptionHandlerRegistry.getRegistry().registerHandler(CliCommand.class, CliCommandOptionHandler.class);
        OptionHandlerRegistry.getRegistry().registerHandler(ProcessRuntime.class, new OptionHandlerRegistry.OptionHandlerFactory() {
            public OptionHandler<?> getHandler(CmdLineParser parser, OptionDef o, Setter setter) {
                return new ProcessRuntimeOptionHandler(parser, o, setter, system);
            }
        });

        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            return handler.run(system);
        } catch (CmdLineException ex) {

            system.err().println(ex.getMessage());
            HelpCommand.printUsage(handler, system.err(), ex);
        } catch (CommandFailedException ex) {

            system.err().println(ex.getMessage());
        } catch (RuntimeException ex) {

            ex.printStackTrace(system.err());
        }

        return -1;
    }

    public static class ProcessRuntimeOptionHandler extends OptionHandler<ProcessRuntime<?, ?, ?>> {

        private final @Nonnull ProcessStream streams;

        public ProcessRuntimeOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super ProcessRuntime<?, ?, ?>> setter, ProcessStream streams) {
            super(parser, option, setter);
            this.streams = streams;
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {

            String scheme = namedParameter("KIND", params, 0);

            String locator;
            int read;
            int delim = scheme.indexOf(':');
            if (delim == -1) {
                locator = namedParameter("LOCATOR", params, 1);
                read = 2;
            } else {
                locator = scheme.substring(delim + 1);
                scheme = scheme.substring(0, delim);
                read = 1;
            }

            CliRuntimeFactory<?> factory = getFactory(scheme);
            if (factory == null) throw new UnknownRuntimeKind(owner, "Unknown runtime source kind: " + scheme);

            ProcessRuntime<?, ?, ?> runtime = factory.createRuntime(locator, streams);
            if (runtime == null) throw new AssertionError(factory.getClass() + " failed to create runtime");

            setter.addValue(runtime);

            return read;
        }

        private @Nonnull String namedParameter(String name, Parameters params, int index) throws CmdLineException {
            try {
                return params.getParameter(index);
            } catch (CmdLineException ex) {
                throw new CmdLineException(owner, ex.getMessage() + " " + name);
            }
        }

        @Override
        public String getDefaultMetaVariable() {
            return "KIND:LOCATOR";
        }

        /*package*/ static @Nonnull List<CliRuntimeFactory<?>> getFactories() {
            List<CliRuntimeFactory<?>> factories = new ArrayList<CliRuntimeFactory<?>>();
            for (Class<? extends CliRuntimeFactory<?>> type: factoryTypes()) {
                factories.add(instantiateFactory(type));
            }
            return factories;
        }

        public static @CheckForNull CliRuntimeFactory<?> getFactory(String name) {
            for (Class<? extends CliRuntimeFactory<?>> type: factoryTypes()) {
                CliRuntimeFactory<?> factory = instantiateFactory(type);
                if (name.equals(factory.getKind())) return factory;
            }

            return null;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Set<Class<? extends CliRuntimeFactory<?>>> factoryTypes() {
            return (Set) new Reflections("com.github.olivergondza.dumpling").getSubTypesOf(CliRuntimeFactory.class);
        }

        private static CliRuntimeFactory<?> instantiateFactory(Class<? extends CliRuntimeFactory<?>> type) {
            try {

                return type.newInstance();
            } catch (InstantiationException ex) {

                AssertionError e = new AssertionError("Cli handler " + type.getName() + " does not declare default constructor");
                e.initCause(ex);
                throw e;
            } catch (IllegalAccessException ex) {

                AssertionError e = new AssertionError("Cli handler " + type.getName() + " does not declare default constructor");
                e.initCause(ex);
                throw e;
            }
        }

        /*package*/ static final class UnknownRuntimeKind extends CmdLineException {

            public UnknownRuntimeKind(CmdLineParser owner, String message) {
                super(owner, message);
            }
        }
    }
}
