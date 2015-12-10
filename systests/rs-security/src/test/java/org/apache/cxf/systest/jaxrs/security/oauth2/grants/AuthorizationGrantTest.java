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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.systest.jaxrs.security.oauth2.SamlCallbackHandler;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.junit.BeforeClass;

/**
 * Some tests for various authorization grants.
 */
public class AuthorizationGrantTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerOAuth2Grants.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerOAuth2Grants.class, true));
    }

    @org.junit.Test
    public void testAuthorizationCodeGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = getAuthorizationCode(client);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, setupProviders(), "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantRefresh() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = getAuthorizationCode(client);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, setupProviders(), "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        // Refresh the access token
        client.type("application/x-www-form-urlencoded").accept("application/json");

        Form form = new Form();
        form.param("grant_type", "refresh_token");
        form.param("refresh_token", accessToken.getRefreshToken());
        form.param("client_id", "consumer-id");
        Response response = client.post(form);

        accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantRefreshWithScope() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = getAuthorizationCode(client, "read_balance");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, setupProviders(), "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
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
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantWithScope() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = getAuthorizationCode(client, "read_balance");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, setupProviders(), "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testImplicitGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Access Token
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("redirect_uri", "http://www.blah.apache.org");
        client.query("response_type", "token");
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
        form.param("oauthDecision", "allow");

        response = client.post(form);

        String location = response.getHeaderString("Location"); 
        String accessToken = location.substring(location.indexOf("access_token=") + "access_token=".length());
        accessToken = accessToken.substring(0, accessToken.indexOf('&'));
        assertNotNull(accessToken);
    }

    @org.junit.Test
    public void testPasswordsCredentialsGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "consumer-id", 
                                            "this-is-a-secret", busFile.toString());

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "password");
        form.param("username", "alice");
        form.param("password", "security");
        Response response = client.post(form);

        ClientAccessToken accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
    }

    @org.junit.Test
    public void testClientCredentialsGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "consumer-id", 
                                            "this-is-a-secret", busFile.toString());

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "client_credentials");
        Response response = client.post(form);

        ClientAccessToken accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
    }
    
    @org.junit.Test
    public void testSAMLAuthorizationGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        
        // Create the SAML Assertion
        String assertion = createToken(address + "token");

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
        form.param("assertion", Base64UrlUtility.encode(assertion));
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
        ClientAccessToken accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
    }

    @org.junit.Test
    public void testJWTAuthorizationGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");
        
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
        return location.substring(location.indexOf("code=") + "code=".length());
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

    private String createToken(String audRestr) throws WSSecurityException {
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(true);
        samlCallbackHandler.setAudience(audRestr);
        
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
}
