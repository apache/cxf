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

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.systest.jaxrs.security.oidc.SpringBusTestServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.xml.security.utils.ClassLoaderUtils;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for various authorization grants. The tests are run multiple times with different OAuthDataProvider
 * implementations:
 * a) JCACHE_PORT - JCache
 * b) JWT_JCACHE_PORT - JCache with useJwtFormatForAccessTokens enabled
 * c) JPA_PORT - JPA provider
 * d) JWT_NON_PERSIST_JCACHE_PORT-  JCache with useJwtFormatForAccessTokens + !persistJwtEncoding
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class AuthorizationGrantTest extends AbstractBusClientServerTestBase {
    private static final SpringBusTestServer JCACHE_SERVER =
        new SpringBusTestServer("grants-server-jcache") { };
    private static final String JCACHE_PORT2 = TestUtil.getPortNumber("grants-server-jcache.2");

    private static final SpringBusTestServer JWT_JCACHE_SERVER =
        new SpringBusTestServer("grants-server-jcache-jwt") { };
    private static final String JWT_JCACHE_PORT2 = TestUtil.getPortNumber("grants-server-jcache-jwt.2");

    private static final SpringBusTestServer JPA_SERVER =
        new SpringBusTestServer("grants-server-jpa") { };
    private static final String JPA_PORT2 = TestUtil.getPortNumber("grants-server-jpa.2");

    private static final SpringBusTestServer JWT_NON_PERSIST_JCACHE_SERVER =
        new SpringBusTestServer("grants-server-jcache-jwt-non-persist") { };
    private static final String JWT_NON_PERSIST_JCACHE_PORT2 =
        TestUtil.getPortNumber("grants-server-jcache-jwt-non-persist.2");

    private static final SpringBusTestServer JCACHE_SERVER_SESSION =
            new SpringBusTestServer("grants-server-jcache-session") { };
    private static final String JCACHE_PORT3 = TestUtil.getPortNumber("grants-server-jcache-session.2");

    private static final String ISSUER = "OIDC IdP";

    final String port;

    public AuthorizationGrantTest(String port) {
        this.port = port;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus().setExtension(OAuth2TestUtils.clientHTTPConduitConfigurer(), HTTPConduitConfigurer.class);

        System.setProperty("issuer", ISSUER);

        assertTrue("server did not launch correctly", launchServer(JCACHE_SERVER));
        assertTrue("server did not launch correctly", launchServer(JWT_JCACHE_SERVER));
        assertTrue("server did not launch correctly", launchServer(JPA_SERVER));
        assertTrue("server did not launch correctly", launchServer(JWT_NON_PERSIST_JCACHE_SERVER));
        assertTrue("server did not launch correctly", launchServer(JCACHE_SERVER_SESSION));
    }

    @Parameters(name = "{0}")
    public static String[] data() {
        return new String[] {
            JCACHE_SERVER.getPort(),
            JWT_JCACHE_SERVER.getPort(),
            JPA_SERVER.getPort(),
            JWT_NON_PERSIST_JCACHE_SERVER.getPort(),
            JCACHE_SERVER_SESSION.getPort()};
    }

    @org.junit.Test
    public void testAuthorizationCodeGrant() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    // The authorization server MUST support the use of the HTTP "GET"
    // method [RFC2616] for the authorization endpoint and MAY support the
    // use of the "POST" method as well.
    @org.junit.Test
    public void testAuthorizationCodeGrantPOST() throws Exception {
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
        form.param("redirect_uri", "http://www.blah.apache.org");
        form.param("response_type", "code");

        OAuthAuthorizationData authzData = client.post(form, OAuthAuthorizationData.class);
        String location = OAuth2TestUtils.getLocation(client, authzData, null);
        String code =  OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantRefresh() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", null);
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

        accessToken = client.post(form, ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantRefreshWithScope() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "read_balance");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", null);
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

        accessToken = client.post(form, ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
        assertEquals("read_balance", accessToken.getApprovedScope());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    // Here we don't specify a scope in the refresh token call
    @org.junit.Test
    public void testAuthorizationCodeGrantRefreshWithoutScope() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
                org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, "read_balance");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                "consumer-id", "this-is-a-secret", null);
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

        accessToken = client.post(form, ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
//        assertEquals("read_balance", accessToken.getApprovedScope());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantWithScope() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
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
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantWithState() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String state = "1234566789";
        String code = OAuth2TestUtils.getAuthorizationCode(client, "read_balance", "consumer-id",
                                                           null, state);
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id", "this-is-a-secret", null);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantWithAudience() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, null, "consumer-id-aud");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, "consumer-id-aud", "this-is-a-secret", null);

        String audPort = JCACHE_PORT2;
        if (JWT_JCACHE_SERVER.getPort().equals(port)) {
            audPort = JWT_JCACHE_PORT2;
        } else if (JPA_SERVER.getPort().equals(port)) {
            audPort = JPA_PORT2;
        } else if (JWT_NON_PERSIST_JCACHE_SERVER.getPort().equals(port)) {
            audPort = JWT_NON_PERSIST_JCACHE_PORT2;
        } else if (JCACHE_SERVER_SESSION.getPort().equals(port)) {
            audPort = JCACHE_PORT3;
        }
        String audience = "https://localhost:" + audPort + "/secured/bookstore/books";
        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code,
                                                                "consumer-id-aud", audience);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testImplicitGrant() throws Exception {
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
        client.query("response_type", "token");
        client.path("authorize-implicit/");

        OAuthAuthorizationData authzData = client.get(OAuthAuthorizationData.class);

        // Now call "decision" to get the access token
        client.path("decision");
        client.type("application/x-www-form-urlencoded");

        Form form = new Form();
        form.param("session_authenticity_token", authzData.getAuthenticityToken());
        form.param("client_id", authzData.getClientId());
        form.param("redirect_uri", authzData.getRedirectUri());
        form.param("oauthDecision", "allow");

        Response response = client.post(form);

        String location = response.getHeaderString("Location");
        String accessToken = OAuth2TestUtils.getSubstring(location, "access_token");
        assertNotNull(accessToken);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken);
        }
    }

    @org.junit.Test
    public void testPasswordsCredentialsGrant() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "consumer-id", "this-is-a-secret", null);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "password");
        form.param("username", "alice");
        form.param("password", "security");

        ClientAccessToken accessToken = client.post(form, ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testClientCredentialsGrant() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "consumer-id", "this-is-a-secret", null);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "client_credentials");

        ClientAccessToken accessToken = client.post(form, ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testSAMLAuthorizationGrant() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "consumer-id", "this-is-a-secret", null);

        // Create the SAML Assertion
        String assertion = OAuth2TestUtils.createToken(address + "token");

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
        form.param("assertion", Base64UrlUtility.encode(assertion));
        form.param("client_id", "consumer-id");

        ClientAccessToken accessToken = client.post(form, ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testJWTAuthorizationGrant() throws Exception {
        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "consumer-id", "this-is-a-secret", null);

        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("DoubleItSTSIssuer", "consumer-id",
                                   "https://localhost:" + port + "/services/token", true, true);

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.param("assertion", token);
        form.param("client_id", "consumer-id");

        ClientAccessToken accessToken = client.post(form, ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    private static void validateAccessToken(String accessToken)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(accessToken);
        JwtClaims jwtClaims = jwtConsumer.getJwtToken().getClaims();

        // Validate claims
        if (!OAuthConstants.CLIENT_CREDENTIALS_GRANT.equals(jwtClaims.getStringProperty(OAuthConstants.GRANT_TYPE))) {
            // We don't have a Subject for the client credential grant
            assertNotNull(jwtClaims.getSubject());
        }
        assertNotNull(jwtClaims.getIssuedAt());
        assertNotNull(jwtClaims.getExpiryTime());
        assertEquals(ISSUER, jwtClaims.getIssuer());

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", AuthorizationGrantTest.class),
                      "password".toCharArray());
        Certificate cert = keystore.getCertificate("alice");
        assertNotNull(cert);

        assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert,
                                                          SignatureAlgorithm.RS256));
    }

    private boolean isAccessTokenInJWTFormat() {
        return JWT_JCACHE_SERVER.getPort().equals(port) || JWT_NON_PERSIST_JCACHE_SERVER.getPort().equals(port);
    }

}
