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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;

import org.junit.Test;

public class StackTraceTest {

    private static final StackTraceElement ELEMENT = StackTrace.element("Yyy", "xxx");

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void negativeStackTraceElement() {
        new StackTrace(ELEMENT).getElement(-1);
    }

    @Test
    public void nonexistentStackTraceElement() {
        assertThat(new StackTrace().getElement(0), nullValue());
    }

    @Test
    public void compareConstructors() {
        StackTrace list = new StackTrace(ELEMENT);
        StackTrace variadic = new StackTrace(Arrays.asList(ELEMENT));

        assertThat(variadic, equalTo(list));
        assertThat(variadic, not(equalTo(new StackTrace())));
    }

    @Test
    public void testToString() {
        StackTrace st = new StackTrace(
                StackTrace.element("Yyy", "xxx", "Yyy.java", 42),
                StackTrace.element("Yyy", "xxx", "Yyy.java")
       );

        assertThat(st.toString(), equalTo(String.format("%n\tat Yyy.xxx(Yyy.java:42)%n\tat Yyy.xxx(Yyy.java)%n")));
    }
}
