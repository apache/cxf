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
package org.apache.cxf.systest.jaxrs.security.oidc;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils.AuthorizationCodeParameters;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.wss4j.common.util.Loader;

import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * Some unit tests to test the various flows in OpenID Connect.
 */
public class OIDCFlowTest extends AbstractBusClientServerTestBase {
    
    static final String PORT = TestUtil.getPortNumber("jaxrs-oidc");
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(OIDCServer.class, true)
        );
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlow() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
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
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
    }
    
    // Just a normal OAuth invocation, check it all works ok
    @org.junit.Test
    public void testAuthorizationCodeOAuth() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "read_balance");
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        // We should not have an IdToken here
        String idToken = accessToken.getParameters().get("id_token");
        assertNull(idToken);
        assertFalse(accessToken.getApprovedScope().contains("openid"));
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithNonce() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid", "consumer-id",
                                                           "123456789", null);
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, "123456789");
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithScope() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid read_balance");
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        assertTrue(accessToken.getApprovedScope().contains("read_balance"));
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithRefresh() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        assertNotNull(accessToken.getRefreshToken());
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
        
        // Refresh the access token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        
        Form form = new Form();
        form.param("grant_type", "refresh_token");
        form.param("refresh_token", accessToken.getRefreshToken());
        form.param("client_id", "consumer-id");
        form.param("scope", "openid");
        Response response = client.post(form);
        
        accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
        accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithState() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid", "consumer-id",
                                                           null, "123456789");
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithAudience() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid", "consumer-id-aud",
                                                           null, null);
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address,  OAuth2TestUtils.setupProviders(), 
                                  "consumer-id-aud", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        String audience = "https://localhost:" + PORT + "/secured/bookstore/books";
        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id-aud", audience);
        assertNotNull(accessToken.getTokenKey());
    }
    
    @org.junit.Test
    public void testImplicitFlow() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
       
        // Get Access Token
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("redirect_uri", "http://www.blah.apache.org");
        client.query("scope", "openid");
        client.query("response_type", "id_token token");
        client.query("nonce", "123456789");
        client.path("authorize-implicit/");
        Response response = client.get();
        
        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);
        
        // Now call "decision" to get the access token
        client.path("decision");
        client.type("application/x-www-form-urlencoded");
        
        Form form = new Form();
        form.param("session_authenticity_token", authzData.getAuthenticityToken());
        form.param("client_id", authzData.getClientId());
        form.param("redirect_uri", authzData.getRedirectUri());
        form.param("scope", authzData.getProposedScope());
        if (authzData.getResponseType() != null) {
            form.param("response_type", authzData.getResponseType());
        }
        if (authzData.getNonce() != null) {
            form.param("nonce", authzData.getNonce());
        }
        form.param("oauthDecision", "allow");
        
        response = client.post(form);
        
        String location = response.getHeaderString("Location"); 
        
        // Check Access Token
        String accessToken = OAuth2TestUtils.getSubstring(location, "access_token");
        assertNotNull(accessToken);
        
        // Check IdToken
        String idToken = OAuth2TestUtils.getSubstring(location, "id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
        
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertNotNull(jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM));
        Assert.assertNotNull(jwt.getClaims().getClaim(IdToken.NONCE_CLAIM));
        OidcUtils.validateAccessTokenHash(accessToken, jwt, true);
    }
    
    @org.junit.Test
    public void testImplicitFlowNoAccessToken() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
       
        // Get Access Token
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("redirect_uri", "http://www.blah.apache.org");
        client.query("scope", "openid");
        client.query("response_type", "id_token");
        client.query("nonce", "123456789");
        client.path("authorize-implicit/");
        Response response = client.get();
        
        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);
        
        // Now call "decision" to get the access token
        client.path("decision");
        client.type("application/x-www-form-urlencoded");
        
        Form form = new Form();
        form.param("session_authenticity_token", authzData.getAuthenticityToken());
        form.param("client_id", authzData.getClientId());
        form.param("redirect_uri", authzData.getRedirectUri());
        form.param("scope", authzData.getProposedScope());
        if (authzData.getResponseType() != null) {
            form.param("response_type", authzData.getResponseType());
        }
        if (authzData.getNonce() != null) {
            form.param("nonce", authzData.getNonce());
        }
        form.param("oauthDecision", "allow");
        
        response = client.post(form);
        
        String location = response.getHeaderString("Location"); 
        
        // Check Access Token - it should not be present
        String accessToken = OAuth2TestUtils.getSubstring(location, "access_token");
        assertNull(accessToken);
        
        // Check IdToken
        String idToken = OAuth2TestUtils.getSubstring(location, "id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
        
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertNull(jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM));
        Assert.assertNotNull(jwt.getClaims().getClaim(IdToken.NONCE_CLAIM));
    }
    
    @org.junit.Test
    public void testHybridCodeIdToken() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(100000000);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get location
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        parameters.setScope("openid");
        parameters.setNonce("123456789");
        parameters.setResponseType("code id_token");
        parameters.setPath("authorize-hybrid/");
        
        String location = OAuth2TestUtils.getLocation(client, parameters);
        assertNotNull(location);
        
        // Check code
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);
        
        // Check id_token
        String idToken = OAuth2TestUtils.getSubstring(location, "id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, "123456789");
        // check the code hash is returned from the implicit authorization endpoint
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertNotNull(jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));
        
        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        
        // Check id_token from the token endpoint
        idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
        // check the code hash is returned from the token endpoint
        jwtConsumer = new JwsJwtCompactConsumer(idToken);
        jwt = jwtConsumer.getJwtToken();
        Assert.assertNotNull(jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));
    }
    
    @org.junit.Test
    public void testHybridCodeToken() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get location
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        parameters.setScope("openid");
        parameters.setNonce("123456789");
        parameters.setResponseType("code token");
        parameters.setPath("authorize-hybrid/");
      
        String location = OAuth2TestUtils.getLocation(client, parameters);
        assertNotNull(location);
                
        // Check code
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);
        
        // Check id_token
        String idToken = OAuth2TestUtils.getSubstring(location, "id_token");
        assertNull(idToken);
        
        // Check Access Token
        String implicitAccessToken = OAuth2TestUtils.getSubstring(location, "access_token");
        assertNotNull(implicitAccessToken);
        
        idToken = OAuth2TestUtils.getSubstring(location, "id_token");
        assertNull(idToken);
        
        // Now get the access token with the code
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = 
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        
        // Check id_token from the token endpoint
        idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
        // check the code hash is returned from the token endpoint
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        // returning c_hash in the id_token returned after exchanging the code is optional
        Assert.assertNull(jwtConsumer.getJwtClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));
    }
    
    @org.junit.Test
    public void testHybridCodeIdTokenToken() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get location
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        parameters.setScope("openid");
        parameters.setNonce("123456789");
        parameters.setResponseType("code id_token token");
        parameters.setPath("authorize-hybrid/");
        
        String location = OAuth2TestUtils.getLocation(client, parameters);
        assertNotNull(location);
        
        // Check code
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);
        
        // Check id_token
        String idToken = OAuth2TestUtils.getSubstring(location, "id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, "123456789");
        
        // check the code hash is returned from the implicit authorization endpoint
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertNotNull(jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));
        
        // Check Access Token
        String accessToken = OAuth2TestUtils.getSubstring(location, "access_token");
        assertNotNull(accessToken);
        
        jwtConsumer = new JwsJwtCompactConsumer(idToken);
        jwt = jwtConsumer.getJwtToken();
        Assert.assertNotNull(jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM));
        OidcUtils.validateAccessTokenHash(accessToken, jwt, true);
        Assert.assertNotNull(jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowUnsignedJWT() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/unsignedjwtservices/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("consumer-id");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setAudiences(
            Collections.singletonList("https://localhost:" + PORT + "/unsignedjwtservices/"));
        
        JwsHeaders headers = new JwsHeaders();
        headers.setAlgorithm("none");
        
        JwtToken token = new JwtToken(headers, claims);
        
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(token);
        String request = jws.getSignedEncodedJws();
        
        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        parameters.setScope("openid");
        parameters.setResponseType("code");
        parameters.setPath("authorize/");
        parameters.setRequest(request);
        
        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowUnsignedJWTWithState() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/unsignedjwtservices/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("consumer-id");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setAudiences(
            Collections.singletonList("https://localhost:" + PORT + "/unsignedjwtservices/"));
        
        JwsHeaders headers = new JwsHeaders();
        headers.setAlgorithm("none");
        
        JwtToken token = new JwtToken(headers, claims);
        
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(token);
        String request = jws.getSignedEncodedJws();
        
        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        parameters.setScope("openid");
        parameters.setResponseType("code");
        parameters.setPath("authorize/");
        parameters.setState("123456789");
        parameters.setRequest(request);
        
        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);
    }
    
    @org.junit.Test
    public void testGetKeys() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        client.accept("application/json");
        
        client.path("keys/");
        Response response = client.get();
        JsonWebKeys jsonWebKeys = response.readEntity(JsonWebKeys.class);
        
        assertEquals(1, jsonWebKeys.getKeys().size());
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithKey() throws Exception {
        URL busFile = OIDCFlowTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
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
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        
        // Now get the key to validate the token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                  "alice", "security", busFile.toString());
        client.accept("application/json");
        
        client.path("keys/");
        Response response = client.get();
        JsonWebKeys jsonWebKeys = response.readEntity(JsonWebKeys.class);
        
        Assert.assertTrue(jwtConsumer.verifySignatureWith(jsonWebKeys.getKeys().get(0),
                                                          SignatureAlgorithm.RS256));
    }
    
    private void validateIdToken(String idToken, String nonce) 
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        
        // Validate claims
        Assert.assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        Assert.assertEquals("OIDC IdP", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
        Assert.assertEquals("consumer-id", jwt.getClaim(JwtConstants.CLAIM_AUDIENCE));
        Assert.assertNotNull(jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        Assert.assertNotNull(jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));
        if (nonce != null) {
            Assert.assertEquals(nonce, jwt.getClaim(IdToken.NONCE_CLAIM));
        }
        
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(Loader.getResource("org/apache/cxf/systest/jaxrs/security/certs/alice.jks").openStream(), 
                      "password".toCharArray());
        Certificate cert = keystore.getCertificate("alice");
        Assert.assertNotNull(cert);
        
        Assert.assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert, 
                                                          SignatureAlgorithm.RS256));
    }
}
