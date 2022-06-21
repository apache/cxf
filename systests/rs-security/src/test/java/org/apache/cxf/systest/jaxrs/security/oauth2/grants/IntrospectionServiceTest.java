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
import java.util.Arrays;
import java.util.Collection;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.TokenIntrospection;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the token introspection service in CXF. The tests are run multiple times with different
 * OAuthDataProvider implementations:
 * a) JCACHE_PORT - JCache
 * b) JWT_JCACHE_PORT - JCache with useJwtFormatForAccessTokens enabled
 * c) JPA_PORT - JPA provider
 * d) JWT_NON_PERSIST_JCACHE_PORT-  JCache with useJwtFormatForAccessTokens + !persistJwtEncoding
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class IntrospectionServiceTest extends AbstractBusClientServerTestBase {

    public static final String JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-introspection-jcache");
    public static final String JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-introspection2-jcache");
    public static final String JWT_JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-introspection-jcache-jwt");
    public static final String JWT_JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-introspection2-jcache-jwt");
    public static final String JPA_PORT = TestUtil.getPortNumber("jaxrs-oauth2-introspection-jpa");
    public static final String JPA_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-introspection2-jpa");
    public static final String JWT_NON_PERSIST_JCACHE_PORT =
        TestUtil.getPortNumber("jaxrs-oauth2-introspection-jcache-jwt-non-persist");
    public static final String JWT_NON_PERSIST_JCACHE_PORT2 =
        TestUtil.getPortNumber("jaxrs-oauth2-introspection2-jcache-jwt-non-persist");

    final String port;

    public IntrospectionServiceTest(String port) {
        this.port = port;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2IntrospectionJCache.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2IntrospectionJCacheJWT.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2IntrospectionJPA.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2IntrospectionJCacheJWTNonPersist.class, true));
    }

    @Parameters(name = "{0}")
    public static Collection<String> data() {

        return Arrays.asList(JCACHE_PORT, JWT_JCACHE_PORT, JPA_PORT, JWT_NON_PERSIST_JCACHE_PORT);
    }

    @org.junit.Test
    public void testTokenIntrospection() throws Exception {
        URL busFile = IntrospectionServiceTest.class.getResource("client.xml");

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

        ClientAccessToken accessToken = OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());

        // Now query the token introspection service
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        Form form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("introspect/");
        Response response = client.post(form);

        TokenIntrospection tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertTrue(tokenIntrospection.isActive());
        assertEquals(tokenIntrospection.getUsername(), "alice");
        assertEquals(tokenIntrospection.getClientId(), "consumer-id");
        assertEquals(tokenIntrospection.getScope(), accessToken.getApprovedScope());
        Long validity = tokenIntrospection.getExp() - tokenIntrospection.getIat();
        assertTrue(validity == accessToken.getExpiresIn());
        Long nbf = tokenIntrospection.getNbf();
        long now = System.currentTimeMillis() / 1000L;
        assertTrue(nbf < now);
    }

    @org.junit.Test
    public void testTokenIntrospectionWithAudience() throws Exception {
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
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, "consumer-id-aud", audience);
        assertNotNull(accessToken.getTokenKey());

        // Now query the token introspection service
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        Form form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("introspect/");
        Response response = client.post(form);

        TokenIntrospection tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertTrue(tokenIntrospection.isActive());
        assertEquals(tokenIntrospection.getUsername(), "alice");
        assertEquals(tokenIntrospection.getClientId(), "consumer-id-aud");
        assertEquals(tokenIntrospection.getScope(), accessToken.getApprovedScope());
        Long validity = tokenIntrospection.getExp() - tokenIntrospection.getIat();
        assertTrue(validity == accessToken.getExpiresIn());
        assertEquals(tokenIntrospection.getAud().get(0), audience);
    }

    @org.junit.Test
    public void testInvalidToken() throws Exception {
        URL busFile = IntrospectionServiceTest.class.getResource("client.xml");

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

        ClientAccessToken accessToken = OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());

        // Now query the token introspection service
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        Form form = new Form();
        form.param("token", accessToken.getTokenKey() + "-xyz");
        client.path("introspect/");
        Response response = client.post(form);

        TokenIntrospection tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertFalse(tokenIntrospection.isActive());
    }

    @org.junit.Test
    public void testRefreshedToken() throws Exception {
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

        ClientAccessToken accessToken = OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
        String originalAccessToken = accessToken.getTokenKey();

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

        // Now query the token introspection service
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");

        // Refreshed token should be ok
        form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("introspect/");
        response = client.post(form);

        TokenIntrospection tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertTrue(tokenIntrospection.isActive());

        // Original token should not be ok
        form = new Form();
        form.param("token", originalAccessToken);
        response = client.post(form);

        tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertFalse(tokenIntrospection.isActive());
    }

    @org.junit.Test
    public void testTokenIntrospectionWithScope() throws Exception {
        URL busFile = IntrospectionServiceTest.class.getResource("client.xml");

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
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), "consumer-id",
                                  "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("read_balance"));

        // Now query the token introspection service
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(), "consumer-id",
                                  "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        Form form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("introspect/");
        Response response = client.post(form);

        TokenIntrospection tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertTrue(tokenIntrospection.isActive());
        assertEquals(tokenIntrospection.getUsername(), "alice");
        assertEquals(tokenIntrospection.getClientId(), "consumer-id");
        assertEquals(tokenIntrospection.getScope(), accessToken.getApprovedScope());
        Long validity = tokenIntrospection.getExp() - tokenIntrospection.getIat();
        assertTrue(validity == accessToken.getExpiresIn());
    }

    //
    // Server implementations
    //

    public static class BookServerOAuth2IntrospectionJCache extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2IntrospectionJCache.class.getResource("introspection-server-jcache.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2IntrospectionJCache();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2IntrospectionJCacheJWT extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2IntrospectionJCacheJWT.class.getResource("introspection-server-jcache-jwt.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2IntrospectionJCacheJWT();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2IntrospectionJPA extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2IntrospectionJPA.class.getResource("introspection-server-jpa.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2IntrospectionJPA();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2IntrospectionJCacheJWTNonPersist extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2IntrospectionJCacheJWTNonPersist.class.getResource(
                "introspection-server-jcache-jwt-non-persist.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2IntrospectionJCacheJWTNonPersist();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
