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
import org.apache.cxf.rs.security.jose.common.HexUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JweJsonProducerTest {
    static final byte[] WRAPPER_BYTES1 = {91, 96, 105, 38, 99, 108, 110, 8, -93, 50, -15, 62, 0, -115, 73, -39};
    static final byte[] WRAPPER_BYTES2 = {-39, 96, 105, 38, 99, 108, 110, 8, -93, 50, -15, 62, 0, -115, 73, 91};
    static final byte[] CEK_BYTES = {-43, 123, 77, 115, 40, 49, -4, -9, -48, -74, 62, 59, 60, 102, -22, -100};
    static final String CEK_32_HEX = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
    static final String SINGLE_RECIPIENT_OUTPUT =
        "{"
        + "\"protected\":\"eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4R0NNIn0\","
        + "\"recipients\":"
        + "["
        + "{\"encrypted_key\":\"b3-M9_CRgT3wEBhhXlpb-BoY7vtA4W_N\"}"
        + "],"
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"GxWlwvTPmHi4ZnQgafiHew\""
        + "}";
    static final String SINGLE_RECIPIENT_FLAT_OUTPUT =
        "{"
        + "\"protected\":\"eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4R0NNIn0\","
        + "\"encrypted_key\":\"b3-M9_CRgT3wEBhhXlpb-BoY7vtA4W_N\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"GxWlwvTPmHi4ZnQgafiHew\""
        + "}";
    static final String SINGLE_RECIPIENT_DIRECT_OUTPUT =
        "{"
        + "\"protected\":\"eyJlbmMiOiJBMTI4R0NNIn0\","
        + "\"recipients\":"
        + "["
        + "{\"header\":{\"alg\":\"dir\"}}"
        + "],"
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"Te59ApbK8wNBDY_1_dgYSw\""
        + "}";
    static final String SINGLE_RECIPIENT_DIRECT_FLAT_OUTPUT =
        "{"
        + "\"protected\":\"eyJlbmMiOiJBMTI4R0NNIn0\","
        + "\"header\":{\"alg\":\"dir\"},"
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"Te59ApbK8wNBDY_1_dgYSw\""
        + "}";
    static final String SINGLE_RECIPIENT_ALL_HEADERS_AAD_OUTPUT =
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
        + "\"aad\":\"" + Base64UrlUtility.encode(JweJsonProducerTest.EXTRA_AAD_SOURCE) + "\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"oVUQGS9608D-INq61-vOaA\""
        + "}";
    static final String MULTIPLE_RECIPIENTS_OUTPUT =
        "{"
        + "\"protected\":\"eyJlbmMiOiJBMTI4R0NNIn0\","
        + "\"unprotected\":{\"jku\":\"https://server.example.com/keys.jwks\",\"alg\":\"A128KW\"},"
        + "\"recipients\":"
        + "["
        + "{"
        + "\"header\":{\"kid\":\"key1\"},"
        + "\"encrypted_key\":\"b3-M9_CRgT3wEBhhXlpb-BoY7vtA4W_N\""
        + "},"
        + "{"
        + "\"header\":{\"kid\":\"key2\"},"
        + "\"encrypted_key\":\"6a_nnEYO45qB_Vp6N2QbFQ7Cv1uecbiE\""
        + "}"
        + "],"
        + "\"aad\":\"WyJ2Y2FyZCIsW1sidmVyc2lvbiIse30sInRleHQiLCI0LjAiXSxbImZuIix7fSwidGV4dCIsIk1lcmlhZG9jIEJyYW5keWJ1Y"
                    + "2siXSxbIm4iLHt9LCJ0ZXh0IixbIkJyYW5keWJ1Y2siLCJNZXJpYWRvYyIsIk1yLiIsIiJdXSxbImJkYXkiLHt9LCJ0ZXh0"
                    + "IiwiVEEgMjk4MiJdLFsiZ2VuZGVyIix7fSwidGV4dCIsIk0iXV1d\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"oVUQGS9608D-INq61-vOaA\""
        + "}";
    static final String EXTRA_AAD_SOURCE =
        "[\"vcard\",["
        + "[\"version\",{},\"text\",\"4.0\"],"
        + "[\"fn\",{},\"text\",\"Meriadoc Brandybuck\"],"
        + "[\"n\",{},\"text\",[\"Brandybuck\",\"Meriadoc\",\"Mr.\",\"\"]],"
        + "[\"bday\",{},\"text\",\"TA 2982\"],"
        + "[\"gender\",{},\"text\",\"M\"]"
        + "]]";
    static final String SINGLE_RECIPIENT_A128CBCHS256_OUTPUT =
        "{"
        + "\"protected\":\"eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0\","
        + "\"recipients\":"
        + "["
        + "{\"encrypted_key\":\"6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ\"}"
        + "],"
        + "\"iv\":\"AxY8DCtDaGlsbGljb3RoZQ\","
        + "\"ciphertext\":\"KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY\","
        + "\"tag\":\"U0m_YmjN04DJvceFICbCVQ\""
        + "}";
    static final String SINGLE_RECIPIENT_A128CBCHS256_DIRECT_OUTPUT =
        "{"
        + "\"protected\":\"eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0\","
        + "\"recipients\":"
        + "["
        + "{\"header\":{\"alg\":\"dir\"}}"
        + "],"
        + "\"iv\":\"AxY8DCtDaGlsbGljb3RoZQ\","
        + "\"ciphertext\":\"KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY\","
        + "\"tag\":\"Mz-VPPyU4RlcuYv1IwIvzw\""
        + "}";
    static final String MULTIPLE_RECIPIENTS_A128CBCHS256_JSON_OUTPUT = 
        "{"
        + "\"protected\":\"eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0\","
        + "\"unprotected\":"
        + "{"
        + "\"jku\":\"https://server.example.com/keys.jwks\","
        + "\"alg\":\"A128KW\""
        + "},"
        + "\"recipients\":["
        + "{"
        + "\"header\":{\"kid\":\"key1\"},"
        + "\"encrypted_key\":\"NrhRVDNccP-gul5SB393C8DlbEtgGdvSgYgH5QRUXJYt-r8_wzef1g\""
        + "},{"
        + "\"header\":{\"kid\":\"key2\"},"
        + "\"encrypted_key\":\"6a_nnEYO45qB_Vp6N2QbFQ7Cv1uecbiE\""
        + "}],"
        + "\"aad\":\"WyJ2Y2FyZCIsW1sidmVyc2lvbiIse30sInRleHQiLCI0LjAiXSxbImZuIix7fSwidGV4dCIsIk1lcmlhZG9jIEJyYW5ke"
        + "WJ1Y2siXSxbIm4iLHt9LCJ0ZXh0IixbIkJyYW5keWJ1Y2siLCJNZXJpYWRvYyIsIk1yLiIsIiJdXSxbImJkYXkiLHt9LCJ0ZXh0Iiwi"
        + "VEEgMjk4MiJdLFsiZ2VuZGVyIix7fSwidGV4dCIsIk0iXV1d\","
        + "\"iv\":\"AxY8DCtDaGlsbGljb3RoZQ\","
        + "\"ciphertext\":\"pwitNt2DsK1zM72z5CxGClCr8ANYuIYZgCnohazsPyZhvR8atJnnlkR3fdSpyXYJcNx2LP-gcm3oNWiaAk0H2A\","
        + "\"tag\":\"nNSN9kYhubsQ9QELBmZIhA\""
        + "}";
    
   

    @Test
    public void testSingleRecipientGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_OUTPUT, ContentAlgorithm.A128GCM,
                              WRAPPER_BYTES1, JweCompactReaderWriterTest.INIT_VECTOR_A1,
                              CEK_BYTES, false);
    }
    @Test
    public void testSingleRecipientDirectGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_DIRECT_OUTPUT, ContentAlgorithm.A128GCM,
                              null, JweCompactReaderWriterTest.INIT_VECTOR_A1,
                              CEK_BYTES, false);
    }
    @Test
    public void testSingleRecipientDirectFlatGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_DIRECT_FLAT_OUTPUT, ContentAlgorithm.A128GCM,
                              null, JweCompactReaderWriterTest.INIT_VECTOR_A1,
                              CEK_BYTES, true);
    }
    @Test
    public void testSingleRecipientFlatGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_FLAT_OUTPUT, ContentAlgorithm.A128GCM,
                              WRAPPER_BYTES1, JweCompactReaderWriterTest.INIT_VECTOR_A1,
                              CEK_BYTES, true);
    }
    @Test
    public void testSingleRecipientA128CBCHS256() throws Exception {
        String text = "Live long and prosper.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_A128CBCHS256_OUTPUT, ContentAlgorithm.A128CBC_HS256,
                              Base64UrlUtility.decode(JweCompactReaderWriterTest.KEY_ENCRYPTION_KEY_A3),
                              JweCompactReaderWriterTest.INIT_VECTOR_A3,
                              JweCompactReaderWriterTest.CONTENT_ENCRYPTION_KEY_A3,
                              false);
    }
    @Test
    public void testSingleRecipientDirectA128CBCHS256() throws Exception {
        String text = "Live long and prosper.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_A128CBCHS256_DIRECT_OUTPUT, ContentAlgorithm.A128CBC_HS256,
                              null,
                              JweCompactReaderWriterTest.INIT_VECTOR_A3,
                              JweCompactReaderWriterTest.CONTENT_ENCRYPTION_KEY_A3,
                              false);
    }

    private String doTestSingleRecipient(String text,
                                         String expectedOutput,
                                         ContentAlgorithm contentEncryptionAlgo,
                                         final byte[] wrapperKeyBytes,
                                         final byte[] iv,
                                         final byte[] cek,
                                         boolean canBeFlat) throws Exception {
        JweHeaders headers = new JweHeaders(KeyAlgorithm.A128KW,
                                            contentEncryptionAlgo);
        final JweEncryptionProvider jwe;
        if (wrapperKeyBytes == null) {
            headers.asMap().remove("alg");
            SecretKey cekKey = CryptoUtils.createSecretKeySpec(cek, "AES");
            jwe = JweUtils.getDirectKeyJweEncryption(cekKey, contentEncryptionAlgo);
        } else {
            SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(wrapperKeyBytes, "AES");
            jwe = JweUtils.createJweEncryptionProvider(wrapperKey, headers);
        }
        JweJsonProducer p = new JweJsonProducer(headers, StringUtils.toBytesUTF8(text), canBeFlat) {
            protected JweEncryptionInput createEncryptionInput(JweHeaders jsonHeaders) {
                JweEncryptionInput input = super.createEncryptionInput(jsonHeaders);
                input.setCek(cek);
                input.setIv(iv);
                return input;
            }
        };
        String jweJson = p.encryptWith(jwe);
        assertEquals(expectedOutput, jweJson);
        return jweJson;
    }
    @Test
    public void testSingleRecipientAllTypeOfHeadersAndAad() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES1, "AES");

        JweHeaders protectedHeaders = new JweHeaders(ContentAlgorithm.A128GCM);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setJsonWebKeysUrl("https://server.example.com/keys.jwks");

        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(wrapperKey,
                                                                         KeyAlgorithm.A128KW,
                                                                         ContentAlgorithm.A128GCM,
                                                                         null);
        JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                sharedUnprotectedHeaders,
                                                StringUtils.toBytesUTF8(text),
                                                StringUtils.toBytesUTF8(EXTRA_AAD_SOURCE),
                                                false) {
            protected JweEncryptionInput createEncryptionInput(JweHeaders jsonHeaders) {
                JweEncryptionInput input = super.createEncryptionInput(jsonHeaders);
                input.setCek(CEK_BYTES);
                input.setIv(JweCompactReaderWriterTest.INIT_VECTOR_A1);
                return input;
            }
        };
        JweHeaders recepientUnprotectedHeaders = new JweHeaders();
        recepientUnprotectedHeaders.setKeyEncryptionAlgorithm(KeyAlgorithm.A128KW);
        String jweJson = p.encryptWith(jwe, recepientUnprotectedHeaders);
        assertEquals(SINGLE_RECIPIENT_ALL_HEADERS_AAD_OUTPUT, jweJson);
    }
    @Test
    public void testMultipleRecipientsA128GCM() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey wrapperKey1 = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES1, "AES");
        SecretKey wrapperKey2 = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES2, "AES");

        JweHeaders protectedHeaders = new JweHeaders(ContentAlgorithm.A128GCM);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setJsonWebKeysUrl("https://server.example.com/keys.jwks");
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(KeyAlgorithm.A128KW);

        List<JweEncryptionProvider> jweProviders = new LinkedList<>();

        KeyEncryptionProvider keyEncryption1 =
            JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey1, KeyAlgorithm.A128KW);
        ContentEncryptionProvider contentEncryption =
            new AesGcmContentEncryptionAlgorithm(CEK_BYTES, JweCompactReaderWriterTest.INIT_VECTOR_A1,
                                                 ContentAlgorithm.A128GCM);

        JweEncryptionProvider jwe1 = new JweEncryption(keyEncryption1, contentEncryption);
        KeyEncryptionProvider keyEncryption2 =
            JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey2, KeyAlgorithm.A128KW);
        JweEncryptionProvider jwe2 = new JweEncryption(keyEncryption2, contentEncryption);
        jweProviders.add(jwe1);
        jweProviders.add(jwe2);

        List<JweHeaders> perRecipientHeades = new LinkedList<>();
        perRecipientHeades.add(new JweHeaders("key1"));
        perRecipientHeades.add(new JweHeaders("key2"));

        JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                sharedUnprotectedHeaders,
                                                StringUtils.toBytesUTF8(text),
                                                StringUtils.toBytesUTF8(EXTRA_AAD_SOURCE),
                                                false);

        String jweJson = p.encryptWith(jweProviders, perRecipientHeades);
        assertEquals(MULTIPLE_RECIPIENTS_OUTPUT, jweJson);
    }
    
    @Test
    public void testMultipleRecipientsA128CBCHS256GivenCek() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        
        KeyAlgorithm keyAlgo = KeyAlgorithm.A128KW;
        ContentAlgorithm contentAlgo = ContentAlgorithm.A128CBC_HS256;
        
        SecretKey wrapperKey1 = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES1, "AES");
        SecretKey wrapperKey2 = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES2, "AES");

        JweHeaders protectedHeaders = new JweHeaders(contentAlgo);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setJsonWebKeysUrl("https://server.example.com/keys.jwks");
        
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(keyAlgo);

        List<JweEncryptionProvider> jweProviders = new LinkedList<>();

        KeyEncryptionProvider keyEncryption1 =
            JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey1, keyAlgo);
        
        JweEncryptionProvider jwe1 = new AesCbcHmacJweEncryption(contentAlgo, HexUtils.decode(CEK_32_HEX.getBytes()), 
            JweCompactReaderWriterTest.INIT_VECTOR_A3, keyEncryption1);
        KeyEncryptionProvider keyEncryption2 =
            JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey2, keyAlgo);
        JweEncryptionProvider jwe2 = new AesCbcHmacJweEncryption(contentAlgo, CEK_BYTES, 
            JweCompactReaderWriterTest.INIT_VECTOR_A3, keyEncryption2);
        jweProviders.add(jwe1);
        jweProviders.add(jwe2);

        List<JweHeaders> perRecipientHeades = new LinkedList<>();
        perRecipientHeades.add(new JweHeaders("key1"));
        perRecipientHeades.add(new JweHeaders("key2"));

        JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                sharedUnprotectedHeaders,
                                                StringUtils.toBytesUTF8(text),
                                                StringUtils.toBytesUTF8(EXTRA_AAD_SOURCE),
                                                false);

        String jweJson = p.encryptWith(jweProviders, perRecipientHeades);
        assertEquals(MULTIPLE_RECIPIENTS_A128CBCHS256_JSON_OUTPUT, jweJson);
    }
    
    @Test
    public void testMultipleRecipientsA128CBCHS256() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        
        KeyAlgorithm keyAlgo = KeyAlgorithm.A128KW;
        ContentAlgorithm contentAlgo = ContentAlgorithm.A128CBC_HS256;
        
        SecretKey wrapperKey1 = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES1, "AES");
        SecretKey wrapperKey2 = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES2, "AES");

        JweHeaders protectedHeaders = new JweHeaders(contentAlgo);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setJsonWebKeysUrl("https://server.example.com/keys.jwks");
        
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(keyAlgo);

        List<JweEncryptionProvider> jweProviders = new LinkedList<>();

        AesCbcContentEncryptionAlgorithm contentEncryption = new AesCbcContentEncryptionAlgorithm(contentAlgo, true);
        
        KeyEncryptionProvider keyEncryption1 = JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey1, keyAlgo);
        JweEncryptionProvider jwe1 = new AesCbcHmacJweEncryption(keyEncryption1, contentEncryption);
        KeyEncryptionProvider keyEncryption2 = JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey2, keyAlgo);
        JweEncryptionProvider jwe2 = new AesCbcHmacJweEncryption(keyEncryption2, contentEncryption);
        
        jweProviders.add(jwe1);
        jweProviders.add(jwe2);

        List<JweHeaders> perRecipientHeades = new LinkedList<>();
        perRecipientHeades.add(new JweHeaders("key1"));
        perRecipientHeades.add(new JweHeaders("key2"));

        JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                sharedUnprotectedHeaders,
                                                StringUtils.toBytesUTF8(text),
                                                StringUtils.toBytesUTF8(EXTRA_AAD_SOURCE),
                                                false);

        String jweJson = p.encryptWith(jweProviders, perRecipientHeades);
        
        JweJsonConsumer consumer = new JweJsonConsumer(jweJson);
        Assert.assertEquals(keyAlgo, consumer.getSharedUnprotectedHeader().getKeyEncryptionAlgorithm());
        Assert.assertEquals(contentAlgo, consumer.getProtectedHeader().getContentEncryptionAlgorithm());
        
        // Recipient 1
        JweDecryptionProvider jwd1 = JweUtils.createJweDecryptionProvider(wrapperKey1, keyAlgo, contentAlgo);
        JweDecryptionOutput out1 = consumer.decryptWith(jwd1, Collections.singletonMap("kid", "key1"));
        assertEquals(text, out1.getContentText());
        // Recipient 2
        JweDecryptionProvider jwd2 = JweUtils.createJweDecryptionProvider(wrapperKey2, keyAlgo, contentAlgo);

        JweDecryptionOutput out2 = consumer.decryptWith(jwd2, Collections.singletonMap("kid", "key2"));
        assertEquals(text, out2.getContentText());
    }
    
}
