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
