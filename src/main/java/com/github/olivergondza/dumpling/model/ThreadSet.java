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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ThreadSet implements Collection<ProcessThread> {

    private ProcessRuntime runtime;
    private Set<ProcessThread> threads;

    public ThreadSet(ProcessRuntime runtime, Set<ProcessThread> threads) {
        this.runtime = runtime;
        this.threads = threads;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (ProcessThread thread: threads) {
            stringBuilder.append(thread).append("\n\n");
        }
        return stringBuilder.toString();
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

    public Iterator<ProcessThread> iterator() {
        return threads.iterator();
    }

    public Object[] toArray() {
        return threads.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return threads.toArray(a);
    }

    public boolean add(ProcessThread e) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends ProcessThread> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}
