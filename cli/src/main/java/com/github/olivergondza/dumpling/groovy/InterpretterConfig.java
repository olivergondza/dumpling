/*
 * The MIT License
 *
 * Copyright (c) 2015 Red Hat, Inc.
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
package com.github.olivergondza.dumpling.groovy;

import groovy.lang.Binding;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.olivergondza.dumpling.cli.ProcessStream;
import com.github.olivergondza.dumpling.model.ProcessRuntime;

/**
 * Specific config for CLI clients.
 *
 * @author ogondza
 */
public class InterpretterConfig extends GroovyInterpretterConfig {

    /**
     * Default binding to be used in groovy interpreters.
     *
     * Dumpling exposed API is available via <tt>D</tt> property.
     */
    public Binding getDefaultBinding(@Nonnull ProcessStream stream, @Nonnull List<String> args, @Nullable ProcessRuntime<?, ?, ?> runtime) {
        Binding binding = new Binding();
        binding.setProperty("out", stream.out());
        binding.setProperty("err", stream.err());

        binding.setProperty("D", new GroovyApiEntryPoint(args, runtime, "D"));

        return binding;
    }
}
