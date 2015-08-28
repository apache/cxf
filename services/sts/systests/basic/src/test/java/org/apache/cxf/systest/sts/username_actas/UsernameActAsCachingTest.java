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
import org.apache.cxf.BusException;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;


/**
 * In this test case, a CXF client requests a Security Token from an STS, passing a username that
 * it has obtained from an unknown client as an "ActAs" element. This username is obtained
 * by parsing the "security.username" property. The client then invokes on the service 
 * provider using the returned token from the STS.
 */
public class UsernameActAsCachingTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(Server.class);
    
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
                   launchServer(STSServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    /**
     * Test caching the issued token
     */
    @org.junit.Test
    public void testUsernameActAsCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsCachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort2");
        
        //
        // Proxy no. 1
        // 
        DoubleItPortType port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        TokenTestUtils.updateSTSPort((BindingProvider)port, STSPORT2);
        
        TokenStore tokenStore = new MemoryTokenStore();
        ((BindingProvider)port).getRequestContext().put(
            TokenStore.class.getName(), tokenStore
        );

        // Make a successful invocation
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "alice"
        );
        doubleIt(port, 25);
        
        // Change the STSClient so that it can no longer find the STS
        BindingProvider p = (BindingProvider)port;
        clearSTSClient(p);
        
        // This invocation should be successful as the token is cached
        doubleIt(port, 25);
        
        // 
        // Proxy no. 2
        //
        DoubleItPortType port2 = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port2, PORT);
        
        TokenTestUtils.updateSTSPort((BindingProvider)port2, STSPORT2);
        
        // Change the STSClient so that it can no longer find the STS
        p = (BindingProvider)port2;
        clearSTSClient(p);
        
        // This should fail as the cache is not being used
        try {
            doubleIt(port2, 40);
            fail("Failure expected as the token is not stored in the cache");
        } catch (Exception ex) {
            // expected
        }
        
        // Set the cache correctly
        p.getRequestContext().put(TokenStore.class.getName(), tokenStore);
        
        // Make another invocation - this should succeed as the token is cached
        p.getRequestContext().put("security.username", "alice");
        doubleIt(port2, 40);
        
        // Reset the cache - this invocation should fail
        p.getRequestContext().put(TokenStore.class.getName(), new MemoryTokenStore());
        p.getRequestContext().put(SecurityConstants.TOKEN, new SecurityToken());
        try {
            doubleIt(port2, 40);
            fail("Failure expected as the cache is reset");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    /**
     * Test caching the issued token when the STSClient is deployed in an intermediary
     */
    @org.junit.Test
    public void testDifferentUsersCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsCachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort3");
        
        DoubleItPortType port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        TokenTestUtils.updateSTSPort((BindingProvider)port, STSPORT2);
        
        // Disable storing tokens per-proxy
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );
        
        // Make a successful invocation
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "alice"
        );
        doubleIt(port, 25);
        
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "bob"
        );
        doubleIt(port, 30);
        
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "eve"
        );
        try {
            doubleIt(port, 30);
            fail("Failure expected on a bad user");
        } catch (Exception ex) {
            //
        }
        
        // Change the STSClient so that it can no longer find the STS
        BindingProvider p = (BindingProvider)port;
        clearSTSClient(p);
        
        // Make a successful invocation
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "alice"
        );
        doubleIt(port, 25);
        
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "bob"
        );
        doubleIt(port, 30);
        
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "eve2"
        );
        try {
            doubleIt(port, 30);
            fail("Failure expected on a bad user");
        } catch (Exception ex) {
            //
        }
        
        // Reset the cache - this invocation should fail
        p.getRequestContext().put(TokenStore.class.getName(), new MemoryTokenStore());
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "alice"
        );
        try {
            doubleIt(port, 30);
            fail("Failure expected");
        } catch (Exception ex) {
            //
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    /**
     * Test caching the issued token when the STSClient is deployed in an intermediary
     */
    @org.junit.Test
    public void testAppliesToCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsCachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort4");
        
        DoubleItPortType port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        TokenTestUtils.updateSTSPort((BindingProvider)port, STSPORT2);
        
        // Disable storing tokens per-proxy
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );
        
        // Make a successful invocation
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "alice"
        );
        BindingProvider p = (BindingProvider)port;
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew"
        );
        doubleIt(port, 25);
        
        // Make a successful invocation
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "bob"
        );
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew2"
        );
        doubleIt(port, 25);
        
        // Change the STSClient so that it can no longer find the STS
        clearSTSClient(p);
        
        // Make a successful invocation - should work as token is cached
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "alice"
        );
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew"
        );
        doubleIt(port, 25);
        
        // Make a successful invocation - should work as token is cached
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "bob"
        );
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew2"
        );
        doubleIt(port, 25);
        
        // Change appliesTo - should fail
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "alice"
        );
        p.getRequestContext().put(
            SecurityConstants.STS_APPLIES_TO, 
            "http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricnew2"
        );
        try {
            doubleIt(port, 30);
            fail("Failure expected");
        } catch (Exception ex) {
            //
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    /**
     * Test caching the issued token when the STSClient is deployed in an intermediary
     */
    @org.junit.Test
    public void testNoAppliesToCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameActAsCachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameActAsCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort5");
        
        DoubleItPortType port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        TokenTestUtils.updateSTSPort((BindingProvider)port, STSPORT2);
        
        // Disable storing tokens per-proxy
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );
        
        // Make a successful invocation
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "alice"
        );
        // Disable appliesTo
        BindingProvider p = (BindingProvider)port;
        STSClient stsClient = (STSClient)p.getRequestContext().get(SecurityConstants.STS_CLIENT);
        if (stsClient == null) {
            stsClient = (STSClient)p.getRequestContext().get("ws-" + SecurityConstants.STS_CLIENT);
        }
        stsClient.setEnableAppliesTo(false);
        doubleIt(port, 25);
        
        // Change the STSClient so that it can no longer find the STS
        clearSTSClient(p);
        
        // This should work
        doubleIt(port, 25);
        
        // Bob should fail
        ((BindingProvider)port).getRequestContext().put(
            "security.username", "bob"
        );
        try {
            doubleIt(port, 30);
            fail("Failure expected");
        } catch (Exception ex) {
            //
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    private void clearSTSClient(BindingProvider p) throws BusException, EndpointException {
        STSClient stsClient = (STSClient)p.getRequestContext().get(SecurityConstants.STS_CLIENT);
        if (stsClient == null) {
            stsClient = (STSClient)p.getRequestContext().get("ws-" + SecurityConstants.STS_CLIENT);
        }
        stsClient.getClient().destroy();
        stsClient.setWsdlLocation(null);
        stsClient.setLocation(null);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(2 * numToDouble, resp);
    }
}
