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

package org.apache.cxf.systest.ws.fault;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.fault.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A set of tests for (signing and encrypting) SOAP Faults.
 */
public class FaultTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testSoap11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap11Port");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        // Make a successful invocation
        ((BindingProvider)utPort).getRequestContext().put("ws-security.username", "alice");
        utPort.doubleIt(25);
        
        // Now make an invocation using another username
        ((BindingProvider)utPort).getRequestContext().put("ws-security.username", "bob");
        ((BindingProvider)utPort).getRequestContext().put("ws-security.password", "password");
        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSoap12() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap12Port");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        // Make a successful invocation
        ((BindingProvider)utPort).getRequestContext().put("ws-security.username", "alice");
        utPort.doubleIt(25);
        
        // Now make an invocation using another username
        ((BindingProvider)utPort).getRequestContext().put("ws-security.username", "bob");
        ((BindingProvider)utPort).getRequestContext().put("ws-security.password", "password");
        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }
        
        bus.shutdown(true);
    }
    
    
}
