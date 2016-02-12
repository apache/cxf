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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * Some tests for the REST interface of the CXF STS.
 */
public class STSRESTTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSRESTServer.class);
    
    private static final String SAML1_TOKEN_TYPE = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    private static final String SAML2_TOKEN_TYPE = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    private static final String SYMMETRIC_KEY_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/SymmetricKey";
    private static final String PUBLIC_KEY_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    private static final String BEARER_KEYTYPE = 
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    private static final String DEFAULT_ADDRESS = 
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";

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
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
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
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
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
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
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
    public void testIssuePublicKeySAML2Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
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
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
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
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
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
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
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
    public void testIssueSAML2TokenClaims() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
        client.path("saml2.0");
        
        // First check that the role isn't usually in the generated token
        
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
        
        ClaimCollection claims = SAMLUtils.getClaims(assertion);
        assertEquals(1, claims.size());
        Claim claim = claims.get(0);
        String role = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        assertNotEquals(claim.getClaimType().toString(), role);
        
        // Now get another token specifying the role
        client.query("claim", role);
        response = client.get();
        assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // Process the token
        results = processToken(assertionDoc.getDocumentElement());

        assertTrue(results != null && results.size() == 1);
        assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        assertTrue(assertion.isSigned());
        
        claims = SAMLUtils.getClaims(assertion);
        assertEquals(1, claims.size());
        claim = claims.get(0);
        assertEquals(claim.getClaimType().toString(), role);
        assertEquals("ordinary-user", claim.getValues().get(0));
        
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testIssueSAML2TokenViaWSTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
        client.path("saml2.0");
        client.query("wstrustResponse", "true");
        
        Response response = client.get();
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        validateSAMLSecurityTokenResponse(securityResponse, true);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueSAML2TokenViaPOST() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        
        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Issue");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        Response response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        validateSAMLSecurityTokenResponse(securityResponse, true);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testExplicitlyIssueSAML2TokenViaPOST() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.query("action", "issue");
        
        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Issue");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        Response response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        validateSAMLSecurityTokenResponse(securityResponse, true);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testExplicitlyIssueSAML1TokenViaPOST() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.query("action", "issue");
        
        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Issue");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(SAML1_TOKEN_TYPE);
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        Response response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        validateSAMLSecurityTokenResponse(securityResponse, false);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testValidateSAML2Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.path("saml2.0");
        
        // 1. Get a token via GET
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // 2. Now validate it in the STS using POST
        client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.query("action", "validate");
        
        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Validate");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "TokenType", namespace);
        String tokenType = namespace + "/RSTR/Status";
        writer.writeCharacters(tokenType);
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "ValidateTarget", namespace);
        StaxUtils.copy(assertionDoc.getDocumentElement(), writer);
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        StatusType status = null;
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("Status".equals(jaxbElement.getName().getLocalPart())) {
                    status = (StatusType)jaxbElement.getValue();
                    break;
                }
            }
        }
        assertNotNull(status);
        
        // Check the token was valid
        String validCode = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/valid";
        assertEquals(validCode, status.getCode());

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testRenewSAML2Token() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.query("action", "issue");
        
        // 1. Get a token via POST
        
        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Issue");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        Response response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        Element token = validateSAMLSecurityTokenResponse(securityResponse, true);
        
        // 2. Now validate it in the STS using POST
        client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.query("action", "renew");
        
        // Create RequestSecurityToken
        writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Renew");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "RenewTarget", namespace);
        StaxUtils.copy(token, writer);
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        securityResponse = response.readEntity(RequestSecurityTokenResponseType.class);
        
        validateSAMLSecurityTokenResponse(securityResponse, true);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueSAML2TokenPlain() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("text/plain");
        client.path("saml2.0");
        
        Response response = client.get();
        String encodedAssertion = response.readEntity(String.class);
        assertNotNull(encodedAssertion);
        
        byte[] deflatedToken = Base64Utility.decode(encodedAssertion);
        InputStream inputStream = CompressionUtils.inflate(deflatedToken);
        Document doc = 
            StaxUtils.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        
        // Process the token
        List<WSSecurityEngineResult> results = processToken(doc.getDocumentElement());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        assertTrue(assertion.isSigned());

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueJWTTokenPlain() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("text/plain");
        client.path("jwt");
        
        Response response = client.get();
        String token = response.readEntity(String.class);
        assertNotNull(token);
        
        validateJWTToken(token, null);
    }
    
    @org.junit.Test
    public void testIssueJWTTokenAppliesTo() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("text/plain");
        client.path("jwt");
        client.query("appliesTo", DEFAULT_ADDRESS);
        
        Response response = client.get();
        String token = response.readEntity(String.class);
        assertNotNull(token);
        
        validateJWTToken(token, DEFAULT_ADDRESS);
    }
    
    @org.junit.Test
    public void testIssueJWTTokenClaims() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("text/plain");
        client.path("jwt");
        
        // First check that the role isn't usually in the generated token
        
        Response response = client.get();
        String token = response.readEntity(String.class);
        assertNotNull(token);
        
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        
        String role = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        assertTrue(jwt.getClaim(role) == null);
        
        // Now get another token specifying the role
        client.query("claim", role);
        
        response = client.get();
        token = response.readEntity(String.class);
        assertNotNull(token);
        
        // Process the token
        validateJWTToken(token, null);
        
        jwtConsumer = new JwsJwtCompactConsumer(token);
        jwt = jwtConsumer.getJwtToken();
        assertEquals("ordinary-user", jwt.getClaim(role));
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueJWTTokenViaPOST() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        
        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Issue");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(JWT_TOKEN_TYPE);
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        Response response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        RequestedSecurityTokenType requestedSecurityToken = getRequestedSecurityToken(securityResponse);
        assertNotNull(requestedSecurityToken);
        
        String token = ((Element)requestedSecurityToken.getAny()).getTextContent();
        assertNotNull(token);
        
        validateJWTToken(token, null);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testValidateSAMLAndIssueJWT() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
        client.path("saml2.0");
        
        // 1. Get a token via GET
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // 2. Now validate it in the STS using POST
        client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.query("action", "validate");
        
        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Validate");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(JWT_TOKEN_TYPE);
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "ValidateTarget", namespace);
        StaxUtils.copy(assertionDoc.getDocumentElement(), writer);
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        StatusType status = null;
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("Status".equals(jaxbElement.getName().getLocalPart())) {
                    status = (StatusType)jaxbElement.getValue();
                    break;
                }
            }
        }
        assertNotNull(status);
        
        // Check the token was valid
        String validCode = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/valid";
        assertEquals(validCode, status.getCode());
        
        // Check the token
        RequestedSecurityTokenType requestedSecurityToken = getRequestedSecurityToken(securityResponse);
        assertNotNull(requestedSecurityToken);
        
        String token = ((Element)requestedSecurityToken.getAny()).getTextContent();
        assertNotNull(token);
        
        validateJWTToken(token, null);

        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testValidateJWTAndIssueSAML() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("text/plain");
        client.path("jwt");
        
        // 1. Get a token via GET
        Response response = client.get();
        String token = response.readEntity(String.class);
        assertNotNull(token);
        
        // 2. Now validate it in the STS using POST
        client = WebClient.create(address, busFile.toString());

        client.type("application/xml").accept("application/xml");
        client.query("action", "validate");
        
        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Validate");
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();
        
        writer.writeStartElement("wst", "ValidateTarget", namespace);
        writer.writeStartElement(null, "TokenWrapper", null);
        writer.writeCharacters(token);
        writer.writeEndElement();
        writer.writeEndElement();
        
        writer.writeEndElement();
        
        response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));
        
        RequestSecurityTokenResponseType securityResponse = 
            response.readEntity(RequestSecurityTokenResponseType.class);
        
        StatusType status = null;
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("Status".equals(jaxbElement.getName().getLocalPart())) {
                    status = (StatusType)jaxbElement.getValue();
                    break;
                }
            }
        }
        assertNotNull(status);
        
        // Check the token was valid
        String validCode = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/valid";
        assertEquals(validCode, status.getCode());
        
        // Check the token
        validateSAMLSecurityTokenResponse(securityResponse, true);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssueJWTTokenXMLWrapper() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/xml");
        client.path("jwt");
        
        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
        
        // Discard XML wrapper
        validateJWTToken(assertionDoc.getDocumentElement().getFirstChild().getTextContent(), null);
    }
    
    @org.junit.Test
    public void testIssueJWTTokenJSONWrapper() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/json");
        client.path("jwt");
        
        client.get();
    }
    
    @org.junit.Test
    public void testDefaultSAMLFormat() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("*");
        client.path("saml");
        
        Response response = client.get();
        // It should be XML
        Document doc = response.readEntity(Document.class);
        assertNotNull(doc);
    }
    
    @org.junit.Test
    public void testDefaultJWTFormat() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("*");
        client.path("jwt");
        
        Response response = client.get();
        // It should be XML
        Document doc = response.readEntity(Document.class);
        assertNotNull(doc);
    }
    
    @org.junit.Test
    public void testIssueSAMLTokenWithWrongAcceptType() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSRESTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "https://localhost:" + STSPORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("application/json");
        client.path("saml2.0");
        
        Response response = client.get();
        try {
            response.readEntity(Document.class);
            fail("Failure expected on an bad accept type");
        } catch (Exception ex) {
            // expected
        }

        bus.shutdown(true);
    }
    
    private Element validateSAMLSecurityTokenResponse(
        RequestSecurityTokenResponseType securityResponse, boolean saml2
    ) throws Exception {
        RequestedSecurityTokenType requestedSecurityToken = getRequestedSecurityToken(securityResponse);
        assertNotNull(requestedSecurityToken);
        
        // Process the token
        List<WSSecurityEngineResult> results = 
            processToken((Element)requestedSecurityToken.getAny());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion = 
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(assertion != null);
        if (saml2) {
            assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        } else {
            assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
        }
        assertTrue(assertion.isSigned());
        
        return (Element)results.get(0).get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
    }
    
    private RequestedSecurityTokenType getRequestedSecurityToken(RequestSecurityTokenResponseType securityResponse) {
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("RequestedSecurityToken".equals(jaxbElement.getName().getLocalPart())) {
                    return (RequestedSecurityTokenType)jaxbElement.getValue();
                }
            }
        }
        return null;
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
    
    private void validateJWTToken(String token, String audience) 
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        
        // Validate claims
        Assert.assertEquals("DoubleItSTSIssuer", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
        if (audience != null) {
            @SuppressWarnings("unchecked")
            List<String> audiences = (List<String>)jwt.getClaim(JwtConstants.CLAIM_AUDIENCE);
            assertEquals(1, audiences.size());
            Assert.assertEquals(audience, audiences.get(0));
        }
        Assert.assertNotNull(jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        Assert.assertNotNull(jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(Loader.getResource("servicestore.jks").openStream(), "sspass".toCharArray());
        Certificate cert = keystore.getCertificate("mystskey");
        Assert.assertNotNull(cert);
        
        Assert.assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert, 
                                                          SignatureAlgorithm.RS256));
    }
    
}
