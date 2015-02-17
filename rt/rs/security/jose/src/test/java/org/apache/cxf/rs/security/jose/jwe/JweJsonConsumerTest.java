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

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JweJsonConsumerTest extends Assert {

    static final String SINGLE_RECIPIENT_ALL_HEADERS_AAD_MODIFIED_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJlbmMiOiJBMTI4R0NNIn0\","
        + "\"unprotected\":{\"jku\":\"https://server.example.com/keys.jwks\"},"    
        + "\"recipients\":" 
        + "["
        + "{"
        + "\"header\":{\"alg\":\"A128KW\"},"
        + "\"encrypted_key\":\"b3-M9_CRgT3wEBhhXlpb-BoY7vtA4W_N\""
        + "}"
        + "],"
        + "\"aad\":\"" + Base64UrlUtility.encode(JweJsonProducerTest.EXTRA_AAD_SOURCE + ".") + "\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"oVUQGS9608D-INq61-vOaA\""
        + "}";
    
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
    public void testSingleRecipientGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, 
                              JweJsonProducerTest.SINGLE_RECIPIENT_OUTPUT, 
                              JoseConstants.A128GCM_ALGO, 
                              JweJsonProducerTest.WRAPPER_BYTES1,
                              null);
    }
    @Test
    public void testSingleRecipientFlatGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, 
                              JweJsonProducerTest.SINGLE_RECIPIENT_FLAT_OUTPUT, 
                              JoseConstants.A128GCM_ALGO, 
                              JweJsonProducerTest.WRAPPER_BYTES1,
                              null);
    }
    @Test
    public void testSingleRecipientDirectGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, 
                              JweJsonProducerTest.SINGLE_RECIPIENT_DIRECT_OUTPUT, 
                              JoseConstants.A128GCM_ALGO, 
                              null, 
                              JweJsonProducerTest.CEK_BYTES);
    }
    @Test
    public void testSingleRecipientDirectA128CBCHS256() throws Exception {
        String text = "Live long and prosper.";
        doTestSingleRecipient(text, 
                              JweJsonProducerTest.SINGLE_RECIPIENT_A128CBCHS256_DIRECT_OUTPUT, 
                              JoseConstants.A128CBC_HS256_ALGO, 
                              null,
                              JweCompactReaderWriterTest.CONTENT_ENCRYPTION_KEY_A3);
    }
    @Test
    public void testSingleRecipientA128CBCHS256() throws Exception {
        String text = "Live long and prosper.";
        doTestSingleRecipient(text, 
                              JweJsonProducerTest.SINGLE_RECIPIENT_A128CBCHS256_OUTPUT, 
                              JoseConstants.A128CBC_HS256_ALGO, 
                              Base64UrlUtility.decode(JweCompactReaderWriterTest.KEY_ENCRYPTION_KEY_A3),
                              null);
    }
    @Test
    public void testSingleRecipientAllTypeOfHeadersAndAad() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        
        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(JweJsonProducerTest.WRAPPER_BYTES1, 
                                                               "AES");
        JweDecryptionProvider jwe = JweUtils.createJweDecryptionProvider(wrapperKey, JoseConstants.A128KW_ALGO, 
                                                                         JoseConstants.A128GCM_ALGO);
        JweJsonConsumer consumer = new JweJsonConsumer(JweJsonProducerTest.SINGLE_RECIPIENT_ALL_HEADERS_AAD_OUTPUT);
        JweDecryptionOutput out = consumer.decryptWith(jwe);
        assertEquals(text, out.getContentText());
        assertEquals(JweJsonProducerTest.EXTRA_AAD_SOURCE, consumer.getAadText());
    }
    @Test
    public void testSingleRecipientAllTypeOfHeadersAndAadModified() {
        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(JweJsonProducerTest.WRAPPER_BYTES1, 
                                                               "AES");
        JweDecryptionProvider jwe = JweUtils.createJweDecryptionProvider(wrapperKey, JoseConstants.A128KW_ALGO, 
                                                                         JoseConstants.A128GCM_ALGO);
        JweJsonConsumer consumer = new JweJsonConsumer(SINGLE_RECIPIENT_ALL_HEADERS_AAD_MODIFIED_OUTPUT);
        try {
            consumer.decryptWith(jwe);
            fail("AAD check has passed unexpectedly");
        } catch (SecurityException ex) {
            // expected
        }
        
    }
    private void doTestSingleRecipient(String text,
                                       String input, 
                                       String contentEncryptionAlgo,
                                       final byte[] wrapperKeyBytes,
                                       final byte[] cek) throws Exception {
        JweDecryptionProvider jwe = null;
        if (wrapperKeyBytes != null) {
            SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(wrapperKeyBytes, "AES");
            jwe = JweUtils.createJweDecryptionProvider(wrapperKey, JoseConstants.A128KW_ALGO, contentEncryptionAlgo);
        } else {
            SecretKey cekKey = CryptoUtils.createSecretKeySpec(cek, "AES");
            jwe = JweUtils.getDirectKeyJweDecryption(cekKey, contentEncryptionAlgo);
        }
        JweJsonConsumer consumer = new JweJsonConsumer(input);
        JweDecryptionOutput out = consumer.decryptWith(jwe);
        assertEquals(text, out.getContentText());
    }
    
}

