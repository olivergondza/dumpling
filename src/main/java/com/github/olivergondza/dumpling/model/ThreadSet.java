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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import com.github.olivergondza.dumpling.query.SingleThreadSetQuery;

/**
 * Collection of threads in certain {@link ProcessRuntime}.
 *
 * @author ogondza
 * @see ProcessRuntime#getThreads()
 */
public class ThreadSet<
        SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
        RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>,
        ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>
> implements Iterable<ThreadType> {

    private static final @Nonnull String NL = System.getProperty("line.separator", "\n");

    private final @Nonnull RuntimeType runtime;
    private final @Nonnull Set<ThreadType> threads;

    protected ThreadSet(@Nonnull RuntimeType runtime, @Nonnull Set<ThreadType> threads) {
        this.runtime = runtime;
        this.threads = Collections.unmodifiableSet(threads);
    }

    /**
     * Enclosing runtime.
     */
    public @Nonnull RuntimeType getProcessRuntime() {
        return runtime;
    }

    /**
     * Extract the only thread from set.
     *
     * @throws IllegalStateException if not exactly one thread present.
     */
    public @Nonnull ThreadType onlyThread() throws IllegalStateException {
        if (size() != 1) throw new IllegalStateException(
                "Exactly one thread expected in the set. Found " + size()
        );

        return threads.iterator().next();
    }

    /**
     * Get threads blocked by any of current threads.
     */
    public @Nonnull SetType getBlockedThreads() {
        Set<ThreadLock> acquired = new HashSet<ThreadLock>();
        for (ThreadType thread: threads) {
            acquired.addAll(thread.getAcquiredLocks());
        }

        Set<ThreadType> blocked = new HashSet<ThreadType>();
        for (ThreadType thread: runtime.getThreads()) {
            if (acquired.contains(thread.getWaitingOnLock())) {
                blocked.add(thread);
            }
        }

        return derive(blocked);
    }

    /**
     * Get threads blocking any of current threads.
     */
    public @Nonnull SetType getBlockingThreads() {
        Set<ThreadLock> waitingOn = new HashSet<ThreadLock>();
        for (ThreadType thread: threads) {
            if (thread.getWaitingOnLock() != null) {
                waitingOn.add(thread.getWaitingOnLock());
            }
        }

        Set<ThreadType> blocking = new HashSet<ThreadType>();
        for (ThreadType thread: runtime.getThreads()) {
            Set<ThreadLock> threadHolding = thread.getAcquiredLocks();
            threadHolding.retainAll(waitingOn);
            if (!threadHolding.isEmpty()) {
                blocking.add(thread);
            }
        }

        return derive(blocking);
    }

    public @Nonnull SetType ignoring(@Nonnull ThreadSet<?, ?, ?> actualThreads) {
        HashSet<ThreadType> newThreads = new HashSet<ThreadType>(threads);
        newThreads.removeAll(actualThreads.threads);
        return derive(newThreads);
    }

    /**
     * Get subset of current threads.
     *
     * @param pred Predicate to match.
     * @return {@link ThreadSet} scoped to current runtime containing subset of threads that match the predicate.
     */
    public @Nonnull SetType where(ProcessThread.Predicate pred) {
        HashSet<ThreadType> subset = new HashSet<ThreadType>(size() / 2);
        for (@Nonnull ThreadType thread: threads) {
            if (pred.isValid(thread)) subset.add(thread);
        }

        return derive(subset);
    }

    /**
     * Run query using this as an initial thread set.
     */
    public <T extends SingleThreadSetQuery.Result<SetType>> T query(SingleThreadSetQuery<T> query) {
        return query.<SetType, RuntimeType, ThreadType>query((SetType) this);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (ThreadType thread: threads) {
            stringBuilder.append(thread).append(NL + NL);
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs == null) return false;

        if (this == rhs) return true;

        if (!this.getClass().equals(rhs.getClass())) return false;

        ThreadSet<?, ?, ?> other = (ThreadSet) rhs;

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
    public Iterator<ThreadType> iterator() {
        return threads.iterator();
    }

    /**
     * Create derived set from this one.
     *
     * @return New thread collection bound to same runtime.
     */
    public @Nonnull SetType derive(Collection<? extends ThreadType> threads) {
        if (threads.isEmpty()) return runtime.getEmptyThreadSet();

        Set<ThreadType> threadSet = threads instanceof Set
                ? (Set<ThreadType>) threads
                : new HashSet<ThreadType>(threads)
        ;
        return runtime.createSet(threadSet);
    }

    // Groovy interop

    public @Nonnull SetType grep() {
        // Do not invoke grep(Collection) as it was added in 2.0
        return derive(DefaultGroovyMethods.grep((Object) threads));
    }

    public @Nonnull SetType grep(Object filter) {
        // Do not invoke grep(Collection, Object) as it was added in 2.0
        return derive(DefaultGroovyMethods.grep((Object) threads, filter));
    }

    public @Nonnull SetType findAll() {
        return derive(DefaultGroovyMethods.findAll(threads));
    }

    public @Nonnull SetType findAll(Closure<ThreadType> predicate) {
        return derive(DefaultGroovyMethods.findAll(threads, predicate));
    }

    public @Nonnull ThreadSet<SetType, RuntimeType, ThreadType> asImmutable() {
        return this;
    }

    // ThreadSet does not actually implement Set
    public @Nonnull Set<ThreadType> toSet() {
        return Collections.unmodifiableSet(threads);
    }

    public @Nonnull SetType intersect(ThreadSet<?, ?, ?> other) {
        if (!runtime.equals(other.runtime)) throw new IllegalStateException(
                "Unable to intersect thread sets bound to different runtime"
        );

        return derive(DefaultGroovyMethods.intersect((Set) threads, (Set) other.threads));
    }

    @SuppressWarnings("cast")
    public @Nonnull SetType plus(SetType other) {
        final ThreadSet<?, ?, ?> typedOther = other;
        if (!runtime.equals(typedOther.runtime)) throw new IllegalStateException(
                "Unable to merge thread sets bound to different runtime"
        );

        return derive(DefaultGroovyMethods.plus(threads, (Set<ThreadType>) typedOther.threads));
    }
}
