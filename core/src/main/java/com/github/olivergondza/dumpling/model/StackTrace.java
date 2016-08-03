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
package com.github.olivergondza.dumpling.model;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Stacktrace of a single thread.
 *
 * @author ogondza
 */
public class StackTrace extends ModelObject {

    public static StackTraceElement element(String declaringClass, String methodName, String fileName, int lineNumber) {
        return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
    }

    /**
     * Get {@link StackTraceElement} its source line number is unknown.
     */
    public static StackTraceElement element(String declaringClass, String methodName, String fileName) {
        return new StackTraceElement(declaringClass, methodName, fileName, -1);
    }

    /**
     * Get {@link StackTraceElement} its source (file name and line number) is unknown.
     */
    public static StackTraceElement element(String declaringClass, String methodName) {
        return new StackTraceElement(declaringClass, methodName, null, -1);
    }

    /**
     * Get {@link StackTraceElement} for native method.
     */
    public static StackTraceElement nativeElement(String declaringClass, String methodName) {
        return new StackTraceElement(declaringClass, methodName, null, -2);
    }

    /**
     * Get {@link StackTraceElement} for native method.
     *
     * Some native method {@link StackTraceElement}s have filename, even though it is not shown.
     */
    public static StackTraceElement nativeElement(String declaringClass, String methodName, String fileName) {
        return new StackTraceElement(declaringClass, methodName, fileName, -2);
    }

    private @Nonnull StackTraceElement[] elements;

    public StackTrace(@Nonnull StackTraceElement... elements) {
        this.elements = elements.clone(); // Shallow copy is ok here as StackTraceElement is immutable
    }

    public StackTrace(@Nonnull List<StackTraceElement> elements) {
        this.elements = elements.toArray(new StackTraceElement[elements.size()]);
    }

    public int size() {
        return elements.length;
    }

    /**
     * Get element of given stack depth.
     *
     * @return Stack element of null if not present.
     */
    public @CheckForNull StackTraceElement getElement(@Nonnegative int depth) {
        if (depth < 0) throw new ArrayIndexOutOfBoundsException(depth);

        return elements.length > depth
                ? elements[depth]
                : null
        ;
    }

    /**
     * Get innermost stack frame or null when there is no trace attached.
     */
    public @CheckForNull StackTraceElement getHead() {
        return getElement(0);
    }

    public @CheckForNull StackTraceElement head() {
        return getElement(0);
    }

    /**
     * Get all the stack trace elements.
     */
    public @Nonnull List<StackTraceElement> getElements() {
        return Arrays.asList(elements);
    }

    @Override
    public void toString(PrintStream stream, Mode mode) {
        for (StackTraceElement e: elements) {
            stream.println();
            stream.append("\tat ").append(e.toString());
        }

        stream.println();
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(elements);
    }

    @Override
    public boolean equals(Object rhs) {
        if (this == rhs) return true;
        if (rhs == null) return false;
        if (getClass() != rhs.getClass()) return false;

        StackTrace other = (StackTrace) rhs;
        if (!Arrays.equals(elements, other.elements)) return false;
        return true;
    }
}
