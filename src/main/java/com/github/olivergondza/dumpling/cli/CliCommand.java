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

import org.kohsuke.args4j.CmdLineException;


/**
 * CLI handler, usually a query invoker.
 *
 * Used to make queries available though CLI. Command arguments are bound using args4j.
 *
 * @author ogondza
 */
public interface CliCommand {
    /**
     * Name of the command.
     */
    String getName();

    /**
     * Get one-line description of command.
     */
    String getDescription();

    /**
     * Invoke the command.
     *
     * @param in Input stream for command.
     * @param out Stdout for command.
     * @param err Stderr for command.
     * @return Command return code.
     */
    int run(InputStream in, PrintStream out, PrintStream err) throws CmdLineException;
}
