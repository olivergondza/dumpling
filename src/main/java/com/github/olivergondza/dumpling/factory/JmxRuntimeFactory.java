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

import java.io.File;
import java.io.IOException;
import java.lang.Thread.State;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.github.olivergondza.dumpling.cli.CliRuntimeFactory;
import com.github.olivergondza.dumpling.cli.CommandFailedException;
import com.github.olivergondza.dumpling.cli.ProcessStream;
import com.github.olivergondza.dumpling.model.ProcessRuntime;
import com.github.olivergondza.dumpling.model.ProcessThread;
import com.github.olivergondza.dumpling.model.ProcessThread.Builder;
import com.github.olivergondza.dumpling.model.StackTrace;
import com.github.olivergondza.dumpling.model.ThreadLock;
import com.github.olivergondza.dumpling.model.ThreadStatus;

public final class JmxRuntimeFactory implements CliRuntimeFactory {

    @Override
    public @Nonnull String getKind() {
        return "jmx";
    }

    @Override
    public @Nonnull ProcessRuntime createRuntime(@Nonnull String locator, @Nonnull ProcessStream process) throws CommandFailedException {
        try {
            return fromConnection(locateConnection(locator));
        } catch (FailedToInitializeJmxConnection ex) {
            throw new CommandFailedException(ex);
        }
    }

    public @Nonnull ProcessRuntime forRemoteProcess(@Nonnull String host, int port) throws FailedToInitializeJmxConnection {
        return forRemoteProcess(host, port, null, null);
    }

    public @Nonnull ProcessRuntime forRemoteProcess(@Nonnull String host, int port, String username, String password) throws FailedToInitializeJmxConnection {
        return fromConnection(new RemoteConnector(host, port, username, password).getServerConnection());
    }

    public @Nonnull ProcessRuntime forLocalProcess(int pid) throws FailedToInitializeJmxConnection {
        return fromConnection(new LocalConnector(pid).getServerConnection());
    }

    private @Nonnull MBeanServerConnection locateConnection(@Nonnull String locator) throws FailedToInitializeJmxConnection {
        try {
            int pid = Integer.parseInt(locator);
            return new LocalConnector(pid).getServerConnection();
        } catch (NumberFormatException ex) {
            return new RemoteConnector(locator).getServerConnection();
        }
    }

    private @Nonnull ProcessRuntime fromConnection(@Nonnull MBeanServerConnection connection) {
        try {
            return extractRuntime(connection);
        } catch (MBeanException ex) {
            throw new FailedToInitializeJmxConnection("Remote MBean thrown an exception", ex);
        } catch (JMException ex) {
            throw new FailedToInitializeJmxConnection("Remote method call failed", ex);
        } catch (IOException ex) {
            throw new FailedToInitializeJmxConnection("Remote connection broken", ex);
        }
    }

    private @Nonnull ProcessRuntime extractRuntime(@Nonnull MBeanServerConnection connection) throws MBeanException, JMException, IOException {
        final CompositeData[] threads = getRemoteThreads(connection);
        HashSet<Builder> builders = new HashSet<ProcessThread.Builder>(threads.length);

        for (CompositeData thread: threads) {
            State state = Thread.State.valueOf((String) thread.get("threadState"));

            ProcessThread.Builder builder = new ProcessThread.Builder()
                    .setName((String) thread.get("threadName"))
                    .setId((Long) thread.get("threadId"))
                    .setThreadStatus(ThreadStatus.fromState(state))
                    .setStacktrace(getStackTrace(thread))
            ;

            final List<ThreadLock.Monitor> monitors = getMonitors(thread);
            final List<ThreadLock> synchonizers = getSynchronizers(thread);

            builder.setAcquiredMonitors(monitors).setAcquiredSynchronizers(synchonizers);
            builders.add(builder);
        }

        return new ProcessRuntime(builders);
    }

    private List<ThreadLock.Monitor> getMonitors(CompositeData thread) {
        final CompositeData[] rawMonitors = (CompositeData[]) thread.get("lockedMonitors");
        if (rawMonitors.length == 0) return Collections.emptyList();

        final List<ThreadLock.Monitor> monitors = new ArrayList<ThreadLock.Monitor>(rawMonitors.length);
        for (CompositeData rm: rawMonitors) {
            monitors.add(new ThreadLock.Monitor(
                    createLock(rm), (Integer) rm.get("lockedStackDepth")
            ));
        }
        return monitors;
    }

    private List<ThreadLock> getSynchronizers(CompositeData thread) {
        final CompositeData[] rawSynchonizers = (CompositeData[]) thread.get("lockedSynchronizers");
        if (rawSynchonizers.length == 0) return Collections.emptyList();

        final List<ThreadLock> synchonizers = new ArrayList<ThreadLock>(rawSynchonizers.length);
        for (CompositeData rs: rawSynchonizers) {
            synchonizers.add(createLock(rs));
        }
        return synchonizers;
    }

    private ThreadLock createLock(CompositeData rm) {
        return new ThreadLock(
                (String) rm.get("className"),
                (Integer) rm.get("identityHashCode")
        );
    }

    private @Nonnull StackTraceElement[] getStackTrace(CompositeData thread) {
        final CompositeData[] traceSource = (CompositeData[]) thread.get("stackTrace");
        final StackTraceElement[] traceFrames = new StackTraceElement[traceSource.length];
        for (int i = 0; i < traceSource.length; i++) {
            final CompositeData frame = traceSource[i];

            final String className = (String) frame.get("className");
            final String methodName = (String) frame.get("methodName");
            final String fileName = (String) frame.get("fileName");
            final int lineNumber = (Integer) frame.get("lineNumber");

            traceFrames[i] = StackTrace.element(className, methodName, fileName, lineNumber);
        }
        return traceFrames;
    }

