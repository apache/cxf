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

package org.apache.cxf.systest.jaxrs.security.oauth2.filters;

import java.net.URL;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for the OAuth 2.0 filters
 */
public class OAuth2JwtFiltersTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerOAuth2FiltersJwt.PORT;
    public static final String OAUTH_PORT = BookServerOAuth2ServiceJwt.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2FiltersJwt.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2ServiceJwt.class, true));
    }
    @org.junit.Test
    public void testServiceWithJwtToken() throws Exception {
        String oauthServiceAddress = "https://localhost:" + OAUTH_PORT + "/services/";
        String rsAddress = "https://localhost:" + PORT + "/secured/bookstore/books";
        doTestServiceWithJwtTokenAndScope(oauthServiceAddress, rsAddress);
    }
    @org.junit.Test
    public void testServiceWithJwtTokenStoredAsJoseKey() throws Exception {
        String oauthServiceAddress = "https://localhost:" + OAUTH_PORT + "/services2/";
        String rsAddress = "https://localhost:" + PORT + "/secured2/bookstore/books";
        doTestServiceWithJwtTokenAndScope(oauthServiceAddress, rsAddress);
    }
    @org.junit.Test
    public void testServiceWithJwtTokenAndLocalValidation() throws Exception {
        String oauthServiceAddress = "https://localhost:" + OAUTH_PORT + "/services/";
        String rsAddress = "https://localhost:" + PORT + "/securedLocalValidation/bookstore/books";
        doTestServiceWithJwtTokenAndScope(oauthServiceAddress, rsAddress);
    }
    private void doTestServiceWithJwtTokenAndScope(String oauthService, String rsAddress) throws Exception {
        URL busFile = OAuth2JwtFiltersTest.class.getResource("client.xml");

        // Get Authorization Code


        WebClient oauthClient = WebClient.create(oauthService, OAuth2TestUtils.setupProviders(),
                                                 "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = OAuth2TestUtils.getAuthorizationCode(oauthClient, "create_book");
        assertNotNull(code);

        // Now get the access token
        oauthClient = WebClient.create(oauthService, OAuth2TestUtils.setupProviders(),
                                       "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken =
            OAuth2TestUtils.getAccessTokenWithAuthorizationCode(oauthClient, code);
        assertNotNull(accessToken.getTokenKey());

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(accessToken.getTokenKey());
        JwsSignatureVerifier verifier = JwsUtils.loadSignatureVerifier(
            "org/apache/cxf/systest/jaxrs/security/alice.rs.properties", null);
        assertTrue(jwtConsumer.verifySignatureWith(verifier));
        JwtClaims claims = jwtConsumer.getJwtClaims();
        assertEquals("consumer-id", claims.getStringProperty(OAuthConstants.CLIENT_ID));
        assertEquals("alice", claims.getStringProperty("username"));
        // Now invoke on the service with the access token
        WebClient client = WebClient.create(rsAddress, OAuth2TestUtils.setupProviders(),
                                            busFile.toString());
        client.header("Authorization", "Bearer " + accessToken.getTokenKey());

        Response response = client.type("application/xml").post(new Book("book", 123L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testServiceLocalValidationWithNoToken() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");

        // Now invoke on the service with the faked access token
        String address = "https://localhost:" + PORT + "/securedLocalValidation/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            busFile.toString());

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
}
