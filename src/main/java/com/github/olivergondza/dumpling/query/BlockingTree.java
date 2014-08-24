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
package com.github.olivergondza.dumpling.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

/**
 * Get a forest of all blocking trees.
 *
 * Not-blocked threads are roots.
 *
 * @author ogondza
 */
public class BlockingTree implements SingleRuntimeQuery<Set<BlockingTree.Tree>> {

    public @Nonnull Set<Tree> query(ThreadSet threads) {
        @Nonnull Set<Tree> roots = new HashSet<Tree>();
        for (ProcessThread thread: threads.getProcessRuntime().getThreads()) {
            if (thread.getWaitingOnLock() == null && !thread.getAcquiredLocks().isEmpty()) {
                roots.add(new Tree(thread, buildDown(thread)));
            }
        }

        return Collections.unmodifiableSet(filter(roots, threads));
    }

    private @Nonnull Set<Tree> buildDown(ProcessThread thread) {
        @Nonnull Set<Tree> newTrees = new HashSet<Tree>();
        for(ProcessThread t: thread.getBlockedThreads()) {
            newTrees.add(new Tree(t, buildDown(t)));
        }

        return newTrees;
    }

    private @Nonnull Set<Tree> filter(Set<Tree> roots, ThreadSet threads) {
        Set<Tree> filtered = new HashSet<Tree>();
        for (Tree r: roots) {
            // Add whitelisted items including their subtrees
            if (threads.contains(r.getRoot())) {
                filtered.add(r);
            }

            // Remove nodes with all children filtered out
            final Set<Tree> filteredLeaves = filter(r.getLeaves(), threads);
            if (filteredLeaves.isEmpty()) continue;

            filtered.add(new Tree(r.getRoot(), filteredLeaves));
        }

        return filtered;
    }

    /**
     * Blocking tree.
     *
     * @author ogondza
     */
    public static class Tree {

        private final @Nonnull ProcessThread root;
        private final @Nonnull Set<Tree> leaves;

        public Tree(@Nonnull ProcessThread root, @Nonnull Set<Tree> leaves) {
            this.root = root;
            this.leaves = Collections.unmodifiableSet(leaves);
        }

        private Tree(@Nonnull ProcessThread root, @Nonnull ThreadSet threads) {
            this.root = root;
            Set<Tree> leaves = new HashSet<Tree>(threads.size());
            for (ProcessThread l: threads) {
                leaves.add(new Tree(l));
            }
            this.leaves = Collections.unmodifiableSet(leaves);
        }

        public Tree(@Nonnull ProcessThread root, @Nonnull Tree... leaves) {
            this.root = root;
            this.leaves = Collections.unmodifiableSet(new HashSet<Tree>(Arrays.asList(leaves)));
        }

        /**
         * Create leave.
         */
        public Tree(@Nonnull ProcessThread root) {
            this.root = root;
            this.leaves = Collections.emptySet();
        }

        public @Nonnull ProcessThread getRoot() {
            return root;
        }

        public @Nonnull Set<Tree> getLeaves() {
            return leaves;
        }

        @Override
        public String toString() {
            return toString("");
        }

        public String toString(String prefix) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append(prefix).append(root.getHeader());
            for (Tree l: leaves) {
                sb.append(l.toString(prefix + "\t"));
            }

            return sb.toString();
        }

        @Override
        public int hashCode() {
            int hashCode = 31 * root.hashCode();
            for (Tree l: leaves) {
                hashCode += l.hashCode() * 7;
            }

            return hashCode;
        }

        @Override
        public boolean equals(Object rhs) {
            if (rhs == null) return false;
            if (rhs == this) return true;

            if (!rhs.getClass().equals(this.getClass())) return false;

            Tree other = (Tree) rhs;
            return this.root.equals(other.root) && this.leaves.equals(other.leaves);
        }
    }
}
