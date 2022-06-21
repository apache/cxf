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
 * Some unit tests for the token revocation service in CXF. The tests are run multiple times with different
 * OAuthDataProvider implementations:
 * a) JCACHE_PORT - JCache
 * b) JWT_JCACHE_PORT - JCache with useJwtFormatForAccessTokens enabled
 * c) JPA_PORT - JPA provider
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class RevocationServiceTest extends AbstractBusClientServerTestBase {

    public static final String JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-revocation-jcache");
    public static final String JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-revocation2-jcache");
    public static final String JWT_JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-revocation-jcache-jwt");
    public static final String JWT_JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-revocation2-jcache-jwt");
    public static final String JPA_PORT = TestUtil.getPortNumber("jaxrs-oauth2-revocation-jpa");
    public static final String JPA_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-revocation2-jpa");

    final String port;

    public RevocationServiceTest(String port) {
        this.port = port;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2RevocationJCache.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2RevocationJCacheJWT.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2RevocationJPA.class, true));
    }

    @Parameters(name = "{0}")
    public static Collection<String> data() {

        return Arrays.asList(JCACHE_PORT, JWT_JCACHE_PORT, JPA_PORT);
    }

    @org.junit.Test
    public void testAccessTokenRevocation() throws Exception {
        URL busFile = RevocationServiceTest.class.getResource("client.xml");

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

        // Now query the token introspection service to make sure the token is valid
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        Form form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("introspect/");
        Response response = client.post(form);

        TokenIntrospection tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertTrue(tokenIntrospection.isActive());

        // Now revoke the token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("revoke/");
        response = client.post(form);
        assertEquals(200, response.getStatus());

        // Now check the token introspection service again to make sure the token is not valid
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("introspect/");
        response = client.post(form);

        tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertFalse(tokenIntrospection.isActive());
    }

    @org.junit.Test
    public void testRefreshTokenRevocation() throws Exception {
        URL busFile = RevocationServiceTest.class.getResource("client.xml");

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

        // Now revoke the refresh token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        Form form = new Form();
        form.param("token", accessToken.getRefreshToken());
        client.path("revoke/");
        Response response = client.post(form);
        assertEquals(200, response.getStatus());

        // Now check we can't get an access token with the revoked refresh token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.type("application/x-www-form-urlencoded").accept("application/json");

        form = new Form();
        form.param("grant_type", "refresh_token");
        form.param("refresh_token", accessToken.getRefreshToken());
        form.param("client_id", "consumer-id");
        client.path("token");
        response = client.post(form);
        assertEquals(400, response.getStatus());
    }

    @org.junit.Test
    public void testAccessTokenRevocationWrongClient() throws Exception {
        URL busFile = RevocationServiceTest.class.getResource("client.xml");

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

        // Now query the token introspection service to make sure the token is valid
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        Form form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("introspect/");
        Response response = client.post(form);

        TokenIntrospection tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertTrue(tokenIntrospection.isActive());

        // Now try to revoke the token as a different client (this should fail, but no exception is thrown)
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id-aud", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("revoke/");
        response = client.post(form);

        // Now check the token introspection service again to make sure the token is still valid
        // (the token revocation call failed)
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "consumer-id", "this-is-a-secret", busFile.toString());
        client.accept("application/json").type("application/x-www-form-urlencoded");
        form = new Form();
        form.param("token", accessToken.getTokenKey());
        client.path("introspect/");
        response = client.post(form);

        tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertTrue(tokenIntrospection.isActive());
    }

    //
    // Server implementations
    //

    public static class BookServerOAuth2RevocationJCache extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2RevocationJCache.class.getResource("revocation-server-jcache.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2RevocationJCache();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2RevocationJCacheJWT extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2RevocationJCacheJWT.class.getResource("revocation-server-jcache-jwt.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2RevocationJCacheJWT();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2RevocationJPA extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2RevocationJPA.class.getResource("revocation-server-jpa.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2RevocationJPA();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }


}
