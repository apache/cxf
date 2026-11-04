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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;

import org.junit.Test;

import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.runInPhase;
import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.serverRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JwsContainerRequestFilterTest {
    private static final String BAD_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
        + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";
    private static final String GOOD_KEY = "09Y_RK7l5rAY9QY7EblYQNuYbu9cy1j7ovCbkeIyAKN8LIeRL-3H8g"
        + "c8kZSYzAQ1uTRC_egZ_8cgZSZa9T5nmQ";
    private static final String PAYLOAD = "{\"id\":\"123\"}";

    @Test
    public void testDeleteBodyIsVerified() throws Exception {
        Message message = serverRequest("DELETE", sign(PAYLOAD, GOOD_KEY));
        filter(message);

        assertNull(message.getExchange().get(Response.class));
        assertEquals(PAYLOAD, IOUtils.readStringFromStream(message.getContent(InputStream.class)));
    }

    @Test
    public void testDeleteBodySignedWithWrongKeyIsRejected() throws Exception {
        Message message = serverRequest("DELETE", sign(PAYLOAD, BAD_KEY));
        filter(message);

        assertRejected(message);
    }

    @Test
    public void testZeroContentLengthDoesNotSkipVerification() throws Exception {
        // the header claims there is no body but the stream has one: the stream decides
        Message message = serverRequest("DELETE", sign(PAYLOAD, BAD_KEY), "0");
        filter(message);

        assertRejected(message);
    }

    @Test
    public void testGetWithoutBodyIsAccepted() throws Exception {
        Message message = serverRequest("GET", new byte[0]);
        filter(message);

        assertNull(message.getExchange().get(Response.class));
    }

    private static void filter(Message message) throws Exception {
        JwsContainerRequestFilter filter = new JwsContainerRequestFilter();
        filter.setSignatureVerifier(new HmacJwsSignatureVerifier(GOOD_KEY, SignatureAlgorithm.HS256));
        filter.setValidateHttpHeaders(false);
        runInPhase(message, () -> filter.filter(new ContainerRequestContextImpl(message, true, false)));
    }

    private static void assertRejected(Message message) {
        Response response = message.getExchange().get(Response.class);
        assertNotNull(response);
        assertEquals(400, response.getStatus());
    }

    private static byte[] sign(String payload, String key) {
        JwsHeaders headers = new JwsHeaders(SignatureAlgorithm.HS256);
        headers.setContentType("application/json");
        return new JwsCompactProducer(headers, payload)
            .signWith(new HmacJwsSignatureProvider(key, SignatureAlgorithm.HS256))
            .getBytes(StandardCharsets.UTF_8);
    }
}
