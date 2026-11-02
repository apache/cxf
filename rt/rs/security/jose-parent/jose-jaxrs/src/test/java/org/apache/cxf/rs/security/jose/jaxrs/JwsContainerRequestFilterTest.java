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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JwsContainerRequestFilterTest {
    private static final String BAD_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
        + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";
    private static final String GOOD_KEY = "09Y_RK7l5rAY9QY7EblYQNuYbu9cy1j7ovCbkeIyAKN8LIeRL-3H8g"
        + "c8kZSYzAQ1uTRC_egZ_8cgZSZa9T5nmQ";
    private static final String PAYLOAD = "{\"id\":\"123\"}";

    @After
    public void tearDown() throws Exception {
        setThreadLocalMessage(null);
    }

    @Test
    public void testDeleteBodyIsVerified() throws Exception {
        Message message = createMessage("DELETE", sign(PAYLOAD, GOOD_KEY));
        createFilter().filter(new ContainerRequestContextImpl(message, true, false));

        assertNull(message.getExchange().get(Response.class));
        assertEquals(PAYLOAD, IOUtils.readStringFromStream(message.getContent(InputStream.class)));
    }

    @Test
    public void testDeleteBodySignedWithWrongKeyIsRejected() throws Exception {
        Message message = createMessage("DELETE", sign(PAYLOAD, BAD_KEY));
        createFilter().filter(new ContainerRequestContextImpl(message, true, false));

        Response response = message.getExchange().get(Response.class);
        assertNotNull(response);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testGetWithoutBodyIsAccepted() throws Exception {
        Message message = createMessage("GET", "");
        createFilter().filter(new ContainerRequestContextImpl(message, true, false));

        assertNull(message.getExchange().get(Response.class));
    }

    private static JwsContainerRequestFilter createFilter() {
        JwsContainerRequestFilter filter = new JwsContainerRequestFilter();
        filter.setSignatureVerifier(new HmacJwsSignatureVerifier(GOOD_KEY, SignatureAlgorithm.HS256));
        filter.setValidateHttpHeaders(false);
        return filter;
    }

    private static String sign(String payload, String key) {
        JwsHeaders headers = new JwsHeaders(SignatureAlgorithm.HS256);
        headers.setContentType("application/json");
        return new JwsCompactProducer(headers, payload)
            .signWith(new HmacJwsSignatureProvider(key, SignatureAlgorithm.HS256));
    }

    private static Message createMessage(String httpMethod, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, httpMethod);
        message.setContent(InputStream.class, new ByteArrayInputStream(bytes));
        MetadataMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", "application/jose");
        headers.putSingle("Content-Length", Integer.toString(bytes.length));
        message.put(Message.PROTOCOL_HEADERS, headers);
        setThreadLocalMessage(message);
        return message;
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
