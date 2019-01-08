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
package org.apache.cxf.rs.security.jose.jws;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JwsJsonConsumerTest {
    private static final String DUAL_SIGNED_DOCUMENT =
        "{\"payload\":\n"
        + "\t\"eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ\",\n"
        + "\t\"signatures\":[\n"
        + "\t\t\t{\"protected\":\"eyJhbGciOiJSUzI1NiJ9\",\n"
        + "\t\t\t \"header\":\n"
        + "\t\t\t\t{\"kid\":\"2010-12-29\"},\n"
        + "\t\t\t \"signature\":\n"
        + "\t\t\t\t\"cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5"
        +           "jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb"
        +           "1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOY"
        +           "EUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw\"},\n"
        + "\t\t\t{\"protected\":\"eyJhbGciOiJFUzI1NiJ9\",\n"
        + "\t\t\t \"header\":\n"
        + "\t\t\t\t{\"kid\":\"e9bc097a-ce51-4036-9562-d2ade882db0d\"},\n"
        + "\t\t\t \"signature\":\n"
        + "\t\t\t\t\"DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q\"}]\n"
        + "}";

    private static final String KID_OF_THE_FIRST_SIGNER = "2010-12-29";
    private static final String KID_OF_THE_SECOND_SIGNER = "e9bc097a-ce51-4036-9562-d2ade882db0d";

    @Test
    public void testVerifySignedWithProtectedHeaderOnlyUnencodedPayload() {
        JwsJsonConsumer consumer =
            new JwsJsonConsumer(JwsJsonProducerTest.SIGNED_JWS_JSON_FLAT_UNENCODED_DOCUMENT);
        assertEquals(JwsJsonProducerTest.UNSIGNED_PLAIN_DOCUMENT, consumer.getJwsPayload());
        assertEquals(JwsJsonProducerTest.UNSIGNED_PLAIN_DOCUMENT, consumer.getDecodedJwsPayload());
        assertTrue(consumer.verifySignatureWith(
            new HmacJwsSignatureVerifier(JwsJsonProducerTest.ENCODED_MAC_KEY_1, SignatureAlgorithm.HS256)));
        JwsHeaders headers = consumer.getSignatureEntries().get(0).getProtectedHeader();
        List<String> critical = headers.getCritical();
        assertEquals(1, critical.size());
        assertEquals(JoseConstants.JWS_HEADER_B64_STATUS_HEADER, critical.get(0));
    }

    @Test
    public void testVerifyDualSignedDocument() throws Exception {
        JwsJsonConsumer consumer = new JwsJsonConsumer(DUAL_SIGNED_DOCUMENT);
        JsonWebKeys jwks = readKeySet("jwkPublicJsonConsumerSet.txt");

        List<JwsJsonSignatureEntry> sigEntries = consumer.getSignatureEntries();
        assertEquals(2, sigEntries.size());
        // 1st signature
        String firstKid = sigEntries.get(0).getKeyId();
        assertEquals(KID_OF_THE_FIRST_SIGNER, firstKid);
        JsonWebKey rsaKey = jwks.getKey(firstKid);
        assertNotNull(rsaKey);
        assertTrue(sigEntries.get(0).verifySignatureWith(rsaKey));
        // 2nd signature
        String secondKid = sigEntries.get(1).getKeyId();
        assertEquals(KID_OF_THE_SECOND_SIGNER, secondKid);
        JsonWebKey ecKey = jwks.getKey(secondKid);
        assertNotNull(ecKey);
        assertTrue(sigEntries.get(1).verifySignatureWith(ecKey));
    }
    @Test
    public void testVerifySingleEntryInDualSignedDocument() throws Exception {
        JwsJsonConsumer consumer = new JwsJsonConsumer(DUAL_SIGNED_DOCUMENT);
        JsonWebKeys jwks = readKeySet("jwkPublicJsonConsumerSet.txt");

        List<JwsJsonSignatureEntry> sigEntries = consumer.getSignatureEntries();
        assertEquals(2, sigEntries.size());
        // 1st signature
        String firstKid = sigEntries.get(0).getKeyId();
        assertEquals(KID_OF_THE_FIRST_SIGNER, firstKid);
        JsonWebKey rsaKey = jwks.getKey(firstKid);
        assertNotNull(rsaKey);
        JwsSignatureVerifier jws = JwsUtils.getSignatureVerifier(rsaKey);
        assertTrue(consumer.verifySignatureWith(jws));
        List<JwsJsonSignatureEntry> remainingEntries =
            consumer.verifyAndGetNonValidated(Collections.singletonList(jws));
        assertEquals(1, remainingEntries.size());
        assertEquals(KID_OF_THE_SECOND_SIGNER, remainingEntries.get(0).getKeyId());

    }
    public JsonWebKeys readKeySet(String fileName) throws Exception {
        InputStream is = JwsJsonConsumerTest.class.getResourceAsStream(fileName);
        return JwkUtils.readJwkSet(is);
    }
}