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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ModelObject;
import com.github.olivergondza.dumpling.model.ModelObject.Mode;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

/**
 * Print trees of blocking threads.
 *
 * @author ogondza
 */
public final class BlockingTree implements SingleThreadSetQuery<BlockingTree.Result<?, ?, ?>> {

    private boolean showStackTraces = false;

    public BlockingTree showStackTraces() {
        this.showStackTraces = true;
        return this;
    }

    /**
     * @param threads Only show tree branches that contain threads in this set.
     * Provide all threads in runtime to analyze whole runtime.
     */
    @Override
    public @Nonnull <
            SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
            RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>,
            ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>
    > Result<SetType, RuntimeType, ThreadType> query(SetType threads) {
        return new Result<SetType, RuntimeType, ThreadType>(threads, showStackTraces);
    }

    /**
     * Forest of all blocking trees found.
     *
     * @author ogondza
     */
    public static final class Result<
            SetType extends ThreadSet<SetType, RuntimeType, ThreadType>,
            RuntimeType extends ProcessRuntime<RuntimeType, SetType, ThreadType>,
            ThreadType extends ProcessThread<ThreadType, SetType, RuntimeType>
    > extends SingleThreadSetQuery.Result<SetType, RuntimeType, ThreadType> {

        private static final @Nonnull Deadlocks DEADLOCKS = new Deadlocks();

        private final @Nonnull Set<Tree<ThreadType>> trees;
        private final @Nonnull SetType involved;
        private final Deadlocks.Result<SetType, RuntimeType, ThreadType> deadlocks;

        private final @Nonnull SetType deadlockedThreads;

        /*package*/ Result(@Nonnull SetType threads, boolean showStackTraces) {
            super(showStackTraces);
            deadlocks = DEADLOCKS.query(threads);
            deadlockedThreads = deadlocks.involvedThreads();

            @Nonnull Set<Tree<ThreadType>> roots = new LinkedHashSet<Tree<ThreadType>>();
            for (ThreadType thread: threads.getProcessRuntime().getThreads()) {
                // consider only unblocked threads or possibly deadlocked ones
                if (thread.getWaitingToLock() != null && !deadlockedThreads.contains(thread)) continue;
                // No thread can be blocked by this
                if (thread.getAcquiredLocks().isEmpty()) continue;
                // No blocked and not-deadlocked threads to report
                if (thread.getBlockedThreads().ignoring(deadlockedThreads).isEmpty()) continue;

                roots.add(new Tree<ThreadType>(thread, buildDown(thread)));
            }

            this.trees = Collections.unmodifiableSet(filter(roots, threads));

            LinkedHashSet<ThreadType> involved = new LinkedHashSet<ThreadType>();
            for (Tree<ThreadType> root: trees) {
                flatten(root, involved);
            }

            for (SetType deadlock: deadlocks.getDeadlocks()) {
                for (ThreadType deadlockedThread: deadlock) {
                    involved.add(deadlockedThread);
                }
            }

            this.involved = threads.derive(involved);
        }

        private @Nonnull Set<Tree<ThreadType>> buildDown(ThreadType thread) {
            @Nonnull Set<Tree<ThreadType>> newTrees = new HashSet<Tree<ThreadType>>();
            for(ThreadType t: thread.getBlockedThreads().ignoring(deadlockedThreads)) {
                newTrees.add(new Tree<ThreadType>(t, buildDown(t)));
            }

            return newTrees;
        }

        private @Nonnull Set<Tree<ThreadType>> filter(Set<Tree<ThreadType>> roots, SetType threads) {
            Set<Tree<ThreadType>> filtered = new LinkedHashSet<Tree<ThreadType>>();
            for (Tree<ThreadType> r: roots) {
                // Add whitelisted items including their subtrees
                if (threads.contains(r.getRoot())) {
                    filtered.add(r);
                }

                // Remove nodes with all children filtered out
                final Set<Tree<ThreadType>> filteredLeaves = filter(r.getLeaves(), threads);
                if (filteredLeaves.isEmpty()) continue;

                filtered.add(new Tree<ThreadType>(r.getRoot(), filteredLeaves));
            }

            return filtered;
        }

        private void flatten(Tree<ThreadType> tree, Set<ThreadType> accumulator) {
            accumulator.add(tree.getRoot());
            for (Tree<ThreadType> leaf: tree.getLeaves()) {
                flatten(leaf, accumulator);
            }
        }

        /**
         * All trees detected.
         *
         * Empty, when there are no blocked/blocking threads.
         */
        public @Nonnull Set<Tree<ThreadType>> getTrees() {
            return trees;
        }

        /**
         * Get tree roots, blocking threads that are not blocked.
         *
         * @since 0.2
         */
        public @Nonnull SetType getRoots() {
            Set<ThreadType> roots = new LinkedHashSet<ThreadType>(trees.size());
            for (Tree<ThreadType> tree: trees) {
                roots.add(tree.getRoot());
            }

            return involved.derive(roots);
        }

        @Override
        protected void printResult(PrintStream out) {
            for (Tree<ThreadType> tree: trees) {
                tree.toString(out, Mode.HUMAN);
                out.println();
            }

            if (!deadlocks.getDeadlocks().isEmpty()) {
                out.println();
                deadlocks.printResult(out);
            }
        }

        @Override
        protected SetType involvedThreads() {
            return involved;
        }

        @Override
        protected void printSummary(PrintStream out) {
            out.printf("All threads: %d; Roots: %d", involved.size(), trees.size());
            if (!deadlocks.getDeadlocks().isEmpty()) {
                out.print(' ');
                deadlocks.printSummary(out);
            } else {
                out.println();
            }
        }
    }

