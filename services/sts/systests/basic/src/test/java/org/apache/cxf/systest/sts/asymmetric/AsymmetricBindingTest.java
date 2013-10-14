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
import java.security.cert.X509Certificate;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;


import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;

import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * Test the Asymmetric binding. The CXF client gets a token from the STS by authenticating via a
 * Username Token over the symmetric binding, and then sends it to the CXF endpoint using 
 * the asymmetric binding.
 * 
 * It tests both DOM + StAX clients against the DOM server
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
        
        // DOM
        doubleIt(asymmetricSaml1Port, 25);
        
        // Streaming
        asymmetricSaml1Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(asymmetricSaml1Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)asymmetricSaml1Port, STSPORT2);
        }
        SecurityTestUtil.enableStreaming(asymmetricSaml1Port);
        doubleIt(asymmetricSaml1Port, 25);
        
        ((java.io.Closeable)asymmetricSaml1Port).close();
        bus.shutdown(true);
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
        
        // DOM
        doubleIt(asymmetricSaml2Port, 30);
        TokenTestUtils.verifyToken(asymmetricSaml2Port);
        
        // Streaming
        asymmetricSaml2Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(asymmetricSaml2Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)asymmetricSaml2Port, STSPORT2);
        }
        SecurityTestUtil.enableStreaming(asymmetricSaml2Port);
        doubleIt(asymmetricSaml2Port, 25);
        
        ((java.io.Closeable)asymmetricSaml2Port).close();
        bus.shutdown(true);
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
        
        // Set the X509Certificate manually on the STSClient (just to test that we can)
        BindingProvider bindingProvider = (BindingProvider)asymmetricSaml1EncryptedPort;
        STSClient stsClient = 
            (STSClient)bindingProvider.getRequestContext().get(SecurityConstants.STS_CLIENT);
        Crypto crypto = CryptoFactory.getInstance("clientKeystore.properties");
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myclientkey");
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        stsClient.setUseKeyCertificate(certs[0]);
        
        doubleIt(asymmetricSaml1EncryptedPort, 40);
        
        // TODO Streaming - The encrypted issued token is placed under the Signature
        // and hence an error is thrown on the receiving side
        asymmetricSaml1EncryptedPort = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(asymmetricSaml1EncryptedPort, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)asymmetricSaml1EncryptedPort, STSPORT2);
        }
        bindingProvider = (BindingProvider)asymmetricSaml1EncryptedPort;
        stsClient = 
            (STSClient)bindingProvider.getRequestContext().get(SecurityConstants.STS_CLIENT);
        stsClient.setUseKeyCertificate(certs[0]);
        
        SecurityTestUtil.enableStreaming(asymmetricSaml1EncryptedPort);
        // doubleIt(asymmetricSaml1EncryptedPort, 25);
        
        ((java.io.Closeable)asymmetricSaml1EncryptedPort).close();
        bus.shutdown(true);
    }
  
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
}
