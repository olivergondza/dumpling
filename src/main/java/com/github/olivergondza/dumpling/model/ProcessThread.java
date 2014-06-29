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


public class ProcessThread {

    private Builder state;

    private ProcessThread(Builder builder) {
        this.state = builder.clone();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return state.name;
    }

    public long getId() {
        return state.tid;
    }

    public ThreadStatus getThreadStatus() {
        return state.status;
    }

    public Thread.State getState() {
        return state.state;
    }

    @Override
    public String toString() {
        return state.toString();
    }

    public static class Builder implements Cloneable {

        private String name;
        private boolean daemon;
        private int priority;
        private long tid;
        private StackTraceElement[] stackTrace;
        private Thread.State state;
        private ThreadStatus status;

        public ProcessThread build() {
            return new ProcessThread(this);
        }

        @Override
        public Builder clone() {
            try {
                return (Builder) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setId(long tid) {
            this.tid = tid;
            return this;
        }

        public Builder setDaemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder setStacktrace(StackTraceElement[] stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder setState(Thread.State state) {
            this.state = state;
            return this;
        }

        public Builder setStatus(ThreadStatus status) {
            this.status = status;
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('"').append(name).append('"');
            if (daemon) sb.append(" daemon");
            sb.append(" prio=").append(priority);
            sb.append(" tid=").append(tid);

            sb.append("\n   java.lang.Thread.State: ").append(status.getCode()).append(status.getName());

            for (StackTraceElement traceLine: stackTrace) {
                sb.append("\n\tat ").append(traceLine);
            }
            return sb.toString();
        }
    }
}