    /**
     * Blocking tree node.
     *
     * A <tt>root</tt> with directly blocked subtrees (<tt>leaves</tt>). If
     * leave set is empty root thread does not block any other threads.
     *
     * @author ogondza
     */
    public final static class Tree<ThreadType extends ProcessThread<ThreadType, ?, ?>> extends ModelObject {

        private final @Nonnull ThreadType root;
        private final @Nonnull Set<Tree<ThreadType>> leaves;

        private Tree(@Nonnull ThreadType root, @Nonnull Set<Tree<ThreadType>> leaves) {
            this.root = root;
            this.leaves = Collections.unmodifiableSet(leaves);
        }

        /*package*/ Tree(@Nonnull ThreadType root, @Nonnull Tree<ThreadType>... leaves) {
            this(root, new LinkedHashSet<Tree<ThreadType>>(Arrays.asList(leaves)));
        }

        public @Nonnull ThreadType getRoot() {
            return root;
        }

        public @Nonnull Set<Tree<ThreadType>> getLeaves() {
            return leaves;
        }

        @Override
        public void toString(PrintStream stream, Mode mode) {
            writeInto("", stream, mode);
        }

        private void writeInto(String prefix, PrintStream sb, Mode mode) {
            sb.append(prefix);
            root.printHeader(sb, mode);
            sb.println();
            for (Tree<ThreadType> l: leaves) {
                l.writeInto(prefix + "\t", sb, mode);
            }
        }

        @Override
        public int hashCode() {
            int hashCode = 31 * root.hashCode();
            for (Tree<ThreadType> l: leaves) {
                hashCode += l.hashCode() * 7;
            }

            return hashCode;
        }

        @Override
        public boolean equals(Object rhs) {
            if (rhs == null) return false;
            if (rhs == this) return true;

            if (!rhs.getClass().equals(this.getClass())) return false;

            @SuppressWarnings("unchecked")
            Tree<ThreadType> other = (Tree<ThreadType>) rhs;
            return this.root.equals(other.root) && this.leaves.equals(other.leaves);
        }
    }
}
