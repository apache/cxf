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
package org.apache.cxf.systest.sts.bearer;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.SAMLParms;

import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * Test the Bearer TokenType over TLS.
 */
public class BearerTest extends AbstractBusClientServerTestBase {
    
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
    public void testSAML2Bearer() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BearerTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = BearerTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2BearerPort");
        DoubleItPortType transportSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)transportSaml2Port, STSPORT);
        }
        
        doubleIt(transportSaml2Port, 45);
    }
    
    @org.junit.Test
    public void testSAML2UnsignedBearer() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BearerTest.class.getResource("cxf-unsigned-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = BearerTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2BearerPort");
        DoubleItPortType transportSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, PORT);
        if (standalone) {
            TokenTestUtils.updateSTSPort((BindingProvider)transportSaml2Port, STSPORT);
        }
        
        //
        // Create a SAML2 Bearer Assertion and add it to the TokenStore so that the
        // IssuedTokenInterceptorProvider does not invoke on the STS
        //
        Client client = ClientProxy.getClient(transportSaml2Port);
        Endpoint ep = client.getEndpoint();
        String id = "1234";
        ep.getEndpointInfo().setProperty(TokenStore.class.getName(), new MemoryTokenStore());
        ep.getEndpointInfo().setProperty(SecurityConstants.TOKEN_ID, id);
        TokenStore store = (TokenStore)ep.getEndpointInfo().getProperty(TokenStore.class.getName());

        SAMLParms samlParms = new SAMLParms();
        samlParms.setCallbackHandler(new Saml2CallbackHandler());
        AssertionWrapper assertion = new AssertionWrapper(samlParms);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Element assertionElement = assertion.toDOM(db.newDocument());
        
        SecurityToken tok = new SecurityToken(id);
        tok.setTokenType(WSConstants.WSS_SAML2_TOKEN_TYPE);
        tok.setToken(assertionElement);
        store.add(tok);
        
        doubleIt(transportSaml2Port, 50);
    }
    
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
}
