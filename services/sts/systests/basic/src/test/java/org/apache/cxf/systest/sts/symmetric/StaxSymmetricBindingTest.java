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
package org.apache.cxf.systest.sts.symmetric;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;


import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * Test the Symmetric binding. The CXF client gets a token from the STS by authenticating via a
 * Username Token over the symmetric binding, and then sends it to the CXF endpoint using 
 * the symmetric binding.
 * 
 * It tests both DOM + StAX clients against the StAX server
 */
public class StaxSymmetricBindingTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = allocatePort(StaxServer.class);
    
    private static boolean standalone;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(StaxServer.class, true)
        );
        String deployment = System.getProperty("sts.deployment");
        if ("standalone".equals(deployment) || deployment == null) {
            standalone = true;
            assertTrue(
                    "Server failed to launch",
                    // run the server in the same process
                    // set this to false to fork
                    launchServer(STSServer.class, true)
            );
        }
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    
    @org.junit.Test
    public void testUsernameTokenSAML1() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML1Port");
        DoubleItPortType symmetricSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml1Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml1Port, STSPORT2);
        }

        // DOM
        doubleIt(symmetricSaml1Port, 25);
        TokenTestUtils.verifyToken(symmetricSaml1Port);
        
        // Streaming
        symmetricSaml1Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml1Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml1Port, STSPORT);
        }
        SecurityTestUtil.enableStreaming(symmetricSaml1Port);
        doubleIt(symmetricSaml1Port, 25);
        
        ((java.io.Closeable)symmetricSaml1Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUsernameTokenSAML2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML2Port");
        DoubleItPortType symmetricSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml2Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml2Port, STSPORT2);
        }
        
        // DOM
        doubleIt(symmetricSaml2Port, 30);
        TokenTestUtils.verifyToken(symmetricSaml2Port);
        
        // Streaming
        symmetricSaml2Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml2Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml2Port, STSPORT);
        }
        SecurityTestUtil.enableStreaming(symmetricSaml2Port);
        doubleIt(symmetricSaml2Port, 25);
        
        ((java.io.Closeable)symmetricSaml2Port).close();
        bus.shutdown(true);
    }
    
    // TODO (WSS-484) Uncomment when we upgrade to WSS4J 2.0-SNAPSHOT again
    @org.junit.Test
    @org.junit.Ignore
    public void testUsernameTokenSAML1Encrypted() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML1EncryptedPort");
        DoubleItPortType symmetricSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml1Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml1Port, STSPORT2);
        }

        // DOM
        doubleIt(symmetricSaml1Port, 25);
        
        // TODO Streaming - The encrypted issued token is placed under the ReferenceList
        // and hence an error is thrown on the receiving side
        symmetricSaml1Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml1Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml1Port, STSPORT2);
        }
        SecurityTestUtil.enableStreaming(symmetricSaml1Port);
        // doubleIt(symmetricSaml1Port, 25);

        ((java.io.Closeable)symmetricSaml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUsernameTokenSAML2SecureConversation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML2SecureConversationPort");
        DoubleItPortType symmetricSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml2Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml2Port, STSPORT2);
        }
        
        // DOM
        doubleIt(symmetricSaml2Port, 30);
        
        // Streaming
        symmetricSaml2Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml2Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml2Port, STSPORT2);
        }
        SecurityTestUtil.enableStreaming(symmetricSaml2Port);
        doubleIt(symmetricSaml2Port, 25);
        
        ((java.io.Closeable)symmetricSaml2Port).close();
        bus.shutdown(true);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
}
