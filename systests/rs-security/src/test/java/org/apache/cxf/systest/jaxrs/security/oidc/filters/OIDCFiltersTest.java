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

import java.net.URL;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for the OIDC filters
 */
public class OIDCFiltersTest extends AbstractBusClientServerTestBase {

    public static final String PORT = BookServerOIDCFilters.PORT;
    public static final String OIDC_PORT = BookServerOIDCService.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOIDCFilters.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOIDCService.class, true));
    }

    @org.junit.Test
    public void testClientCodeRequestFilter() throws Exception {
        URL busFile = OIDCFiltersTest.class.getResource("client.xml");

        // Make an invocation + get back the redirection to the OIDC IdP
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), busFile.toString());

        WebClient.getConfig(client).getRequestContext().put(
                org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        Response response = client.get();

        String location = response.getHeaderString("Location");
        // Now make an invocation on the OIDC IdP using another WebClient instance

        WebClient idpClient = WebClient.create(location, OAuth2TestUtils.setupProviders(),
                                               "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(idpClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code + State
        String authzCodeLocation = makeAuthorizationCodeInvocation(idpClient);
        String state = getSubstring(authzCodeLocation, "state");
        assertNotNull(state);
        String code = getSubstring(authzCodeLocation, "code");
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

    private String makeAuthorizationCodeInvocation(WebClient client) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
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
        form.param("state", authzData.getState());
        form.param("oauthDecision", "allow");

        response = client.post(form);
        return response.getHeaderString("Location");
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

}
