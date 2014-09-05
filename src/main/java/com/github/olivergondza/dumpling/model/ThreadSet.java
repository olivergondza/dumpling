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

import groovy.lang.Closure;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import com.github.olivergondza.dumpling.query.SingleThreadSetQuery;

public class ThreadSet implements Iterable<ProcessThread> {

    private @Nonnull ProcessRuntime runtime;
    private @Nonnull Set<ProcessThread> threads;

    public ThreadSet(@Nonnull ProcessRuntime runtime, @Nonnull Set<ProcessThread> threads) {
        this.runtime = runtime;
        this.threads = threads;
    }

    public @Nonnull ProcessRuntime getProcessRuntime() {
        return runtime;
    }

    /**
     * Extract the only thread from set.
     *
     * @throws IllegalStateException if not exactly one thread present.
     */
    public @Nonnull ProcessThread onlyThread() throws IllegalStateException {
        if (size() != 1) throw new IllegalStateException(
                "Exactly one thread expected in the set. Found " + size()
        );

        return threads.iterator().next();
    }

    /**
     * Get threads blocked by any of current threads.
     */
    public @Nonnull ThreadSet getBlockedThreads() {
        Set<ThreadLock> acquired = new HashSet<ThreadLock>();
        for (ProcessThread thread: threads) {
            acquired.addAll(thread.getAcquiredLocks());
        }

        Set<ProcessThread> blocked = new HashSet<ProcessThread>();
        for (ProcessThread thread: runtime.getThreads()) {
            if (acquired.contains(thread.getWaitingOnLock())) {
                blocked.add(thread);
            }
        }

        return derive(blocked);
    }

    /**
     * Get threads blocking any of current threads.
     */
    public @Nonnull ThreadSet getBlockingThreads() {
        Set<ThreadLock> waitingOn = new HashSet<ThreadLock>();
        for (ProcessThread thread: threads) {
            if (thread.getWaitingOnLock() != null) {
                waitingOn.add(thread.getWaitingOnLock());
            }
        }

        Set<ProcessThread> blocking = new HashSet<ProcessThread>();
        for (ProcessThread thread: runtime.getThreads()) {
            Set<ThreadLock> threadHolding = thread.getAcquiredLocks();
            threadHolding.retainAll(waitingOn);
            if (!threadHolding.isEmpty()) {
                blocking.add(thread);
            }
        }

        return derive(blocking);
    }

    public @Nonnull ThreadSet ignoring(@Nonnull ThreadSet actualThreads) {
        HashSet<ProcessThread> newThreads = new HashSet<ProcessThread>(threads);
        newThreads.removeAll(actualThreads.threads);
        return derive(newThreads);
    }

    public @Nonnull ThreadSet where(ProcessThread.Predicate pred) {
        HashSet<ProcessThread> subset = new HashSet<ProcessThread>(size() / 2);
        for (@Nonnull ProcessThread thread: threads) {
            if (pred.isValid(thread)) subset.add(thread);
        }

        return new ThreadSet(runtime, subset);
    }

    /**
     * Run query using this as an initial thread set.
     */
    public <T extends SingleThreadSetQuery.Result> T query(SingleThreadSetQuery<T> query) {
        return query.query(this);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (ProcessThread thread: threads) {
            stringBuilder.append(thread).append("\n\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs == null) return false;

        if (this == rhs) return true;

        if (!this.getClass().equals(rhs.getClass())) return false;

        ThreadSet other = (ThreadSet) rhs;

        return runtime.equals(other.runtime) && threads.equals(other.threads);
    }

    @Override
    public int hashCode() {
        return runtime.hashCode() + threads.hashCode() * 31;
    }

    public int size() {
        return threads.size();
    }

    public boolean isEmpty() {
        return threads.isEmpty();
    }

    public boolean contains(Object o) {
        return threads.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return threads.containsAll(c);
    }

    @Override
    public Iterator<ProcessThread> iterator() {
        return threads.iterator();
    }

    /**
     * Create derived set from this one.
     *
     * @return New thread collection bound to same runtime.
     */
    public @Nonnull ThreadSet derive(Collection<ProcessThread> threads) {
        if (threads.isEmpty()) return runtime.getEmptyThreadSet();

        Set<ProcessThread> threadSet = threads instanceof Set
                ? (Set<ProcessThread>) threads
                : new HashSet<ProcessThread>(threads)
        ;
        return new ThreadSet(runtime, threadSet);
    }

    // Groovy interop

    public @Nonnull ThreadSet grep() {
        // Do not invoke grep(Collection) as it was added in 2.0
        return derive(DefaultGroovyMethods.grep((Object) threads));
    }

    public @Nonnull ThreadSet grep(Object filter) {
        // Do not invoke grep(Collection, Object) as it was added in 2.0
        return derive(DefaultGroovyMethods.grep((Object) threads, filter));
    }

    public @Nonnull ThreadSet findAll() {
        return derive(DefaultGroovyMethods.findAll(threads));
    }

    public @Nonnull ThreadSet findAll(Closure<ProcessThread> predicate) {
        return derive(DefaultGroovyMethods.findAll(threads, predicate));
    }

    public @Nonnull ThreadSet asImmutable() {
        return this;
    }

    public @Nonnull ThreadSet toSet() {
        return this;
    }

    public @Nonnull ThreadSet intersect(ThreadSet other) {
        if (!runtime.equals(other.runtime)) throw new IllegalStateException(
                "Unable to intersect thread sets bound to different runtime"
        );

        return derive(DefaultGroovyMethods.intersect(threads, other.threads));
    }

    public @Nonnull ThreadSet plus(ThreadSet other) {
        if (!runtime.equals(other.runtime)) throw new IllegalStateException(
                "Unable to merge thread sets bound to different runtime"
        );

        return derive(DefaultGroovyMethods.plus(threads, other.threads));
    }
}
