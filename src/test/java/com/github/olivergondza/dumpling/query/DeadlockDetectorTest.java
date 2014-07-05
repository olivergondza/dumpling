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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import com.github.olivergondza.dumpling.factory.JvmRuntimeFactory;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ThreadSet;

public class DeadlockDetectorTest {

    volatile boolean running = false;
    @Test
    public void noDeadlockShouldBePresentNormally() {

        Thread thread = new Thread("Running thread") {
            @Override
            public void run() {
                running = true;
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // TODO Auto-generated catch block
                        ex.printStackTrace();
                    }
                }
                running = false;
            }
        };
        thread.start();

        pause(100);

        assertTrue("No deadlock should be present", deadlocks().isEmpty());

        assertTrue(running);
    }

    @Test
    public void discoverActualDeadlock() {
        final Object lockA = new Object();
        final Object lockB = new Object();

        new Thread("Deadlock thread A") {
            @Override
            public void run() {
                while (true) {
                    synchronized (lockA) {
                        pause(100);
                        synchronized (lockB) {
                            hashCode();
                        }
                    }
                }
            }
        }.start();

        new Thread("Deadlock thread B") {
            @Override
            public void run() {
                while (true) {
                    synchronized (lockB) {
                        pause(100);
                        synchronized (lockA) {
                            hashCode();
                        }
                    }
                }
            }
        }.start();

        pause(1000);

        final Set<ThreadSet> deadlocks = deadlocks();

        assertEquals("One deadlock should be present", 1, deadlocks.size());
        for (ThreadSet deadlock: deadlocks) {
            assertEquals("Deadlock should contain of 2 threads", 2, deadlock.size());
            for (ProcessThread thread: deadlock) {
                assertTrue(thread.getName().matches("Deadlock thread [AB]"));
            }
        }
    }

    private static void pause(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
        }
    }

    private Set<ThreadSet> deadlocks() {
        return new DeadlockDetector().getAll(new JvmRuntimeFactory().currentRuntime());
    }
}
