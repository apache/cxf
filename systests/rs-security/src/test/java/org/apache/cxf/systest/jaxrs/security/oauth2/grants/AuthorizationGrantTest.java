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
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.xml.security.utils.ClassLoaderUtils;

import org.junit.AfterClass;
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
    public static final String JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-grants-jcache");
    public static final String JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-grants2-jcache");
    public static final String JWT_JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-grants-jcache-jwt");
    public static final String JWT_JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-grants2-jcache-jwt");
    public static final String JPA_PORT = TestUtil.getPortNumber("jaxrs-oauth2-grants-jpa");
    public static final String JPA_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-grants2-jpa");
    public static final String JWT_NON_PERSIST_JCACHE_PORT =
        TestUtil.getPortNumber("jaxrs-oauth2-grants-jcache-jwt-non-persist");
    public static final String JWT_NON_PERSIST_JCACHE_PORT2 =
        TestUtil.getPortNumber("jaxrs-oauth2-grants2-jcache-jwt-non-persist");

    final String port;

    public AuthorizationGrantTest(String port) {
        this.port = port;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2GrantsJCache.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2GrantsJCacheJWT.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2GrantsJPA.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2GrantsJCacheJWTNonPersist.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
    }

    @Parameters(name = "{0}")
    public static Collection<String> data() {

        return Arrays.asList(JCACHE_PORT, JWT_JCACHE_PORT, JPA_PORT, JWT_NON_PERSIST_JCACHE_PORT);
    }

    @org.junit.Test
    public void testAuthorizationCodeGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
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
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
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
        Response response = client.post(form);

        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);
        String location = OAuth2TestUtils.getLocation(client, authzData, null);
        String code =  OAuth2TestUtils.getSubstring(location, "code");
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

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantRefresh() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
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
        Response response = client.post(form);

        accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantRefreshWithScope() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
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

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantWithScope() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
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
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantWithState() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String state = "1234566789";
        String code = OAuth2TestUtils.getAuthorizationCode(client, "read_balance", "consumer-id",
                                                           null, state);
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
    }

    @org.junit.Test
    public void testAuthorizationCodeGrantWithAudience() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = OAuth2TestUtils.getAuthorizationCode(client, null, "consumer-id-aud");
        assertNotNull(code);

        // Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id-aud", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String audPort = JCACHE_PORT2;
        if (JWT_JCACHE_PORT.equals(port)) {
            audPort = JWT_JCACHE_PORT2;
        } else if (JPA_PORT.equals(port)) {
            audPort = JPA_PORT2;
        } else if (JWT_NON_PERSIST_JCACHE_PORT.equals(port)) {
            audPort = JWT_NON_PERSIST_JCACHE_PORT2;
        }
        String audience = "https://localhost:" + audPort + "/secured/bookstore/books";
        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code,
                                                                "consumer-id-aud", audience);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testImplicitGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());
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
        String accessToken = OAuth2TestUtils.getSubstring(location, "access_token");
        assertNotNull(accessToken);

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken);
        }
    }

    @org.junit.Test
    public void testPasswordsCredentialsGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "consumer-id", "this-is-a-secret",
                                            busFile.toString());

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

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testClientCredentialsGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "consumer-id", "this-is-a-secret",
                                            busFile.toString());

        // Get Access Token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "client_credentials");
        Response response = client.post(form);

        ClientAccessToken accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        if (isAccessTokenInJWTFormat()) {
            // We don't have a Subject for the client credential grant,
            // so validate manually here as opposed to calling validateAccessToken
            JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(accessToken.getTokenKey());

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                          "password".toCharArray());
            Certificate cert = keystore.getCertificate("alice");
            assertNotNull(cert);

            assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert,
                                                              SignatureAlgorithm.RS256));
        }
    }

    @org.junit.Test
    public void testSAMLAuthorizationGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());

        // Create the SAML Assertion
        String assertion = OAuth2TestUtils.createToken(address + "token");

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

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    @org.junit.Test
    public void testJWTAuthorizationGrant() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());

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
        Response response = client.post(form);

        ClientAccessToken accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());

        if (isAccessTokenInJWTFormat()) {
            validateAccessToken(accessToken.getTokenKey());
        }
    }

    private void validateAccessToken(String accessToken)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(accessToken);
        JwtToken jwt = jwtConsumer.getJwtToken();

        // Validate claims
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        assertNotNull(jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));
        assertEquals("jwt-issuer", jwt.getClaim(JwtConstants.CLAIM_ISSUER));

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        Certificate cert = keystore.getCertificate("alice");
        assertNotNull(cert);

        assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert,
                                                          SignatureAlgorithm.RS256));
    }

    private boolean isAccessTokenInJWTFormat() {
        return JWT_JCACHE_PORT.equals(port) || JWT_NON_PERSIST_JCACHE_PORT.equals(port);
    }

    //
    // Server implementations
    //

    public static class BookServerOAuth2GrantsJCache extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2GrantsJCache.class.getResource("grants-server-jcache.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2GrantsJCache();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2GrantsJCacheJWT extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2GrantsJCacheJWT.class.getResource("grants-server-jcache-jwt.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2GrantsJCacheJWT();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2GrantsJPA extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2GrantsJPA.class.getResource("grants-server-jpa.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2GrantsJPA();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2GrantsJCacheJWTNonPersist extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2GrantsJCacheJWT.class.getResource("grants-server-jcache-jwt-non-persist.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2GrantsJCacheJWTNonPersist();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
