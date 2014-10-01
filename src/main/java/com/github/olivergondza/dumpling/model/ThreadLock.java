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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Thread lock identified by classname and id contributed by subclass.
 *
 * @author ogondza
 */
public abstract class ThreadLock {

    protected final @Nonnull String className;

    /**
     * @param stackDepth Position of lock in stacktrace. See {@link MonitorInfo#getLockedStackDepth()}.
     */
    protected ThreadLock(@Nonnull String className) {
        this.className = className;
    }

    public @Nonnull String getClassName() {
        return className;
    }

    /**
     * {@link ThreadLock} identified with lock hashCode.
     *
     * @author ogondza
     */
    public static class WithHashCode extends ThreadLock {

        private final int identityHashCode;

        public WithHashCode(@Nonnull Object instance) {
            super(instance.getClass().getCanonicalName());
            identityHashCode = System.identityHashCode(instance);
        }

        public WithHashCode(@Nonnull String className, int identityHashCode) {
            super(className);
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

        public WithAddress(@Nonnull String className, long address) {
            super(className);
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

    /**
     * Monitor with stack trace position.
     *
     * @author ogondza
     */
    public static final class Monitor {

        private final @Nonnegative int depth;
        private final @Nonnull ThreadLock lock;

        public Monitor(@Nonnull ThreadLock lock, @Nonnegative int depth) {
            this.depth = depth;
            this.lock = lock;
        }

        public @Nonnegative int getDepth() {
            return depth;
        }

        public @Nonnull ThreadLock getLock() {
            return lock;
        }

        @Override
        public boolean equals(Object lhs) {
            if (lhs == null) return false;
            if (!lhs.getClass().equals(this.getClass())) return false;

            Monitor other = (Monitor) lhs;
            return depth == other.depth && lock.equals(other.lock);
        }

        @Override
        public int hashCode() {
            return lock.hashCode() + depth;
        }

        @Override
        public String toString() {
            return lock.toString();
        }
    }
}
