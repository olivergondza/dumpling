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

import java.io.File;
import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.github.olivergondza.dumpling.model.ThreadStatus;
import com.github.olivergondza.dumpling.model.jmx.JmxRuntime;
import com.github.olivergondza.dumpling.model.jmx.JmxThread;

/**
 * Create runtime from running process via JMX interface.
 *
 * A process can be identified by process ID or by host and port combination.
 *
 * @author ogondza
 */
public final class JmxRuntimeFactory {

    private static final ObjectName THREADING_MBEAN;
    private static final ObjectName RUNTIME_MBEAN;
    static {
        try {
            THREADING_MBEAN = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
            RUNTIME_MBEAN = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    public @Nonnull JmxRuntime forConnectionString(@Nonnull String locator) throws FailedToInitializeJmxConnection {
        try {
            int pid = Integer.parseInt(locator);
            return forLocalProcess(pid);
        } catch (NumberFormatException ex) {
            // No a PID - remote process
        }

        String username = null;
        String password = null;
        List<String> chunks = Arrays.asList(locator.split("[:@]"));
        Collections.reverse(chunks);

        int port = Integer.parseInt(chunks.get(0));
        String host = chunks.get(1);
        if (chunks.size() == 4) {
            password = chunks.get(2);
            username = chunks.get(3);
        }
        return forRemoteProcess(host, port, username, password);
    }

    public @Nonnull JmxRuntime forRemoteProcess(@Nonnull String host, int port) throws FailedToInitializeJmxConnection {
        return forRemoteProcess(host, port, null, null);
    }

    public @Nonnull JmxRuntime forRemoteProcess(@Nonnull String host, int port, String username, String password) throws FailedToInitializeJmxConnection {
        return fromConnection(new RemoteConnector(host, port, username, password).getServerConnection());
    }

    public @Nonnull JmxRuntime forLocalProcess(int pid) throws FailedToInitializeJmxConnection {
        return fromConnection(new LocalConnector(pid).getServerConnection());
    }

    private @Nonnull JmxRuntime fromConnection(@Nonnull MBeanServerConnection connection) {
        return extractRuntime(connection);
    }

    private @Nonnull JmxRuntime extractRuntime(@Nonnull MBeanServerConnection connection) {
        final List<ThreadInfo> threads = getRemoteThreads(connection);
        HashSet<JmxThread.Builder> builders = new HashSet<JmxThread.Builder>(threads.size());

        for (ThreadInfo thread: threads) {
            JmxThread.Builder builder = new JmxThread.Builder();
            final ThreadStatus status = fillThreadInfoData(thread, builder);

            final LockInfo lockInfo = thread.getLockInfo();
            if (lockInfo != null) {
                builder.setWaitingToLock(getSynchronizer(lockInfo));
            }

            builders.add(builder);
        }

        return new JmxRuntime(builders, new Date(), getVmName(connection));
    }

    private List<ThreadInfo> getRemoteThreads(@Nonnull MBeanServerConnection connection) {
        ThreadMXBean proxy = JMX.newMXBeanProxy(connection, THREADING_MBEAN, ThreadMXBean.class);
        return Arrays.asList(proxy.dumpAllThreads(true, true));
    }

    @SuppressWarnings("null")
    private @Nonnull String getVmName(@Nonnull MBeanServerConnection connection) {
        RuntimeMXBean proxy = JMX.newMXBeanProxy(connection, RUNTIME_MBEAN, RuntimeMXBean.class);
        Map<String, String> props = proxy.getSystemProperties();

        return String.format(
                "Dumpling JMX thread dump %s (%s):",
                props.get("java.vm.name"),
                props.get("java.vm.version")
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
                Throwable cause = ex.getCause(); // Unwrap and rethrow as FailedToInitializeJmxConnection if necessary
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
            AssertionError ex = new AssertionError(msg);
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

            JMXServiceURL serviceUrl = getServiceUrl();
            try {
                return JMXConnectorFactory.connect(serviceUrl, map).getMBeanServerConnection();
            } catch (SecurityException ex) {
                throw new FailedToInitializeJmxConnection("Failed to initialize connection to " + serviceUrl + ": " + ex.getMessage(), ex);
            } catch (IOException ex) {
                throw new FailedToInitializeJmxConnection("Failed to initialize connection to " + serviceUrl + ": " + ex.getMessage(), ex);
            }
        }

        private JMXServiceURL getServiceUrl() {
            try {
                return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
            } catch (MalformedURLException ex) {
                throw new FailedToInitializeJmxConnection(ex);
            }
        }
    }

    public static final class FailedToInitializeJmxConnection extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public FailedToInitializeJmxConnection(Throwable ex) {
            super(ex.getMessage(), ex);
        }

        public FailedToInitializeJmxConnection(String string, Throwable ex) {
            super(string, ex);
        }

        public FailedToInitializeJmxConnection(String message) {
            super(message);
        }
    }
}
