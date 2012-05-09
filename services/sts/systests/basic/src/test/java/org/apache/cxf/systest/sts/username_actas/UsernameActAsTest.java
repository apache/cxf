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
package org.apache.cxf.systest.sts.username_actas;

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
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;


/**
 * In this test case, a CXF client requests a Security Token from an STS, passing a username that
 * it has obtained from an unknown client as an "ActAs" element. This username is obtained
 * by parsing the "ws-security.username" property. The client then invokes on the service 
 * provider using the returned token from the STS. 
 */
public class UsernameActAsTest extends AbstractBusClientServerTestBase {
    
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
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }

    @org.junit.Test
    public void testUsernameActAs() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort");
        DoubleItPortType bearerPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bearerPort, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)bearerPort, STSPORT2);
        }
        
        // Transport port
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        doubleIt(bearerPort, 25);
        
        DoubleItPortType bearerPort2 = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bearerPort2, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)bearerPort2, STSPORT2);
        }
        
        ((BindingProvider)bearerPort2).getRequestContext().put(
            "ws-security.username", "eve"
        );
        // This time we expect a failure as the server validator doesn't accept "eve".
        try {
            doubleIt(bearerPort2, 30);
            fail("Failure expected on an unknown user");
        } catch (Exception ex) {
            // expected
        }
    }
    
    /**
     * Test caching the issued token
     */
    @org.junit.Test
    public void testUsernameActAsCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort");
        
        //
        // Proxy no. 1
        // 
        DoubleItPortType bearerPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bearerPort, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)bearerPort, STSPORT2);
        }
        
        TokenStore tokenStore = new MemoryTokenStore();
        ((BindingProvider)bearerPort).getRequestContext().put(
            TokenStore.class.getName(), tokenStore
        );

        // Make a successful invocation
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        doubleIt(bearerPort, 25);
        
        // Change the STSClient so that it can no longer find the STS
        STSClient stsClient = new STSClient(bus);
        stsClient.setOnBehalfOf(new WSSUsernameCallbackHandler());
        BindingProvider p = (BindingProvider)bearerPort;
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        
        // This invocation should be successful as the token is cached
        doubleIt(bearerPort, 25);
        
        // 
        // Proxy no. 2
        //
        DoubleItPortType bearerPort2 = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bearerPort2, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)bearerPort2, STSPORT2);
        }
        
        // Change the STSClient so that it can no longer find the STS
        stsClient = new STSClient(bus);
        stsClient.setOnBehalfOf(new WSSUsernameCallbackHandler());
        p = (BindingProvider)bearerPort2;
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        
        // This should fail as the cache is not being used
        try {
            doubleIt(bearerPort2, 40);
            fail("Failure expected as the token is not stored in the cache");
        } catch (Exception ex) {
            // expected
        }
        
        // Set the cache correctly
        p.getRequestContext().put(TokenStore.class.getName(), tokenStore);
        
        // Make another invocation - this should succeed as the token is cached
        p.getRequestContext().put("ws-security.username", "alice");
        doubleIt(bearerPort2, 40);
        
        // Reset the cache - this invocation should fail
        p.getRequestContext().put(TokenStore.class.getName(), new MemoryTokenStore());
        try {
            doubleIt(bearerPort2, 40);
            fail("Failure expected as the cache is reset");
        } catch (Exception ex) {
            // expected
        }
    }
    
    /**
     * Test caching the issued token when the STSClient is deployed in an intermediary
     */
    @org.junit.Test
    public void testDifferentUsersCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort");
        
        DoubleItPortType bearerPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bearerPort, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)bearerPort, STSPORT2);
        }
        
        // Disable storing tokens per-proxy
        ((BindingProvider)bearerPort).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );
        
        // Make a successful invocation
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        doubleIt(bearerPort, 25);
        
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "bob"
        );
        doubleIt(bearerPort, 30);
        
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "eve"
        );
        try {
            doubleIt(bearerPort, 30);
            fail("Failure expected on a bad user");
        } catch (Exception ex) {
            //
        }
        
        // Change the STSClient so that it can no longer find the STS
        STSClient stsClient = new STSClient(bus);
        stsClient.setOnBehalfOf(new WSSUsernameCallbackHandler());
        BindingProvider p = (BindingProvider)bearerPort;
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        
        // Make a successful invocation
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        doubleIt(bearerPort, 25);
        
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "bob"
        );
        doubleIt(bearerPort, 30);
        
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "eve2"
        );
        try {
            doubleIt(bearerPort, 30);
            fail("Failure expected on a bad user");
        } catch (Exception ex) {
            //
        }
        
        // Reset the cache - this invocation should fail
        p.getRequestContext().put(TokenStore.class.getName(), new MemoryTokenStore());
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        try {
            doubleIt(bearerPort, 30);
            fail("Failure expected");
        } catch (Exception ex) {
            //
        }
    }
    
    /**
     * Test caching the issued token when the STSClient is deployed in an intermediary
     */
    @org.junit.Test
    public void testAppliesToCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort");
        
        DoubleItPortType bearerPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bearerPort, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)bearerPort, STSPORT2);
        }
        
        // Disable storing tokens per-proxy
        ((BindingProvider)bearerPort).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );
        
        // Make a successful invocation
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        BindingProvider p = (BindingProvider)bearerPort;
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew"
        );
        doubleIt(bearerPort, 25);
        
        // Make a successful invocation
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "bob"
        );
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew2"
        );
        doubleIt(bearerPort, 25);
        
        // Change the STSClient so that it can no longer find the STS
        STSClient stsClient = new STSClient(bus);
        stsClient.setOnBehalfOf(new WSSUsernameCallbackHandler());
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        
        // Make a successful invocation - should work as token is cached
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew"
        );
        doubleIt(bearerPort, 25);
        
        // Make a successful invocation - should work as token is cached
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "bob"
        );
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew2"
        );
        doubleIt(bearerPort, 25);
        
        // Change appliesTo - should fail
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew2"
        );
        try {
            doubleIt(bearerPort, 30);
            fail("Failure expected");
        } catch (Exception ex) {
            //
        }
    }
    
    /**
     * Test caching the issued token when the STSClient is deployed in an intermediary
     */
    @org.junit.Test
    public void testNoAppliesToCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort");
        
        DoubleItPortType bearerPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bearerPort, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)bearerPort, STSPORT2);
        }
        
        // Disable storing tokens per-proxy
        ((BindingProvider)bearerPort).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );
        
        // Make a successful invocation
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "alice"
        );
        // Disable appliesTo
        BindingProvider p = (BindingProvider)bearerPort;
        STSClient stsClient = (STSClient)p.getRequestContext().get(SecurityConstants.STS_CLIENT);
        stsClient.setEnableAppliesTo(false);
        doubleIt(bearerPort, 25);
        
        // Change the STSClient so that it can no longer find the STS
        stsClient = new STSClient(bus);
        stsClient.setOnBehalfOf(new WSSUsernameCallbackHandler());
        stsClient.setEnableAppliesTo(false);
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        
        // This should work
        doubleIt(bearerPort, 25);
        
        // Bob should fail
        ((BindingProvider)bearerPort).getRequestContext().put(
            "ws-security.username", "bob"
        );
        try {
            doubleIt(bearerPort, 30);
            fail("Failure expected");
        } catch (Exception ex) {
            //
        }
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(2 * numToDouble, resp);
    }
}
