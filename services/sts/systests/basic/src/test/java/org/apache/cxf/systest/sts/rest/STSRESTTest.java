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
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;
import org.apache.xml.security.utils.ClassLoaderUtils;

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

    private static TLSClientParameters tlsClientParameters = new TLSClientParameters();

    private WebClient webClient;

    @org.junit.BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSRESTServer.class, true)
        );

        tlsClientParameters = getTLSClientParameters();
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
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
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }

    @org.junit.Test
    public void testIssueSAML1Token() throws Exception {
        WebClient client = webClient()
            .path("saml1.1")
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
    }

    @org.junit.Test
    public void testIssueSymmetricKeySaml1() throws Exception {
        WebClient client = webClient()
            .path("saml1.1")
            .query("keyType", STSConstants.SYMMETRIC_KEY_KEYTYPE)
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
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
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
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
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
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
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
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
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
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
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
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
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
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
        assertNotNull(assertionDoc);

        // Process the token
        SamlAssertionWrapper assertion = processToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);

        ClaimCollection claims = SAMLUtils.getClaims(assertion);
        assertEquals(1, claims.size());
        Claim claim = claims.get(0);
        String role = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        assertNotEquals(claim.getClaimType().toString(), role);

        // Now get another token specifying the role
        client.query("claim", role);
        assertionDoc = client.get(Document.class);
        assertNotNull(assertionDoc);

        // Process the token
        assertion = processToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);

        claims = SAMLUtils.getClaims(assertion);
        assertEquals(1, claims.size());
        claim = claims.get(0);
        assertEquals(claim.getClaimType().toString(), role);
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
        SamlAssertionWrapper assertion = processToken(doc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }

    @org.junit.Test
    public void testIssueJWTTokenPlain() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.TEXT_PLAIN);

        String token = client.get(String.class);
        assertNotNull(token);

        validateJWTToken(token, null);
    }

    @org.junit.Test
    public void testIssueJWTTokenAppliesTo() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .query("appliesTo", DEFAULT_ADDRESS)
            .accept(MediaType.TEXT_PLAIN);

        String token = client.get(String.class);
        assertNotNull(token);

        validateJWTToken(token, DEFAULT_ADDRESS);
    }

    @org.junit.Test
    public void testIssueJWTTokenClaims() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.TEXT_PLAIN);

        // First check that the role isn't usually in the generated token

        String token = client.get(String.class);
        assertNotNull(token);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();

        assertNull(jwt.getClaim("roles"));

        // Now get another token specifying the role
        client.query("claim", "roles");

        token = client.get(String.class);
        assertNotNull(token);

        // Process the token
        validateJWTToken(token, null);

        jwtConsumer = new JwsJwtCompactConsumer(token);
        jwt = jwtConsumer.getJwtToken();
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
        assertNotNull(requestedSecurityToken);

        String token = ((Element)requestedSecurityToken.getAny()).getTextContent();
        assertNotNull(token);

        validateJWTToken(token, null);
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
    }

    @org.junit.Test
    public void testIssueJWTTokenXMLWrapper() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.APPLICATION_XML);

        Document assertionDoc = client.get(Document.class);
        assertNotNull(assertionDoc);

        // Discard XML wrapper
        validateJWTToken(assertionDoc.getDocumentElement().getFirstChild().getTextContent(), null);
    }

    @org.junit.Test
    public void testIssueJWTTokenJSONWrapper() throws Exception {
        WebClient client = webClient()
            .path("jwt")
            .accept(MediaType.APPLICATION_JSON);

        String token = new ObjectMapper().readTree(client.get(InputStream.class)).get("token").asText();
        validateJWTToken(token, null);
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
        WebClient client = webClient();

        client.accept(MediaType.WILDCARD);
        client.path("jwt");

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
        assertNotNull(requestedSecurityToken);

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
        return null;
    }

    private static SamlAssertionWrapper processToken(Document assertionDoc)
        throws Exception {
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
        requestData.setDisableBSPEnforcement(true);
        requestData.setCallbackHandler(new org.apache.cxf.systest.sts.common.CommonCallbackHandler());
        Crypto crypto = CryptoFactory.getInstance("serviceKeystore.properties");
        requestData.setDecCrypto(crypto);
        requestData.setSigVerCrypto(crypto);
        requestData.setWsDocInfo(new WSDocInfo(assertionElement.getOwnerDocument()));

        return new SAMLTokenProcessor().handleToken(assertionElement, requestData);
    }

    private static void validateJWTToken(String token, String audience)
        throws Exception {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();

        // Validate claims
        assertEquals("DoubleItSTSIssuer", jwt.getClaims().getIssuer());
        if (audience != null) {
            List<String> audiences = jwt.getClaims().getAudiences();
            assertEquals(1, audiences.size());
            assertEquals(audience, audiences.get(0));
        }
        assertNotNull(jwt.getClaims().getExpiryTime());
        assertNotNull(jwt.getClaims().getIssuedAt());

        final Certificate cert;
        try (InputStream is = ClassLoaderUtils.getResourceAsStream("keys/servicestore.jks", STSRESTTest.class)) {
            cert = CryptoUtils.loadCertificate(is, "sspass".toCharArray(), "mystskey", null);
        }
        assertNotNull(cert);

        assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert,
                                                          SignatureAlgorithm.RS256));
    }

    private WebClient webClient() throws Exception {
        closeClient();

        webClient = WebClient.create("https://localhost:" + STSPORT + "/SecurityTokenService/token");
        webClient.getConfiguration().getHttpConduit().setTlsClientParameters(tlsClientParameters);
        return webClient;
    }

    private static TLSClientParameters getTLSClientParameters() throws Exception {
        final TLSClientParameters tlsCP = new TLSClientParameters();
        tlsCP.setDisableCNCheck(true);

        final KeyStore keyStore;
        try (InputStream is = ClassLoaderUtils.getResourceAsStream("keys/clientstore.jks", STSRESTTest.class)) {
            keyStore = CryptoUtils.loadKeyStore(is, "cspass".toCharArray(), null);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "ckpass".toCharArray());
        tlsCP.setKeyManagers(kmf.getKeyManagers());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        tlsCP.setTrustManagers(tmf.getTrustManagers());

        return tlsCP;
    }

}
