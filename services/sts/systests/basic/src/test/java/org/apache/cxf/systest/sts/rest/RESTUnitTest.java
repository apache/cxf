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
package org.apache.cxf.systest.sts.rest;

import java.net.URL;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;
import org.junit.BeforeClass;

/**
 * Some unit tests for the REST interface of the CXF STS.
 */
public class RESTUnitTest extends AbstractBusClientServerTestBase {
    
    private static final String SYMMETRIC_KEY_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/SymmetricKey";
    private static final String PUBLIC_KEY_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    private static final String BEARER_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    private static final String DEFAULT_ADDRESS = 
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";
    
    static final String STSPORT = allocatePort(STSRESTServer.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSRESTServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    
    @org.junit.Test
    public void testIssueSAML2Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml2.0");
        
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // Process the token
        List<WSSecurityEngineResult> results = processToken(assertionDoc.getDocumentElement());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        assertTrue(assertion.isSigned());

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueSAML1Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml1.1");
        
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // Process the token
        List<WSSecurityEngineResult> results = processToken(assertionDoc.getDocumentElement());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
        assertTrue(assertion.isSigned());

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueSymmetricKeySaml1() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml1.1");
        client.query("keyType", SYMMETRIC_KEY_KEYTYPE);
        
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // Process the token
        List<WSSecurityEngineResult> results = processToken(assertionDoc.getDocumentElement());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
        assertTrue(assertion.isSigned());
        
        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && methods.size() > 0) {
            confirmMethod = methods.get(0);
        }
        assertTrue(OpenSAMLUtil.isMethodHolderOfKey(confirmMethod));
        SAMLKeyInfo subjectKeyInfo = assertion.getSubjectKeyInfo();
        assertTrue(subjectKeyInfo.getSecret() != null);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testIssuePublicKeySAML2Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml2.0");
        client.query("keyType", PUBLIC_KEY_KEYTYPE);
        
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // Process the token
        List<WSSecurityEngineResult> results = processToken(assertionDoc.getDocumentElement());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        assertTrue(assertion.isSigned());
        
        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && methods.size() > 0) {
            confirmMethod = methods.get(0);
        }
        assertTrue(OpenSAMLUtil.isMethodHolderOfKey(confirmMethod));
        SAMLKeyInfo subjectKeyInfo = assertion.getSubjectKeyInfo();
        assertTrue(subjectKeyInfo.getCerts() != null);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueBearerSAML1Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml1.1");
        client.query("keyType", BEARER_KEYTYPE);
        
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // Process the token
        List<WSSecurityEngineResult> results = processToken(assertionDoc.getDocumentElement());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
        assertTrue(assertion.isSigned());
        
        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && methods.size() > 0) {
            confirmMethod = methods.get(0);
        }
        assertTrue(confirmMethod.contains("bearer"));

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueSAML2TokenAppliesTo() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml2.0");
        client.query("appliesTo", DEFAULT_ADDRESS);
        
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // Process the token
        List<WSSecurityEngineResult> results = processToken(assertionDoc.getDocumentElement());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        assertTrue(assertion.isSigned());

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueSAML2TokenUnknownAppliesTo() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml2.0");
        client.query("appliesTo", "https://localhost:8081/tripleit/");
        
        Response response = client.get();
        try {
            response.readEntity(Document.class);
            fail("Failure expected on an unknown AppliesTo address");
        } catch (Exception ex) {
            // expected
        }

        bus.shutdown(true);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testIssueJWTToken() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, "alice", "clarinet", busFile.toString());

        client.type("application/json").accept("application/json");
        client.path("jwt");
        
        client.get();
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testIssueSAML2TokenViaWSTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = RESTUnitTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml2.0");
        
        Response response = client.get();
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        RequestedSecurityTokenType requestedSecurityToken = null;
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("RequestedSecurityToken".equals(jaxbElement.getName().getLocalPart())) {
                    requestedSecurityToken = (RequestedSecurityTokenType)jaxbElement.getValue();
                    break;
                }
            }
        }
        assertNotNull(requestedSecurityToken);
        
        // Process the token
        List<WSSecurityEngineResult> results = 
            processToken((Element)requestedSecurityToken.getAny());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        assertTrue(assertion.isSigned());

        bus.shutdown(true);
    }
    
    private List<WSSecurityEngineResult> processToken(Element assertionElement)
        throws Exception {
        RequestData requestData = new RequestData();
        requestData.setDisableBSPEnforcement(true);
        CallbackHandler callbackHandler = new org.apache.cxf.systest.sts.common.CommonCallbackHandler();
        requestData.setCallbackHandler(callbackHandler);
        Crypto crypto = CryptoFactory.getInstance("serviceKeystore.properties");
        requestData.setDecCrypto(crypto);
        requestData.setSigVerCrypto(crypto);
        
        Processor processor = new SAMLTokenProcessor();
        return processor.handleToken(
            assertionElement, requestData, new WSDocInfo(assertionElement.getOwnerDocument())
        );
    }
    
}
