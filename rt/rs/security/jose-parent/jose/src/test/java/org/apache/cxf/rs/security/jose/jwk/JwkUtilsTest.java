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
package org.apache.cxf.rs.security.jose.jwk;

import java.security.interfaces.RSAPublicKey;

import org.apache.cxf.rs.security.jose.common.JoseUtils;

import org.junit.Assert;
import org.junit.Test;

public class JwkUtilsTest extends Assert {
    private static final String RSA_KEY = "{"
      + "\"kty\": \"RSA\","
      + "\"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAt"
      +      "VT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn6"
      +      "4tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FD"
      +      "W2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n9"
      +      "1CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINH"
      +      "aQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\","
      + "\"e\": \"AQAB\","
      + "\"alg\": \"RS256\","
      + "\"kid\": \"2011-04-29\""
      + "}";
    private static final String EC_256_KEY = "{"
        + "\"kty\": \"EC\","
        + "\"x\": \"CEuRLUISufhcjrj-32N0Bvl3KPMiHH9iSw4ohN9jxrA\","
        + "\"y\": \"EldWz_iXSK3l_S7n4w_t3baxos7o9yqX0IjzG959vHc\","
        + "\"crv\": \"P-256\""
        + "}";
    private static final String EC_384_KEY = "{"
        + "\"kty\": \"EC\","
        + "\"x\": \"2jCG5DmKUql9YPn7F2C-0ljWEbj8O8-vn5Ih1k7Wzb-y3NpBLiG1BiRa392b1kcQ\","
        + "\"y\": \"7Ragi9rT-5tSzaMbJlH_EIJl6rNFfj4V4RyFM5U2z4j1hesX5JXa8dWOsE-5wPIl\","
        + "\"crv\": \"P-384\""
        + "}";
    private static final String EC_521_KEY = "{"
        + "\"kty\": \"EC\","
        + "\"x\": \"Aeq3uMrb3iCQEt0PzSeZMmrmYhsKP5DM1oMP6LQzTFQY9-F3Ab45xiK4AJxltXEI-87g3gRwId88hTyHgq180JDt\","
        + "\"y\": \"ARA0lIlrZMEzaXyXE4hjEkc50y_JON3qL7HSae9VuWpOv_2kit8p3pyJBiRb468_U5ztLT7FvDvtimyS42trhDTu\","
        + "\"crv\": \"P-521\""
        + "}";
    private static final String OCTET_KEY_1 = "{"
        + "\"kty\": \"oct\","
        + "\"k\": \"ZW8Eg8TiwoT2YamLJfC2leYpLgLmUAh_PcMHqRzBnMg\""
        + "}";
    private static final String OCTET_KEY_2 = "{"
        + "\"kty\": \"oct\","
        + "\"k\": \"NGbwp1rC4n85A1SaNxoHow\""
        + "}";
    @Test
    public void testRsaKeyModulus() throws Exception {
        JsonWebKey jwk = JwkUtils.readJwkKey(RSA_KEY);
        String modulus = jwk.getStringProperty(JsonWebKey.RSA_MODULUS);
        assertEquals(256, JoseUtils.decode(modulus).length);
        
        RSAPublicKey pk = JwkUtils.toRSAPublicKey(jwk);
        JsonWebKey jwk2 = JwkUtils.fromRSAPublicKey(pk, jwk.getAlgorithm());
        String modulus2 = jwk2.getStringProperty(JsonWebKey.RSA_MODULUS);
        assertEquals(256, JoseUtils.decode(modulus2).length);
        assertEquals(modulus2, modulus);
    }
    @Test
    public void testRsaKeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(RSA_KEY);
        assertEquals("NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs", thumbprint);
    }
    @Test
    public void testOctetKey1Thumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(OCTET_KEY_1);
        assertEquals("7WWD36NF4WCpPaYtK47mM4o0a5CCeOt01JXSuMayv5g", thumbprint);
    }
    @Test
    public void testOctetKey2Thumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(OCTET_KEY_2);
        assertEquals("5_qb56G0OJDw-lb5mkDaWS4MwuY0fatkn9LkNqUHqMk", thumbprint);
    }
    @Test
    public void testEc256KeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(EC_256_KEY);
        assertEquals("j4UYwo9wrtllSHaoLDJNh7MhVCL8t0t8cGPPzChpYDs", thumbprint);
    }
    @Test
    public void testEc384KeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(EC_384_KEY);
        assertEquals("vZtaWIw-zw95JNzzURg1YB7mWNLlm44YZDZzhrPNetM", thumbprint);
    }
    @Test
    public void testEc521KeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(EC_521_KEY);
        assertEquals("rz4Ohmpxg-UOWIWqWKHlOe0bHSjNUFlHW5vwG_M7qYg", thumbprint);
    }
    
}
