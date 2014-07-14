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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URISyntaxException;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;
import com.github.olivergondza.dumpling.cli.AbstractCliTest;

public class GroovyTest extends AbstractCliTest {

    @Test
    public void executeScript() {
        invoke("print runtime.threads.size()");
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));
        assertThat(out.toString(), equalTo("8"));
    }

    @Test
    public void filter() {
        invoke("print runtime.threads.onlyNamed('owning_thread').collect { it.name }");
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));
        assertThat(out.toString(), equalTo("[owning_thread]"));
    }

    @Test
    public void loadSymbolsFromOtherDumplingPackages() {
        invoke("new DeadlockDetector(); new ThreadLock('', 0); new JvmRuntimeFactory(); new CommandFailedException('')");
        assertThat(err.toString(), equalTo(""));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void failTheScript() {
        invoke("new ThereIsNoSuchClass()");
        assertThat(err.toString(), containsString("dumpling-script: 1: unable to resolve class ThereIsNoSuchClass"));
        assertThat(out.toString(), equalTo(""));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void groovyGrep() {
        invoke("print runtime.threads.grep().getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void groovyGrepWithArg() {
        invoke("print runtime.threads.grep { it.name == 'blocked_thread' }.getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void groovyFindAll() {
        invoke("print runtime.threads.findAll().getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void groovyFindAllWithArg() {
        invoke("print runtime.threads.findAll { it.name == 'blocked_thread' }.getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void groovyAsImmutable() {
        invoke("print runtime.threads.asImmutable().getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void groovyIntersect() {
        invoke("print runtime.threads.intersect(runtime.threads).getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void groovyPlus() {
        invoke("threadSum = runtime.threads + runtime.threads; print threadSum.getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(exitValue, equalTo(0));
    }

    @Test
    public void groovyToSet() {
        invoke("print runtime.threads.toSet().getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(exitValue, equalTo(0));
    }

    private void invoke(String script) {
        stdin(script);
        try {
            run("groovy", "--in", "threaddump", Util.resourceFile("producer-consumer.log").getAbsolutePath());
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }
}
