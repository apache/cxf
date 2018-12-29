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

package org.apache.cxf.transport.websocket.atmosphere;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.cxf.transport.websocket.WebSocketUtils;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.AtmosphereResponseImpl;
import org.atmosphere.cpr.FrameworkConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DefaultProtocolInterceptorTest {

    @Test
    public void testCreateResponseWithHeadersFiltering() throws Exception {
        DefaultProtocolInterceptor dpi = new DefaultProtocolInterceptor();
        AtmosphereRequest request = AtmosphereRequestImpl.newInstance();
        AtmosphereResponse response = AtmosphereResponseImpl.newInstance();
        AtmosphereResourceImpl resource = new AtmosphereResourceImpl();
        resource.transport(AtmosphereResource.TRANSPORT.WEBSOCKET);
        request.localAttributes().put(FrameworkConfig.ATMOSPHERE_RESOURCE, resource);
        response.request(request);
        String payload = "hello cxf";
        String contentType = "text/plain";
        response.headers().put("Content-Type", contentType);

        byte[] transformed = dpi.createResponse(response, payload.getBytes(), true);
        verifyTransformed("200",
                          new String[]{"Content-Type", contentType},
                          payload, transformed);

        response.headers().put("X-fruit", "peach");
        response.headers().put("X-vegetable", "tomato");
        transformed = dpi.createResponse(response, payload.getBytes(), true);
        verifyTransformed("200",
                          new String[]{"Content-Type", contentType},
                          payload, transformed);

        dpi.includedheaders("X-f.*");
        transformed = dpi.createResponse(response, payload.getBytes(), true);
        verifyTransformed("200",
                          new String[]{"Content-Type", contentType, "X-Fruit", "peach"},
                          payload, transformed);

        dpi.includedheaders("X-.*");
        transformed = dpi.createResponse(response, payload.getBytes(), true);
        verifyTransformed("200",
                          new String[]{"Content-Type", contentType, "X-Fruit", "peach", "X-vegetable", "tomato"},
                          payload, transformed);

        dpi.excludedheaders(".*able");
        transformed = dpi.createResponse(response, payload.getBytes(), true);
        verifyTransformed("200",
                          new String[]{"Content-Type", contentType, "X-Fruit", "peach"},
                          payload, transformed);
    }

    private void verifyTransformed(String code, String[] headers, String body, byte[] transformed) throws Exception {
        InputStream in = new ByteArrayInputStream(transformed);
        String c = WebSocketUtils.readLine(in);
        Map<String, String> hs = WebSocketUtils.readHeaders(in, false);
        byte[] b = WebSocketUtils.readBody(in);
        assertEquals(code, c);
        assertEquals(headers.length >> 1, hs.size());
        for (int i = 0; i < headers.length; i += 2) {
            assertEquals(headers[i + 1], hs.get(headers[i]));
        }
        assertEquals(body, new String(b));
    }
}