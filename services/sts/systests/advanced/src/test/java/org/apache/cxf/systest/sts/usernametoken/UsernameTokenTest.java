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
package org.apache.cxf.systest.sts.usernametoken;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.systest.sts.deployment.StaxSTSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * In this test case, a CXF client sends a Username Token via (1-way) TLS to a CXF provider.
 * The provider dispatches the Username Token to an STS for validation (via TLS). It also
 * includes a test where the service provider sends the token for validation using the
 * WS-Trust "Issue" binding, and sending the token "OnBehalfOf". Roles are also requested, and
 * access is only granted to the service if the "admin-user" role is in effect.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class UsernameTokenTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(Server.class);
    private static final String STAX_PORT = allocatePort(StaxServer.class);

    final TestParam test;
    
    public UsernameTokenTest(TestParam type) {
        this.test = type;
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxSTSServer.class, true)
        );
    }
    
    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {
       
        return Arrays.asList(new TestParam[][] {{new TestParam(PORT, false, "")},
                                                {new TestParam(PORT, true, "")},
                                                {new TestParam(STAX_PORT, false, "")},
                                                {new TestParam(STAX_PORT, true, "")},
        });
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = UsernameTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportUTPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportUTPort);
        }
        
        doubleIt(transportUTPort, 25);
        
        ((java.io.Closeable)transportUTPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testBadUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("cxf-bad-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportUTPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportUTPort);
        }
        
        try {
            doubleIt(transportUTPort, 30);
            fail("Expected failure on a bad password");
        } catch (javax.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }
        
        ((java.io.Closeable)transportUTPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUsernameTokenAuthorization() throws Exception {
        // Token transformation is not supported for the streaming code
        if (STAX_PORT.equals(test.getPort())) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = UsernameTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTAuthorizationPort");
        DoubleItPortType transportUTPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportUTPort);
        }
        
        doubleIt(transportUTPort, 25);
        
        ((java.io.Closeable)transportUTPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUnauthorizedUsernameToken() throws Exception {
        // Token transformation is not supported for the streaming code
        if (STAX_PORT.equals(test.getPort())) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("cxf-bad-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTAuthorizationPort");
        DoubleItPortType transportUTPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportUTPort);
        }
        
        try {
            doubleIt(transportUTPort, 30);
            fail("Expected failure on a bad password");
        } catch (javax.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }
        
        ((java.io.Closeable)transportUTPort).close();
        bus.shutdown(true);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
}
