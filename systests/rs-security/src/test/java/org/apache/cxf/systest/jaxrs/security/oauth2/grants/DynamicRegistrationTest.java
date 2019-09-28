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
import java.util.Collections;

import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistration;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistrationResponse;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the dynamic registration service in CXF. The tests are run multiple times with different
 * OAuthDataProvider implementations:
 * a) JCACHE_PORT - JCache
 * b) JWT_JCACHE_PORT - JCache with useJwtFormatForAccessTokens enabled
 * c) JPA_PORT - JPA provider
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class DynamicRegistrationTest extends AbstractBusClientServerTestBase {

    public static final String JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-dynamic-reg-jcache");
    public static final String JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-dynamic-reg2-jcache");
    public static final String JWT_JCACHE_PORT = TestUtil.getPortNumber("jaxrs-oauth2-dynamic-reg-jcache-jwt");
    public static final String JWT_JCACHE_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-dynamic-reg2-jcache-jwt");
    public static final String JPA_PORT = TestUtil.getPortNumber("jaxrs-oauth2-dynamic-reg-jpa");
    public static final String JPA_PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-dynamic-reg2-jpa");

    final String port;

    public DynamicRegistrationTest(String port) {
        this.port = port;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2DynamicRegistrationJCache.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2DynamicRegistrationJCacheJWT.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2DynamicRegistrationJPA.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
    }

    @Parameters(name = "{0}")
    public static Collection<String> data() {

        return Arrays.asList(JCACHE_PORT, JWT_JCACHE_PORT, JPA_PORT);
    }

    @org.junit.Test
    public void testDynamicRegistration() throws Exception {
        URL busFile = DynamicRegistrationTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());

        // 1. Register a client
        client.accept("application/json").type("application/json");
        client.path("register/");

        ClientRegistration registration = new ClientRegistration();
        registration.setClientName("new client");
        registration.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));

        Response response = client.post(registration);

        ClientRegistrationResponse registrationResponse = response.readEntity(ClientRegistrationResponse.class);
        assertNotNull(registrationResponse.getClientId());
        assertNotNull(registrationResponse.getClientSecret());
        assertNotNull(registrationResponse.getRegistrationClientUri());
        assertNotNull(registrationResponse.getRegistrationAccessToken());

        // 2. Get Authorization Code
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        String code = OAuth2TestUtils.getAuthorizationCode(client, null, registrationResponse.getClientId());
        assertNotNull(code);

        // 3. Now get the access token
        client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                  registrationResponse.getClientId(), registrationResponse.getClientSecret(),
                                  busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(client, code, registrationResponse.getClientId(), null);
        assertNotNull(accessToken.getTokenKey());
    }

    @org.junit.Test
    public void testRedirectURIIsRequired() throws Exception {
        URL busFile = DynamicRegistrationTest.class.getResource("client.xml");

        String address = "https://localhost:" + port + "/services/";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            "alice", "security", busFile.toString());

        // 1. Register a client
        client.accept("application/json").type("application/json");
        client.path("register/");

        ClientRegistration registration = new ClientRegistration();
        registration.setClientName("new client");
        registration.setScope("newscope");

        Response response = client.post(registration);
        assertEquals(400, response.getStatus());
    }

    //
    // Server implementations
    //

    public static class BookServerOAuth2DynamicRegistrationJCache extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2DynamicRegistrationJCache.class.getResource("dynamic-reg-server-jcache.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2DynamicRegistrationJCache();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2DynamicRegistrationJCacheJWT extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2DynamicRegistrationJCacheJWT.class.getResource("dynamic-reg-server-jcache-jwt.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2DynamicRegistrationJCacheJWT();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class BookServerOAuth2DynamicRegistrationJPA extends AbstractBusTestServerBase {
        private static final URL SERVER_CONFIG_FILE =
            BookServerOAuth2DynamicRegistrationJPA.class.getResource("dynamic-reg-server-jpa.xml");

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
            BusFactory.setDefaultBus(springBus);
            setBus(springBus);

            try {
                new BookServerOAuth2DynamicRegistrationJPA();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }


}
