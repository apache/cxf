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

package org.apache.cxf.systest.ws.addressing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;

import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the addition of WS-Addressing Message Addressing Properties
 * in the non-decoupled case.
 */
public class DecoupledJMSTest extends MAPTestBase {
    static final String PORT = allocatePort(DecoupledJMSTest.class);
    private static final String ADDRESS = "jms:jndi:dynamicQueues/testqueue0001?"
        + "jndiInitialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
        + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=tcp://localhost:" 
        + EmbeddedJMSBrokerLauncher.PORT;
    
    
    private static final String CONFIG =
        "org/apache/cxf/systest/ws/addressing/jms_decoupled.xml";

    public String getConfigFileName() {
        return CONFIG;
    }
    protected void updateAddressPort(Object o, int port) throws MalformedURLException {
    }
    public String getPort() {
        return PORT;
    }

    @Test
    @Override
    public void testImplicitMAPs() throws Exception {
        super.testImplicitMAPs();
    }
    
    public String getAddress() {
        return ADDRESS;
    }
    
    public URL getWSDLURL() {
        return null;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        
        Map<String, String> props = new HashMap<String, String>();
        if (System.getProperty("org.apache.activemq.default.directory.prefix") != null) {
            props.put("org.apache.activemq.default.directory.prefix", 
                      System.getProperty("org.apache.activemq.default.directory.prefix"));
        }
        props.put("java.util.logging.config.file", System
            .getProperty("java.util.logging.config.file"));
        assertTrue("server did not launch correctly", launchServer(EmbeddedJMSBrokerLauncher.class,
                                                                   props, null));

        assertTrue("server did not launch correctly", 
                   launchServer(Server.class, null, 
                                new String[] {ADDRESS, GreeterImpl.class.getName()}, false));
    }
    
    @WebService(serviceName = "SOAPServiceAddressing", 
                portName = "SoapPort", 
                endpointInterface = "org.apache.hello_world_soap_http.Greeter", 
                targetNamespace = "http://apache.org/hello_world_soap_http")
    public static class GreeterImpl extends org.apache.cxf.systest.ws.addressing.AbstractGreeterImpl {
        
    }
}

