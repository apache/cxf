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

package org.apache.cxf.transport.websocket;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class WebSocketUtilsTest {
    private static final byte[] TEST_BODY_BYTES = "buenos dias".getBytes();
    private static final byte[] TEST_HEADERS_BYTES = "200\r\nContent-Type: text/xml;charset=utf-8\r\n".getBytes();
    private static final byte[] TEST_ID_BYTES =
        (WebSocketConstants.DEFAULT_RESPONSE_ID_KEY + ": 31415926-5358-9793-2384-626433832795\r\n").getBytes();
    private static final Map<String, String> TEST_HEADERS_MAP;
    private static final byte[] CRLF = "\r\n".getBytes();


    static {
        TEST_HEADERS_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        TEST_HEADERS_MAP.put(WebSocketUtils.SC_KEY, "200");
        TEST_HEADERS_MAP.put("Content-Type", "text/xml;charset=utf-8");
        TEST_HEADERS_MAP.put(WebSocketConstants.DEFAULT_RESPONSE_ID_KEY, "31415926-5358-9793-2384-626433832795");
    }
    @Test
    public void testBuildResponse() {
        byte[] r = WebSocketUtils.buildResponse(TEST_BODY_BYTES, 0, TEST_BODY_BYTES.length);
        verifyBytes(CRLF, 0, r, 0, 2);
        verifyBytes(TEST_BODY_BYTES, 0, r, 2, TEST_BODY_BYTES.length);
        assertEquals(2 + TEST_BODY_BYTES.length, r.length);

        r = WebSocketUtils.buildResponse(TEST_HEADERS_BYTES, TEST_BODY_BYTES, 0, TEST_BODY_BYTES.length);
        verifyBytes(TEST_HEADERS_BYTES, 0, r, 0, TEST_HEADERS_BYTES.length);
        verifyBytes(CRLF, 0, r, TEST_HEADERS_BYTES.length, 2);
        verifyBytes(TEST_BODY_BYTES, 0, r, TEST_HEADERS_BYTES.length + 2, TEST_BODY_BYTES.length);
        assertEquals(TEST_HEADERS_BYTES.length + 2 + TEST_BODY_BYTES.length, r.length);

        r = WebSocketUtils.buildResponse(TEST_HEADERS_MAP, TEST_BODY_BYTES, 0, TEST_BODY_BYTES.length);
        verifyBytes(TEST_HEADERS_BYTES, 0, r, 0, TEST_HEADERS_BYTES.length);
        verifyBytes(TEST_ID_BYTES, 0, r, TEST_HEADERS_BYTES.length, TEST_ID_BYTES.length);
        verifyBytes(CRLF, 0, r, TEST_HEADERS_BYTES.length + TEST_ID_BYTES.length, 2);
        verifyBytes(TEST_BODY_BYTES, 0, r,
                    TEST_HEADERS_BYTES.length + TEST_ID_BYTES.length + 2, TEST_BODY_BYTES.length);
        assertEquals(TEST_HEADERS_BYTES.length + TEST_ID_BYTES.length + 2 + TEST_BODY_BYTES.length, r.length);

        // with some offset
        r = WebSocketUtils.buildResponse(TEST_BODY_BYTES, 3, 3);
        verifyBytes(CRLF, 0, r, 0, 2);
        verifyBytes(TEST_BODY_BYTES, 3, r, 2, 3);
        assertEquals(2 + 3, r.length);
    }

    private void verifyBytes(byte[] expected, int epos, byte[] result, int rpos, int length) {
        for (int i = 0; i < length; i++) {
            if (result[rpos + i] != expected[epos + i]) {
                fail("Wrong byte at position result[" + (rpos + i) + "]. Expected "
                    + expected[epos + i] + " but was " + result[rpos + i]);
            }
        }
    }
}