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
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweJsonProducer;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;

import org.junit.Test;

import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.clientRequest;
import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.clientResponse;
import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.clientResponseWithoutEntity;
import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.runInPhase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JweJsonClientResponseFilterTest {
    private static final byte[] KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final String PAYLOAD = "{\"id\":\"123\"}";

    @Test
    public void testDeleteResponseIsDecrypted() throws Exception {
        ClientResponseContext res = clientResponse(encrypt(PAYLOAD));

        JweJsonClientResponseFilter filter = new JweJsonClientResponseFilter();
        filter.setDecryptionProvider(JweUtils.getDirectKeyJweDecryption(KEY, ContentAlgorithm.A128GCM));
        runInPhase(() -> filter.filter(clientRequest("DELETE"), res));

        assertEquals(PAYLOAD, IOUtils.readStringFromStream(res.getEntityStream()));
    }

    @Test
    public void testResponseWithoutEntityIsAccepted() throws Exception {
        ClientResponseContext res = clientResponseWithoutEntity();

        JweJsonClientResponseFilter filter = new JweJsonClientResponseFilter();
        filter.setDecryptionProvider(JweUtils.getDirectKeyJweDecryption(KEY, ContentAlgorithm.A128GCM));
        runInPhase(() -> filter.filter(clientRequest("GET"), res));

        assertNull(res.getEntityStream());
    }

    private static byte[] encrypt(String payload) {
        JweHeaders headers = new JweHeaders(ContentAlgorithm.A128GCM);
        headers.setContentType("application/json");
        return new JweJsonProducer(headers, payload.getBytes(StandardCharsets.UTF_8))
            .encryptWith(JweUtils.getDirectKeyJweEncryption(KEY, ContentAlgorithm.A128GCM))
            .getBytes(StandardCharsets.UTF_8);
    }
}
