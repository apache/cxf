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

import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.client.ClientResponseContext;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;

import org.junit.Test;

import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.clientRequest;
import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.clientResponse;
import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.clientResponseWithoutEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class JwsClientResponseFilterTest {
    private static final String BAD_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
        + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";
    private static final String GOOD_KEY = "09Y_RK7l5rAY9QY7EblYQNuYbu9cy1j7ovCbkeIyAKN8LIeRL-3H8g"
        + "c8kZSYzAQ1uTRC_egZ_8cgZSZa9T5nmQ";
    private static final String PAYLOAD = "{\"id\":\"123\"}";

    @Test
    public void testDeleteResponseIsVerified() throws Exception {
        ClientResponseContext res = clientResponse(sign(PAYLOAD, GOOD_KEY));
        createFilter().filter(clientRequest("DELETE"), res);

        assertEquals(PAYLOAD, IOUtils.readStringFromStream(res.getEntityStream()));
    }

    @Test
    public void testDeleteResponseSignedWithWrongKeyIsRejected() throws Exception {
        ClientResponseContext res = clientResponse(sign(PAYLOAD, BAD_KEY));
        try {
            createFilter().filter(clientRequest("DELETE"), res);
            fail("Failure expected on a DELETE response signed with the wrong key");
        } catch (JwsException ex) {
            // expected
        }
    }

    @Test
    public void testEmptyDeleteResponseIsAccepted() throws Exception {
        ClientResponseContext res = clientResponse(new byte[0]);
        createFilter().filter(clientRequest("DELETE"), res);

        assertEquals("", IOUtils.readStringFromStream(res.getEntityStream()));
    }

    @Test
    public void testCheckEmptyStreamDoesNotTrustZeroContentLength() throws Exception {
        ClientResponseContext res = clientResponse(sign(PAYLOAD, BAD_KEY), "0");
        JwsClientResponseFilter filter = createFilter();
        filter.setCheckEmptyStream(true);
        try {
            filter.filter(clientRequest("POST"), res);
            fail("Failure expected on a body signed with the wrong key despite Content-Length: 0");
        } catch (JwsException ex) {
            // expected
        }
    }

    @Test
    public void testResponseWithoutEntityIsAccepted() throws Exception {
        ClientResponseContext res = clientResponseWithoutEntity();
        createFilter().filter(clientRequest("GET"), res);

        assertNull(res.getEntityStream());
    }

    private static JwsClientResponseFilter createFilter() {
        JwsClientResponseFilter filter = new JwsClientResponseFilter();
        filter.setSignatureVerifier(new HmacJwsSignatureVerifier(GOOD_KEY, SignatureAlgorithm.HS256));
        filter.setValidateHttpHeaders(false);
        return filter;
    }

    private static byte[] sign(String payload, String key) {
        JwsHeaders headers = new JwsHeaders(SignatureAlgorithm.HS256);
        headers.setContentType("application/json");
        return new JwsCompactProducer(headers, payload)
            .signWith(new HmacJwsSignatureProvider(key, SignatureAlgorithm.HS256))
            .getBytes(StandardCharsets.UTF_8);
    }
}
