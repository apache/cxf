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
package org.apache.cxf.rs.security.oauth2.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class DynamicRegistrationServiceTest {

    @Test
    public void testRejectsUnallowedRegisteredScope() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();
        service.setAllowedClientScopes(Collections.singletonList("read"));

        ClientRegistration request = new ClientRegistration();
        request.setScope("admin read");

        Client client = createClient();

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> service.applyClientRegistration(request, client));

        assertNotNull(ex.getResponse());
        OAuthError error = (OAuthError)ex.getResponse().getEntity();
        assertNotNull(error);
        assertEquals("invalid_client_metadata", error.getError());
    }

    @Test
    public void testAcceptsAllowedRegisteredScopes() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();
        service.setAllowedClientScopes(Arrays.asList("read", "write"));

        ClientRegistration request = new ClientRegistration();
        request.setScope("read write");

        Client client = createClient();
        service.applyClientRegistration(request, client);

        assertEquals(Arrays.asList("read", "write"), client.getRegisteredScopes());
    }

    @Test
    public void testAcceptsRegisteredScopesWhenAllowlistNotConfigured() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        ClientRegistration request = new ClientRegistration();
        request.setScope("openid");

        Client client = createClient();
        service.applyClientRegistration(request, client);

        assertEquals(Collections.singletonList("openid"), client.getRegisteredScopes());
    }

    @Test
    public void testAcceptsAllowedRedirectUrlsWebApp() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        ClientRegistration request = new ClientRegistration();
        request.setScope("read write");
        request.setRedirectUris(List.of("https://localhost", "http://localhost"));

        Client client = createClient();
        service.applyClientRegistration(request, client);

        assertEquals(Arrays.asList("https://localhost", "http://localhost"), client.getRedirectUris());
    }

    @Test
    public void testRejectsNotAllowedRedirectUrlsWebApp() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        final List<String> schemes = List.of("http", "https");
        for (String scheme: schemes) {
            ClientRegistration request = new ClientRegistration();
            request.setScope("read write");
            request.setRedirectUris(List.of(scheme + "://localhost"));
    
            Client client = createClient();
            client.setAllowedGrantTypes(Collections.singletonList(OAuthConstants.IMPLICIT_GRANT));
            assertThrows(BadRequestException.class, () -> service.applyClientRegistration(request, client));
        }
    }

    @Test
    public void testAcceptsAllowedRedirectUrlsNativeApp() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        final List<String> hosts = List.of("localhost", "127.0.0.1", "[::1]");
        for (String host: hosts) {
            ClientRegistration request = new ClientRegistration();
            request.setScope("read write");
            request.setRedirectUris(List.of("http://" + host));
            request.setApplicationType("native");

            Client client = createClient();
            service.applyClientRegistration(request, client);

            assertEquals(Arrays.asList("http://" + host), client.getRedirectUris());
        }
    }

    @Test
    public void testRejectsNotAllowedRedirectUrlsNativeApp() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        ClientRegistration request = new ClientRegistration();
        request.setScope("read write");
        request.setRedirectUris(List.of("http://test"));
        request.setApplicationType("native");

        Client client = createClient();
        assertThrows(BadRequestException.class, () -> service.applyClientRegistration(request, client));
    }

    @Test
    public void testRejectsNotAllowedRedirectUrls() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        final List<String> uris = List.of("custom://test", "//test", "");
        for (String uri: uris) {
            ClientRegistration request = new ClientRegistration();
            request.setScope("read write");
            request.setRedirectUris(List.of(uri));
    
            Client client = createClient();
            assertThrows(BadRequestException.class, () -> service.applyClientRegistration(request, client));
        }
    }

    private static Client createClient() {
        Client client = new Client("client", "secret", true);
        client.setAllowedGrantTypes(Collections.singletonList(OAuthConstants.CLIENT_CREDENTIALS_GRANT));
        return client;
    }

    private static final class TestDynamicRegistrationService extends DynamicRegistrationService {
        void applyClientRegistration(ClientRegistration request, Client client) {
            fromClientRegistrationToClient(request, client);
        }
    }
}
