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
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory;
import com.github.olivergondza.dumpling.factory.JmxRuntimeFactory.FailedToInitializeJmxConnection;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * Wrapper around tools.jar classes to be loaded using isolated classloader.
 *
 * This is necessary to access those classes without tempering with system classloader.
 *
 * This is not part of Dumpling API.
 *
 * @author ogondza
 */
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

        String agent = vm.getSystemProperties().getProperty("java.home")
                + File.separator + "lib" + File.separator
                + "management-agent.jar"
        ;

        try {
            vm.loadAgent(agent);
        } catch (AgentLoadException ex) {
            throw failed("Unable to load agent", ex);
        } catch (AgentInitializationException ex) {
            throw failed("Unable to initialize agent", ex);
        }

        address = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        if (address != null) return address;

        throw new AssertionError("Agent not loaded");
    }

    private static FailedToInitializeJmxConnection failed(String message, Exception ex) {
        return new JmxRuntimeFactory.FailedToInitializeJmxConnection(message + ": " + ex.getMessage(), ex);
    }
}