    private CompositeData[] getRemoteThreads(@Nonnull MBeanServerConnection connection) throws MBeanException, JMException, IOException {
        // [blockedCount, blockedTime, inNative, lockInfo, lockName, lockOwnerId, lockOwnerName, lockedMonitors, lockedSynchronizers, stackTrace, suspended, threadId, threadName, threadState, waitedCount, waitedTime]
        return (CompositeData[]) connection.invoke(
                new ObjectName("java.lang:type=Threading"),
                "dumpAllThreads",
                new Boolean[] {true, true},
                new String[] {boolean.class.getName(), boolean.class.getName()}
        );
    }

    private static final class LocalConnector {
        private static final String CONNECTOR_CLASS_NAME = "com.github.olivergondza.dumpling.factory.jmx.JmxLocalProcessConnector";
        private final @Nonnegative int pid;

        private LocalConnector(@Nonnegative int pid) {
            this.pid = pid;
        }

        /* Delegate to JmxLocalProcessConnector in separated classloader */
        private @Nonnull MBeanServerConnection getServerConnection() {
            ClassLoader classLoader = loadToolsJarClasses();

            try {
                final Class<?> type = classLoader.loadClass(CONNECTOR_CLASS_NAME);
                final Method method = type.getDeclaredMethod("getServerConnection", int.class);
                method.setAccessible(true);
                return (MBeanServerConnection) method.invoke(null, pid);
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause(); // Unwrap and rethrow as FailedToInitializeJmxConnection is necessary
                if (cause instanceof FailedToInitializeJmxConnection) throw (FailedToInitializeJmxConnection) cause;
                throw new FailedToInitializeJmxConnection(cause);
            } catch (ClassNotFoundException ex) {
                throw assertionError("Unable to invoke " + CONNECTOR_CLASS_NAME, ex);
            } catch (NoSuchMethodException ex) {
                throw assertionError("Unable to invoke " + CONNECTOR_CLASS_NAME, ex);
            } catch (SecurityException ex) {
                throw assertionError("Unable to invoke " + CONNECTOR_CLASS_NAME, ex);
            } catch (IllegalAccessException ex) {
                throw assertionError("Unable to invoke " + CONNECTOR_CLASS_NAME, ex);
            } catch (IllegalArgumentException ex) {
                throw assertionError("Unable to invoke " + CONNECTOR_CLASS_NAME, ex);
            }
        }

        private AssertionError assertionError(String msg, Throwable cause) {
            AssertionError ex = new AssertionError("Unable to invoke " + CONNECTOR_CLASS_NAME);
            ex.initCause(cause);
            return ex;
        }

        private ClassLoader loadToolsJarClasses() {
            final ClassLoader currentClassLoader = this.getClass().getClassLoader();

            try {
                Class.forName("com.sun.tools.attach.VirtualMachine");
                return currentClassLoader;
            } catch (ClassNotFoundException ex) {
                // Using null as parent classloader to baypass parent-first policy
                return new URLClassLoader(locateJars(), null);
            }
        }

        private URL[] locateJars() {
            final String dumplingJar = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            final String javaHome = System.getProperty("java.home");
            try {
                return jarUrlArray(dumplingJar, javaHome + "/lib/tools.jar", javaHome + "/../lib/tools.jar");
            } catch (MalformedURLException ex) {
                throw new FailedToInitializeJmxConnection(ex);
            }
        }

        private URL[] jarUrlArray(@Nonnull String... jars) throws MalformedURLException {

            ArrayList<URL> out = new ArrayList<URL>(jars.length);
            for (String jar: jars) {
                File file = new File(jar);
                if (file.isFile()) {
                    out.add(file.toURI().toURL());
                }
            }

            return out.toArray(new URL[out.size()]);
        }
    }

    /*package*/ static final class RemoteConnector {
        /*package*/ final @Nonnull String host;
        /*package*/ final @Nonnegative int port;
        /*package*/ String username;
        /*package*/ String password;

        /*package*/ RemoteConnector(@Nonnull String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        /*package*/ RemoteConnector(@Nonnull String locator) {

            List<String> chunks = Arrays.asList(locator.split("[:@]"));
            Collections.reverse(chunks);

            port = Integer.parseInt(chunks.get(0));
            host = chunks.get(1);
            if (chunks.size() == 4) {
                password = chunks.get(2);
                username = chunks.get(3);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(host).append(':').append(port);
            if (username != null) {
                sb.insert(0, '@').insert(0, password).insert(0, ':').insert(0, username);
            }

            return sb.toString();
        }

        private @Nonnull MBeanServerConnection getServerConnection() {

            HashMap<String, String[]> map = new HashMap<String, String[]>();
            if (username != null) {
                map.put(JMXConnector.CREDENTIALS, new String[] {username, password});
            }

            try {
                return JMXConnectorFactory.connect(getServiceUrl(), map).getMBeanServerConnection();
            } catch (SecurityException ex) {
                throw new FailedToInitializeJmxConnection(ex);
            } catch (IOException ex) {
                throw new FailedToInitializeJmxConnection(ex);
            }
        }

        private JMXServiceURL getServiceUrl() {
            try {
                //return new JMXServiceURL("rmi", host, port);
                return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
            } catch (MalformedURLException ex) {
                throw new FailedToInitializeJmxConnection(ex);
            }
        }
    }

    public static final class FailedToInitializeJmxConnection extends RuntimeException {

        public FailedToInitializeJmxConnection(Throwable ex) {
            super(ex.getMessage(), ex);
        }

        public FailedToInitializeJmxConnection(String string, Throwable ex) {
            super(string, ex);
        }
    }
}