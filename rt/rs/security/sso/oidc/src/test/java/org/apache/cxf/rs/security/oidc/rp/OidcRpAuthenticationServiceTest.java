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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContextManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcRpAuthenticationServiceTest {
    private static final URI REQUEST_URI = URI.create("https://app.example.com:8080/services/rp/complete");

    @Test
    public void testRejectsCrossOriginRedirect() {
        Response response = completeWithState("https://evil.example.com/phish");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString("Location"));
    }

    @Test
    public void testRejectsProtocolRelativeRedirect() {
        Response response = completeWithState("//evil.example.com/phish");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString("Location"));
    }

    @Test
    public void testRejectsDoubleEncodedCrossOriginRedirect() {
        String attackerLocation = "https%253A%252F%252Fevil.example.com%252Fphish";
        String callbackLocation = UrlUtils.urlDecode(attackerLocation);
        Response response = completeWithState(callbackLocation);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString("Location"));
    }

    @Test
    public void testAllowsSameOriginAbsoluteRedirect() {
        Response response = completeWithState("https://app.example.com:8080/services/protected");

        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        assertEquals("https://app.example.com:8080/services/protected",
                     response.getHeaderString("Location"));
    }

    @Test
    public void testAllowsRelativeRedirect() {
        Response response = completeWithState("/services/protected");

        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        assertEquals("/services/protected", response.getHeaderString("Location"));
    }

    private Response completeWithState(String location) {
        OidcClientTokenContext context = new OidcClientTokenContextImpl();
        MultivaluedHashMap<String, String> state = new MultivaluedHashMap<>();
        state.putSingle("state", location);
        ((OidcClientTokenContextImpl)context).setState(state);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getAbsolutePath()).thenReturn(REQUEST_URI);
        MessageImpl message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        MessageContext messageContext = new MessageContextImpl(message) {
            @Override
            public UriInfo getUriInfo() {
                return uriInfo;
            }
        };

        OidcRpAuthenticationService service = new OidcRpAuthenticationService();
        service.setClientTokenContextManager(mock(ClientTokenContextManager.class));
        setMessageContext(service, messageContext);
        return service.completeAuthentication(context);
    }

    private void setMessageContext(OidcRpAuthenticationService service,
                                   org.apache.cxf.jaxrs.ext.MessageContext messageContext) {
        try {
            Field field = OidcRpAuthenticationService.class.getDeclaredField("mc");
            field.setAccessible(true);
            field.set(service, messageContext);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
