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

package org.apache.cxf.rs.security.saml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class SamlFormInHandlerTest {

    private static final String BASE_ADDRESS = "https://sp.example.com/services";
    private static final String ALLOWED_ORIGIN = "https://app.example.com";

    @After
    public void tearDown() throws Exception {
        setThreadLocalMessage(null);
    }

    @Test
    public void testRelayStateOnAnotherHostIsNotFollowed() throws Exception {
        assertNull(filter("https://evil.example.com/"));
    }

    @Test
    public void testRelayStateWithUserInfoIsNotFollowed() throws Exception {
        assertNull(filter("https://sp.example.com@evil.example.com/"));
        assertNull(filter("https://user@sp.example.com/app/page"));
    }

    @Test
    public void testProtocolRelativeRelayStateIsNotFollowed() throws Exception {
        assertNull(filter("//evil.example.com/"));
    }

    @Test
    public void testRelayStateOnAnotherPortIsNotFollowed() throws Exception {
        assertNull(filter("https://sp.example.com:8443/app/page"));
    }

    @Test
    public void testRelayStateWithExplicitDefaultPortIsFollowed() throws Exception {
        Response r = filter("https://sp.example.com:443/app/page");
        assertNotNull(r);
        assertEquals(302, r.getStatus());
        assertEquals("https://sp.example.com:443/app/page", r.getLocation().toString());
    }

    @Test
    public void testRelayStateOnAllowedOriginIsFollowed() throws Exception {
        Response r = filter(ALLOWED_ORIGIN + "/app/page", ALLOWED_ORIGIN);
        assertNotNull(r);
        assertEquals(302, r.getStatus());
        assertEquals(ALLOWED_ORIGIN + "/app/page", r.getLocation().toString());
    }

    @Test
    public void testRelayStateOnLookalikeOriginIsNotFollowed() throws Exception {
        assertNull(filter("https://app.example.com@evil.example.com/", ALLOWED_ORIGIN));
        assertNull(filter("https://app.example.com.evil.example.com/", ALLOWED_ORIGIN));
        assertNull(filter("http://app.example.com/", ALLOWED_ORIGIN));
        assertNull(filter("http://app.example.com:443/", ALLOWED_ORIGIN));
        assertNull(filter("https://app.example.com:8443/", ALLOWED_ORIGIN));
    }

    @Test
    public void testInvalidAllowedOriginIsRejected() {
        SamlFormInHandler handler = new SamlFormInHandler();
        for (String origin : Arrays.asList(null, "app.example.com", "https://app.example.com/",
            "https://app.example.com/app", "https://user@app.example.com", "https://app.example.com?a=b",
            "https://app.example.com#a", "https://my_service", "mailto:admin@app.example.com", "https://[")) {
            assertThrows(origin, IllegalArgumentException.class,
                () -> handler.setAllowedRelayStateOrigins(Arrays.asList(origin)));
        }
    }

    @Test
    public void testValidAllowedOriginsAreAccepted() {
        SamlFormInHandler handler = new SamlFormInHandler();
        handler.setAllowedRelayStateOrigins(null);
        handler.setAllowedRelayStateOrigins(Arrays.asList(ALLOWED_ORIGIN, "https://app.example.com:8443"));
    }

    @Test
    public void testSameOriginRelayStateIsFollowed() throws Exception {
        Response r = filter("https://sp.example.com/app/page");
        assertNotNull(r);
        assertEquals(302, r.getStatus());
        assertEquals("https://sp.example.com/app/page", r.getLocation().toString());
    }

    @Test
    public void testRelativeRelayStateIsFollowed() throws Exception {
        Response r = filter("/app/page");
        assertNotNull(r);
        assertEquals(302, r.getStatus());
        assertEquals(BASE_ADDRESS + "/app/page", r.getLocation().toString());
    }

    @Test
    public void testRelayStateForThisServiceIsNotRedirected() throws Exception {
        assertNull(filter(BASE_ADDRESS + "/resource"));
    }

    private static Response filter(String relayState, String... allowedOrigins) throws Exception {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.getExchange().setInMessage(message);
        message.put(Message.ENDPOINT_ADDRESS, BASE_ADDRESS);
        // the handler keeps form values encoded, so the RelayState is sent as is
        String form = "SAMLToken=token&RelayState=" + relayState;
        message.setContent(InputStream.class, new ByteArrayInputStream(form.getBytes(StandardCharsets.UTF_8)));
        setThreadLocalMessage(message);

        SamlFormInHandler handler = new SamlFormInHandler() {
            @Override
            protected void handleToken(Message m, String assertion) {
                // the assertion is assumed to be valid
            }
        };
        handler.setAllowedRelayStateOrigins(Arrays.asList(allowedOrigins));
        handler.filter(new ContainerRequestContextImpl(message, false, false));
        return message.getExchange().get(Response.class);
    }

    private static void setThreadLocalMessage(Message message) throws Exception {
        Field f = PhaseInterceptorChain.class.getDeclaredField("CURRENT_MESSAGE");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Message> tl = (ThreadLocal<Message>) f.get(null);
        if (message == null) {
            tl.remove();
        } else {
            tl.set(message);
        }
    }
}
