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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.systest.sts.TLSClientParametersUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.apache.cxf.ws.security.trust.STSUtils.WST_NS_05_12;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    private static final String DEFAULT_ADDRESS =
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";

    private static Crypto serviceCrypto;

    private WebClient webClient;

    @org.junit.BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSRESTServer.class, true)
        );

        serviceCrypto = CryptoFactory.getInstance("serviceKeystore.properties");
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();

        serviceCrypto = null;
    }

    @org.junit.After
    public void closeClient() throws Exception {
        if (null != webClient) {
            webClient.close();
        }
    }

    @org.junit.Test
    public void testIssueSAML2Token() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }

    @org.junit.Test
    public void testIssueSAML1Token() throws Exception {
        WebClient client = webClient()
            .path("saml1.1")
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
    }

    @org.junit.Test
    public void testIssueSymmetricKeySaml1() throws Exception {
        WebClient client = webClient()
            .path("saml1.1")
            .query("keyType", STSConstants.SYMMETRIC_KEY_KEYTYPE)
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(OpenSAMLUtil.isMethodHolderOfKey(confirmMethod));
        SAMLKeyInfo subjectKeyInfo = assertion.getSubjectKeyInfo();
        assertNotNull(subjectKeyInfo.getSecret());
    }

    @org.junit.Test
    public void testIssueSymmetricKeySaml1ShortKeyType() throws Exception {
        WebClient client = webClient()
            .path("saml1.1")
            .query("keyType", "SymmetricKey")
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(OpenSAMLUtil.isMethodHolderOfKey(confirmMethod));
        SAMLKeyInfo subjectKeyInfo = assertion.getSubjectKeyInfo();
        assertNotNull(subjectKeyInfo.getSecret());
    }

    @org.junit.Test
    public void testIssuePublicKeySAML2Token() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .query("keyType", STSConstants.PUBLIC_KEY_KEYTYPE)
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(OpenSAMLUtil.isMethodHolderOfKey(confirmMethod));
        SAMLKeyInfo subjectKeyInfo = assertion.getSubjectKeyInfo();
        assertNotNull(subjectKeyInfo.getCerts());
    }

    @org.junit.Test
    public void testIssuePublicKeySAML2TokenShortKeyType() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .query("keyType", "PublicKey")
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(OpenSAMLUtil.isMethodHolderOfKey(confirmMethod));
        SAMLKeyInfo subjectKeyInfo = assertion.getSubjectKeyInfo();
        assertNotNull(subjectKeyInfo.getCerts());
    }

    @org.junit.Test
    public void testIssueBearerSAML1Token() throws Exception {
        WebClient client = webClient()
            .path("saml1.1")
            .query("keyType", STSConstants.BEARER_KEY_KEYTYPE)
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertNotNull(assertion);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(confirmMethod.contains("bearer"));
    }

    @org.junit.Test
    public void testIssueBearerSAML1TokenShorKeyType() throws Exception {
        WebClient client = webClient()
            .path("saml1.1")
            .query("keyType", "Bearer")
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(confirmMethod.contains("bearer"));
    }

    @org.junit.Test
    public void testIssueSAML2TokenAppliesTo() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .query("appliesTo", DEFAULT_ADDRESS)
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }

    @org.junit.Test
    public void testIssueSAML2TokenUnknownAppliesTo() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .query("appliesTo", "https://localhost:8081/tripleit/")
            .accept(MediaType.APPLICATION_XML);

        Response response = client.get();
        try {
            response.readEntity(Document.class);
            fail("Failure expected on an unknown AppliesTo address");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testIssueSAML2TokenClaims() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .accept(MediaType.APPLICATION_XML);

        // First check that the role isn't usually in the generated token
        Document assertionDoc = client.get(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);

        ClaimCollection claims = SAMLUtils.getClaims(assertion);
        assertEquals(1, claims.size());
        Claim claim = claims.get(0);
        String role = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        assertNotEquals(role, claim.getClaimType());

        // Now get another token specifying the role
        client.query("claim", role);
        assertionDoc = client.get(Document.class);

        assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);

        claims = SAMLUtils.getClaims(assertion);
        assertEquals(1, claims.size());
        claim = claims.get(0);
        assertEquals(role, claim.getClaimType());
        assertEquals("ordinary-user", claim.getValues().get(0));
    }

    @org.junit.Test
    public void testIssueSAML2TokenViaWSTrust() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .query("wstrustResponse", "true")
            .accept(MediaType.APPLICATION_XML);

        RequestSecurityTokenResponseType securityResponse =
            client.get(RequestSecurityTokenResponseType.class);

        validateSAMLSecurityTokenResponse(securityResponse, true);
    }

    @org.junit.Test
    public void testIssueSAML2TokenViaPOST() throws Exception {
        WebClient client = webClient()
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Issue");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", WST_NS_05_12);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();

        writer.writeEndElement();

        RequestSecurityTokenResponseType securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        validateSAMLSecurityTokenResponse(securityResponse, true);
    }

    @org.junit.Test
    public void testExplicitlyIssueSAML2TokenViaPOST() throws Exception {
        WebClient client = webClient()
            .query("action", "issue")
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Issue");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", WST_NS_05_12);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();

        writer.writeEndElement();

        RequestSecurityTokenResponseType securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        validateSAMLSecurityTokenResponse(securityResponse, true);
    }

    @org.junit.Test
    public void testExplicitlyIssueSAML1TokenViaPOST() throws Exception {
        WebClient client = webClient()
            .query("action", "issue")
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Issue");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", WST_NS_05_12);
        writer.writeCharacters(SAML1_TOKEN_TYPE);
        writer.writeEndElement();

        writer.writeEndElement();

        RequestSecurityTokenResponseType securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        validateSAMLSecurityTokenResponse(securityResponse, false);
    }

    @org.junit.Test
    public void testValidateSAML2Token() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .accept(MediaType.APPLICATION_XML);

        // 1. Get a token via GET
        Document assertionDoc = client.get(Document.class);
        assertNotNull(assertionDoc);

        // 2. Now validate it in the STS using POST
        client = webClient()
            .query("action", "validate")
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Validate");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", WST_NS_05_12);
        String tokenType = WST_NS_05_12 + "/RSTR/Status";
        writer.writeCharacters(tokenType);
        writer.writeEndElement();

        writer.writeStartElement("wst", "ValidateTarget", WST_NS_05_12);
        StaxUtils.copy(assertionDoc.getDocumentElement(), writer);
        writer.writeEndElement();

        writer.writeEndElement();

        RequestSecurityTokenResponseType securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        assertTrue(getValidationStatus(securityResponse));
    }

    @org.junit.Test
    public void testRenewSAML2Token() throws Exception {
        WebClient client = webClient()
            .query("action", "issue")
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // 1. Get a token via POST

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Issue");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", WST_NS_05_12);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();

        writer.writeEndElement();

        RequestSecurityTokenResponseType securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        Element token = validateSAMLSecurityTokenResponse(securityResponse, true);

        // 2. Now renew it using POST
        client = webClient()
            .query("action", "renew")
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // Create RequestSecurityToken
        writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Renew");
        writer.writeEndElement();

        writer.writeStartElement("wst", "RenewTarget", WST_NS_05_12);
        StaxUtils.copy(token, writer);
        writer.writeEndElement();

        writer.writeEndElement();

        securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        validateSAMLSecurityTokenResponse(securityResponse, true);
    }

    @org.junit.Test
    public void testIssueSAML2TokenPlain() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .accept(MediaType.TEXT_PLAIN);

        String encodedAssertion = client.get(String.class);
        assertNotNull(encodedAssertion);

        byte[] deflatedToken = Base64Utility.decode(encodedAssertion);
        InputStream inputStream = CompressionUtils.inflate(deflatedToken);
        Document doc =
            StaxUtils.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Process the token
        SamlAssertionWrapper assertion = validateSAMLToken(doc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }

    @org.junit.Test
    public void testIssueJWTTokenPlain() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.TEXT_PLAIN);

        String token = client.get(String.class);
        validateJWTToken(token);
    }

    @org.junit.Test
    public void testIssueJWTTokenAppliesTo() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .query("appliesTo", DEFAULT_ADDRESS)
            .accept(MediaType.TEXT_PLAIN);

        String token = client.get(String.class);
        JwtToken jwt = validateJWTToken(token);

        List<String> audiences = jwt.getClaims().getAudiences();
        assertEquals(1, audiences.size());
        assertEquals(DEFAULT_ADDRESS, audiences.get(0));
    }

    @org.junit.Test
    public void testIssueJWTTokenClaims() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.TEXT_PLAIN);

        // First check that the role isn't usually in the generated token

        String token = client.get(String.class);
        JwtToken jwt = validateJWTToken(token);

        assertNull(jwt.getClaim("roles"));

        // Now get another token specifying the role
        client.query("claim", "roles");

        token = client.get(String.class);
        jwt = validateJWTToken(token);

        assertEquals("ordinary-user", jwt.getClaim("roles"));
    }

    @org.junit.Test
    public void testIssueJWTTokenViaPOST() throws Exception {
        WebClient client = webClient()
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Issue");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", WST_NS_05_12);
        writer.writeCharacters(JWT_TOKEN_TYPE);
        writer.writeEndElement();

        writer.writeEndElement();

        RequestSecurityTokenResponseType securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        RequestedSecurityTokenType requestedSecurityToken = getRequestedSecurityToken(securityResponse);

        String token = ((Element)requestedSecurityToken.getAny()).getTextContent();
        validateJWTToken(token);
    }

    @org.junit.Test
    public void testValidateSAMLAndIssueJWT() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .accept(MediaType.APPLICATION_XML);

        // 1. Get a token via GET
        Document assertionDoc = client.get(Document.class);
        assertNotNull(assertionDoc);

        // 2. Now validate it in the STS using POST
        client = webClient()
            .query("action", "validate")
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Validate");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", WST_NS_05_12);
        writer.writeCharacters(JWT_TOKEN_TYPE);
        writer.writeEndElement();

        writer.writeStartElement("wst", "ValidateTarget", WST_NS_05_12);
        StaxUtils.copy(assertionDoc.getDocumentElement(), writer);
        writer.writeEndElement();

        writer.writeEndElement();

        RequestSecurityTokenResponseType securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        assertTrue(getValidationStatus(securityResponse));

        // Check the token
        RequestedSecurityTokenType requestedSecurityToken = getRequestedSecurityToken(securityResponse);

        String token = ((Element)requestedSecurityToken.getAny()).getTextContent();
        validateJWTToken(token);
    }

    @org.junit.Test
    public void testValidateJWTAndIssueSAML() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.TEXT_PLAIN);

        // 1. Get a token via GET
        String token = client.get(String.class);
        assertNotNull(token);

        // 2. Now validate it in the STS using POST
        client = webClient()
            .query("action", "validate")
            .type(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML);

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", WST_NS_05_12);

        writer.writeStartElement("wst", "RequestType", WST_NS_05_12);
        writer.writeCharacters(WST_NS_05_12 + "/Validate");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", WST_NS_05_12);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();

        writer.writeStartElement("wst", "ValidateTarget", WST_NS_05_12);
        writer.writeStartElement("TokenWrapper");
        writer.writeCharacters(token);
        writer.writeEndElement();
        writer.writeEndElement();

        writer.writeEndElement();

        RequestSecurityTokenResponseType securityResponse = client.post(
            new DOMSource(writer.getDocument().getDocumentElement()),
            RequestSecurityTokenResponseType.class);

        assertTrue(getValidationStatus(securityResponse));

        // Check the token
        validateSAMLSecurityTokenResponse(securityResponse, true);
    }

    @org.junit.Test
    public void testIssueJWTTokenXMLWrapper() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);
        assertNotNull(assertionDoc);

        // Discard XML wrapper
        validateJWTToken(assertionDoc.getDocumentElement().getFirstChild().getTextContent());
    }

    @org.junit.Test
    public void testIssueJWTTokenJSONWrapper() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.APPLICATION_JSON);

        String token = new ObjectMapper().readTree(client.get(InputStream.class)).get("token").asText();
        validateJWTToken(token);
    }

    @org.junit.Test
    public void testDefaultSAMLFormat() throws Exception {
        WebClient client = webClient()
            .path("saml")
            .accept(MediaType.WILDCARD);

        // It should be XML
        Document doc = client.get(Document.class);
        assertNotNull(doc);
    }

    @org.junit.Test
    public void testDefaultJWTFormat() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.WILDCARD);

        // It should be XML
        Document doc = client.get(Document.class);
        assertNotNull(doc);
    }

    @org.junit.Test
    public void testIssueSAMLTokenWithWrongAcceptType() throws Exception {
        WebClient client = webClient()
            .path("saml2.0")
            .accept(MediaType.APPLICATION_JSON);

        Response response = client.get();
        try {
            response.readEntity(Document.class);
            fail("Failure expected on an bad accept type");
        } catch (Exception ex) {
            // expected
        }
    }

    private static Element validateSAMLSecurityTokenResponse(
        RequestSecurityTokenResponseType securityResponse, boolean saml2
    ) throws Exception {
        RequestedSecurityTokenType requestedSecurityToken = getRequestedSecurityToken(securityResponse);

        // Process the token
        List<WSSecurityEngineResult> results = processToken((Element) requestedSecurityToken.getAny());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        if (saml2) {
            assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        } else {
            assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
        }
        assertTrue(assertion.isSigned());

        return (Element)results.get(0).get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
    }

    private static RequestedSecurityTokenType getRequestedSecurityToken(
        RequestSecurityTokenResponseType securityResponse) {
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("RequestedSecurityToken".equals(jaxbElement.getName().getLocalPart())) {
                    return (RequestedSecurityTokenType)jaxbElement.getValue();
                }
            }
        }
        fail("RequestedSecurityToken missing");
        return null;
    }

    private static SamlAssertionWrapper validateSAMLToken(Document assertionDoc)
        throws Exception {
        assertNotNull(assertionDoc);
        List<WSSecurityEngineResult> results = processToken(assertionDoc.getDocumentElement());
        assertTrue(results != null && results.size() == 1);

        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper) results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.isSigned());

        return assertion;
    }

    private static List<WSSecurityEngineResult> processToken(Element assertionElement)
        throws Exception {
        RequestData requestData = new RequestData();
//        requestData.setDisableBSPEnforcement(true);
        requestData.setCallbackHandler(new org.apache.cxf.systest.sts.common.CommonCallbackHandler());
        requestData.setDecCrypto(serviceCrypto);
//        requestData.setSigVerCrypto(serviceCrypto);
        requestData.setWsDocInfo(new WSDocInfo(assertionElement.getOwnerDocument()));

        return new SAMLTokenProcessor().handleToken(assertionElement, requestData);
    }

    private static JwtToken validateJWTToken(String token)
        throws Exception {
        assertNotNull(token);
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();

        // Validate claims
        assertEquals("DoubleItSTSIssuer", jwt.getClaims().getIssuer());
        assertNotNull(jwt.getClaims().getExpiryTime());
        assertNotNull(jwt.getClaims().getIssuedAt());

        CryptoType alias = new CryptoType(CryptoType.TYPE.ALIAS);
        alias.setAlias("mystskey");
        X509Certificate stsCertificate = serviceCrypto.getX509Certificates(alias)[0];
        assertTrue(jwtConsumer.verifySignatureWith(stsCertificate, SignatureAlgorithm.RS256));

        return jwt;
    }

    private static boolean getValidationStatus(RequestSecurityTokenResponseType securityResponse) {
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("Status".equals(jaxbElement.getName().getLocalPart())) {
                    return (WST_NS_05_12 + "/status/valid").equals(
                        ((StatusType)jaxbElement.getValue()).getCode());
                }
            }
        }
        fail("Status missing");
        return false;
    }

    private WebClient webClient() throws Exception {
        closeClient();

        webClient = WebClient.create("https://localhost:" + STSPORT + "/SecurityTokenService/token");
        webClient.getConfiguration().getHttpConduit()
            .setTlsClientParameters(TLSClientParametersUtils.getTLSClientParameters());
        return webClient;
    }

}
