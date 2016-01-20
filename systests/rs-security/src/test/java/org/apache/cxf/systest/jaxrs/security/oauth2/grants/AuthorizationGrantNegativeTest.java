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
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
=======
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
>>>>>>> 49b2b81... Reshuffle of the tests to share some common code

import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
<<<<<<< HEAD
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
=======
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
>>>>>>> 49b2b81... Reshuffle of the tests to share some common code
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.SamlCallbackHandler;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
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
    // Authorization code grants
    //

    @org.junit.Test
    public void testAuthorizationCodeBadClient() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("redirect_uri", "http://www.blah.apache.org");
        client.query("response_type", "code");
        client.path("authorize/");
        
        // No client
        Response response = client.get();
        assertEquals(400, response.getStatus());
        
        // Bad client
        client.query("client_id", "bad-consumer-id");
        response = client.get();
        assertEquals(400, response.getStatus());
    }
    
    @org.junit.Test
    public void testAuthorizationCodeBadRedirectionURI() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("response_type", "code");
        client.path("authorize/");
        
        // Bad redirect URI
        client.query("redirect_uri", "http://www.blah.bad.apache.org");
        Response response = client.get();
        assertEquals(400, response.getStatus());
    }
    
    @org.junit.Test
    public void testResponseType() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("redirect_uri", "http://www.blah.apache.org");
        // client.query("response_type", "code");
        client.path("authorize/");
        
        // No response type
        Response response = client.get();
        assertEquals(303, response.getStatus());
        
        client.query("response_type", "unknown");
        response = client.get();
        assertEquals(303, response.getStatus());
    }
    
    @org.junit.Test
    public void testAuthorizationCodeBadScope() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("response_type", "code");
        client.query("redirect_uri", "http://www.blah.bad.apache.org");
        client.query("scope", "unknown-scope");
        client.path("authorize/");
        
        // No redirect URI
        Response response = client.get();
        assertEquals(400, response.getStatus());
    }
    
    // Send the authorization code twice to get an access token
    @org.junit.Test
    public void testRepeatAuthorizationCode() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        // First invocation
        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        ClientAccessToken token = response.readEntity(ClientAccessToken.class);
        assertNotNull(token.getTokenKey());

        // Now try to get a second token
        response = client.post(form);
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on trying to get a second access token");
        } catch (ResponseProcessingException ex) {
            //expected
        }
    }
    
    // Try to refresh the access token twice using the same refresh token
    @org.junit.Test
    public void testRepeatRefreshCall() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "read_balance");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        // Refresh the access token
        client.type("application/x-www-form-urlencoded").accept("application/json");

        Form form = new Form();
        form.param("grant_type", "refresh_token");
        form.param("refresh_token", accessToken.getRefreshToken());
        form.param("client_id", "consumer-id");
        form.param("scope", "read_balance");
        Response response = client.post(form);

        accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
        
        // Now try to refresh it again
        try {
            response = client.post(form);
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on trying to reuse a refresh token");
        } catch (ResponseProcessingException ex) {
            //expected
        }
    }
    
    @org.junit.Test
    public void testRefreshWithBadToken() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "read_balance");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        // Refresh the access token - using a bad token
        client.type("application/x-www-form-urlencoded").accept("application/json");

        Form form = new Form();
        form.param("grant_type", "refresh_token");
        form.param("client_id", "consumer-id");
        form.param("scope", "read_balance");
        Response response = client.post(form);

        // No refresh token
        try {
            response = client.post(form);
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on no refresh token");
        } catch (ResponseProcessingException ex) {
            //expected
        }
        
        // Now specify a bad refresh token
        form.param("refresh_token", "12345");
        try {
            response = client.post(form);
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on a bad refresh token");
        } catch (ResponseProcessingException ex) {
            //expected
        }
    }
    
    @org.junit.Test
    public void testAccessTokenBadCode() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        // First invocation
        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("client_id", "consumer-id");
        
        // No code
        Response response = client.post(form);
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on no code");
        } catch (ResponseProcessingException ex) {
            //expected
        }
        
        // Bad code
        form.param("code", "123456677");
        response = client.post(form);
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on a bad code");
        } catch (ResponseProcessingException ex) {
            //expected
        }
    }
    
    @org.junit.Test
    public void testUnknownGrantType() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "consumer-id", "this-is-a-secret", 
                                            busFile.toString());

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        // form.param("grant_type", "password");
        form.param("username", "alice");
        form.param("password", "security");
        Response response = client.post(form);

        // No grant_type
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on no grant type");
        } catch (ResponseProcessingException ex) {
            //expected
        }
        
        // Unknown grant_type
        form.param("grant_type", "unknown");
        response = client.post(form);
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on an unknown grant type");
        } catch (ResponseProcessingException ex) {
            //expected
        }
    }
    
    @org.junit.Test
    public void testPasswordCredentialsGrantUnknownUsers() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "consumer-id", "this-is-a-secret", 
                                            busFile.toString());

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        Response response = client.post(form);

        // No username
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on no username");
        } catch (ResponseProcessingException ex) {
            //expected
        }
        
        // Bad username
        form.param("username", "alice2");
        response = client.post(form);
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on a bad username");
        } catch (ResponseProcessingException ex) {
            //expected
        }
        
        // No password
        form.param("username", "alice");
        response = client.post(form);
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on no password");
        } catch (ResponseProcessingException ex) {
            //expected
        }
        
        // Bad password
        form.param("password", "security2");
        response = client.post(form);
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on a bad password");
        } catch (ResponseProcessingException ex) {
            //expected
        }
    }
    
    //
    // SAML Authorization grants
    //
    
    @org.junit.Test
    public void testSAML11() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        String assertion = OAuth2TestUtils.createToken(address + "token", false, true);

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
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        String assertion = OAuth2TestUtils.createToken(address + "token2", true, true);

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
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        String assertion = OAuth2TestUtils.createToken(address + "token", true, false);

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
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
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
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
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
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Create the JWT Token
<<<<<<< HEAD
        String token = createToken("DoubleItSTSIssuer", "consumer-id", 
=======
        String token = OAuth2TestUtils.createToken("DoubleItSTSIssuer", "consumer-id", 
                                   "https://localhost:" + PORT + "/services/token", true, false);
        
        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.param("assertion", token);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on an unsigned token");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testJWTNoIssuer() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken(null, "consumer-id", 
>>>>>>> 49b2b81... Reshuffle of the tests to share some common code
                                   "https://localhost:" + PORT + "/services/token", true, true);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.param("assertion", token);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
