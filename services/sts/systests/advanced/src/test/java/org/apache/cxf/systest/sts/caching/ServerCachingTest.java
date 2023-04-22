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

import org.w3c.dom.Element;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test various server Caching properties
 */
public class ServerCachingTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String SAML1_TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    private static final String PUBLIC_KEY_KEYTYPE =
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";

    private static final String PORT = allocatePort(DoubleItServer.class);
    private static final String PORT2 = allocatePort(DoubleItServer.class, 2);

    private static final String DEFAULT_ADDRESS =
        "https://localhost:" + PORT + "/doubleit/services/doubleittransportsaml1alternative";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            ServerCachingTest.class.getResource("cxf-service.xml")
        )));
        assertTrue(launchServer(new DoubleItServer(
            ServerCachingTest.class.getResource("cxf-caching-service.xml")
        )));

        assertTrue(launchServer(new STSServer()));
    }

    @org.junit.Test
    public void testServerSideSAMLTokenCaching() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ServerCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1AlternativePort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port).getRequestContext().put("thread.local.request.context", "true");
        updateAddressPort(port, PORT);

        // Make an initial successful invocation
        doubleIt(port, 25);

        // Store the SAML Assertion that was obtained from the STS
        Client client = ClientProxy.getClient(port);
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
            doubleIt(port, 30);
            fail("Failure expected as the STSClient on the server side is null");
        } catch (Exception ex) {
            // expected
        }
        // Try again using the original SAML token - this should work as it should be cached by the service
        tok.setToken(storedToken);
        doubleIt(port, 35);

        ((java.io.Closeable)port).close();
    }

    @org.junit.Test
    public void testServerSideUsernameTokenCaching() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ServerCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportUTPort =
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)transportUTPort).getRequestContext().put("thread.local.request.context", "true");
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

        ((java.io.Closeable)transportUTPort).close();
    }

    @org.junit.Test
    public void testServerSideBinarySecurityTokenCaching() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ServerCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricBSTPort");
        DoubleItPortType bstPort =
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)bstPort).getRequestContext().put("thread.local.request.context", "true");
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

        ((java.io.Closeable)bstPort).close();
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

        Map<String, Object> properties = new HashMap<>();
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
        assertEquals(numToDouble * 2L, resp);
    }
}
