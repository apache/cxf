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


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.SecretKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JweJsonConsumerTest {

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

    
    @Test
    public void testSingleRecipientGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text,
                              JweJsonProducerTest.SINGLE_RECIPIENT_OUTPUT,
                              ContentAlgorithm.A128GCM,
                              JweJsonProducerTest.WRAPPER_BYTES1,
                              null);
    }
    @Test
    public void testSingleRecipientFlatGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text,
                              JweJsonProducerTest.SINGLE_RECIPIENT_FLAT_OUTPUT,
                              ContentAlgorithm.A128GCM,
                              JweJsonProducerTest.WRAPPER_BYTES1,
                              null);
    }
    @Test
    public void testSingleRecipientDirectGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text,
                              JweJsonProducerTest.SINGLE_RECIPIENT_DIRECT_OUTPUT,
                              ContentAlgorithm.A128GCM,
                              null,
                              JweJsonProducerTest.CEK_BYTES);
    }
    @Test
    public void testSingleRecipientDirectA128CBCHS256() throws Exception {
        String text = "Live long and prosper.";
        doTestSingleRecipient(text,
                              JweJsonProducerTest.SINGLE_RECIPIENT_A128CBCHS256_DIRECT_OUTPUT,
                              ContentAlgorithm.A128CBC_HS256,
                              null,
                              JweCompactReaderWriterTest.CONTENT_ENCRYPTION_KEY_A3);
    }
    @Test
    public void testSingleRecipientA128CBCHS256() throws Exception {
        String text = "Live long and prosper.";
        doTestSingleRecipient(text,
                              JweJsonProducerTest.SINGLE_RECIPIENT_A128CBCHS256_OUTPUT,
                              ContentAlgorithm.A128CBC_HS256,
                              Base64UrlUtility.decode(JweCompactReaderWriterTest.KEY_ENCRYPTION_KEY_A3),
                              null);
    }
    @Test
    public void testSingleRecipientAllTypeOfHeadersAndAad() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";

        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(JweJsonProducerTest.WRAPPER_BYTES1,
                                                               "AES");
        JweDecryptionProvider jwe = JweUtils.createJweDecryptionProvider(wrapperKey,
                                                                         KeyAlgorithm.A128KW,
                                                                         ContentAlgorithm.A128GCM);
        JweJsonConsumer consumer = new JweJsonConsumer(JweJsonProducerTest.SINGLE_RECIPIENT_ALL_HEADERS_AAD_OUTPUT);
        JweDecryptionOutput out = consumer.decryptWith(jwe);
        assertEquals(text, out.getContentText());
        assertEquals(JweJsonProducerTest.EXTRA_AAD_SOURCE, consumer.getAadText());
    }

    @Test
    public void testMultipleRecipients() {
        doTestMultipleRecipients(JweJsonProducerTest.MULTIPLE_RECIPIENTS_OUTPUT);
    }
    @Test
    public void testMultipleRecipientsAutogeneratedCek() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey wrapperKey1 = CryptoUtils.createSecretKeySpec(JweJsonProducerTest.WRAPPER_BYTES1, "AES");
        SecretKey wrapperKey2 = CryptoUtils.createSecretKeySpec(JweJsonProducerTest.WRAPPER_BYTES2, "AES");

        JweHeaders protectedHeaders = new JweHeaders(ContentAlgorithm.A128GCM);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setJsonWebKeysUrl("https://server.example.com/keys.jwks");
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(KeyAlgorithm.A128KW);

        List<JweEncryptionProvider> jweProviders = new LinkedList<>();

        KeyEncryptionProvider keyEncryption1 =
            JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey1, KeyAlgorithm.A128KW);
        ContentEncryptionProvider contentEncryption =
            new AesGcmContentEncryptionAlgorithm(ContentAlgorithm.A128GCM, true);

        JweEncryptionProvider jwe1 = new JweEncryption(keyEncryption1, contentEncryption);
        KeyEncryptionProvider keyEncryption2 =
            JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey2, KeyAlgorithm.A128KW);
        JweEncryptionProvider jwe2 = new JweEncryption(keyEncryption2, contentEncryption);
        jweProviders.add(jwe1);
        jweProviders.add(jwe2);

        List<JweHeaders> perRecipientHeaders = new LinkedList<>();
        perRecipientHeaders.add(new JweHeaders("key1"));
        perRecipientHeaders.add(new JweHeaders("key2"));

        JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                sharedUnprotectedHeaders,
                                                StringUtils.toBytesUTF8(text),
                                                StringUtils.toBytesUTF8(JweJsonProducerTest.EXTRA_AAD_SOURCE),
                                                false);

        String jweJson = p.encryptWith(jweProviders, perRecipientHeaders);
        doTestMultipleRecipients(jweJson);
    }
    private void doTestMultipleRecipients(String jweJson) {
        final String text = "The true sign of intelligence is not knowledge but imagination.";

        SecretKey wrapperKey1 = CryptoUtils.createSecretKeySpec(JweJsonProducerTest.WRAPPER_BYTES1,
                                                               "AES");
        SecretKey wrapperKey2 = CryptoUtils.createSecretKeySpec(JweJsonProducerTest.WRAPPER_BYTES2,
            "AES");
        JweJsonConsumer consumer = new JweJsonConsumer(jweJson);
        KeyAlgorithm keyAlgo = consumer.getSharedUnprotectedHeader().getKeyEncryptionAlgorithm();
        ContentAlgorithm ctAlgo = consumer.getProtectedHeader().getContentEncryptionAlgorithm();
        // Recipient 1
        JweDecryptionProvider jwe1 = JweUtils.createJweDecryptionProvider(wrapperKey1, keyAlgo, ctAlgo);
        JweDecryptionOutput out1 = consumer.decryptWith(jwe1,
                                                        Collections.singletonMap("kid", "key1"));
        assertEquals(text, out1.getContentText());
        // Recipient 2
        JweDecryptionProvider jwe2 = JweUtils.createJweDecryptionProvider(wrapperKey2, keyAlgo, ctAlgo);

        JweDecryptionOutput out2 = consumer.decryptWith(jwe2,
                                                        Collections.singletonMap("kid", "key2"));
        assertEquals(text, out2.getContentText());

        // Extra AAD
        assertEquals(JweJsonProducerTest.EXTRA_AAD_SOURCE, consumer.getAadText());
    }

    @Test
    public void testSingleRecipientAllTypeOfHeadersAndAadModified() {
        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(JweJsonProducerTest.WRAPPER_BYTES1,
                                                               "AES");
        JweDecryptionProvider jwe = JweUtils.createJweDecryptionProvider(wrapperKey,
                                                                         KeyAlgorithm.A128KW,
                                                                         ContentAlgorithm.A128GCM);
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
                                       ContentAlgorithm contentEncryptionAlgo,
                                       final byte[] wrapperKeyBytes,
                                       final byte[] cek) throws Exception {
        final JweDecryptionProvider jwe;
        if (wrapperKeyBytes != null) {
            SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(wrapperKeyBytes, "AES");
            jwe = JweUtils.createJweDecryptionProvider(wrapperKey,
                                                       KeyAlgorithm.A128KW,
                                                       contentEncryptionAlgo);
        } else {
            SecretKey cekKey = CryptoUtils.createSecretKeySpec(cek, "AES");
            jwe = JweUtils.getDirectKeyJweDecryption(cekKey, contentEncryptionAlgo);
        }
        JweJsonConsumer consumer = new JweJsonConsumer(input);
        JweDecryptionOutput out = consumer.decryptWith(jwe);
        assertEquals(text, out.getContentText());
    }

}
