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

import static com.github.olivergondza.dumpling.model.ProcessThread.nameIs;
import static com.github.olivergondza.dumpling.model.ProcessThread.nameContains;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;
import com.github.olivergondza.dumpling.factory.ThreadDumpFactory;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.query.BlockingTree.Tree;

public class BlockingTreeTest extends AbstractCliTest {

    private ProcessRuntime runtime;
    private ProcessThread a, aa, aaa, ab, b, ba;
    private File blockingTreeLog;

    @Before
    public void setUp() throws Exception {
        blockingTreeLog = Util.resourceFile("blocking-tree.log");
        runtime = new ThreadDumpFactory().fromFile(blockingTreeLog);
        a = singleThread("a");
        aa = singleThread("aa");
        aaa = singleThread("aaa");
        ab = singleThread("ab");
        b = singleThread("b");
        ba = singleThread("ba");
    }

    private ProcessThread singleThread(@Nonnull String name) {
        return runtime.getThreads().where(nameIs(name)).onlyThread();
    }

    @Test
    public void fullForest() {
        Set<Tree> full = runtime.query(new BlockingTree());
        Set<Tree> expected = new HashSet<Tree>(Arrays.asList(
                new BlockingTree.Tree(a,
                        new BlockingTree.Tree(aa, new BlockingTree.Tree(aaa)),
                        new BlockingTree.Tree(ab)
                ),
                new BlockingTree.Tree(b, new BlockingTree.Tree(ba))
        ));

        assertThat(full, equalTo(expected));
    }

    @Test
    public void oneChainFromBottom() {
        Set<Tree> as = runtime.getThreads().where(nameIs("aaa")).query(new BlockingTree());
        Set<Tree> expected = new HashSet<Tree>(Arrays.asList(
                new BlockingTree.Tree(a, new BlockingTree.Tree(aa, new BlockingTree.Tree(aaa)))
        ));

        assertThat(as, equalTo(expected));
    }

    @Test
    public void oneChainFromMiddle() {
        Set<Tree> as = runtime.getThreads().where(nameIs("aa")).query(new BlockingTree());
        Set<Tree> expected = new HashSet<Tree>(Arrays.asList(
                new BlockingTree.Tree(a, new BlockingTree.Tree(aa, new BlockingTree.Tree(aaa)))
        ));

        assertThat(as, equalTo(expected));
    }

    @Test
    public void fullRoot() {
        Set<Tree> as = runtime.getThreads().where(nameIs("b")).query(new BlockingTree());
        Set<Tree> expected = new HashSet<Tree>(Arrays.asList(
                new BlockingTree.Tree(b, new BlockingTree.Tree(ba))
        ));

        assertThat(as, equalTo(expected));
    }

    @Test
    public void severalChains() {
        Set<Tree> as = runtime.getThreads().where(nameContains(Pattern.compile("^(aaa|ba)$"))).query(new BlockingTree());
        Set<Tree> expected = new HashSet<Tree>(Arrays.asList(
                new BlockingTree.Tree(a, new BlockingTree.Tree(aa, new BlockingTree.Tree(aaa))),
                new BlockingTree.Tree(b, new BlockingTree.Tree(ba))
        ));

        assertThat(as, equalTo(expected));
    }

    public void cliQuery() throws Exception {
        run("top-contenders", "--in", "threaddump", blockingTreeLog.getAbsolutePath());
        assertThat(err.toString(), equalTo(""));

        // Roots
        assertThat(out.toString(), containsString("\n\"a\""));
        assertThat(out.toString(), containsString("\n\"b\""));

        // Blocked by roots
        assertThat(out.toString(), containsString("\n\t\"aa\""));
        assertThat(out.toString(), containsString("\n\t\"ab\""));
        assertThat(out.toString(), containsString("\n\t\"ba\""));

        // Deeply nested
        assertThat(out.toString(), containsString("\n\t\t\"aaa\""));
    }
}
