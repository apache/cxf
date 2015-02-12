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
package org.apache.cxf.rs.security.jose.jwe;

import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JweJsonProducerTest extends Assert {
    private static final byte[] SECRET_BYTES = {91, 96, 105, 38, 99, 108, 110, 8, -93, 50, -15, 62, 0, -115, 73, -39};
    private static final String SINGLE_RECIPIENT_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4R0NNIn0\","
        + "\"recipients\":" 
        + "["
        + "{\"encrypted_key\":\"fO3KxJioD3Hj1V5E1pjWNNt-3vNl23oc2xgVI1Zu-82fsZ83hQLXrg\"}"
        + "],"
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"5eym8TW_c8SuK0ltJ3rpYIzOeDQz7TALvtu6UG9oMo4vpzs9tX_EFShS8iB7j6jiSdiwkIr3ajwQzaBtQD_A\","
        + "\"tag\":\"5UuOareuoUxY2iCS50WJgg\""
        + "}";
    private static final String SINGLE_RECIPIENT_FLAT_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4R0NNIn0\","
        + "\"encrypted_key\":\"fO3KxJioD3Hj1V5E1pjWNNt-3vNl23oc2xgVI1Zu-82fsZ83hQLXrg\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"5eym8TW_c8SuK0ltJ3rpYIzOeDQz7TALvtu6UG9oMo4vpzs9tX_EFShS8iB7j6jiSdiwkIr3ajwQzaBtQD_A\","
        + "\"tag\":\"5UuOareuoUxY2iCS50WJgg\""
        + "}";
    private static final String SINGLE_RECIPIENT_ALL_HEADERS_AAD_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJlbmMiOiJBMTI4R0NNIn0\","
        + "\"unprotected\":{\"jku\":\"https://server.example.com/keys.jwks\"},"    
        + "\"recipients\":" 
        + "["
        + "{"
        + "\"header\":{\"alg\":\"A128KW\"},"
        + "\"encrypted_key\":\"fO3KxJioD3Hj1V5E1pjWNNt-3vNl23oc2xgVI1Zu-82fsZ83hQLXrg\""
        + "}"
        + "],"
        + "\"aad\":\"WyJ2Y2FyZCIsW1sidmVyc2lvbiIse30sInRleHQiLCI0LjAiXSxbImZuIix7fSwidGV4dCIsIk1lcmlhZG9jIEJyYW5keWJ1Y"
                    + "2siXSxbIm4iLHt9LCJ0ZXh0IixbIkJyYW5keWJ1Y2siLCJNZXJpYWRvYyIsIk1yLiIsIiJdXSxbImJkYXkiLHt9LCJ0ZXh0"
                    + "IiwiVEEgMjk4MiJdLFsiZ2VuZGVyIix7fSwidGV4dCIsIk0iXV1d\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"5eym8TW_c8SuK0ltJ3rpYIzOeDQz7TALvtu6UG9oMo4vpzs9tX_EFShS8iB7j6jiSdiwkIr3ajwQzaBtQD_A\","
        + "\"tag\":\"4UXkQQGddmRB_df95kvhzA\""
        + "}";
    private static final String EXTRA_AAD_SOURCE = 
        "[\"vcard\",["
        + "[\"version\",{},\"text\",\"4.0\"],"
        + "[\"fn\",{},\"text\",\"Meriadoc Brandybuck\"],"
        + "[\"n\",{},\"text\",[\"Brandybuck\",\"Meriadoc\",\"Mr.\",\"\"]],"
        + "[\"bday\",{},\"text\",\"TA 2982\"],"
        + "[\"gender\",{},\"text\",\"M\"]"
        + "]]";
    
    @BeforeClass
    public static void registerBouncyCastleIfNeeded() throws Exception {
        try {
            Cipher.getInstance(Algorithm.AES_GCM_ALGO_JAVA);
            Cipher.getInstance(Algorithm.AES_CBC_ALGO_JAVA);
        } catch (Throwable t) {
            Security.addProvider(new BouncyCastleProvider());    
        }
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.class.getName());    
    }
    
    @Test
    public void testSingleRecipient() throws Exception {
        doTestSingleRecipientFlat(SINGLE_RECIPIENT_OUTPUT, false);
        
    }
    @Test
    public void testSingleRecipientFlat() throws Exception {
        doTestSingleRecipientFlat(SINGLE_RECIPIENT_FLAT_OUTPUT, true);
    }
    
    private void doTestSingleRecipientFlat(String expectedOutput, boolean canBeFlat) throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(SECRET_BYTES, "AES");
        JweHeaders headers = new JweHeaders(JoseConstants.A128KW_ALGO,
                                            JoseConstants.A128GCM_ALGO);
        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(wrapperKey, headers);
        JweJsonProducer p = new JweJsonProducer(headers, StringUtils.toBytesUTF8(text), canBeFlat) {
            protected byte[] generateIv() {
                return JweCompactReaderWriterTest.INIT_VECTOR_A1;
            }
            protected byte[] generateCek() {
                return JweCompactReaderWriterTest.CONTENT_ENCRYPTION_KEY_A1;
            }    
        };
        String jweJson = p.encryptWith(jwe);
        assertEquals(expectedOutput, jweJson);
    }
    @Test
    public void testSingleRecipientAllTypeOfHeadersAndAad() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(SECRET_BYTES, "AES");
        
        JweHeaders protectedHeaders = new JweHeaders(JoseConstants.A128GCM_ALGO);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setJsonWebKeysUrl("https://server.example.com/keys.jwks");
        
        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(wrapperKey, 
                                                                         JoseConstants.A128KW_ALGO,
                                                                         JoseConstants.A128GCM_ALGO,
                                                                         null);
        JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                sharedUnprotectedHeaders,
                                                StringUtils.toBytesUTF8(text),
                                                StringUtils.toBytesUTF8(EXTRA_AAD_SOURCE),
                                                false) {
            protected byte[] generateIv() {
                return JweCompactReaderWriterTest.INIT_VECTOR_A1;
            }
            protected byte[] generateCek() {
                return JweCompactReaderWriterTest.CONTENT_ENCRYPTION_KEY_A1;
            }    
        };
        JweHeaders recepientUnprotectedHeaders = new JweHeaders();
        recepientUnprotectedHeaders.setKeyEncryptionAlgorithm(JoseConstants.A128KW_ALGO);
        String jweJson = p.encryptWith(jwe, recepientUnprotectedHeaders);
        assertEquals(SINGLE_RECIPIENT_ALL_HEADERS_AAD_OUTPUT, jweJson);
    }
}

