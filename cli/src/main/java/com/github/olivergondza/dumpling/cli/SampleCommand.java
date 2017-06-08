/*
 * The MIT License
 *
 * Copyright (c) Red Hat, Inc.
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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ogondza.
 */
public class SampleCommand implements CliCommand {

    private int number = -1;
    @Option(name = "--number", aliases = {"-n"}, usage = "Number of samples to take")
    public void setNumber(int number) throws CmdLineException {
        if (number < 1) throw new CmdLineException("Number of invocations must be positive. " + number + " given.");
        this.number = number;
    }

    @Option(name = "--out", aliases = {"-o"}, usage = "Output files to write the result")
    public String output;

    public long interval = -1;
    @Option(name = "--interval", aliases = {"-i"}, usage = "minimal wait time before invoking second sample")
    public void setInterval(String interval) throws CmdLineException {
        Matcher matcher = Pattern.compile("^(\\d+)([a-z]*)$").matcher(interval);
        if (!matcher.find()) throw new CmdLineException("Invalid interval specified: " + interval);

        int number = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);

        if (unit.isEmpty() || "s".equals(unit)) {
            this.interval = TimeUnit.SECONDS.toMillis(number);
        } else if ("ms".equals(unit)) {
            this.interval = number; // Milliseconds already
        } else if ("m".equals(unit)) {
            this.interval = TimeUnit.MINUTES.toMillis(number);
        } else if ("h".equals(unit)) {
            this.interval = TimeUnit.HOURS.toMillis(number);
        }
    }

    @Argument(required = true, metaVar = "COMMAND")
    private CliCommand handler;

    @Override
    @Nonnull public String getName() {
        return "sample";
    }

    @Override
    @Nonnull public String getDescription() {
        return "Run dumpling command periodically";
    }

    @Override
    public int run(@Nonnull ProcessStream process) throws CmdLineException {
        for (int i = 0; i < number; i++) {
            PrintStream customStdOut = getOutStream(i);
            try {
                process = new ProcessStream(
                        process.in(),
                        customStdOut == null ? process.out() : customStdOut,
                        process.err()
                );
                handler.run(process);
            } finally {
                if (customStdOut != null) customStdOut.close();
            }
        }
        return 0;
    }

    @CheckForNull
    private PrintStream getOutStream(int i) throws CmdLineException {
        if (output == null) return null;
        try {
            return new PrintStream(new FileOutputStream(output + i));
        } catch (FileNotFoundException e) {
            throw new CmdLineException("Unable to write output");
        }
    }
}
