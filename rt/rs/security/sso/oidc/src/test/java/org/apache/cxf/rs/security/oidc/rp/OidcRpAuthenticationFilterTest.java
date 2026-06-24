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
import java.lang.reflect.Method;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.MessageContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcRpAuthenticationFilterTest {

    private static final String BASE_PATH = "https://app.example.com:8080/services/";

    @Test
    public void testDropsCrossOriginState() {
        MultivaluedMap<String, String> state = requestState("https://evil.example.com/phish");
        assertFalse(state.containsKey("state"));
    }

    @Test
    public void testDropsProtocolRelativeState() {
        MultivaluedMap<String, String> state = requestState("//evil.example.com/phish");
        assertFalse(state.containsKey("state"));
    }

    @Test
    public void testDropsUserinfoHostConfusionState() {
        MultivaluedMap<String, String> state = requestState("https://app.example.com:8080@evil.example.com/phish");
        assertFalse(state.containsKey("state"));
    }

    @Test
    public void testKeepsSameOriginState() {
        MultivaluedMap<String, String> state = requestState("https://app.example.com:8080/services/protected");
        assertTrue(state.containsKey("state"));
        assertEquals("https://app.example.com:8080/services/protected", state.getFirst("state"));
    }

    @Test
    public void testKeepsRelativeState() {
        MultivaluedMap<String, String> state = requestState("/services/protected");
        assertTrue(state.containsKey("state"));
        assertEquals("/services/protected", state.getFirst("state"));
    }

    private MultivaluedMap<String, String> requestState(String stateLocation) {
        MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        query.putSingle("state", stateLocation);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters(true)).thenReturn(query);

        ContainerRequestContext rc = mock(ContainerRequestContext.class);
        when(rc.getUriInfo()).thenReturn(uriInfo);
        when(rc.getMediaType()).thenReturn(null);

        MessageContext messageContext = mock(MessageContext.class);
        when(messageContext.get("http.base.path")).thenReturn(BASE_PATH);

        OidcRpAuthenticationFilter filter = new OidcRpAuthenticationFilter();
        setMessageContext(filter, messageContext);

        return invokeToRequestState(filter, rc);
    }

    @SuppressWarnings("unchecked")
    private static MultivaluedMap<String, String> invokeToRequestState(OidcRpAuthenticationFilter filter,
                                                                       ContainerRequestContext rc) {
        try {
            Method method = OidcRpAuthenticationFilter.class.getDeclaredMethod("toRequestState",
                                                                              ContainerRequestContext.class);
            method.setAccessible(true);
            return (MultivaluedMap<String, String>)method.invoke(filter, rc);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void setMessageContext(OidcRpAuthenticationFilter filter, MessageContext messageContext) {
        try {
            Field field = OidcRpAuthenticationFilter.class.getDeclaredField("mc");
            field.setAccessible(true);
            field.set(filter, messageContext);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
