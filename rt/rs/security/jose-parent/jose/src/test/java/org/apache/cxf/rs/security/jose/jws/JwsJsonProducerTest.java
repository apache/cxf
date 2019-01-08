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
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JwsJsonProducerTest {

    public static final String ENCODED_MAC_KEY_1 = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
                       + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";

    public static final String ENCODED_MAC_KEY_2 = "09Y_RK7l5rAY9QY7EblYQNuYbu9cy1j7ovCbkeIyAKN8LIeRL-3H8g"
                       + "c8kZSYzAQ1uTRC_egZ_8cgZSZa9T5nmQ";

    public static final String UNSIGNED_PLAIN_JSON_DOCUMENT = "{"
                       + " \"from\": \"user\"," + " \"to\": \"developer\","
                       + " \"msg\": \"good job!\" " + "}";

    public static final String UNSIGNED_PLAIN_DOCUMENT = "$.02";

    public static final String UNSIGNED_PLAIN_JSON_DOCUMENT_AS_B64URL = "eyAiZnJvbSI6ICJ1c2VyIiwgInRvIjogI"
                       + "mRldmVsb3BlciIsICJtc2ciOiAiZ29vZCBqb2IhIiB9";


    public static final String SIGNED_JWS_JSON_DOCUMENT = "{"
                       + "\"payload\":\""
                       + UNSIGNED_PLAIN_JSON_DOCUMENT_AS_B64URL
                       + "\",\"signatures\":[{\"protected\":\"eyJhbGciOiJIUzI1NiJ9\",\"signature\":"
                       + "\"NNksREOsFCI1nUQEqzCe6XZFa-bRAge2XXMMAU2Jj2I\"}]}";

    public static final String SIGNED_JWS_JSON_FLAT_DOCUMENT = "{"
        + "\"payload\":\""
        + UNSIGNED_PLAIN_JSON_DOCUMENT_AS_B64URL
        + "\",\"protected\":\"eyJhbGciOiJIUzI1NiJ9\",\"signature\":"
        + "\"NNksREOsFCI1nUQEqzCe6XZFa-bRAge2XXMMAU2Jj2I\"}";

    public static final String SIGNED_JWS_JSON_FLAT_UNENCODED_DOCUMENT = "{"
        + "\"payload\":\"" + UNSIGNED_PLAIN_DOCUMENT + "\","
        + "\"protected\":\"eyJhbGciOiJIUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19\","
        + "\"signature\":" + "\"A5dxf2s96_n5FLueVuW1Z_vh161FwXZC4YLPff6dmDY\"}";

    public static final String DUAL_SIGNED_JWS_JSON_DOCUMENT = "{"
                       + "\"payload\":\""
                       + UNSIGNED_PLAIN_JSON_DOCUMENT_AS_B64URL
                       + "\",\"signatures\":[{\"protected\":\"eyJhbGciOiJIUzI1NiJ9\","
                       + "\"signature\":\"NNksREOsFCI1nUQEqzCe6XZFa-bRAge2XXMMAU2Jj2I\"},"
                       + "{\"protected\":\"eyJhbGciOiJIUzI1NiJ9\","
                       + "\"signature\":\"KY2r_Gubar7G86fVyrA7I2-69KA7faKDmebfCCmibdI\"}]}";

    @Test
    public void testSignPlainJsonDocumentPayloadConstruction() {
        JwsJsonProducer producer = new JwsJsonProducer(UNSIGNED_PLAIN_JSON_DOCUMENT);

        assertEquals(UNSIGNED_PLAIN_JSON_DOCUMENT_AS_B64URL,
                      producer.getUnsignedEncodedPayload());
    }


    @Test
    public void testSignWithProtectedHeaderOnly() {
        JwsJsonProducer producer = new JwsJsonProducer(UNSIGNED_PLAIN_JSON_DOCUMENT);
        JwsHeaders headerEntries = new JwsHeaders();
        headerEntries.setSignatureAlgorithm(SignatureAlgorithm.HS256);

        producer.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY_1, SignatureAlgorithm.HS256),
                          headerEntries);
        assertEquals(SIGNED_JWS_JSON_DOCUMENT,
                     producer.getJwsJsonSignedDocument());
    }
    @Test
    public void testSignWithProtectedHeaderOnlyUnencodedPayload() {
        JwsJsonProducer producer = new JwsJsonProducer(UNSIGNED_PLAIN_DOCUMENT, true);
        JwsHeaders headers = new JwsHeaders();
        headers.setSignatureAlgorithm(SignatureAlgorithm.HS256);
        headers.setPayloadEncodingStatus(false);


        producer.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY_1, SignatureAlgorithm.HS256),
                          headers);
        assertEquals(SIGNED_JWS_JSON_FLAT_UNENCODED_DOCUMENT,
                     producer.getJwsJsonSignedDocument());
    }
    @Test
    public void testSignWithProtectedHeaderOnlyFlat() {
        JwsJsonProducer producer = new JwsJsonProducer(UNSIGNED_PLAIN_JSON_DOCUMENT, true);
        JwsHeaders headerEntries = new JwsHeaders();
        headerEntries.setSignatureAlgorithm(SignatureAlgorithm.HS256);

        producer.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY_1, SignatureAlgorithm.HS256),
                          headerEntries);
        assertEquals(SIGNED_JWS_JSON_FLAT_DOCUMENT,
                     producer.getJwsJsonSignedDocument());
    }
    @Test
    public void testDualSignWithProtectedHeaderOnly() {
        JwsJsonProducer producer = new JwsJsonProducer(UNSIGNED_PLAIN_JSON_DOCUMENT);
        JwsHeaders headerEntries = new JwsHeaders();
        headerEntries.setSignatureAlgorithm(SignatureAlgorithm.HS256);

        producer.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY_1, SignatureAlgorithm.HS256),
                          headerEntries);
        producer.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY_2, SignatureAlgorithm.HS256),
                          headerEntries);
        assertEquals(DUAL_SIGNED_JWS_JSON_DOCUMENT,
                     producer.getJwsJsonSignedDocument());
    }

}