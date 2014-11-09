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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nonnull;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public abstract class AbstractCliTest {

    protected InputStream in = null;
    protected ByteArrayOutputStream err = new ByteArrayOutputStream();
    protected ByteArrayOutputStream out = new ByteArrayOutputStream();
    protected int exitValue;

    protected int run(@Nonnull String... args) {
        return exitValue = new Main().run(args, new ProcessStream(in, new PrintStream(out), new PrintStream(err)));
    }

    protected void stdin(String string) {
        try {
            in = new ByteArrayInputStream(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    protected void stdin(File file){
        try {
            in = new FileInputStream(file.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Contains string with platform dependent newlines.
     *
     * Clients are supposed to use <tt>%n</tt> instead of newline char.
     */
    protected Matcher<String> containsString(String str) {
        return org.hamcrest.CoreMatchers.containsString(
                String.format(str)
        );
    }

    protected Matcher<AbstractCliTest> succeeded() {
        return new TypeSafeMatcher<AbstractCliTest>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Successfull execution");
            }

            @Override
            protected void describeMismatchSafely(AbstractCliTest item, Description mismatchDescription) {
                mismatchDescription.appendText("Failed with: ").appendValue(item.exitValue).appendText("\n").appendValue(item.err);
            }

            @Override
            protected boolean matchesSafely(AbstractCliTest item) {
                return item.exitValue == 0;
            }
        };
    }
}
