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

import static com.github.olivergondza.dumpling.Util.resource;
import static com.github.olivergondza.dumpling.model.ProcessThread.nameContains;
import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThreadSet;
import com.github.olivergondza.dumpling.query.BlockingTree.Tree;

public class BlockingTreeTest {

    public static final ThreadDumpFactory factory = new ThreadDumpFactory();
    private ThreadDumpRuntime runtime;
    private ThreadDumpThread a, aa, aaa, ab, b, ba;

    @Before
    public void setUp() {
        InputStream blockingTreeLog = resource("jstack/blocking-tree.log");
        runtime = new ThreadDumpFactory().fromStream(blockingTreeLog);
        a = singleThread("a");
        aa = singleThread("aa");
        aaa = singleThread("aaa");
        ab = singleThread("ab");
        b = singleThread("b");
        ba = singleThread("ba");
    }

    private ThreadDumpThread singleThread(@Nonnull String name) {
        return runtime.getThreads().where(nameIs(name)).onlyThread();
    }

    @Test
    public void fullForest() {
        Set<Tree<ThreadDumpThread>> full = new BlockingTree().query(runtime.getThreads()).getTrees();

        @SuppressWarnings("unchecked")
        Set<Tree<ThreadDumpThread>> expected = new HashSet<Tree<ThreadDumpThread>>(Arrays.asList(
                tree(a,
                        tree(aa, tree(aaa)),
                        tree(ab)
                ),
                tree(b, tree(ba))
        ));

        assertThat(full, equalTo(expected));
    }

    @Test
    public void oneChainFromBottom() {
        Set<Tree<ThreadDumpThread>> as = new BlockingTree().query(runtime.getThreads().where(nameIs("aaa"))).getTrees();

        @SuppressWarnings("unchecked")
        Set<Tree<ThreadDumpThread>> expected = new HashSet<Tree<ThreadDumpThread>>(Collections.singletonList(
                tree(a, tree(aa, tree(aaa)))
        ));

        assertThat(as, equalTo(expected));
    }

    @Test
    public void oneChainFromMiddle() {
        Set<Tree<ThreadDumpThread>> as = new BlockingTree().query(runtime.getThreads().where(nameIs("aa"))).getTrees();

        @SuppressWarnings("unchecked")
        Set<Tree<ThreadDumpThread>> expected = new HashSet<Tree<ThreadDumpThread>>(Collections.singletonList(
                tree(a, tree(aa, tree(aaa)))
        ));

        assertThat(as, equalTo(expected));
    }

    @Test
    public void fullRoot() {
        Set<Tree<ThreadDumpThread>> as = new BlockingTree().query(runtime.getThreads().where(nameIs("b"))).getTrees();

        @SuppressWarnings("unchecked")
        Set<Tree<ThreadDumpThread>> expected = new HashSet<Tree<ThreadDumpThread>>(Collections.singletonList(
                tree(b, tree(ba))
        ));

        assertThat(as, equalTo(expected));
    }

    @Test
    public void severalChains() {
        Set<Tree<ThreadDumpThread>> as = new BlockingTree().query(
                runtime.getThreads().where(nameContains(Pattern.compile("^(aaa|ba)$")))
        ).getTrees();

        @SuppressWarnings("unchecked")
        Set<Tree<ThreadDumpThread>> expected = new HashSet<Tree<ThreadDumpThread>>(Arrays.asList(
                tree(a, tree(aa, tree(aaa))),
                tree(b, tree(ba))
        ));

        assertThat(as, equalTo(expected));
    }

    private Tree<ThreadDumpThread> tree(@Nonnull ThreadDumpThread root, @Nonnull Tree<ThreadDumpThread>... leaves) {
        return new BlockingTree.Tree<ThreadDumpThread>(root, leaves);
    }

    @Test
    public void roots() {
        ThreadDumpThreadSet as = new BlockingTree().query(runtime.getThreads()).getRoots();
        ThreadDumpThreadSet expected = runtime.getThreads().where(nameContains(Pattern.compile("^[ab]$")));

        assertThat(as, equalTo(expected));
    }

    @Test
    public void deadlock() {
        runtime = new ThreadDumpFactory().fromStream(resource("jstack/deadlock.log"));

        ThreadDumpThread blocking = runtime.getThreads().where(nameContains("ajp-127.0.0.1-8009-24")).onlyThread();
        ThreadDumpThread blocked = runtime.getThreads().where(nameContains("ajp-127.0.0.1-8009-133")).onlyThread();

        @SuppressWarnings("unchecked")
        Set<Tree<ThreadDumpThread>> expected = new HashSet<Tree<ThreadDumpThread>>(Collections.singletonList(
                new Tree<ThreadDumpThread>(blocking, new Tree<ThreadDumpThread>(blocked))
        ));

        assertThat(new BlockingTree().query(runtime.getThreads()).getTrees(), equalTo(expected));
    }

    @Test
    public void handleThreadsBlockedOnDeadlocks() {
        runtime = factory.fromStream(resource("jstack/deadlock-and-friends.log"));
        ThreadDumpThreadSet ts = runtime.getThreads();

        @SuppressWarnings("unchecked")
        Set<Tree<ThreadDumpThread>> expected = new HashSet<Tree<ThreadDumpThread>>(Collections.singletonList(
                new Tree<ThreadDumpThread>(
                        ts.where(nameContains("ajp-127.0.0.1-8009-103")).onlyThread(),
                        new Tree<ThreadDumpThread>(
                                ts.where(nameContains("ajp-127.0.0.1-8009-46")).onlyThread(),
                                new Tree<ThreadDumpThread>(
                                        ts.where(nameContains("ajp-127.0.0.1-8009-94")).onlyThread()
                                )
                        )
                )
        ));

        assertThat(new BlockingTree().query(runtime.getThreads()).getTrees(), equalTo(expected));
    }

    @Test
    public void parkingBlockage() {
        verifyParkingBlockage("jstack/ReentrantLock-parking-blockage.log");
        verifyParkingBlockage("jstack/ReentrantReadWriteLock-parking-blockage-read.log");
        verifyParkingBlockage("jstack/ReentrantReadWriteLock-parking-blockage-write.log");
    }

    private void verifyParkingBlockage(String resource) {
        ThreadDumpRuntime rl = factory.fromStream(resource(resource));
        Tree<ThreadDumpThread> rlt = Util.only(new BlockingTree().query(rl.getThreads()).getTrees());
        assertEquals("Thread-1", rlt.getRoot().getName());
        Tree<ThreadDumpThread> blocked = Util.only(rlt.getLeaves());
        assertEquals("main", blocked.getRoot().getName());
        assertThat(blocked.getLeaves().size(), equalTo(0));
    }
}
