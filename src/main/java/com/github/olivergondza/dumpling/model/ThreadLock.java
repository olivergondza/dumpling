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

public class ThreadLock {

    final String className;
    final long identityHashCode;

    public ThreadLock(String className, long identityHashCode) {
        this.className = className;
        this.identityHashCode = identityHashCode;
    }

    public String getClassName() {
        return className;
    }

    public long getIdentityHashCode() {
        return identityHashCode;
    }

    @Override
    public boolean equals(Object lhs) {
        if (lhs == null) return false;
        if (!lhs.getClass().equals(this.getClass())) return false;

        ThreadLock other = (ThreadLock) lhs;
        return identityHashCode == other.identityHashCode;
    }

    @Override
    public int hashCode() {
        return 7 + 31 * new Long(identityHashCode).hashCode();
    }

    @Override
    public String toString() {
        return String.format("<%x> (a %s)", identityHashCode, className);
    }
}
