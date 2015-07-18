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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Thread lock identified by classname and id.
 *
 * @author ogondza
 */
public class ThreadLock extends ModelObject {

    protected final @Nonnull String className;
    private final long id;

    public static @Nonnull ThreadLock fromInstance(@Nonnull Object instance) {
        return new ThreadLock(
                instance.getClass().getCanonicalName(),
                System.identityHashCode(instance)
        );
    }

    public ThreadLock(@Nonnull String className, long id) {
        this.className = className;
        this.id = id;
    }

    public @Nonnull String getClassName() {
        return className;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object lhs) {
        if (lhs == null) return false;
        if (!(lhs instanceof ThreadLock)) return false;

        ThreadLock other = (ThreadLock) lhs;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return 7 + 31 * new Long(id).hashCode();
    }

    @Override
    public void toString(PrintStream stream, Mode mode) {
        String format = mode.isHuman() ? "<0x%x> (a %s)" : "<0x%016x> (a %s)";
        stream.format(format, id, className);

    }

    /**
     * @deprecated Kept for backward compatibility. Use {@link ThreadLock} instead.
     */
    @Deprecated
    public static final class WithHashCode extends ThreadLock {

        public WithHashCode(@Nonnull Object instance) {
            super(
                    instance.getClass().getCanonicalName(),
                    System.identityHashCode(instance)
            );
        }

        public WithHashCode(@Nonnull String className, int identityHashCode) {
            super(className, identityHashCode);
        }

        public long getIdentityHashCode() {
            return getId();
        }
    }

    /**
     * @deprecated Kept for backward compatibility. Use {@link ThreadLock} instead.
     */
    @Deprecated
    public static final class WithAddress extends ThreadLock {

        public WithAddress(@Nonnull String className, long address) {
            super(className, address);
        }

        public long getAddress() {
            return getId();
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
