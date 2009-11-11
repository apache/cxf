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

package org.apache.cxf.management.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.management.ManagementConstants;


public final class ManagementConsole {
    private static MBeanServerConnection mbsc;
    private static final String DEFAULT_JMXSERVICE_URL = 
        "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi";
    private static final Logger LOG = LogUtils.getL7dLogger(ManagementConsole.class);
    
    String jmxServerURL;
    String portName;
    String serviceName;
    String operationName;
    
    ManagementConsole() {
        
    } 
    
    public void getManagedObjectAttributes(ObjectName name) throws Exception {

        if (mbsc == null) {
            LOG.log(Level.SEVERE , "NO_MBEAN_SERVER");
            return;
        }
        MBeanInfo info = mbsc.getMBeanInfo(name);
        MBeanAttributeInfo[] attrs = info.getAttributes();
        if (attrs == null) {
            return;
        }
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].isReadable()) {
                try {
                    Object o = mbsc.getAttribute(name, attrs[i].getName());
                    System.out.println("\t\t" + attrs[i].getName() + " = " + o);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
  
    
    void connectToMBserver() throws IOException {
        jmxServerURL = jmxServerURL == null ? DEFAULT_JMXSERVICE_URL : jmxServerURL; 
        JMXServiceURL url = new JMXServiceURL(jmxServerURL);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        mbsc = jmxc.getMBeanServerConnection();
    }
    
    void listAllManagedEndpoint() {        
        try {
            ObjectName queryEndpointName = new ObjectName(ManagementConstants.DEFAULT_DOMAIN_NAME 
                                                          + ":type=Bus.Service.Endpoint,*");
            Set<ObjectName> endpointNames = CastUtils.cast(mbsc.queryNames(queryEndpointName, null));
            System.out.println("The endpoints are : ");
            for (ObjectName oName : endpointNames) {
                System.out.println(oName);
                getManagedObjectAttributes(oName);
            }        
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "FAIL_TO_LIST_ENDPOINTS", new Object[]{e});
        }
    }
    
    ObjectName getEndpointObjectName() 
        throws MalformedObjectNameException, NullPointerException {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":type=Bus.Service.Endpoint,");
        buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=\"" + serviceName + "\",");
        buffer.append(ManagementConstants.PORT_NAME_PROP + "=\"" + portName + "\",*");        
        return new ObjectName(buffer.toString());
    }
    
    private void invokeEndpoint(String operation) {
        ObjectName endpointName = null;
        ObjectName queryEndpointName;
        try {
            queryEndpointName = getEndpointObjectName();
            Set<ObjectName> endpointNames = CastUtils.cast(mbsc.queryNames(queryEndpointName, null));
            // now get the ObjectName with the busId 
            Iterator it = endpointNames.iterator();
        
            if (it.hasNext()) {
                // only deal with the first endpoint object which retrun from the list.
                endpointName = (ObjectName)it.next();
                mbsc.invoke(endpointName, operation, new Object[0], new String[0]);
                System.out.println("invoke endpoint " + endpointName 
                                   + " operation " + operation + " succeed!");
            }
            
        } catch (Exception e) {
            if (null == endpointName) {
                LOG.log(Level.SEVERE, "FAIL_TO_CREATE_ENDPOINT_OBEJCTNAME", new Object[]{e});
                
            } else {
                LOG.log(Level.SEVERE, "FAIL_TO_INVOKE_MANAGED_OBJECT_OPERATION",
                    new Object[]{endpointName, operation, e.toString()});
            }
        } 
    }
    
    void startEndpoint() {
        invokeEndpoint("start");        
    }
    
    void stopEndpoint() {
        invokeEndpoint("stop");
    }
    
    void restartEndpoint() {
        invokeEndpoint("stop");
        invokeEndpoint("start");
    }
    
    
    boolean parserArguments(String[] args) {
        portName = "";
        serviceName = "";        
        operationName = "";
        boolean result = false;
        
        int i;
        String arg;
        try {
            for (i = 0; i < args.length; i++) {
                arg = args[i];
                if ("--port".equals(arg) || "-p".equals(arg)) {
                    portName = args[++i];
                    continue;
                }
                if ("--service".equals(arg) || "-s".equals(arg)) {
                    serviceName = args[++i];
                    continue;
                }
                if ("--jmx".equals(arg) || "-j".equals(arg)) {
                    jmxServerURL = args[++i];
                    continue;
                }
                if ("--operation".equals(arg) || "-o".equals(arg)) {
                    operationName = args[++i];
                    // it is the key option
                    result = true;
                    continue;
                }
            }
        } catch (Exception ex) {
            // can't paraser the argument rightly
            return false;
        }
        return result;
    }
    
    private static void printUsage() {
        System.out.println("Management Console for CXF Managed Endpoints");
        System.out.println("You can start and stop the endpoints which export as JMX managed objects");
        System.out.println("Usage: -o list ");
        System.out.println("       -o {start|stop|restart} -p PORTQNAME -s SERVICEQNAME ");
        System.out.println("Valid options:");        
        System.out.println(" -o [--operation] {list|start|stop|restart}  call the managed endpoint "
                        + "operation");
        System.out.println("                          list: show all the managed endpoints' objectNames and");
        System.out.println("                                attributes");
        System.out.println("                          start: start the endpoint with the -p and -s "
                        + "arguments");
        System.out.println("                          stop: stop the endpoint with the -p and -s arguments");
        System.out.println("                          restart: restart the endpoint with the -p and -s "
                        + "arguments");       
        
        System.out.println(" -p [--port] arg          ARG: the port Qname of the managed endpoint");        
        System.out.println(" -s [--service] arg       ARG: the service Qname of the managed endpoint");
        System.out.println(" -j [--jmx] arg           ARG: the JMXServerURL for connecting to the mbean " 
                           + "server");
        System.out.println("                           if not using this option, the JMXServerURL will be "
                           + "set as");
        System.out.println("                           \"service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi"
                        + "\"");
        
        
    }
    
    public void doManagement() {
        try {
            connectToMBserver();
            if ("list".equalsIgnoreCase(operationName)) {
                listAllManagedEndpoint();
                return;
            }
            if ("start".equalsIgnoreCase(operationName)) {
                startEndpoint();
                return;
            }
            if ("stop".equalsIgnoreCase(operationName)) {
                stopEndpoint();
                return;
            }
            if ("restart".equalsIgnoreCase(operationName)) {
                restartEndpoint();
                return;
            } 
            printUsage();           
        } catch (IOException e) {            
            LOG.log(Level.SEVERE, "FAIL_TO_CONNECT_TO_MBEAN_SERVER", new Object[]{jmxServerURL}); 
        } 
        
    }
  
    /**
     * @param args
     */
    public static void main(String[] args) {
        ManagementConsole mc = new ManagementConsole();
        if (mc.parserArguments(args)) {
            mc.doManagement();
        } else {
            printUsage();
        }
        
    }
   

}
