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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JsonWebKeysProvider;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.grants.code.CodeVerifierTransformer;
import org.apache.cxf.rs.security.oauth2.grants.code.DigestCodeVerifier;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.idp.OidcProviderMetadata;
import org.apache.cxf.rs.security.oidc.rp.IdTokenReader;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils.AuthorizationCodeParameters;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.xml.security.utils.ClassLoaderUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests to test the various flows in OpenID Connect. The tests are run multiple times
 * with different OAuthDataProvider implementations:
 * a) JCACHE_SERVER - JCache
 * b) JWT_JCACHE_SERVER - JCache with useJwtFormatForAccessTokens enabled
 * c) JPA_SERVER - JPA provider
 * d) JWT_NON_PERSIST_JCACHE_SERVER-  JCache with useJwtFormatForAccessTokens + !persistJwtEncoding
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class OIDCFlowTest extends AbstractBusClientServerTestBase {

    private static final SpringBusTestServer JCACHE_SERVER = new SpringBusTestServer("oidc-server-jcache");
    private static final SpringBusTestServer JWT_JCACHE_SERVER = new SpringBusTestServer("oidc-server-jcache-jwt");
    private static final SpringBusTestServer JPA_SERVER = new SpringBusTestServer("oidc-server-jpa");
    private static final SpringBusTestServer JWT_NON_PERSIST_JCACHE_SERVER =
            new SpringBusTestServer("oidc-server-jcache-jwt-non-persist");

    final String port;
    final Map<String, Object> properties;

    public OIDCFlowTest(String port, Map<String, Object> properties) {
        this.port = port;
        this.properties = properties;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus().setExtension(OAuth2TestUtils.clientHTTPConduitConfigurer(), HTTPConduitConfigurer.class);

        assertTrue("Server failed to launch", launchServer(JCACHE_SERVER));
        assertTrue("Server failed to launch", launchServer(JWT_JCACHE_SERVER));
        assertTrue("Server failed to launch", launchServer(JPA_SERVER));
        assertTrue("Server failed to launch", launchServer(JWT_NON_PERSIST_JCACHE_SERVER));
    }
    
    @Before
    public void setUp() {
        BusFactory.getDefaultBus(false).setProperties(properties);
    }
    
    @After
    public void tearDown() {
        properties.keySet().forEach(name -> BusFactory.getDefaultBus(false).setProperty(name, null));
    }

    @Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{
            new Object[] {JCACHE_SERVER.getPort(), Map.of()},
            new Object[] {JWT_JCACHE_SERVER.getPort(), Map.of()},
            new Object[] {JPA_SERVER.getPort(), Map.of("share.httpclient.http.conduit", false)},
            new Object[] {JWT_NON_PERSIST_JCACHE_SERVER.getPort(), Map.of()}
        };
    }

    @org.junit.Test
    public void testAuthorizationCodeFlow() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);

        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    // Authorization Servers MUST support the use of the HTTP GET and POST methods defined in RFC 2616
    // [RFC2616] at the Authorization Endpoint.
    @org.junit.Test
    public void testAuthorizationCodeFlowPOST() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);

        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Make initial authorization request
        client.type("application/x-www-form-urlencoded");

        client.path("authorize/");

        Form form = new Form();
        form.param("client_id", "consumer-id");
        form.param("scope", "openid");
        form.param("redirect_uri", "http://www.blah.apache.org");
        form.param("response_type", "code");
        Response response = client.post(form);

        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);
        String location = OAuth2TestUtils.getLocation(client, authzData, null);
        String code =  OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    // Just a normal OAuth invocation, check it all works ok
    @org.junit.Test
    public void testAuthorizationCodeOAuth() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "read_balance");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        // We should not have an IdToken here
        String idToken = accessToken.getParameters().get("id_token");
        assertNull(idToken);
        assertFalse(accessToken.getApprovedScope().contains("openid"));

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowWithNonce() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid", "consumer-id",
                                                           "123456789", null);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, "123456789");

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowWithScope() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid read_balance");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        assertTrue(accessToken.getApprovedScope().contains("read_balance"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowWithRefresh() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        assertNotNull(accessToken.getRefreshToken());

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);

        // Refresh the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
            "consumer-id", "this-is-a-secret", null);
        client.path("token");
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
        assertNotNull(accessToken.getParameters().get("id_token"));
        assertNotNull(idToken);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowWithState() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid", "consumer-id",
                                                           null, "123456789");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowWithAudience() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid", "consumer-id-aud",
                                                           null, null);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id-aud", "this-is-a-secret", null);

        String audience = "https://localhost:" + port + "/secured/bookstore/books";
        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id-aud", audience);
        assertNotNull(accessToken.getTokenKey());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowWithPKCE() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address,  OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId("consumer-id");
        parameters.setScope(OidcUtils.OPENID_SCOPE);
        parameters.setResponseType(OAuthConstants.CODE_RESPONSE_TYPE);
        parameters.setPath("authorize/");
        String codeVerifier = Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(32));
        CodeVerifierTransformer transformer = new DigestCodeVerifier();
        parameters.setCodeChallenge(transformer.transformCodeVerifier(codeVerifier));
        parameters.setCodeChallengeMethod(transformer.getChallengeMethod());

        String location = OAuth2TestUtils.getLocation(client, parameters);
        String code = OAuth2TestUtils.getSubstring(location, "code");

        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null, codeVerifier);
        assertNotNull(accessToken.getTokenKey());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testImplicitFlow() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
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
        assertNotNull(jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM));
        assertNotNull(jwt.getClaims().getClaim(IdToken.NONCE_CLAIM));
        OidcUtils.validateAccessTokenHash(accessToken, jwt, true);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken);
        }
    }

    // Authorization Servers MUST support the use of the HTTP GET and POST methods defined in RFC 2616
    // [RFC2616] at the Authorization Endpoint.
    @org.junit.Test
    public void testImplicitFlowPOST() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Access Token
        client.type("application/x-www-form-urlencoded");

        client.path("authorize-implicit/");

        Form form = new Form();
        form.param("client_id", "consumer-id");
        form.param("scope", "openid");
        form.param("redirect_uri", "http://www.blah.apache.org");
        form.param("response_type", "id_token token");
        form.param("nonce", "123456789");
        Response response = client.post(form);

        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);

        // Now call "decision" to get the access token
        client.path("decision");
        client.type("application/x-www-form-urlencoded");

        form = new Form();
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
        assertNotNull(jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM));
        assertNotNull(jwt.getClaims().getClaim(IdToken.NONCE_CLAIM));
        OidcUtils.validateAccessTokenHash(accessToken, jwt, true);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken);
        }
    }

    @org.junit.Test
    public void testImplicitFlowNoAccessToken() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
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
        assertNull(jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM));
        assertNotNull(jwt.getClaims().getClaim(IdToken.NONCE_CLAIM));
    }

    @org.junit.Test
    public void testHybridCodeIdToken() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
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
        assertNotNull(jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

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
        assertNotNull(jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testHybridCodeToken() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
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
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

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
        assertNull(jwtConsumer.getJwtClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testHybridCodeIdTokenToken() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
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
        assertNotNull(jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));

        // Check Access Token
        String accessToken = OAuth2TestUtils.getSubstring(location, "access_token");
        assertNotNull(accessToken);

        jwtConsumer = new JwsJwtCompactConsumer(idToken);
        jwt = jwtConsumer.getJwtToken();
        assertNotNull(jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM));
        OidcUtils.validateAccessTokenHash(accessToken, jwt, true);
        assertNotNull(jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM));

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken);
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowUnsignedJWT() throws Exception {
        String address = "https://localhost:" + port + "/unsignedjwtservices/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        JwtClaims claims = new JwtClaims();
        claims.setIssuer("consumer-id");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(
            Collections.singletonList("https://localhost:" + port + "/unsignedjwtservices/"));

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
        String address = "https://localhost:" + port + "/unsignedjwtservices/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        JwtClaims claims = new JwtClaims();
        claims.setIssuer("consumer-id");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(
            Collections.singletonList("https://localhost:" + port + "/unsignedjwtservices/"));

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
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        client.accept("application/json");

        client.path("keys/");
        Response response = client.get();
        JsonWebKeys jsonWebKeys = response.readEntity(JsonWebKeys.class);

        assertEquals(1, jsonWebKeys.getKeys().size());
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowWithKey() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "openid");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);

        // Now get the key to validate the token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "alice", "security", null);
        client.accept("application/json");

        client.path("keys/");
        Response response = client.get();
        JsonWebKeys jsonWebKeys = response.readEntity(JsonWebKeys.class);

        assertTrue(jwtConsumer.verifySignatureWith(jsonWebKeys.getKeys().get(0),
                                                          SignatureAlgorithm.RS256));
    }

    @org.junit.Test
    public void testAuthorizationCodeFlowRefreshToken() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client,
            String.join(" ", OidcUtils.getOpenIdScope(), OAuthConstants.REFRESH_TOKEN_SCOPE),
            "consumer-id-oidc");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id-oidc", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id-oidc", null);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));

        IdToken idToken = getIdToken(accessToken, address + "keys/", "consumer-id-oidc");
        assertNotNull(idToken);
        Long issuedAt = idToken.getIssuedAt();

        TimeUnit.SECONDS.sleep(1L);

        accessToken = OAuthClientUtils.refreshAccessToken(
            client,
            new Consumer("consumer-id-oidc"),
            accessToken);
        idToken = getIdToken(accessToken, address + "keys/", "consumer-id-oidc");

        assertNotEquals(issuedAt, idToken.getIssuedAt());
    }

    @org.junit.Test
    public void testOIDCProviderMetadata() throws Exception {
        final String issuerURL = "https://localhost:" + port + "/services/";
        final OidcProviderMetadata oidcProviderMetadata = OidcUtils.getOidcProviderMetadata(issuerURL);

        assertEquals(issuerURL, oidcProviderMetadata.getIssuer().toString());
        assertNotNull(oidcProviderMetadata.getResponseTypesSupported());
    }

    private void validateIdToken(String idToken, String nonce)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();

        // Validate claims
        assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        assertEquals("OIDC IdP", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
        assertEquals("consumer-id", jwt.getClaim(JwtConstants.CLAIM_AUDIENCE));
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));
        if (nonce != null) {
            assertEquals(nonce, jwt.getClaim(IdToken.NONCE_CLAIM));
        }

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        Certificate cert = keystore.getCertificate("alice");
        assertNotNull(cert);

        assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert,
                                                          SignatureAlgorithm.RS256));
    }

    private void validateAccessToken(String accessToken)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(accessToken);
        JwtToken jwt = jwtConsumer.getJwtToken();

        // Validate claims
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        Certificate cert = keystore.getCertificate("alice");
        assertNotNull(cert);

        assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert,
                                                          SignatureAlgorithm.RS256));
    }

    private static IdToken getIdToken(ClientAccessToken accessToken, String jwksUri, String clientId) {
        WebClient c = WebClient.create(jwksUri,
            Collections.singletonList(new JsonWebKeysProvider()),
            "alice", "security", null)
            .accept(MediaType.APPLICATION_JSON);
        IdTokenReader idTokenReader = new IdTokenReader();
        idTokenReader.setJwkSetClient(c);
        idTokenReader.setIssuerId("OIDC IdP");

        return idTokenReader.getIdToken(accessToken, new Consumer(clientId));
    }

    private boolean isAccessTokenInJWTFormat() {
        return JWT_JCACHE_SERVER.getPort().equals(port) || JWT_NON_PERSIST_JCACHE_SERVER.getPort().equals(port);
    }

}
