/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.management.jmx;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

import org.apache.cxf.common.logging.LogUtils;



/**
 * Deal with the MBeanServer Connections
 *
 */
public final class MBServerConnectorFactory {

    public static final String DEFAULT_SERVICE_URL = "service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi";

    private static final Logger LOG = LogUtils.getL7dLogger(MBServerConnectorFactory.class);

    private static MBeanServer server;

    private static String serviceUrl = DEFAULT_SERVICE_URL;

    private static Map<String, ?> environment;

    private static boolean threaded;

    private static boolean daemon;

    private static JMXConnectorServer connectorServer;

    private static Remote remoteServerStub;

    private static RMIJRMPServerImpl rmiServer;

    private static class MBServerConnectorFactoryHolder {
        private static final MBServerConnectorFactory INSTANCE =
            new MBServerConnectorFactory();
    }

    private static class MBeanServerHolder {
        private static final MBeanServer INSTANCE =
            MBeanServerFactory.createMBeanServer();
    }

    private MBServerConnectorFactory() {

    }

    static int getServerPort(final String url) {
        int portStart = url.indexOf("localhost") + 10;
        int portEnd;
        int port = 0;
        if (portStart > 0) {
            portEnd = indexNotOfNumber(url, portStart);
            if (portEnd > portStart) {
                final String portString = url.substring(portStart, portEnd);
                port = Integer.parseInt(portString);
            }
        }
        return port;
    }

    private static int indexNotOfNumber(String str, int index) {
        int i = 0;
        for (i = index; i < str.length(); i++) {
            if (str.charAt(i) < '0' || str.charAt(i) > '9') {
                return i;
            }
        }
        return -1;
    }

    public static MBServerConnectorFactory getInstance() {
        return MBServerConnectorFactoryHolder.INSTANCE;
    }

    public void setMBeanServer(MBeanServer ms) {
        server = ms;
    }

    public void setServiceUrl(String url) {
        serviceUrl = url;
    }

    public void setEnvironment(Map<String, ?> env) {
        environment = env;
    }

    public void setThreaded(boolean fthread) {
        threaded = fthread;
    }

    public void setDaemon(boolean fdaemon) {
        daemon = fdaemon;
    }


    public void createConnector() throws IOException {

        if (server == null) {
            server = MBeanServerHolder.INSTANCE;
        }

        // Create the JMX service URL.
        final JMXServiceURL url = new JMXServiceURL(serviceUrl);

        // if the URL is localhost, start up an Registry
        if (serviceUrl.indexOf("localhost") > -1
            && url.getProtocol().compareToIgnoreCase("rmi") == 0) {
            try {
                int port = getRegistryPort(serviceUrl);
                new JmxRegistry(port, getBindingName(url));

            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "CREATE_REGISTRY_FAULT_MSG", new Object[]{ex});
            }
        }

        rmiServer = new RMIJRMPServerImpl(getServerPort(serviceUrl), null, null, environment);

        // Create the connector server now.
        connectorServer = new RMIConnectorServer(url, environment, rmiServer, server);

        if (threaded) {
             // Start the connector server asynchronously (in a separate thread).
            Thread connectorThread = new Thread() {
                public void run() {
                    try {
                        connectorServer.start();
                        remoteServerStub = rmiServer.toStub();
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, "START_CONNECTOR_FAILURE_MSG", new Object[]{ex});
                    }
                }
            };

            connectorThread.setName("JMX Connector Thread [" + serviceUrl + "]");
            connectorThread.setDaemon(daemon);
            connectorThread.start();
        } else {
             // Start the connector server in the same thread.
            connectorServer.start();
            remoteServerStub = rmiServer.toStub();
        }

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("JMX connector server started: " + connectorServer);
        }
    }

    static int getRegistryPort(final String url) {
        int serverStart = url.indexOf("/jndi/rmi://");
        final String serverPart = url.substring(serverStart + 12);
        int portStart = serverPart.indexOf(':') + 1;

        int portEnd;
        int port = 0;
        if (portStart > 0) {
            portEnd = indexNotOfNumber(serverPart, portStart);
            if (portEnd > portStart) {
                final String portString = serverPart.substring(portStart, portEnd);
                port = Integer.parseInt(portString);
            }
        }
        return port;
    }

    protected static String getBindingName(final JMXServiceURL jmxServiceURL) {
        final String urlPath = jmxServiceURL.getURLPath();

        try {
            if (urlPath.startsWith("/jndi/")) {
                return new URI(urlPath.substring(6)).getPath()
                        .replaceAll("^/+", "").replaceAll("/+$", "");
            }
        } catch (URISyntaxException e) {
            // ignore
        }

        return "jmxrmi"; // use the default
    }

    public void destroy() throws IOException {
        connectorServer.stop();
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("JMX connector server stopped: " + connectorServer);
        }
    }

    /*
     * Better to use the internal API than re-invent the wheel.
     */
    @SuppressWarnings("restriction")
    private class JmxRegistry extends sun.rmi.registry.RegistryImpl {
        private final String lookupName;

        JmxRegistry(final int port, final String lookupName) throws RemoteException {
            super(port);
            this.lookupName = lookupName;
        }

        @Override
        public Remote lookup(String s) throws RemoteException, NotBoundException {
            return lookupName.equals(s) ? remoteServerStub : null;
        }

        @Override
        public void bind(String s, Remote remote) throws RemoteException, AlreadyBoundException, AccessException {
        }

        @Override
        public void unbind(String s) throws RemoteException, NotBoundException, AccessException {
        }

        @Override
        public void rebind(String s, Remote remote) throws RemoteException, AccessException {
        }

        @Override
        public String[] list() throws RemoteException {
            return new String[] {lookupName};
        }
    }
}
