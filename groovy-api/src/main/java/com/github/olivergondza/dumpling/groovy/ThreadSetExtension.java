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
package com.github.olivergondza.dumpling.groovy;

import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;
import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Set;

/**
 * Extension methods for ThreadSet class.
 */
/*package*/ class ThreadSetExtension {
    public static ThreadSet asImmutable(ThreadSet t) {
        return t;
    }

    public static Set toSet(ThreadSet t) {
        return t.getThreadsAsSet();
    }

    public static ThreadSet grep(ThreadSet t, Object filter) {
        return t.derive(DefaultGroovyMethods.grep(t.getThreadsAsSet(), filter));
    }

    public static ThreadSet grep(ThreadSet t) {
        return t.derive(DefaultGroovyMethods.grep(t.getThreadsAsSet()));
    }

    public static ThreadSet findAll(ThreadSet t, Closure closure) {
        return t.derive(DefaultGroovyMethods.findAll((Object) t.getThreadsAsSet(), closure));
    }

    public static ThreadSet findAll(ThreadSet t) {
        return t.derive(DefaultGroovyMethods.findAll(t.getThreadsAsSet()));
    }

    public static ThreadSet intersect(ThreadSet t, Iterable other) {
        if (!(other instanceof ThreadSet)) throw new IllegalArgumentException(
                "Unable to intersect ThreadSet with " + other.getClass()
        );

        ThreadSet rhs = (ThreadSet) other;
        if (!t.getProcessRuntime().equals(rhs.getProcessRuntime())) throw new IllegalArgumentException(
                "Unable to intersect ThreadSets bound to different ProcessRuntimes"
        );

        return t.derive(DefaultGroovyMethods.intersect(t.getThreadsAsSet(), rhs));
    }

    public static ThreadSet plus(ThreadSet t, Iterable other) {
        if (!(other instanceof ThreadSet)) throw new IllegalArgumentException(
                "Unable to merge ThreadSet with " + other.getClass()
        );

        ThreadSet rhs = (ThreadSet) other;
        if (!t.getProcessRuntime().equals(rhs.getProcessRuntime())) throw new IllegalArgumentException(
                "Unable to merge ThreadSets bound to different ProcessRuntimes"
        );

        return t.derive(DefaultGroovyMethods.plus((Set<? extends ProcessThread>) t.getThreadsAsSet(), rhs));
    }
}
