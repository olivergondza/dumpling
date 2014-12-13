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
package com.github.olivergondza.dumpling.cli;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import org.junit.Test;

import com.github.olivergondza.dumpling.Util;

public class GroovyCommandTest extends AbstractCliTest {

    @Test
    public void executeScript() {
        invoke("print runtime.threads.size()");
        assertThat(err.toString(), equalTo(""));
        assertThat(this, succeeded());
        assertThat(out.toString(), equalTo("8"));
    }

    @Test
    public void filter() {
        invoke("runtime.threads.where(nameIs('owning_thread')).collect { it.name }");
        assertThat(err.toString(), equalTo(""));
        assertThat(this, succeeded());
        assertThat(out.toString().trim(), equalTo("[owning_thread]"));
    }

    @Test
    public void groovyGrep() {
        invoke("print runtime.threads.grep().getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(this, succeeded());
    }

    @Test
    public void groovyGrepWithArg() {
        invoke("print runtime.threads.grep { it.name == 'blocked_thread' }.getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(this, succeeded());
    }

    @Test
    public void groovyFindAll() {
        invoke("print runtime.threads.findAll().getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(this, succeeded());
    }

    @Test
    public void groovyFindAllWithArg() {
        invoke("print runtime.threads.findAll { it.name == 'blocked_thread' }.getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(this, succeeded());
    }

    @Test
    public void groovyAsImmutable() {
        invoke("print runtime.threads.asImmutable().getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(this, succeeded());
    }

    @Test
    public void groovyIntersect() {
        invoke("print runtime.threads.intersect(runtime.threads).getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(this, succeeded());
    }

    @Test
    public void groovyPlus() {
        invoke("threadSum = runtime.threads + runtime.threads; print threadSum.getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(this, succeeded());
    }

    @Test
    public void groovyToSet() {
        invoke("print runtime.threads.toSet().getClass()");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("class com.github.olivergondza.dumpling.model.ThreadSet"));
        assertThat(this, succeeded());
    }

    @Test
    public void stateFilter() {
        String choices = "it.status.new || it.status.runnable || it.status.sleeping || it.status.waiting || it.status.parked || it.status.blocked || it.status.terminated";
        invoke("print runtime.threads.grep { " + choices + " }.empty");
        assertThat(err.toString(), equalTo(""));
        assertThat(out.toString(), equalTo("false"));
        assertThat(this, succeeded());
    }

    @Test
    public void runScriptThatDoesNotExist() {
        run("groovy", "--script", "no_such_file");
        assertThat(out.toString(), equalTo(""));
        assertThat(err.toString(), equalTo("no_such_file (No such file or directory)\n"));
        assertThat(exitValue, not(equalTo(0)));
    }

    @Test
    public void runScriptThatDoesExist() throws Exception {
        File file = File.createTempFile("dumpling-GroovyCommandTest", "script");
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.println("print 'groovy out'; return 42;");
            writer.close();

            run("groovy", "--script", file.getAbsolutePath());
            assertThat(out.toString(), equalTo("groovy out"));
            assertThat(err.toString(), equalTo(""));
            assertThat(exitValue, equalTo(42));
        } finally {
            file.delete();
        }
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
