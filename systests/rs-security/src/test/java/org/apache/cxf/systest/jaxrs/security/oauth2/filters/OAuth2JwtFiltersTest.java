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

import java.net.URI;
import java.util.Collections;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.services.AuthorizationMetadata;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;

import org.junit.BeforeClass;

import static org.apache.cxf.rs.security.oauth2.utils.OAuthConstants.BEARER_AUTHORIZATION_SCHEME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for the OAuth 2.0 filters
 */
public class OAuth2JwtFiltersTest extends AbstractBusClientServerTestBase {
    private static final String PORT = TestUtil.getPortNumber("jaxrs-oauth2-filtersJwt");
    private static final String OAUTH_PORT = TestUtil.getPortNumber("jaxrs-oauth2-serviceJwt");

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus().setExtension(OAuth2TestUtils.clientHTTPConduitConfigurer(), HTTPConduitConfigurer.class);
        getStaticBus().setFeatures(Collections.singletonList(new org.apache.cxf.ext.logging.LoggingFeature()));

        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2FiltersJwt.class));
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2ServiceJwt.class));
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

    @org.junit.Test
    public void testServiceWithJwtTokenAndJwsJwksValidation() throws Exception {
        String oauthServiceAddress = "https://localhost:" + OAUTH_PORT + "/services/";
        String rsAddress = "https://localhost:" + PORT + "/securedJwsJwksValidation/bookstore/books";
        doTestServiceWithJwtTokenAndScope(oauthServiceAddress, rsAddress);
    }

    private void doTestServiceWithJwtTokenAndScope(String oauthService, String rsAddress) throws Exception {
        final AuthorizationMetadata authorizationMetadata = OAuthClientUtils.getAuthorizationMetadata(oauthService);

        final String scope = "create_book";

        final URI authorizationURI = OAuthClientUtils.getAuthorizationURI(
            authorizationMetadata.getAuthorizationEndpoint().toString(),
            "consumer-id", null, null, scope);

        // Get Authorization Code
        WebClient oauthClient = WebClient.create(authorizationURI.toString(), OAuth2TestUtils.setupProviders(),
                                                 "alice", "security", null);
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        final String location = OAuth2TestUtils.getLocation(oauthClient,
            oauthClient.accept(MediaType.APPLICATION_JSON).get(OAuthAuthorizationData.class),
            null);
        final String code = OAuth2TestUtils.getSubstring(location, "code");
        assertNotNull(code);

        // Now get the access token
        final ClientAccessToken accessToken = OAuthClientUtils.getAccessToken(
            authorizationMetadata.getTokenEndpoint().toString(),
            new Consumer("consumer-id", "this-is-a-secret"),
            new AuthorizationCodeGrant(code),
            true);
        assertNotNull(accessToken.getTokenKey());

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(accessToken.getTokenKey());
        JwsSignatureVerifier verifier = JwsUtils.loadSignatureVerifier(
            "org/apache/cxf/systest/jaxrs/security/alice.rs.properties", null);
        assertTrue(jwtConsumer.verifySignatureWith(verifier));
        JwtClaims claims = jwtConsumer.getJwtClaims();
        assertEquals("consumer-id", claims.getStringProperty(OAuthConstants.CLIENT_ID));
        assertEquals("alice", claims.getStringProperty("username"));
        assertTrue(claims.getStringProperty(OAuthConstants.SCOPE).contains(scope));
        // Now invoke on the service with the access token
        WebClient client = WebClient.create(rsAddress, OAuth2TestUtils.setupProviders())
            .authorization(new ClientAccessToken(BEARER_AUTHORIZATION_SCHEME, accessToken.getTokenKey()));

        Book returnedBook = client.type("application/xml").post(new Book("book", 123L), Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testServiceLocalValidationWithNoToken() throws Exception {
        // Now invoke on the service with the faked access token
        String address = "https://localhost:" + PORT + "/securedLocalValidation/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders());

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    //
    // Server implementations
    //
    public static class BookServerOAuth2FiltersJwt extends AbstractBusTestServerBase {
        @Override
        protected void run() {
            setBus(new SpringBusFactory().createBus(getClass().getResource("filters-serverJwt.xml")));
        }
    }

    public static class BookServerOAuth2ServiceJwt extends AbstractBusTestServerBase {
        protected void run() {
            setBus(new SpringBusFactory().createBus(getClass().getResource("oauth20-serverJwt.xml")));
        }
    }

}
