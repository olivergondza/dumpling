/*
 * The MIT License
 *
 * Copyright (c) 2015 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Dispose registered resources.
 */
public class DisposeRule implements TestRule {

    private final List<Disposable> discard = new ArrayList<Disposable>();

    public <T extends Thread> T register(final T thread) {
        if (thread == null) return thread;

        discard.add(new Disposable() {
            @Override @SuppressWarnings("deprecation")
            public void dispose() throws Exception {
                thread.stop();
            }
        });
        return thread;
    }

    public <T extends Process> T register(final T process) {
        if (process == null) return process;

        discard.add(new Disposable() {
            @Override
            public void dispose() throws Exception {
                process.destroy();
                process.waitFor();
            }
        });
        return process;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    for (Disposable item: discard) {
                        try {
                            item.dispose();
                        } catch (Exception ex) {
                            ex.printStackTrace(System.err);
                        }
                    }
                }
            }
        };
    }

    private interface Disposable {
        void dispose() throws Exception;
    }
}
