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

package org.apache.cxf.common.util;

/**
 * Base64 URL Encoding/Decoding utility.
 *
 * Character 62 ('+') is '-', Character 63 ('/') is '_';
 * Padding characters are dropped after the encoding.
 *
 */

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public final class Base64UrlUtility {
    private Base64UrlUtility() {
        //utility class, never constructed
    }

    public static byte[] decode(String encoded) throws Base64Exception {
        return Base64Utility.decode(encoded, true);
    }

    public static String encode(String str) {
        return encode(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String encode(byte[] id) {
        return encodeChunk(id, 0, id.length);
    }

    public static String encodeChunk(byte[] id, int offset, int length) {
        char[] chunk = Base64Utility.encodeChunk(id, offset, length, true);
        if (chunk != null) {
            return new String(chunk);
        }
        return null;
    }

    public static void encodeAndStream(byte[] id,
                                       int o,
                                       int l,
                                       OutputStream os) throws IOException {
        Base64Utility.encodeAndStream(id, o, l, true, os);
    }
}
