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

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.TokenIntrospection;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.junit.BeforeClass;

/**
 * Some unit tests for the token introspection service in CXF.
 */
public class IntrospectionServiceTest extends AbstractBusClientServerTestBase {
    
    public static final String PORT = BookServerOAuth2Introspection.PORT;
    public static final String PORT2 = TestUtil.getPortNumber("jaxrs-oauth2-introspection2");
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerOAuth2Introspection.class, true));
    }
    
    @org.junit.Test
    public void testTokenIntrospection() throws Exception {
        URL busFile = IntrospectionServiceTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
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
        assertEquals(tokenIntrospection.isActive(), true);
        assertEquals(tokenIntrospection.getUsername(), "alice");
        assertEquals(tokenIntrospection.getClientId(), "consumer-id");
        assertEquals(tokenIntrospection.getScope(), accessToken.getApprovedScope());
        Long validity = tokenIntrospection.getExp() - tokenIntrospection.getIat();
        assertTrue(validity == accessToken.getExpiresIn());
    }
    
    @org.junit.Test
    public void testTokenIntrospectionWithAudience() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
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
        
        String audience = "https://localhost:" + PORT2 + "/secured/bookstore/books";
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
        assertEquals(tokenIntrospection.isActive(), true);
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
        
        String address = "https://localhost:" + PORT + "/services/";
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
        assertEquals(tokenIntrospection.isActive(), false);
    }
    
    @org.junit.Test
    public void testRefreshedToken() throws Exception {
        URL busFile = AuthorizationGrantTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
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
        assertEquals(tokenIntrospection.isActive(), true);
        
        // Original token should not be ok
        form = new Form();
        form.param("token", originalAccessToken);
        response = client.post(form);
        
        tokenIntrospection = response.readEntity(TokenIntrospection.class);
        assertEquals(tokenIntrospection.isActive(), false);
    }
    
    @org.junit.Test
    public void testTokenIntrospectionWithScope() throws Exception {
        URL busFile = IntrospectionServiceTest.class.getResource("client.xml");
        
        String address = "https://localhost:" + PORT + "/services/";
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
        assertEquals(tokenIntrospection.isActive(), true);
        assertEquals(tokenIntrospection.getUsername(), "alice");
        assertEquals(tokenIntrospection.getClientId(), "consumer-id");
        assertEquals(tokenIntrospection.getScope(), accessToken.getApprovedScope());
        Long validity = tokenIntrospection.getExp() - tokenIntrospection.getIat();
        assertTrue(validity == accessToken.getExpiresIn());
    }
    
}
