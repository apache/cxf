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
package org.apache.cxf.systest.sts.issuer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * Some tests where the STS address is not hard-coded in the client
 */
public class IssuerTest extends AbstractBusClientServerTestBase {
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(Server.class);
    
    // These tests require port numbers in the WSDLs and so we can't easily do variable substitution
    private static boolean portFree = true;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
        
        try {
            ServerSocket sock = new ServerSocket(30101);
            sock.close();
            
            assertTrue(
                       "Server failed to launch",
                       // run the server in the same process
                       // set this to false to fork
                       launchServer(STSServer.class, true)
            );
        } catch (IOException ex) {
            portFree = false;
            // portFree is set to false + the test won't run
        }
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    // In this test, the client uses the address defined in the <sp:Issuer> address in the policy
    // of the service provider to contact the STS. The client is configured with the STS's service
    // Policy. Useful if you want a simple way to avoid hardcoding the STS host/port in the client.
    @org.junit.Test
    public void testSAML1Issuer() throws Exception {
        
        if (!portFree) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = IssuerTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = IssuerTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, PORT);

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
        bus.shutdown(true);
    }
    
    // Test getting the STS details via WS-MEX
    @org.junit.Test
    public void testSAML2MEX() throws Exception {
        
        if (!portFree) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = IssuerTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = IssuerTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
        DoubleItPortType transportSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, PORT);

        doubleIt(transportSaml2Port, 25);

        ((java.io.Closeable)transportSaml2Port).close();
        bus.shutdown(true);
    }
    
    // Test getting the STS details via WS-MEX + SOAP 1.2
    @org.junit.Test
    public void testSAML2MEXSoap12() throws Exception {
        
        if (!portFree) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = IssuerTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = IssuerTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Soap12Port");
        DoubleItPortType transportSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, PORT);

        doubleIt(transportSaml2Port, 25);

        ((java.io.Closeable)transportSaml2Port).close();
        bus.shutdown(true);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
}
