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
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JsonWebKeysProvider;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.rs.api.GetTokenRequest;
import org.apache.cxf.sts.rs.api.RealmSecurityTokenService;
import org.apache.cxf.sts.rs.api.TokenRequest;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some tests for the REST interface of the CXF STS.
 */
public class STSRealmRestTest extends AbstractBusClientServerTestBase {

    private static final String STSPORT = allocatePort(STSRealmRestServer.class);

    private static final String REALM = "realmA";

    private static final String SAML1_TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    private static final String SAML2_TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    private static final String DEFAULT_ADDRESS =
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";

    private static TLSClientParameters tlsClientParameters = new TLSClientParameters();
    private static Crypto serviceCrypto;

    @org.junit.BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSRealmRestServer.class, true)
        );

        tlsClientParameters = getTLSClientParameters();
        serviceCrypto = CryptoFactory.getInstance("serviceKeystore.properties");
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testIssueSAML2Token() throws Exception {
        Document assertionDoc = client().getXMLToken(REALM, "saml2.0", null, null, null, false)
            .readEntity(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }

    @org.junit.Test
    public void testIssueSAML1Token() throws Exception {
        Document assertionDoc = client().getXMLToken(REALM, "saml1.1", null, null, null, false)
            .readEntity(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
    }

    @org.junit.Test
    public void testIssueSymmetricKeySaml1() throws Exception {
        Document assertionDoc = client()
            .getXMLToken(REALM, "saml1.1", STSConstants.SYMMETRIC_KEY_KEYTYPE, null, null, false)
            .readEntity(Document.class);

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
        Document assertionDoc = client()
            .getXMLToken(REALM, "saml1.1", "SymmetricKey", null, null, false)
            .readEntity(Document.class);

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
        Document assertionDoc = client()
            .getXMLToken(REALM, "saml2.0", STSConstants.PUBLIC_KEY_KEYTYPE, null, null, false)
            .readEntity(Document.class);

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
        Document assertionDoc = client()
            .getXMLToken(REALM, "saml2.0", "PublicKey", null, null, false)
            .readEntity(Document.class);

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
        Document assertionDoc = client()
            .getXMLToken(REALM, "saml1.1", STSConstants.BEARER_KEY_KEYTYPE, null, null, false)
            .readEntity(Document.class);

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
    public void testIssueBearerSAML1TokenShorKeyType() throws Exception {
        Document assertionDoc = client()
            .getXMLToken(REALM, "saml1.1", "Bearer", null, null, false)
            .readEntity(Document.class);

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
        Document assertionDoc = client()
            .getXMLToken(REALM, "saml2.0", null, null, DEFAULT_ADDRESS, false)
            .readEntity(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }

    @org.junit.Test
    public void testIssueSAML2TokenUnknownAppliesTo() throws Exception {
        Response response = client()
            .getXMLToken(REALM, "saml2.0", null, null, "https://localhost:8081/tripleit/", false);
        try {
            response.readEntity(Document.class);
            fail("Failure expected on an unknown AppliesTo address");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testIssueSAML2TokenClaims() throws Exception {
        // First check that the role isn't usually in the generated token
        Document assertionDoc = client().getXMLToken(REALM, "saml2.0", null, null, null, false)
            .readEntity(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);

        ClaimCollection claims = SAMLUtils.getClaims(assertion);
        assertEquals(1, claims.size());
        Claim claim = claims.get(0);
        String role = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        assertNotEquals(role, claim.getClaimType().toString());

        // Now get another token specifying the role
        assertionDoc = client().getXMLToken(REALM, "saml2.0", null, Collections.singletonList(role), null, false)
            .readEntity(Document.class);

        assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);

        claims = SAMLUtils.getClaims(assertion);
        assertEquals(1, claims.size());
        claim = claims.get(0);
        assertEquals(role, claim.getClaimType().toString());
        assertEquals("ordinary-user", claim.getValues().get(0));
    }

    @org.junit.Test
    public void testIssueSAML2TokenViaWSTrust() throws Exception {
        RequestSecurityTokenResponseType securityResponse = client()
            .getXMLToken(REALM, "saml2.0", null, null, null, true).readEntity(RequestSecurityTokenResponseType.class);

        validateSAMLSecurityTokenResponse(securityResponse, true);
    }

    @org.junit.Test
    public void testIssueSAML2TokenViaPOST() throws Exception {
        // Create GetTokenRequest
        GetTokenRequest getTokenRequest = new GetTokenRequest();
        getTokenRequest.setTokenType(SAML2_TOKEN_TYPE);

        Document assertionDoc = client().getToken(REALM, getTokenRequest)
            .readEntity(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }

    @org.junit.Test
    public void testIssueSAML1TokenViaPOST() throws Exception {
        // Create GetTokenRequest
        GetTokenRequest getTokenRequest = new GetTokenRequest();
        getTokenRequest.setTokenType(SAML1_TOKEN_TYPE);

        Document assertionDoc = client().getToken(REALM, getTokenRequest)
            .readEntity(Document.class);

        SamlAssertionWrapper assertion = validateSAMLToken(assertionDoc);
        assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
    }

    @org.junit.Test
    public void testValidateSAML2Token() throws Exception {
        // 1. Get a token via GET
        Document assertionDoc = client().getXMLToken(REALM, "saml2.0", null, null, null, false)
            .readEntity(Document.class);
        assertNotNull(assertionDoc);

        // 2. Now validate it in the STS using POST
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setToken(assertionDoc.getDocumentElement());
        // TODO: remove
        tokenRequest.setTokenType(STSConstants.STATUS);
        RequestSecurityTokenResponseType securityResponse = client().validate(REALM, tokenRequest)
            .readEntity(RequestSecurityTokenResponseType.class);

        assertTrue(getValidationStatus(securityResponse));
    }

    @org.junit.Test
    public void testRenewSAML2Token() throws Exception {
        // 1. Get a token via GET
        Document assertionDoc = client().getXMLToken(REALM, "saml2.0", null, null, null, false)
            .readEntity(Document.class);
        assertNotNull(assertionDoc);

        // 2. Now renew it using POST
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setToken(assertionDoc.getDocumentElement());
        RequestSecurityTokenResponseType securityResponse = client().renew(REALM, tokenRequest)
            .readEntity(RequestSecurityTokenResponseType.class);

        validateSAMLSecurityTokenResponse(securityResponse, true);
    }

    @org.junit.Test
    public void testIssueSAML2TokenPlain() throws Exception {
        String encodedAssertion = client().getPlainToken(REALM, "saml2.0", null, null, null)
            .readEntity(String.class);
        assertNotNull(encodedAssertion);

        byte[] deflatedToken = Base64Utility.decode(encodedAssertion);
        InputStream inputStream = CompressionUtils.inflate(deflatedToken);
        Document doc =
            StaxUtils.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        SamlAssertionWrapper assertion = validateSAMLToken(doc);
        assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
    }
//
    @org.junit.Test
    public void testIssueJWTTokenPlain() throws Exception {
        String token = client().getPlainToken(REALM, "jwt", null, null, null)
            .readEntity(String.class);
        validateJWTToken(token);
    }

    @org.junit.Test
    public void testIssueJWTTokenAppliesTo() throws Exception {
        String token = client().getPlainToken(REALM, "jwt", null, null, DEFAULT_ADDRESS)
            .readEntity(String.class);
        JwtToken jwt = validateJWTToken(token);

        List<String> audiences = jwt.getClaims().getAudiences();
        assertEquals(1, audiences.size());
        assertEquals(DEFAULT_ADDRESS, audiences.get(0));
    }

    @org.junit.Test
    public void testIssueJWTTokenClaims() throws Exception {
        // First check that the role isn't usually in the generated token

        String token = client().getPlainToken(REALM, "jwt", null, null, null)
            .readEntity(String.class);
        JwtToken jwt = validateJWTToken(token);

        assertNull(jwt.getClaim("roles"));

        // Now get another token specifying the role
        token = client().getPlainToken(REALM, "jwt", null, Collections.singletonList("roles"), null)
            .readEntity(String.class);
        jwt = validateJWTToken(token);

        assertEquals("ordinary-user", jwt.getClaim("roles"));
    }

    @org.junit.Test
    public void testIssueJWTTokenViaPOST() throws Exception {
        GetTokenRequest getTokenRequest = new GetTokenRequest();
        getTokenRequest.setTokenType(JWT_TOKEN_TYPE);

        Document assertionDoc = client().getToken(REALM, getTokenRequest)
            .readEntity(Document.class);

        // Process the token
        validateJWTToken(assertionDoc.getDocumentElement().getFirstChild().getTextContent());
    }

    @org.junit.Test
    public void testValidateSAMLAndIssueJWT() throws Exception {
        // 1. Get a token via GET
        Document assertionDoc = client().getXMLToken(REALM, "saml2.0", null, null, null, false)
            .readEntity(Document.class);
        assertNotNull(assertionDoc);

        // 2. Now validate it in the STS using POST
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setToken(assertionDoc.getDocumentElement());
        tokenRequest.setTokenType(JWT_TOKEN_TYPE);
        RequestSecurityTokenResponseType securityResponse = client().validate(REALM, tokenRequest)
            .readEntity(RequestSecurityTokenResponseType.class);

        assertTrue(getValidationStatus(securityResponse));

        // Check the token
        RequestedSecurityTokenType requestedSecurityToken = getRequestedSecurityToken(securityResponse);

        String token = ((Element)requestedSecurityToken.getAny()).getTextContent();
        assertNotNull(token);

        validateJWTToken(token);
    }

    @org.junit.Test
    public void testValidateJWTAndIssueSAML() throws Exception {
        // 1. Get a token via GET
        Document token = client().getXMLToken(REALM, "jwt", null, null, null, false)
            .readEntity(Document.class);
        assertNotNull(token);

        // 2. Now validate it in the STS using POST
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setToken(token.getDocumentElement());
        tokenRequest.setTokenType(SAML2_TOKEN_TYPE);
        RequestSecurityTokenResponseType securityResponse = client().validate(REALM, tokenRequest)
            .readEntity(RequestSecurityTokenResponseType.class);

        assertTrue(getValidationStatus(securityResponse));

        // Check the token
        validateSAMLSecurityTokenResponse(securityResponse, true);
    }

    @org.junit.Test
    public void testIssueJWTTokenXMLWrapper() throws Exception {
        Document assertionDoc = client().getXMLToken(REALM, "jwt", null, null, null, false)
            .readEntity(Document.class);
        assertNotNull(assertionDoc);

        // Discard XML wrapper
        validateJWTToken(assertionDoc.getDocumentElement().getFirstChild().getTextContent());
    }

    @org.junit.Test
    public void testIssueJWTTokenJSONWrapper() throws Exception {
        InputStream response = client().getJSONToken(REALM, "jwt", null, null, null)
            .readEntity(InputStream.class);
        assertNotNull(response);

        String token = new ObjectMapper().readTree(response).get("token").asText();
        validateJWTToken(token);
    }

    @org.junit.Ignore
    @org.junit.Test
    public void testDefaultSAMLFormat() throws Exception {
        RealmSecurityTokenService client = client();
        WebClient.client(client).accept(MediaType.WILDCARD);
        Document doc = client.getPlainToken(REALM, "saml", null, null, null)
            .readEntity(Document.class);

        // It should be XML
        assertNotNull(doc);
    }

    @org.junit.Ignore
    @org.junit.Test
    public void testDefaultJWTFormat() throws Exception {
        RealmSecurityTokenService client = client();
        WebClient.client(client).accept(MediaType.WILDCARD);
        Document doc = client.getPlainToken(REALM, "jwt", null, null, null)
            .readEntity(Document.class);

        // It should be XML
        assertNotNull(doc);
    }

    @org.junit.Test
    public void testIssueSAMLTokenWithWrongAcceptType() throws Exception {
        Response response = client().getJSONToken(REALM, "saml2.0", null, null, null);

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
        List<WSSecurityEngineResult> results =
            processToken((Element)requestedSecurityToken.getAny());

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

        assertTrue(jwtConsumer.verifySignatureWith(
                client().getPublicVerificationKeys(REALM).getKeys().iterator().next(),
                SignatureAlgorithm.getAlgorithm(jwt.getJwsHeaders().getAlgorithm())));

        return jwt;
    }

    private static boolean getValidationStatus(RequestSecurityTokenResponseType securityResponse) {
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("Status".equals(jaxbElement.getName().getLocalPart())) {
                    return (STSConstants.VALID_CODE).equals(
                        ((StatusType)jaxbElement.getValue()).getCode());
                }
            }
        }
        fail("Status missing");
        return false;
    }

    private static RealmSecurityTokenService client() throws Exception {
        final RealmSecurityTokenService client = JAXRSClientFactory.create(
            "https://localhost:" + STSPORT + "/RealmSecurityTokenService",
            RealmSecurityTokenService.class,
            Collections.singletonList(new JsonWebKeysProvider()));
        WebClient.getConfig(client).getHttpConduit().setTlsClientParameters(tlsClientParameters);
        return client;
    }

    private static TLSClientParameters getTLSClientParameters() throws Exception {
        final TLSClientParameters tlsCP = new TLSClientParameters();
        tlsCP.setDisableCNCheck(true);

        final KeyStore keyStore;
        try (InputStream is = ClassLoaderUtils.getResourceAsStream("keys/clientstore.jks", STSRealmRestTest.class)) {
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
