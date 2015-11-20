package com.github.olivergondza.dumpling.model;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import com.github.olivergondza.dumpling.factory.IllegalRuntimeStateException;
import com.github.olivergondza.dumpling.model.ThreadLock.Monitor;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpRuntime;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread;
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThread.Builder;

public class ProcessRuntimeTest {

    @Test
    public void multipleThreadsHoldingSameMonitor() {
        Monitor commonLock = new Monitor(new ThreadLock("java.lang.Object", 42), 0);
        StackTraceElement ste = new StackTraceElement("java.lang.Object", "toString", "Object.java", 42);

        HashSet<Builder> set = new HashSet<ThreadDumpThread.Builder>();
        set.add(new ThreadDumpThread.Builder().setId(1).setName("1").setAcquiredMonitors(commonLock).setStacktrace(ste));
        set.add(new ThreadDumpThread.Builder().setId(2).setName("2").setAcquiredMonitors(commonLock).setStacktrace(ste));

        try {
            new ThreadDumpRuntime(set, Arrays.asList("Fake"));
            fail();
        } catch (IllegalRuntimeStateException ex) {
            assertThat(ex.getMessage(), containsString(
                    "Multiple threads own the same monitor '<0x2a> (a java.lang.Object)':"
            ));
        }
    }

    @Test
    public void multipleThreadsHoldingSameSynchronizer() {
        ThreadLock commonLock = new ThreadLock("java.lang.Object", 42);

        HashSet<Builder> set = new HashSet<ThreadDumpThread.Builder>();
        set.add(new ThreadDumpThread.Builder().setId(1).setName("1").setAcquiredSynchronizers(commonLock));
        set.add(new ThreadDumpThread.Builder().setId(2).setName("2").setAcquiredSynchronizers(commonLock));

        try {
            new ThreadDumpRuntime(set, Arrays.asList("Fake"));
            fail();
        } catch (IllegalRuntimeStateException ex) {
            assertThat(ex.getMessage(), containsString(
                    "Multiple threads own the same synchronizer '<0x2a> (a java.lang.Object)':"
            ));
        }
    }
}
