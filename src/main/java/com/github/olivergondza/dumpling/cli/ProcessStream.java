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

import javax.annotation.Nonnull;

/**
 * Input/output stream aggregator.
 *
 * @author ogondza
 */
public final class ProcessStream {

    private final @Nonnull InputStream in;
    private final @Nonnull PrintStream out;
    private final @Nonnull PrintStream err;

    /*package*/ static @Nonnull ProcessStream system() {
        return new ProcessStream(System.in, System.out, System.err);
    }

    /*package*/ ProcessStream(@Nonnull InputStream in, @Nonnull PrintStream out, @Nonnull PrintStream err) {
        this.in = in;
        this.out = out;
        this.err = err;
    }

    public @Nonnull InputStream in() {
        return in;
    }

    public @Nonnull PrintStream out() {
        return out;
    }

    public @Nonnull PrintStream err() {
        return err;
    }
}
