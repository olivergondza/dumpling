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

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Stacktrace of a single thread.
 *
 * @author ogondza
 */
public class StackTrace {

    private static final @Nonnull String NL = System.getProperty("line.separator", "\n");

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

    public int size() {
        return elements.length;
    }

    public @Nonnull StackTraceElement getElement(@Nonnegative int depth) {
        return elements[depth];
    }

    /**
     * Get all the stack trace elements.
     */
    public @Nonnull List<StackTraceElement> getElements() {
        return Arrays.asList(elements);
    }

    /**
     * @deprecated because of the typo.
     */
    @Deprecated
    public @Nonnull List<StackTraceElement> getElemens() {
        return Arrays.asList(elements);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (StackTraceElement e: elements) {
            sb.append(NL).append("\tat ").append(e);
        }

        sb.append(NL);
        return sb.toString();
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

    /**
     * Approximate correct waiting verb for stack trace element.
     *
     * Waiting verb can not be estimated using thread status as it might not be
     * in sync with stacktrace/lock information. Waiting verb is whatever precedes
     * the lock information in threaddump ("waiting on", "waiting to lock" etc.).
     * This method yields reasonable result only for stacktrace of non-runnable thread.
     *
     * Here for the lack of better place.
     */
    /*package*/ static String waitingVerb(StackTraceElement element) {
        if (parking.equals(element)) return "parking to wait for";
        if (sleeping.equals(element)) return "waiting on";
        if (waiting.equals(element)) return "waiting on";

        return "waiting to lock";
    }
    private static final StackTraceElement parking = StackTrace.nativeElement("sun.misc.Unsafe", "park");
    private static final StackTraceElement sleeping = StackTrace.nativeElement("java.lang.Thread", "sleep");
    private static final StackTraceElement waiting = StackTrace.nativeElement("java.lang.Object", "wait", "Object.java");
}
