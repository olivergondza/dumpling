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

public class StackTrace {

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
}
