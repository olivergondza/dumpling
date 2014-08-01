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

import java.lang.management.MonitorInfo;

import javax.annotation.Nonnull;

/**
 * Thread lock identified by classname and id contributed by subclass.
 *
 * @author ogondza
 */
public abstract class ThreadLock {

    protected final @Nonnull String className;
    protected final int stackDepth;

    /**
     * @param stackDepth Position of lock in stacktrace. See {@link MonitorInfo#getLockedStackDepth()}.
     */
    protected ThreadLock(int stackDepth, @Nonnull String className) {
        this.stackDepth = stackDepth;
        this.className = className;
    }

    public @Nonnull String getClassName() {
        return className;
    }

    public int getStackDepth() {
        return stackDepth;
    }

    /**
     * {@link ThreadLock} identified with lock hashCode.
     *
     * @author ogondza
     */
    public static class WithHashCode extends ThreadLock {

        private final int identityHashCode;

        public WithHashCode(int stackDepth, @Nonnull String className, int identityHashCode) {
            super(stackDepth, className);
            this.identityHashCode = identityHashCode;
        }

        public long getIdentityHashCode() {
            return identityHashCode;
        }

        @Override
        public boolean equals(Object lhs) {
            if (lhs == null) return false;
            if (!lhs.getClass().equals(this.getClass())) return false;

            ThreadLock.WithHashCode other = (ThreadLock.WithHashCode) lhs;
            return identityHashCode == other.identityHashCode;
        }

        @Override
        public int hashCode() {
            return 7 + 31 * identityHashCode;
        }

        @Override
        public String toString() {
            return String.format("%s@%x", className, identityHashCode);
        }
    }

    /**
     * {@link ThreadLock} identified with lock address.
     *
     * @author ogondza
     */
    public static class WithAddress extends ThreadLock {

        private final long address;

        public WithAddress(int stackDepth, @Nonnull String className, long address) {
            super(stackDepth, className);
            this.address = address;
        }

        public long getAddress() {
            return address;
        }

        @Override
        public boolean equals(Object lhs) {
            if (lhs == null) return false;
            if (!lhs.getClass().equals(this.getClass())) return false;

            ThreadLock.WithAddress other = (ThreadLock.WithAddress) lhs;
            return address == other.address;
        }

        @Override
        public int hashCode() {
            return 7 + 67 * new Long(address).hashCode();
        }

        @Override
        public String toString() {
            return String.format("<0x%x> (a %s)", address, className);
        }
    }
}
