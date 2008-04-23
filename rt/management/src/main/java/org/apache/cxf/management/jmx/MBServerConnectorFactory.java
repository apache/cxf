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
import java.rmi.registry.LocateRegistry;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.cxf.common.logging.LogUtils;



/** 
 * Deal with the MBeanServer Connections 
 *
 */
public final class MBServerConnectorFactory {    
        
    public static final String DEFAULT_SERVICE_URL = "service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi";
    
    private static final Logger LOG = LogUtils.getL7dLogger(MBServerConnectorFactory.class);

    private static MBServerConnectorFactory factory;
    private static MBeanServer server;

    private static String serviceUrl = DEFAULT_SERVICE_URL;

    private static Map<String, ?> environment;

    private static boolean threaded;

    private static boolean daemon;

    private static JMXConnectorServer connectorServer;
    
    private MBServerConnectorFactory() {
        
    }
    
    private int getURLLocalHostPort(String url) {        
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
        if (factory == null) {
            factory = new MBServerConnectorFactory();
        } 
        return factory;        
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
            server = MBeanServerFactory.createMBeanServer(); 
        }
        
        // Create the JMX service URL.
        JMXServiceURL url = new JMXServiceURL(serviceUrl);       
        
        // if the URL is localhost, start up an Registry
        if (serviceUrl.indexOf("localhost") > -1
            && url.getProtocol().compareToIgnoreCase("rmi") == 0) {
            try {
                int port = getURLLocalHostPort(serviceUrl);
                try {
                    LocateRegistry.createRegistry(port);
                } catch (Exception ex) {
                    // the registry may had been created
                    LocateRegistry.getRegistry(port);
                }
               
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "CREATE_REGISTRY_FAULT_MSG", new Object[]{ex});
            }
        }
        
        // Create the connector server now.
        connectorServer = 
            JMXConnectorServerFactory.newJMXConnectorServer(url, environment, server);

       
        if (threaded) {
             // Start the connector server asynchronously (in a separate thread).
            Thread connectorThread = new Thread() {
                public void run() {
                    try {
                        connectorServer.start();
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
        }

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("JMX connector server started: " + connectorServer);
        }    
    }

    public void destroy() throws IOException {        
        connectorServer.stop();        
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("JMX connector server stopped: " + connectorServer);
        } 
    }

}
