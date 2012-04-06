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
package org.apache.cxf.systest.sts.asymmetric;

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
 * Test the Asymmetric binding. The CXF client gets a token from the STS by authenticating via a
 * Username Token over the symmetric binding, and then sends it to the CXF endpoint using 
 * the asymmetric binding.
 */
public class AsymmetricBindingTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = allocatePort(Server.class);
    
    private static boolean standalone;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, true)
        );
        String deployment = System.getProperty("sts.deployment");
        if ("standalone".equals(deployment)) {
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
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }

    @org.junit.Test
    public void testUsernameTokenSAML1() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AsymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = AsymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML1Port");
        DoubleItPortType asymmetricSaml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(asymmetricSaml1Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)asymmetricSaml1Port, STSPORT2);
        }
        
        doubleIt(asymmetricSaml1Port, 25);
    }

    @org.junit.Test
    public void testUsernameTokenSAML2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AsymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = AsymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2Port");
        DoubleItPortType asymmetricSaml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(asymmetricSaml2Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)asymmetricSaml2Port, STSPORT2);
        }
        
        doubleIt(asymmetricSaml2Port, 30);

        TokenTestUtils.verifyToken(asymmetricSaml2Port);
    }

    @org.junit.Test
    public void testUsernameTokenSAML1Encrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AsymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = AsymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML1EncryptedPort");
        DoubleItPortType asymmetricSaml1EncryptedPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(asymmetricSaml1EncryptedPort, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)asymmetricSaml1EncryptedPort, STSPORT2);
        }
        
        doubleIt(asymmetricSaml1EncryptedPort, 40);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
}
