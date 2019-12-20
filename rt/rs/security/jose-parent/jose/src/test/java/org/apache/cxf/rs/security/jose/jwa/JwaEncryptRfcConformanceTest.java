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
package org.apache.cxf.rs.security.jose.jwa;

import org.apache.cxf.rs.security.jose.support.Serialization;

import org.junit.Test;

public abstract class JwaEncryptRfcConformanceTest extends AbstractEncryptTest {

    @Test
    public void testOctA128GcmJweCompact() throws Exception {
        test("OCTET", "A128KW", "A128GCM", Serialization.COMPACT, PLAIN_TEXT, JWKS_PRIVATE_KEYS);
    }

    @Test
    public void testOctA128GcmJweJsonFlattened() throws Exception {
        test("OCTET", "A128KW", "A128GCM", Serialization.FLATTENED, PLAIN_TEXT, JWKS_PRIVATE_KEYS);
    }

    @Test
    public void testOctA128GcmJweJson() throws Exception {
        test("OCTET", "A128KW", "A128GCM", Serialization.JSON, PLAIN_TEXT, JWKS_PRIVATE_KEYS);
    }

    @Test
    public void testRsaOaepA128GcmJweCompact() throws Exception {
        test("RSA", "RSA-OAEP", "A128GCM", Serialization.COMPACT);
    }

    @Test
    public void testRsaOaepA128GcmJweJsonFlattened() throws Exception {
        test("RSA", "RSA-OAEP", "A128GCM", Serialization.FLATTENED);
    }

    @Test
    public void testRsaOaepA128GcmJweJson() throws Exception {
        test("RSA", "RSA-OAEP", "A128GCM", Serialization.JSON);
    }

    @Test
    public void testEcdhDirectA128GcmJweCompact() throws Exception {
        test("EC", "ECDH-ES", "A128GCM", Serialization.COMPACT);
    }

    @Test
    public void testEcdhDirectA128GcmJweJsonFlattened() throws Exception {
        test("EC", "ECDH-ES", "A128GCM", Serialization.FLATTENED);
    }

    @Test
    public void testEcdhDirectA128GcmJweJson() throws Exception {
        test("EC", "ECDH-ES", "A128GCM", Serialization.JSON);
    }

    @Test
    public void testEcdhA128KwA128GcmJweCompact() throws Exception {
        test("EC", "ECDH-ES+A128KW", "A128GCM", Serialization.COMPACT);
    }

    @Test
    public void testEcdhA128KwA128GcmJweJsonFlattened() throws Exception {
        test("EC", "ECDH-ES+A128KW", "A128GCM", Serialization.FLATTENED);
    }

    @Test
    public void testEcdhA128KwA128GcmJweJson() throws Exception {
        test("EC", "ECDH-ES+A128KW", "A128GCM", Serialization.JSON);
    }

    @Test
    public void testEcdhA128KwA128CbcJweCompact() throws Exception {
        test("EC", "ECDH-ES+A128KW", "A128CBC-HS256", Serialization.COMPACT);
    }

    @Test
    public void testEcdhA128KwA128CbcJweJsonFlattened() throws Exception {
        test("EC", "ECDH-ES+A128KW", "A128CBC-HS256", Serialization.FLATTENED);
    }

    @Test
    public void testEcdhA128KwA128CbcJweJson() throws Exception {
        test("EC", "ECDH-ES+A128KW", "A128CBC-HS256", Serialization.JSON);
    }

    @Test
    public void testRsa15A128GcmCompact() throws Exception {
        test("RSA", "RSA1_5", "A128GCM", Serialization.COMPACT);
    }

    @Test
    public void testRsa15A128GcmJsonFlattened() throws Exception {
        test("RSA", "RSA1_5", "A128GCM", Serialization.FLATTENED);
    }

    @Test
    public void testRsa15A128GcmJson() throws Exception {
        test("RSA", "RSA1_5", "A128GCM", Serialization.JSON);
    }

    @Test
    public void testRsa15A128CbcJweCompact() throws Exception {
        test("RSA", "RSA1_5", "A128CBC-HS256", Serialization.COMPACT);
    }

    @Test
    public void testRsa15A128CbcJweJsonFlattened() throws Exception {
        test("RSA", "RSA1_5", "A128CBC-HS256", Serialization.FLATTENED);
    }

    @Test
    public void testRsa15A128CbcJweJson() throws Exception {
        test("RSA", "RSA1_5", "A128CBC-HS256", Serialization.JSON);
    }

    @Test
    public void testOctA128CbcJweCompact() throws Exception {
        test("OCTET", "A128KW", "A128CBC-HS256", Serialization.COMPACT, PLAIN_TEXT, JWKS_PRIVATE_KEYS);
    }

    @Test
    public void testOctA128CbcJweJsonFlattened() throws Exception {
        test("OCTET", "A128KW", "A128CBC-HS256", Serialization.FLATTENED, PLAIN_TEXT, JWKS_PRIVATE_KEYS);
    }

    @Test
    public void testOctA128CbcJweJson() throws Exception {
        test("OCTET", "A128KW", "A128CBC-HS256", Serialization.JSON, PLAIN_TEXT, JWKS_PRIVATE_KEYS);
    }

}