<<<<<<< HEAD
        ClientAccessToken accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
=======
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on no issuer");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testJWTNoExpiry() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("DoubleItSTSIssuer", "consumer-id", 
                                   "https://localhost:" + PORT + "/services/token", false, true);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.param("assertion", token);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on no expiry");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testJWTBadAudienceRestriction() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("DoubleItSTSIssuer", "consumer-id", 
                                   "https://localhost:" + PORT + "/services/badtoken", true, true);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.param("assertion", token);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on a bad audience restriction");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testJWTUnauthenticatedSignature() throws Exception {
        URL busFile = AuthorizationGrantNegativeTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Create the JWT Token
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("consumer-id");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 60);
        claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
        String audience = "https://localhost:" + PORT + "/services/token";
        claims.setAudiences(Collections.singletonList(audience));
        
        // Sign the JWT Token
        Properties signingProperties = new Properties();
        signingProperties.put("rs.security.keystore.type", "jks");
        signingProperties.put("rs.security.keystore.password", "security");
        signingProperties.put("rs.security.keystore.alias", "smallkey");
        signingProperties.put("rs.security.keystore.file", 
            "org/apache/cxf/systest/jaxrs/security/certs/smallkeysize.jks");
        signingProperties.put("rs.security.key.password", "security");
        signingProperties.put("rs.security.signature.algorithm", "RS256");

        JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);

        JwsSignatureProvider sigProvider = 
            JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);

        String token = jws.signWith(sigProvider);
        
        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.param("assertion", token);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
        try {
            response.readEntity(ClientAccessToken.class);
            fail("Failure expected on an unauthenticated token");
        } catch (Exception ex) {
            // expected
        }
>>>>>>> 49b2b81... Reshuffle of the tests to share some common code
    }
    */
    
<<<<<<< HEAD
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
    
    private String getAuthorizationCode(WebClient client) {
        return getAuthorizationCode(client, null);
    }

    private String getAuthorizationCode(WebClient client, String scope) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("redirect_uri", "http://www.blah.apache.org");
        client.query("response_type", "code");
        if (scope != null) {
            client.query("scope", scope);
        }
        client.path("authorize/");
        Response response = client.get();

        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);

        // Now call "decision" to get the authorization code grant
        client.path("decision");
        client.type("application/x-www-form-urlencoded");

        Form form = new Form();
        form.param("session_authenticity_token", authzData.getAuthenticityToken());
        form.param("client_id", authzData.getClientId());
        form.param("redirect_uri", authzData.getRedirectUri());
        if (authzData.getProposedScope() != null) {
            form.param("scope", authzData.getProposedScope());
        }
        form.param("oauthDecision", "allow");

        response = client.post(form);
        String location = response.getHeaderString("Location"); 
        return getSubstring(location, "code");
    }

    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, String code) {
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);

        return response.readEntity(ClientAccessToken.class);
    }
    
    private String getSubstring(String parentString, String substringName) {
        String foundString = 
            parentString.substring(parentString.indexOf(substringName + "=") + (substringName + "=").length());
        int ampersandIndex = foundString.indexOf('&');
        if (ampersandIndex < 1) {
            ampersandIndex = foundString.length();
        }
        return foundString.substring(0, ampersandIndex);
    }
=======
>>>>>>> 49b2b81... Reshuffle of the tests to share some common code
}
