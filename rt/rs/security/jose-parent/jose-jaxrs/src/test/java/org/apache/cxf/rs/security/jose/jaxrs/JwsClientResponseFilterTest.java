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
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JwsClientResponseFilterTest {
    private static final String BAD_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
        + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";
    private static final String GOOD_KEY = "09Y_RK7l5rAY9QY7EblYQNuYbu9cy1j7ovCbkeIyAKN8LIeRL-3H8g"
        + "c8kZSYzAQ1uTRC_egZ_8cgZSZa9T5nmQ";
    private static final String PAYLOAD = "{\"id\":\"123\"}";

    @Test
    public void testDeleteResponseIsVerified() throws Exception {
        InputStream[] entity = {stream(sign(PAYLOAD, GOOD_KEY))};
        createFilter().filter(request("DELETE"), response(entity));

        assertEquals(PAYLOAD, IOUtils.readStringFromStream(entity[0]));
    }

    @Test
    public void testDeleteResponseSignedWithWrongKeyIsRejected() throws Exception {
        InputStream[] entity = {stream(sign(PAYLOAD, BAD_KEY))};
        try {
            createFilter().filter(request("DELETE"), response(entity));
            fail("Failure expected on a DELETE response signed with the wrong key");
        } catch (JwsException ex) {
            // expected
        }
    }

    private static JwsClientResponseFilter createFilter() {
        JwsClientResponseFilter filter = new JwsClientResponseFilter();
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

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static ClientRequestContext request(String httpMethod) {
        return (ClientRequestContext)Proxy.newProxyInstance(
            JwsClientResponseFilterTest.class.getClassLoader(),
            new Class<?>[] {ClientRequestContext.class},
            (proxy, method, args) -> "getMethod".equals(method.getName()) ? httpMethod : null);
    }

    private static ClientResponseContext response(InputStream[] entity) {
        MetadataMap<String, String> headers = new MetadataMap<>();
        return (ClientResponseContext)Proxy.newProxyInstance(
            JwsClientResponseFilterTest.class.getClassLoader(),
            new Class<?>[] {ClientResponseContext.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                case "getStatus":
                    return 200;
                case "hasEntity":
                    return !IOUtils.isEmpty(entity[0]);
                case "getEntityStream":
                    return entity[0];
                case "setEntityStream":
                    entity[0] = (InputStream)args[0];
                    return null;
                case "getHeaders":
                    return headers;
                default:
                    throw new UnsupportedOperationException(method.getName());
                }
            });
    }
}
