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

package org.apache.cxf.systest.jaxrs.security.oauth2.grants;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.systest.jaxrs.security.oauth2.SamlCallbackHandler;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.junit.BeforeClass;

/**
 * Some (negative) tests for various authorization grants.
 */
public class AuthorizationGrantNegativeTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerOAuth2GrantsNegative.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerOAuth2GrantsNegative.class, true));
    }

    //
    // SAML Authorization grants
    //
    
    @org.junit.Test
    public void testSAML11() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        String assertion = createToken(address + "token", false, true);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
        form.param("assertion", Base64UrlUtility.encode(assertion));
        form.param("client_id", "consumer-id");
        
        try {
            Response response = client.post(form);
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on a SAML 1.1 assertion");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testSAMLAudRestr() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        String assertion = createToken(address + "token2", true, true);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
        form.param("assertion", Base64UrlUtility.encode(assertion));
        form.param("client_id", "consumer-id");
        
        try {
            Response response = client.post(form);
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on a bad audience restriction");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testSAMLUnsigned() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        String assertion = createToken(address + "token", true, false);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
        form.param("assertion", Base64UrlUtility.encode(assertion));
        form.param("client_id", "consumer-id");
        
        try {
            Response response = client.post(form);
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on an unsigned assertion");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testSAMLHolderOfKey() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(true);
        samlCallbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        samlCallbackHandler.setAudience(address + "token");
        
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        samlAssertion.signAssertion(
            samlCallback.getIssuerKeyName(),
            samlCallback.getIssuerKeyPassword(),
            samlCallback.getIssuerCrypto(),
            samlCallback.isSendKeyValue(),
            samlCallback.getCanonicalizationAlgorithm(),
            samlCallback.getSignatureAlgorithm()
        );
        String assertion = samlAssertion.assertionToString();

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
        form.param("assertion", Base64UrlUtility.encode(assertion));
        form.param("client_id", "consumer-id");
        
        try {
            Response response = client.post(form);
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on an incorrect subject confirmation method");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testSAMLUnauthenticatedSignature() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(true);
        samlCallbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        samlCallbackHandler.setAudience(address + "token");
        samlCallbackHandler.setIssuerKeyName("smallkey");
        samlCallbackHandler.setIssuerKeyPassword("security");
        samlCallbackHandler.setCryptoPropertiesFile("org/apache/cxf/systest/jaxrs/security/smallkey.properties");
        
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        samlAssertion.signAssertion(
            samlCallback.getIssuerKeyName(),
            samlCallback.getIssuerKeyPassword(),
            samlCallback.getIssuerCrypto(),
            samlCallback.isSendKeyValue(),
            samlCallback.getCanonicalizationAlgorithm(),
            samlCallback.getSignatureAlgorithm()
        );
        String assertion = samlAssertion.assertionToString();

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
        form.param("assertion", Base64UrlUtility.encode(assertion));
        form.param("client_id", "consumer-id");
        
        try {
            Response response = client.post(form);
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on an incorrect subject confirmation method");
        } catch (Exception ex) {
            // expected
        }
    }
    /*
    @org.junit.Test
    public void testJWTAuthorizationGrant() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        
        // Create the JWT Token
        String token = createToken("DoubleItSTSIssuer", "consumer-id", 
                                   "https://localhost:" + PORT + "/services/token", true, true);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.param("assertion", token);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
        ClientAccessToken accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
    }
    */
    
    private List<Object> setupProviders() {
        List<Object> providers = new ArrayList<Object>();
        JSONProvider<OAuthAuthorizationData> jsonP = new JSONProvider<OAuthAuthorizationData>();
        jsonP.setNamespaceMap(Collections.singletonMap("http://org.apache.cxf.rs.security.oauth",
                                                       "ns2"));
        providers.add(jsonP);
        OAuthJSONProvider oauthProvider = new OAuthJSONProvider();
        providers.add(oauthProvider);
        
        return providers;
    }

    private String createToken(String audRestr, boolean saml2, boolean sign) throws WSSecurityException {
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(sign);
        samlCallbackHandler.setAudience(audRestr);
        if (!saml2) {
            samlCallbackHandler.setSaml2(false);
            samlCallbackHandler.setConfirmationMethod(SAML1Constants.CONF_BEARER);
        }
        
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        if (samlCallback.isSignAssertion()) {
            samlAssertion.signAssertion(
                samlCallback.getIssuerKeyName(),
                samlCallback.getIssuerKeyPassword(),
                samlCallback.getIssuerCrypto(),
                samlCallback.isSendKeyValue(),
                samlCallback.getCanonicalizationAlgorithm(),
                samlCallback.getSignatureAlgorithm()
            );
        }
        
        return samlAssertion.assertionToString();
    }
    /*
    private String createToken(String issuer, String subject, String audience, 
                               boolean expiry, boolean sign) {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject(subject);
        if (issuer != null) {
            claims.setIssuer(issuer);
        }
        claims.setIssuedAt(new Date().getTime() / 1000L);
        if (expiry) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 60);
            claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
        }
        if (audience != null) {
            claims.setAudiences(Collections.singletonList(audience));
        }
        
        if (sign) {
            // Sign the JWT Token
            Properties signingProperties = new Properties();
            signingProperties.put("rs.security.keystore.type", "jks");
            signingProperties.put("rs.security.keystore.password", "password");
            signingProperties.put("rs.security.keystore.alias", "alice");
            signingProperties.put("rs.security.keystore.file", 
                                  "org/apache/cxf/systest/jaxrs/security/certs/alice.jks");
            signingProperties.put("rs.security.key.password", "password");
            signingProperties.put("rs.security.signature.algorithm", "RS256");
            
            JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
            JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
            
            JwsSignatureProvider sigProvider = 
                JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);
            
            return jws.signWith(sigProvider);
        }
        
        JwsHeaders jwsHeaders = new JwsHeaders(SignatureAlgorithm.NONE);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
        return jws.getSignedEncodedJws();
    }
    */
}
