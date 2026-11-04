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

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;

import org.junit.Test;

import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.serverRequest;
import static org.apache.cxf.rs.security.jose.jaxrs.JoseFilterTestSupport.serverRequestWithoutStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JweContainerRequestFilterTest {
    private static final byte[] KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final String PAYLOAD = "{\"id\":\"123\"}";

    @Test
    public void testDeleteBodyIsDecrypted() throws Exception {
        Message message = serverRequest("DELETE", encrypt(PAYLOAD));

        JweContainerRequestFilter filter = new JweContainerRequestFilter();
        filter.setDecryptionProvider(JweUtils.getDirectKeyJweDecryption(KEY, ContentAlgorithm.A128GCM));
        filter.filter(new ContainerRequestContextImpl(message, true, false));

        assertEquals(PAYLOAD, IOUtils.readStringFromStream(message.getContent(InputStream.class)));
    }

    @Test
    public void testGetWithoutEntityStreamIsAccepted() throws Exception {
        Message message = serverRequestWithoutStream("GET");

        JweContainerRequestFilter filter = new JweContainerRequestFilter();
        filter.setDecryptionProvider(JweUtils.getDirectKeyJweDecryption(KEY, ContentAlgorithm.A128GCM));
        filter.filter(new ContainerRequestContextImpl(message, true, false));

        assertNull(message.getContent(InputStream.class));
    }

    private static byte[] encrypt(String payload) {
        JweHeaders headers = new JweHeaders(ContentAlgorithm.A128GCM);
        headers.setContentType("application/json");
        return JweUtils.getDirectKeyJweEncryption(KEY, ContentAlgorithm.A128GCM)
            .encrypt(payload.getBytes(StandardCharsets.UTF_8), headers).getBytes(StandardCharsets.UTF_8);
    }
}
