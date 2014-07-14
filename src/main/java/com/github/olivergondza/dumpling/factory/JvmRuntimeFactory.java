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
package com.github.olivergondza.dumpling.factory;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ProcessThread.Builder;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public class JvmRuntimeFactory {

    public @Nonnull ProcessRuntime currentRuntime() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        Map<Long, ThreadInfo> infos = infos();

        HashSet<ProcessThread.Builder> state = new HashSet<ProcessThread.Builder>(threads.size());

        for (Thread thread: threads) {
            Builder builder = ProcessThread.builder()
                    .setName(thread.getName())
                    .setId(thread.getId())
                    .setDaemon(thread.isDaemon())
                    .setPriority(thread.getPriority())
                    .setStacktrace(thread.getStackTrace())
                    .setState(thread.getState())
                    .setStatus(status(thread))
            ;

            ThreadInfo info = infos.get(thread.getId());
            if (info == null) throw new AssertionError("No thread info for thread " + thread.getName());
            builder.setAcquiredLocks(locks(info));
            LockInfo lock = info.getLockInfo();
            if (lock != null) builder.setLock(lock(lock));

            state.add(builder);
        }

        return new ProcessRuntime(state);
    }

    private Map<Long, ThreadInfo> infos() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<Long, ThreadInfo> infos= new HashMap<Long, ThreadInfo>();
        for (ThreadInfo info: threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), true, true)) {

            infos.put(info.getThreadId(), info);
        }

        return infos;
    }

    private Set<ThreadLock> locks(ThreadInfo threadInfo) {
        MonitorInfo[] monitors = threadInfo.getLockedMonitors();
        LockInfo[] synchronizers = threadInfo.getLockedSynchronizers();

        Set<ThreadLock> locks = new HashSet<ThreadLock>(monitors.length + synchronizers.length);
        for (LockInfo info: monitors) {
            locks.add(lock(info));
        }

        for (LockInfo info: synchronizers) {
            locks.add(lock(info));
        }

        return locks;
    }

    private ThreadLock lock(LockInfo info) {
        return new ThreadLock(info.getClassName(), info.getIdentityHashCode());
    }

    private ThreadStatus status(Thread thread) {

        try {
            int code = threadStatus.getInt(thread);
            return ThreadStatus.valueOf(code);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }

        return ThreadStatus.UNKNOWN;
    }

    private static Field threadStatus;
    static {
        try {
            threadStatus = Thread.class.getDeclaredField("threadStatus");
            threadStatus.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }
}
