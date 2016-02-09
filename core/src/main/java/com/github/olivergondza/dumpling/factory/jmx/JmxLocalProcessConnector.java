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
package com.github.olivergondza.dumpling.factory.jmx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory;
import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory.FailedToInitializeJmxConnection;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 * Wrapper around tools.jar classes to be loaded using isolated classloader.
 *
 * This is necessary to access those classes without tempering with system classloader.
 *
 * This is not part of Dumpling API.
 *
 * @author ogondza
 */
@SuppressWarnings("unused") // Invoked via reflection
/*package*/ final class JmxLocalProcessConnector {
    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    // This has to be called by reflection so it can as well be private to stress this is not an API
    @SuppressWarnings("unused")
    private static MBeanServerConnection getServerConnection(int pid) {
        VirtualMachine vm = getVm(pid);

        try {
            JMXServiceURL serviceURL = new JMXServiceURL(connectorAddress(vm));
            return JMXConnectorFactory.connect(serviceURL).getMBeanServerConnection();
        } catch (MalformedURLException ex) {
            throw failed("JMX connection failed", ex);
        } catch (IOException ex) {
            throw failed("JMX connection failed", ex);
        }

    }

    private static VirtualMachine getVm(int pid) {
        try {
            return VirtualMachine.attach(String.valueOf(pid));
        } catch (AttachNotSupportedException ex) {
            throw failed("VM does not support attach operation", ex);
        } catch (IOException ex) {
            throw failed("VM attach failed", ex);
        }
    }

    private static String connectorAddress(VirtualMachine vm) throws IOException {
        String address = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        if (address != null) return address;

        Properties systemProperties = vm.getSystemProperties();

        // If the JVM is not able to listen to JMX connections, it is necessary to have the agent loaded.
        // There does not seem to be a portable way to do so. This mostly works for hotspot:
        // Java 6 to 8: The management-agent.jar needs to be loaded
        // Java 7+, jcmd can be used to load the agent
        // Java 9+, management-agent.jar is deleted
        // https://bugs.openjdk.java.net/browse/JDK-8043939

        // jcmd approach first
        if (vm instanceof HotSpotVirtualMachine) {
            HotSpotVirtualMachine hsvm = (HotSpotVirtualMachine) vm;
            try {
                InputStream in = hsvm.executeJCmd("ManagementAgent.start_local");
                in.close(); // Is there anything interesting?
            } catch (IOException ex) {
                throw failed("Unable to load agent", ex);
            }

            address = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
            if (address != null) return address;

            throw failedUnsupported("Unable to get connection address despite `jcmd ManagementAgent.start_local` succeeded", systemProperties);
        }

        String agentPath = systemProperties.getProperty("java.home")
                + File.separator + "lib" + File.separator
                + "management-agent.jar"
        ;
        if (new File(agentPath).exists()) {
            try {
                vm.loadAgent(agentPath);
            } catch (AgentLoadException ex) {
                throw failed("Unable to load agent", ex);
            } catch (AgentInitializationException ex) {
                throw failed("Unable to initialize agent", ex);
            }

            address = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
            if (address != null) return address;

            throw failedUnsupported("Unable to get connection address despite management-agent.jar loaded successfully", systemProperties);
        }

        throw failedUnsupported("Unable to connect to JVM", systemProperties);
    }

    private static FailedToInitializeJmxConnection failed(String message, Exception ex) {
        return new JmxRuntimeFactory.FailedToInitializeJmxConnection(message + ": " + ex.getMessage(), ex);
    }

    private static FailedToInitializeJmxConnection failedUnsupported(String message, Properties systemProperties) {
        String unsupported = String.format(
                "%nDumpling is talking to unsupported JVM. Report this as a bug together with following details: vendor: %s; version: %s; os: %s",
                systemProperties.getProperty("java.vm.vendor"),
                systemProperties.getProperty("java.vm.version"),
                systemProperties.getProperty("os.version")
        );
        return new JmxRuntimeFactory.FailedToInitializeJmxConnection(message + unsupported);
    }
}
