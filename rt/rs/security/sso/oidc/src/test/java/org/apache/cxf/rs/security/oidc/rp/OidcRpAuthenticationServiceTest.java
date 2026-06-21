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
package org.apache.cxf.rs.security.oidc.rp;

import java.lang.reflect.Field;
import java.net.URI;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContextManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcRpAuthenticationServiceTest {

    private static final String BASE_PATH = "https://app.example.com:8080/services/";

    @Test
    public void testRejectsCrossOriginRedirectFromState() {
        Response response = complete("https://evil.example.com/phish");
        assertEquals(200, response.getStatus());
        assertNull(response.getLocation());
    }

    @Test
    public void testRejectsProtocolRelativeRedirectFromState() {
        Response response = complete("//evil.example.com/phish");
        assertEquals(200, response.getStatus());
        assertNull(response.getLocation());
    }

    @Test
    public void testRejectsUserinfoHostConfusionFromState() {
        Response response = complete("https://app.example.com:8080@evil.example.com/phish");
        assertEquals(200, response.getStatus());
        assertNull(response.getLocation());
    }

    @Test
    public void testAllowsSameOriginRedirectFromState() {
        Response response = complete("https://app.example.com:8080/services/protected");
        assertEquals(303, response.getStatus());
        assertEquals(URI.create("https://app.example.com:8080/services/protected"), response.getLocation());
    }

    @Test
    public void testAllowsRelativeRedirectFromState() {
        Response response = complete("/services/protected");
        assertEquals(303, response.getStatus());
        assertEquals(URI.create("/services/protected"), response.getLocation());
    }

    private Response complete(String stateLocation) {
        MessageContext messageContext = mock(MessageContext.class);
        when(messageContext.get("http.base.path")).thenReturn(BASE_PATH);

        MultivaluedMap<String, String> state = new MultivaluedHashMap<>();
        state.putSingle("state", stateLocation);

        OidcClientTokenContext tokenContext = mock(OidcClientTokenContext.class);
        when(tokenContext.getState()).thenReturn(state);

        OidcRpAuthenticationService service = new OidcRpAuthenticationService();
        service.setClientTokenContextManager(mock(ClientTokenContextManager.class));
        setMessageContext(service, messageContext);

        return service.completeAuthentication(tokenContext);
    }

    private static void setMessageContext(OidcRpAuthenticationService service, MessageContext messageContext) {
        try {
            Field field = OidcRpAuthenticationService.class.getDeclaredField("mc");
            field.setAccessible(true);
            field.set(service, messageContext);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
