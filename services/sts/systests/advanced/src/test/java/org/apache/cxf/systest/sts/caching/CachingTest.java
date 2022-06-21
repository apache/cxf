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
import java.time.Instant;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
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
 * Test various Caching properties relating to the STSClient and also at the validation side.
 */
public class CachingTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            ServerCachingTest.class.getResource("cxf-service.xml")
        )));
        assertTrue(launchServer(new STSServer()));
    }

    @org.junit.Test
    public void testSTSClientCaching() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port).getRequestContext().put("thread.local.request.context", "true");
        updateAddressPort(port, PORT);

        // Make a successful invocation
        doubleIt(port, 25);

        // Change the STSClient so that it can no longer find the STS
        BindingProvider p = (BindingProvider)port;
        clearSTSClient(p, bus);

        // This should succeed as the token is cached
        doubleIt(port, 30);

        // This should fail as the cached token is manually removed
        Client client = ClientProxy.getClient(port);
        Endpoint ep = client.getEndpoint();
        ep.remove(SecurityConstants.TOKEN_ID);
        ep.remove(SecurityConstants.TOKEN);

        try {
            doubleIt(port, 35);
            fail("Expected failure on clearing the cache");
        } catch (SOAPFaultException ex) {
            // Expected
        }

        ((java.io.Closeable)port).close();
    }

    @org.junit.Test
    public void testDisableProxyCaching() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port2");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port).getRequestContext().put("thread.local.request.context", "true");
        updateAddressPort(port, PORT);

        // Disable storing tokens per-proxy
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, "false"
        );

        // Make a successful invocation
        doubleIt(port, 25);

        // Change the STSClient so that it can no longer find the STS
        BindingProvider p = (BindingProvider)port;
        clearSTSClient(p, bus);

        // This should fail as it can't get the token
        try {
            doubleIt(port, 35);
            fail("Expected failure");
        } catch (SOAPFaultException ex) {
            // Expected
        }

        ((java.io.Closeable)port).close();
    }

    @org.junit.Test
    public void testImminentExpiry() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port).getRequestContext().put("thread.local.request.context", "true");
        updateAddressPort(port, PORT);

        // Make a successful invocation
        doubleIt(port, 25);

        Client client = ClientProxy.getClient(port);
        Endpoint ep = client.getEndpoint();
        String id = (String)ep.get(SecurityConstants.TOKEN_ID);
        TokenStore store = (TokenStore)ep.getEndpointInfo().getProperty(TokenStore.class.getName());
        SecurityToken tok = store.getToken(id);
        assertNotNull(tok);

        // Make the token "about to expire"
        tok.setExpires(Instant.now().plusSeconds(5L));
        assertTrue(tok.isAboutToExpire(10L));

        doubleIt(port, 25);

        ((java.io.Closeable)port).close();
    }

    private static void clearSTSClient(BindingProvider p, Bus bus) {
        p.getRequestContext().put(SecurityConstants.STS_CLIENT, new STSClient(bus));
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
