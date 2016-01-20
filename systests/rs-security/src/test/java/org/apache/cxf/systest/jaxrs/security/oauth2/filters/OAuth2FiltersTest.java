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
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

/**
 * Some tests for the OAuth 2.0 filters
 */
public class OAuth2FiltersTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerOAuth2Filters.PORT;
    public static final String OAUTH_PORT = BookServerOAuth2Service.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerOAuth2Filters.class, true));
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerOAuth2Service.class, true));
    }

    @org.junit.Test
    public void testServiceWithToken() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");
        
        // Get Authorization Code
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

        WebClient oauthClient = WebClient.create(oauthService, OAuth2TestUtils.setupProviders(), 
                                                 "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = OAuth2TestUtils.getAuthorizationCode(oauthClient);
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

        // Now invoke on the service with the access token
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            busFile.toString());
        client.header("Authorization", "Bearer " + accessToken.getTokenKey());
        
        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
        
        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }
    
    @org.junit.Test
    public void testServiceWithFakeToken() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");
        
        // Now invoke on the service with the faked access token
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            busFile.toString());
        client.header("Authorization", "Bearer " + UUID.randomUUID().toString());
        
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testServiceWithNoToken() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");
        
        // Now invoke on the service with the faked access token
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            busFile.toString());
        
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testServiceWithEmptyToken() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");
        
        // Now invoke on the service with the faked access token
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            busFile.toString());
        client.header("Authorization", "Bearer ");
        
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testServiceWithTokenAndScope() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");
        
        // Get Authorization Code
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

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

        // Now invoke on the service with the access token
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(),
                                            busFile.toString());
        client.header("Authorization", "Bearer " + accessToken.getTokenKey());
        
        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
        
        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }
    
    @org.junit.Test
    public void testServiceWithTokenAndIncorrectScopeVerb() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");
        
        // Get Authorization Code
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

        WebClient oauthClient = WebClient.create(oauthService, OAuth2TestUtils.setupProviders(), 
                                                 "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = OAuth2TestUtils.getAuthorizationCode(oauthClient, "read_book");
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

        // Now invoke on the service with the access token
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            busFile.toString());
        client.header("Authorization", "Bearer " + accessToken.getTokenKey());

        // We don't have the scope to post a book here
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testServiceWithTokenAndIncorrectScopeURI() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");
        
        // Get Authorization Code
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

        WebClient oauthClient = WebClient.create(oauthService, OAuth2TestUtils.setupProviders(), 
                                                 "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = OAuth2TestUtils.getAuthorizationCode(oauthClient, "create_image");
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

        // Now invoke on the service with the access token
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            busFile.toString());
        client.header("Authorization", "Bearer " + accessToken.getTokenKey());

        // We don't have the scope to post a book here
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testServiceWithTokenAndMultipleScopes() throws Exception {
        URL busFile = OAuth2FiltersTest.class.getResource("client.xml");
        
        // Get Authorization Code
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

        WebClient oauthClient = WebClient.create(oauthService, OAuth2TestUtils.setupProviders(), 
                                                 "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = OAuth2TestUtils.getAuthorizationCode(oauthClient, 
                                                           "read_book create_image create_book");
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

        // Now invoke on the service with the access token
        String address = "https://localhost:" + PORT + "/secured/bookstore/books";
        WebClient client = WebClient.create(address, OAuth2TestUtils.setupProviders(), 
                                            busFile.toString());
        client.header("Authorization", "Bearer " + accessToken.getTokenKey());
        
        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
        
        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

}
