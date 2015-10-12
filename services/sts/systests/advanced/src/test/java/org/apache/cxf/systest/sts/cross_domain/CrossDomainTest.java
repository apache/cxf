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
package org.apache.cxf.systest.sts.cross_domain;

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
 * Some tests that illustrate how CXF clients can get tokens from different STS instances for 
 * service invocations.
 */
public class CrossDomainTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    
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
            
            sock = new ServerSocket(30102);
            sock.close();
            
            assertTrue(
                       "Server failed to launch",
                       // run the server in the same process
                       // set this to false to fork
                       launchServer(STSServer2.class, true)
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

    // In this test, a CXF client checks to see that the location defined on its STSClient is different
    // from that configured in the Issuer of the IssuedToken policy supplied in the WSDL of the
    // service provider. It obtains a SAML Token from the configured STS first, and then sends it in
    // the security header to the second STS. The returned token is then sent to the service provider.
    // This illustrates cross-domain SSO: https://issues.apache.org/jira/browse/CXF-3520
    @org.junit.Test
    @org.junit.Ignore
    public void testCrossDomain() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CrossDomainTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CrossDomainTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCrossDomainPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);
        
        ((java.io.Closeable)transportPort).close();
        bus.shutdown(true);
    }
    
    // The Service references STS "b". The WSDL of STS "b" has an IssuedToken that references STS "a".
    // So the client gets the WSDL of "b" via WS-MEX, which in turn has an IssuedToken policy.
    // The client has a configured STSClient for this + uses it to get a token from "a", and in
    // turn to use the returned token to get a token from "b", to access the service.
    @org.junit.Test
    public void testCrossDomainMEX() throws Exception {
        
        if (!portFree) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CrossDomainTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CrossDomainTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCrossDomainMEXPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);
        
        ((java.io.Closeable)transportPort).close();
        bus.shutdown(true);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
}
