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
package com.github.olivergondza.dumpling;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import javax.annotation.Nonnull;

public class Util {

    public static final @Nonnull String NL = System.getProperty("line.separator", "\n");

    public static File resourceFile(Class<?> testClass, String resource) throws URISyntaxException {
        URL res = testClass.getResource(testClass.getSimpleName() + "/" + resource);
        if (res == null) throw new AssertionError("Resource does not exist: " + testClass.getSimpleName() + "/" + resource);

        return new File(res.toURI());
    }

    public static File resourceFile(String resource) throws URISyntaxException {
        URL res = Util.class.getResource(resource);
        if (res == null) throw new AssertionError("Resource does not exist: " + Util.class.getSimpleName() + "/" + resource);

        return new File(res.toURI());
    }

    public static void pause(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
        }
    }

    public static String formatTrace(String... frames) {
        StringBuilder sb = new StringBuilder();
        for (String frame: frames) {
            sb.append('\t').append(frame).append(NL);
        }

        return sb.toString();
    }

    public static String multiline(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String line: lines) {
            sb.append(line).append(NL);
        }

        return sb.toString();
    }

    public static int currentPid() {
        return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", ""));
    }

    public static <T> T only(Iterable<T> set) {
        Iterator<T> it = set.iterator();
        T elem = it.next();
        if (elem == null || it.hasNext()) throw new AssertionError(
                "One set element exected: " + set
        );

        return elem;
    }
}
