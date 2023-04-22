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
package org.apache.cxf.systest.jaxrs.security.oidc.filters;

import java.net.URI;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for the OIDC filters
 */
public class OIDCFiltersTest extends AbstractBusClientServerTestBase {

    private static final String PORT = BookServerOIDCFilters.PORT;
    private static final String OIDC_PORT = BookServerOIDCService.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus().setExtension(OAuth2TestUtils.clientHTTPConduitConfigurer(), HTTPConduitConfigurer.class);

        assertTrue("server did not launch correctly", launchServer(BookServerOIDCFilters.class));
        assertTrue("server did not launch correctly", launchServer(BookServerOIDCService.class));
    }

    @org.junit.Test
    public void testClientCodeRequestFilter() throws Exception {
        // Make an invocation + get back the redirection to the OIDC IdP
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), null);

        WebClient.getConfig(client).getRequestContext().put(
                org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        Response response = client.get();

        URI location = response.getLocation();
        // Now make an invocation on the OIDC IdP using another WebClient instance

        WebClient idpClient = WebClient.create(location.toString(), OAuth2TestUtils.setupProviders(),
                                               "bob", "security", null)
            .type("application/json").accept("application/json");
        // Save the Cookie for the second request...
        WebClient.getConfig(idpClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Make initial authorization request
        final OAuthAuthorizationData authzData = idpClient.get(OAuthAuthorizationData.class);

        // Get Authorization Code + State
        String authzCodeLocation = OAuth2TestUtils.getLocation(idpClient, authzData, null);
        String state = OAuth2TestUtils.getSubstring(authzCodeLocation, "state");
        assertNotNull(state);
        String code = OAuth2TestUtils.getSubstring(authzCodeLocation, "code");
        assertNotNull(code);

        // Add Referer
        String referer = "https://localhost:" + OIDC_PORT + "/services/authorize";
        client.header("Referer", referer);

        // Now invoke back on the service using the authorization code
        client.query("code", code);
        client.query("state", state);

        Response serviceResponse = client.type("application/xml").post(new Book("book", 123L));
        assertEquals(serviceResponse.getStatus(), 200);

        Book returnedBook = serviceResponse.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    //
    // Server implementations
    //
    public static class BookServerOIDCFilters extends AbstractBusTestServerBase {
        public static final String PORT = TestUtil.getPortNumber("jaxrs-oidc-filters");

        @Override
        protected void run() {
            setBus(new SpringBusFactory().createBus(getClass().getResource("filters-server.xml")));
        }
    }

    public static class BookServerOIDCService extends AbstractBusTestServerBase {
        public static final String PORT = TestUtil.getPortNumber("jaxrs-filters-oidc-service");

        @Override
        protected void run() {
            setBus(new SpringBusFactory().createBus(getClass().getResource("oidc-server.xml")));
        }
    }

}
