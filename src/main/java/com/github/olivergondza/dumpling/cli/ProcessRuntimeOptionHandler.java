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

import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.reflections.Reflections;

import com.github.olivergondza.dumpling.model.ProcessRuntime;

public class ProcessRuntimeOptionHandler extends OptionHandler<ProcessRuntime> {

    public ProcessRuntimeOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super ProcessRuntime> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        String scheme = params.getParameter(0);
        CliRuntimeFactory factory = getFactory(scheme);
        if (factory == null) throw new CmdLineException(owner, "Unknown runtime source kind: " + scheme);

        String locator = params.getParameter(1);
        ProcessRuntime runtime = factory.createRuntime(locator);
        if (runtime == null) throw new AssertionError(factory.getClass() + " failed to create runtime");

        setter.addValue(runtime);

        return 2;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "KIND LOCATOR";
    }

    public static CliRuntimeFactory getFactory(String name) {
        Reflections reflections = new Reflections("com.github.olivergondza.dumpling");
        final Set<Class<? extends CliRuntimeFactory>> types = reflections.getSubTypesOf(CliRuntimeFactory.class);

        for (Class<? extends CliRuntimeFactory> type: types) {
            CliRuntimeFactory factory = factory(type);
            if (name.equals(factory.getKind())) return factory;
        }

        return null;
    }

    private static CliRuntimeFactory factory(Class<? extends CliRuntimeFactory> type) {
        try {

            return type.newInstance();
        } catch (ReflectiveOperationException ex) {

            throw new AssertionError("Cli handler " + type.getName() + " does not declare default contructor");
        }
    }
}
