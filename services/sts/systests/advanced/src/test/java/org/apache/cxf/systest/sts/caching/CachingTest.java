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
package org.apache.cxf.systest.sts.caching;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;

import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * Test various Caching properties relating to the STSClient and also at the validation side.
 */
public class CachingTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String SAML1_TOKEN_TYPE = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    private static final String PUBLIC_KEY_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    
    private static final String PORT = allocatePort(Server.class);
    private static final String PORT2 = allocatePort(Server.class, 2);
    
    private static final String DEFAULT_ADDRESS = 
        "https://localhost:" + PORT + "/doubleit/services/doubleittransportsaml1alternative";

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
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }

    @org.junit.Test
    public void testSTSClientCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, PORT);
        
        // Make a successful invocation
        doubleIt(transportSaml1Port, 25);
        
        // Change the STSClient so that it can no longer find the STS
        BindingProvider p = (BindingProvider)transportSaml1Port;
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, new STSClient(bus));
        
        // This should succeed as the token is cached
        doubleIt(transportSaml1Port, 30);
        
        // This should fail as the cached token is manually removed
        Client client = ClientProxy.getClient(transportSaml1Port);
        Endpoint ep = client.getEndpoint();
        ep.remove(SecurityConstants.TOKEN_ID);

        try {
            doubleIt(transportSaml1Port, 35);
            fail("Expected failure on clearing the cache");
        } catch (SOAPFaultException ex) {
            // Expected
        }
    }
    
    @org.junit.Test
    public void testDisableProxyCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, PORT);
        
        // Disable storing tokens per-proxy
        ((BindingProvider)transportSaml1Port).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );
        
        // Make a successful invocation
        doubleIt(transportSaml1Port, 25);
        
        // Change the STSClient so that it can no longer find the STS
        BindingProvider p = (BindingProvider)transportSaml1Port;
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, new STSClient(bus));
        
        // This should fail as it can't get the token
        try {
            doubleIt(transportSaml1Port, 35);
            fail("Expected failure");
        } catch (SOAPFaultException ex) {
            // Expected
        }
    }
    
    @org.junit.Test
    public void testServerSideSAMLTokenCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1AlternativePort");
        DoubleItPortType transportSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, PORT);
        
        // Make an initial successful invocation
        doubleIt(transportSaml1Port, 25);
        
        // Store the SAML Assertion that was obtained from the STS
        Client client = ClientProxy.getClient(transportSaml1Port);
        Endpoint ep = client.getEndpoint();
        String id = (String)ep.get(SecurityConstants.TOKEN_ID);
        TokenStore store = (TokenStore)ep.getEndpointInfo().getProperty(TokenStore.class.getName());
        SecurityToken tok = store.getToken(id);
        assertNotNull(tok);
        Element storedToken = tok.getToken();
        
        // Get another security token by invoking on the STS directly and save it on the client port
        SecurityToken token = 
            requestSecurityToken(SAML1_TOKEN_TYPE, PUBLIC_KEY_KEYTYPE, bus, DEFAULT_ADDRESS);
        assertNotNull(token);
        tok.setToken(token.getToken());
        
        // Try another invocation - this will fail as the STSClient on the server side is disabled
        // after the first invocation
        try {
            doubleIt(transportSaml1Port, 30);
            fail("Failure expected as the STSClient on the server side is null");
        } catch (Throwable ex) {
            // expected
        }
        // Try again using the original SAML token - this should work as it should be cached by the service
        tok.setToken(storedToken);
        doubleIt(transportSaml1Port, 35);
    }
    
    @org.junit.Test
    public void testServerSideUsernameTokenCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportUTPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, PORT);
        
        // Make an initial successful invocation
        doubleIt(transportUTPort, 25);
        
        BindingProvider p = (BindingProvider)transportUTPort;
        try {
            // The STSClient on the server side is disabled after the first invocation
            p.getRequestContext().put(SecurityConstants.USERNAME, "bob");
            doubleIt(transportUTPort, 30);
            fail("Failure expected as the STSClient on the server side is null");
        } catch (Exception ex) {
            // expected
        }
        
        // This will pass as the UsernameToken should be cached
        p.getRequestContext().put(SecurityConstants.USERNAME, "alice");
        doubleIt(transportUTPort, 35);
    }
    
    @org.junit.Test
    public void testServerSideBinarySecurityTokenCaching() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricBSTPort");
        DoubleItPortType bstPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bstPort, PORT2);
        
        // Make an initial successful invocation
        doubleIt(bstPort, 25);
        
        BindingProvider p = (BindingProvider)bstPort;
        try {
            // The STSClient on the server side is disabled after the first invocation
            p.getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "myservicekey");
            p.getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES, "serviceKeystore.properties");
            doubleIt(bstPort, 30);
            fail("Failure expected as the STSClient on the server side is null");
        } catch (Exception ex) {
            // expected
        }
        
        // This will pass as the BinarySecurityToken should be cached
        p.getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "myclientkey");
        p.getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES, "clientKeystore.properties");
        doubleIt(bstPort, 35);
    }
    
    private SecurityToken requestSecurityToken(
        String tokenType, 
        String keyType,
        Bus bus,
        String endpointAddress
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER, 
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );

        if (PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            properties.put(SecurityConstants.STS_TOKEN_USERNAME, "myservicekey");
            properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "serviceKeystore.properties");
            stsClient.setUseCertificateForConfirmationKeyInfo(true);
        }

        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setKeyType(keyType);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");

        return stsClient.requestSecurityToken(endpointAddress);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
}
