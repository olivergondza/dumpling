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

import static com.github.olivergondza.dumpling.factory.MXBeanFactoryUtils.fillThreadInfoData;
import static com.github.olivergondza.dumpling.factory.MXBeanFactoryUtils.getSynchronizer;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;
import com.github.olivergondza.dumpling.model.jvm.JvmRuntime;
import com.github.olivergondza.dumpling.model.jvm.JvmThread;

/**
 * Create {@link ProcessRuntime} from state of current JVM process.
 *
 * @author ogondza
 */
public class JvmRuntimeFactory {

    private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private static String jvmId = String.format(
            "Dumpling JVM thread dump %s (%s):",
            System.getProperty("java.vm.name"),
            System.getProperty("java.vm.version")
    );

    public @Nonnull JvmRuntime currentRuntime() {
        IllegalRuntimeStateException error = null;
        for (int retry = 0; retry < 5; retry++) {
            try {
                return _currentRuntime();
            } catch (IllegalRuntimeStateException ex) {
                error = ex;
            }
        }

        throw error;
    }

    private JvmRuntime _currentRuntime() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        Map<Long, ThreadInfo> infos = infos();

        HashSet<JvmThread.Builder> state = new HashSet<JvmThread.Builder>(threads.size());

        for (Thread thread: threads) {
            ThreadInfo info = infos.get(thread.getId());
            // The thread was terminated between Thread.getAllStackTraces() and ThreadMXBean.getThreadInfo()
            if (info == null) continue;

            JvmThread.Builder builder = new JvmThread.Builder(thread)
                    .setDaemon(thread.isDaemon())
                    .setPriority(thread.getPriority())
            ;
            final ThreadStatus status = fillThreadInfoData(info, builder);

            LockInfo lockInfo = info.getLockInfo();
            if (lockInfo != null) {
                ThreadLock lock = getSynchronizer(lockInfo);
                if (status.isBlocked()) {
                    builder.setWaitingToLock(lock);
                } else if (status.isWaiting() || status.isParked()) {
                    builder.setWaitingOnLock(lock);

                    // Remove monitor we are waiting on (https://github.com/olivergondza/dumpling/issues/68)
                    List<ThreadLock.Monitor> reportedMonitors = builder.getAcquiredMonitors();
                    List<ThreadLock.Monitor> filteredMonitors = new ArrayList<ThreadLock.Monitor>(reportedMonitors.size());
                    for (ThreadLock.Monitor monitor : reportedMonitors) {
                        if (monitor.getLock().equals(lock)) continue;

                        filteredMonitors.add(monitor);
                    }
                    builder.setAcquiredMonitors(filteredMonitors);
                } else {
                    throw new IllegalRuntimeStateException(
                            "Thread declares lock while %s: %n%s%n", status, info
                    );
                }
            }

            state.add(builder);
        }

        return new JvmRuntime(state, new Date(), jvmId);
    }

    private Map<Long, ThreadInfo> infos() {
        Map<Long, ThreadInfo> infos = new HashMap<Long, ThreadInfo>();
        for (ThreadInfo info: threadMXBean.dumpAllThreads(true, true)) {
            infos.put(info.getThreadId(), info);
        }

        return infos;
    }
}
