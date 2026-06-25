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
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJsonConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProducer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JwsJsonContainerRequestFilterTest {
    private static final String BAD_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
        + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";
    private static final String GOOD_KEY = "09Y_RK7l5rAY9QY7EblYQNuYbu9cy1j7ovCbkeIyAKN8LIeRL-3H8g"
        + "c8kZSYzAQ1uTRC_egZ_8cgZSZa9T5nmQ";

    @Test
    public void testUsesValidatedSignatureMetadata() throws Exception {
        String payload = "{\"role\":\"user\"}";
        String signedDocument = createSignedDocument(payload);

        JwsJsonConsumer consumer = new JwsJsonConsumer(signedDocument);
        assertEquals("application/xml",
                     consumer.getSignatureEntries().get(0).getUnionHeader().getContentType());
        assertEquals("application/json",
                     consumer.getSignatureEntries().get(1).getUnionHeader().getContentType());

        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, "POST");
        message.setContent(java.io.InputStream.class,
            new ByteArrayInputStream(signedDocument.getBytes(StandardCharsets.UTF_8)));

        MetadataMap<String, String> headers = new MetadataMap<>();
        headers.putSingle(HttpHeaders.CONTENT_TYPE, "application/xml");
        headers.putSingle(HttpHeaders.CONTENT_LENGTH,
                          Integer.toString(signedDocument.getBytes(StandardCharsets.UTF_8).length));
        message.put(Message.PROTOCOL_HEADERS, headers);

        JwsJsonContainerRequestFilter filter = new JwsJsonContainerRequestFilter();
        filter.setSignatureVerifier(new HmacJwsSignatureVerifier(GOOD_KEY, SignatureAlgorithm.HS256));
        filter.setValidateHttpHeaders(false);

        runInPhase(message, () -> {
            try {
                ContainerRequestContext context = new ContainerRequestContextImpl(message, true, false);
                filter.filter(context);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        assertNull(message.getExchange().get(Response.class));
        assertEquals("application/json",
                     headers.getFirst(HttpHeaders.CONTENT_TYPE));
        assertEquals(payload,
                     IOUtils.readStringFromStream(message.getContent(java.io.InputStream.class)));
    }

    private String createSignedDocument(String payload) {
        JwsJsonProducer producer = new JwsJsonProducer(payload);
        producer.signWith(new HmacJwsSignatureProvider(BAD_KEY, SignatureAlgorithm.HS256),
                          createHeaders("application/xml"));
        producer.signWith(new HmacJwsSignatureProvider(GOOD_KEY, SignatureAlgorithm.HS256),
                          createHeaders("application/json"));
        return producer.getJwsJsonSignedDocument();
    }

    private JwsHeaders createHeaders(String contentType) {
        JwsHeaders headers = new JwsHeaders();
        headers.setSignatureAlgorithm(SignatureAlgorithm.HS256);
        headers.setContentType(contentType);
        headers.setHeader("http." + HttpHeaders.CONTENT_TYPE, contentType);
        return headers;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void runInPhase(Message message, Runnable action) {
        PhaseInterceptorChain chain = new PhaseInterceptorChain(new PhaseManagerImpl().getInPhases());
        chain.add(new PhaseInterceptor<Message>() {
            @Override
            public void handleMessage(Message message) throws Fault {
                action.run();
            }

            @Override
            public void handleFault(Message message) {
            }

            @Override
            public java.util.Set getAfter() {
                return Collections.emptySet();
            }

            @Override
            public java.util.Set getBefore() {
                return Collections.emptySet();
            }

            @Override
            public String getId() {
                return "test-jws-json-request-filter";
            }

            @Override
            public String getPhase() {
                return Phase.INVOKE;
            }

            @Override
            public java.util.Collection getAdditionalInterceptors() {
                return Collections.emptyList();
            }
        });
        message.setInterceptorChain(chain);
        chain.doIntercept(message);
    }
}
