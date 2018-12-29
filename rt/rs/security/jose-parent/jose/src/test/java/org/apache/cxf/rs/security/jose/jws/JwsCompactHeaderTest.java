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

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JwsCompactHeaderTest {

    /**
     * JWS string, which lacks the "alg" header field.
     *
     * => Must be rejected by verification operation, since the spec declares
     * that the "alg" header field must be present in the compact serialization.
     */
    public static final String MISSING_ALG_HEADER_FIELD_IN_JWS =
        "eyAiZ2xhIiA6ICJDQU1IIiB9.eyAibXNnIjogIllvdSBjYW4ndCB0b3VjaCB0aGlzISIgfQ"
        + ".Sqd_AuwlPPqv4L1EV4zPuR-HfFJpe9kOfvc597RlcoE";

    /**
     * JWS string, which contains two "alg" header fields. Bogus "alg" header
     * field first.
     *
     * => Must be rejected by verification operation, since the spec declares
     * that the "alg" header field must be present once in the compact
     * serialization.
     */
    public static final String TWO_ALG_HEADER_FIELDS_IN_JWS_BOGUS_FIRST =
        "eyAiYWxnIjogIkJvZ3VzIiwgImFsZyI6ICJIUzI1NiIgfQ.eyAibXNnIjogIllvdSBjYW4ndCB0b3VjaCB0aGlzISIgfQ"
        + ".FIgpDi1Wp9iIxxXfBw8Zce2kiZ8gmqAaVYPduRFR8kU";

    /**
     * JWS string, which contains two "alg" header fields. Bogus "alg" header
     * field last.
     *
     * => Must be rejected by verification operation, since the spec declares
     * that the "alg" header field must be present once in the compact
     * serialization.
     */
    public static final String TWO_ALG_HEADER_FIELDS_IN_JWS_BOGUS_LAST =
        "eyAiYWxnIjogIkhTMjU2IiwgImFsZyI6ICJCb2d1cyIgfQ.eyAibXNnIjogIllvdSBjYW4ndCB0b3VjaCB0aGlzISIgfQ"
        + ".Ftwla-nAg0Nty8ILVhjlIETOy2Tw1JsD3bBq55AS0PU";

    /**
     * JWS string, which contains an invalid "alg" header field value.
     *
     * (1): Algorithm not supported/known
     *
     * => Must be rejected by verification operation, since the spec declares
     * that the signature is not valid if the "alg" value does not represent a
     * supported algorithm. "alg" values should either be registered in the IANA
     * JSON Web Signature and Encryption Algorithms registry defined in JWA or
     * be a value that contains a Collision-Resistant Name.
     */
    public static final String INVALID_ALG_HEADER_VALUE_IN_JWS_1 = "tba";

    /**
     * JWS string, which contains an invalid "alg" header field value.
     *
     * (2): Wrong value encoding
     *
     * => Must be rejected by verification operation, since the spec declares
     * that the "alg" value is a case-sensitive string containing a StringOrURI
     * value.
     */
    public static final String INVALID_ALG_HEADER_VALUE_IN_JWS_2 = "tba";

    /**
     * JWS string, which contains a "alg" header field value of "none". The
     * signature has been generated with "HS256" and the signed JWS has been
     * altered afterwards to the value "none".
     *
     * => Must be rejected by verification operation, since the "none" algorithm
     * is considered harmful.
     */
    public static final String ALG_HEADER_VALUE_HS256_IN_JWS =
        "eyAiYWxnIjogIkhTMjU2IiB9"
        + ".eyAibXNnIjogIllvdSBjYW4ndCB0b3VjaCB0aGlzISIgfQ"
        + ".as_gclokwAmukh3zVF1X5sUCCfSc8TbjDdhdvk6C5c8";
    public static final String ALG_HEADER_VALUE_NONE_IN_JWS =
        "eyAiYWxnIjogIm5vbmUiIH0"
        + ".eyAibXNnIjogIllvdSBjYW4ndCB0b3VjaCB0aGlzISIgfQ"
        + ".as_gclokwAmukh3zVF1X5sUCCfSc8TbjDdhdvk6C5c8";


    /**
     * Support material (keys, etc.)
     */
    private static final String ENCODED_MAC_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
                    + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";

    // JWS string, which contains crit header field
    // JWS string, which contains more than three parts
    // JWS string, which contains less than three parts
    // JWS string, which contains null bytes padding

    @Test
    public void verifyJwsWithMissingAlgHeaderField() throws Exception {
        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(MISSING_ALG_HEADER_FIELD_IN_JWS);

        assertFalse(jwsConsumer.verifySignatureWith(new HmacJwsSignatureVerifier(ENCODED_MAC_KEY,
                                                        SignatureAlgorithm.HS256)));
    }

    @Test
    public void verifyJwsWithTwoAlgHeaderFieldsBogusFieldFirst() throws Exception {
        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(TWO_ALG_HEADER_FIELDS_IN_JWS_BOGUS_FIRST);

        boolean result = jwsConsumer.verifySignatureWith(new HmacJwsSignatureVerifier(ENCODED_MAC_KEY,
                                                                     SignatureAlgorithm.HS256));
        assertFalse(result);
    }

    @Test
    public void verifyJwsWithTwoAlgHeaderFieldsBogusFieldLast() throws Exception {
        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(TWO_ALG_HEADER_FIELDS_IN_JWS_BOGUS_LAST);

        assertFalse(jwsConsumer.verifySignatureWith(new HmacJwsSignatureVerifier(ENCODED_MAC_KEY,
                                                        SignatureAlgorithm.HS256)));
    }

    @Test
    public void verifyJwsWithAlgHeaderValueNone() throws Exception {
        JwsCompactConsumer jwsConsumerOriginal = new JwsCompactConsumer(ALG_HEADER_VALUE_HS256_IN_JWS);

        JwsCompactConsumer jwsConsumerAltered = new JwsCompactConsumer(ALG_HEADER_VALUE_NONE_IN_JWS);

        assertTrue(jwsConsumerOriginal.verifySignatureWith(new HmacJwsSignatureVerifier(ENCODED_MAC_KEY,
                                                           SignatureAlgorithm.HS256)));

        assertFalse(jwsConsumerAltered.verifySignatureWith(new HmacJwsSignatureVerifier(ENCODED_MAC_KEY,
                                                           SignatureAlgorithm.HS256)));
    }

    @Test
    public void testCriticalHeader() {
        String payload = "this is a JWS with critical header";
        String criticalParameter = "criticalParameter";
        String criticalParameter1 = "criticalParameter1";
        String criticalParameter2 = "criticalParameter2";
        String criticalParameter3 = "criticalParameter3";
        String criticalValue = "criticalValue";
        String criticalValue1 = "criticalValue1";
        String criticalValue2 = "criticalValue2";
        String criticalValue3 = "criticalValue3";
        JwsCompactProducer producer = new JwsCompactProducer(payload);
        producer.getJwsHeaders().setSignatureAlgorithm(SignatureAlgorithm.HS512);
        List<String> criticalHeader = new ArrayList<>();
        criticalHeader.add(criticalParameter1);
        producer.getJwsHeaders().setCritical(criticalHeader);
        producer.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY, SignatureAlgorithm.HS256));
        String signedJws = producer.getSignedEncodedJws();
        JwsCompactConsumer consumer = new JwsCompactConsumer(signedJws);
        assertFalse(consumer.validateCriticalHeaders());

        criticalHeader.add(criticalParameter2);
        criticalHeader.add(criticalParameter3);
        producer = new JwsCompactProducer(payload);
        producer.getJwsHeaders().setSignatureAlgorithm(SignatureAlgorithm.HS512);
        producer.getJwsHeaders().setCritical(criticalHeader);
        producer.getJwsHeaders().setHeader(criticalParameter1, criticalValue1);
        producer.getJwsHeaders().setHeader(criticalParameter2, criticalValue2);
        producer.getJwsHeaders().setHeader(criticalParameter3, criticalValue3);
        producer.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY, SignatureAlgorithm.HS256));
        signedJws = producer.getSignedEncodedJws();
        consumer = new JwsCompactConsumer(signedJws);
        assertTrue(consumer.validateCriticalHeaders());

        criticalHeader = new ArrayList<>();
        criticalHeader.add(criticalParameter);
        criticalHeader.add(criticalParameter);
        producer = new JwsCompactProducer(payload);
        producer.getJwsHeaders().setSignatureAlgorithm(SignatureAlgorithm.HS512);
        producer.getJwsHeaders().setHeader(criticalParameter, criticalValue);
        producer.getJwsHeaders().setCritical(criticalHeader);
        producer.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY, SignatureAlgorithm.HS256));
        signedJws = producer.getSignedEncodedJws();
        consumer = new JwsCompactConsumer(signedJws);
        assertFalse(consumer.validateCriticalHeaders());
    }
}
