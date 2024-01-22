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

import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactReaderWriterTest;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JweCompactReaderWriterTest {
    // A1 example
    static final byte[] CONTENT_ENCRYPTION_KEY_A1 = {
        (byte)177, (byte)161, (byte)244, (byte)128, 84, (byte)143, (byte)225,
        115, 63, (byte)180, 3, (byte)255, 107, (byte)154, (byte)212, (byte)246,
        (byte)138, 7, 110, 91, 112, 46, 34, 105, 47,
        (byte)130, (byte)203, 46, 122, (byte)234, 64, (byte)252};
    static final String RSA_MODULUS_ENCODED_A1 = "oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUW"
           + "cJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3S"
           + "psk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2a"
           + "sbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMS"
           + "tPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2dj"
           + "YgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw";
    static final String RSA_PUBLIC_EXPONENT_ENCODED_A1 = "AQAB";
    static final String RSA_PRIVATE_EXPONENT_ENCODED_A1 =
        "kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5N"
        + "WV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD9"
        + "3Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghk"
        + "qDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vl"
        + "t3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSnd"
        + "VTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ";

    static final byte[] INIT_VECTOR_A1 = {(byte)227, (byte)197, 117, (byte)252, 2, (byte)219,
        (byte)233, 68, (byte)180, (byte)225, 77, (byte)219};

    // A3 example
    static final byte[] CONTENT_ENCRYPTION_KEY_A3 = {
        4, (byte)211, 31, (byte)197, 84, (byte)157, (byte)252, (byte)254, 11, 100,
        (byte)157, (byte)250, 63, (byte)170, 106, (byte)206, 107, 124, (byte)212,
        45, 111, 107, 9, (byte)219, (byte)200, (byte)177, 0, (byte)240, (byte)143,
        (byte)156, 44, (byte)207};
    static final byte[] INIT_VECTOR_A3 = {
        3, 22, 60, 12, 43, 67, 104, 105, 108, 108, 105, 99, 111, 116, 104, 101};
    static final String KEY_ENCRYPTION_KEY_A3 = "GawgguFyGrWKav7AX4VKUg";
    private static final String JWE_OUTPUT_A3 =
        "eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0"
        + ".6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ"
        + ".AxY8DCtDaGlsbGljb3RoZQ"
        + ".KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY"
        + ".U0m_YmjN04DJvceFICbCVQ";

    

    @Test
    public void testEncryptDecryptAesWrapA128CBCHS256() throws Exception {
        final String specPlainText = "Live long and prosper.";

        byte[] cekEncryptionKey = Base64UrlUtility.decode(KEY_ENCRYPTION_KEY_A3);

        AesWrapKeyEncryptionAlgorithm keyEncryption =
            new AesWrapKeyEncryptionAlgorithm(cekEncryptionKey, KeyAlgorithm.A128KW);
        JweEncryptionProvider encryption = new AesCbcHmacJweEncryption(ContentAlgorithm.A128CBC_HS256,
                                                           CONTENT_ENCRYPTION_KEY_A3,
                                                           INIT_VECTOR_A3,
                                                           keyEncryption);
        String jweContent = encryption.encrypt(specPlainText.getBytes(StandardCharsets.UTF_8), null);
        assertEquals(JWE_OUTPUT_A3, jweContent);

        AesWrapKeyDecryptionAlgorithm keyDecryption = new AesWrapKeyDecryptionAlgorithm(cekEncryptionKey);
        JweDecryptionProvider decryption = new AesCbcHmacJweDecryption(keyDecryption);
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);
    }

    @Test
    public void testECDHESDirectKeyEncryption() throws Exception {
        ECPrivateKey bobPrivateKey =
            CryptoUtils.getECPrivateKey(JsonWebKey.EC_CURVE_P256,
                                        "VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw");

        final ECPublicKey bobPublicKey =
            CryptoUtils.getECPublicKey(JsonWebKey.EC_CURVE_P256,
                                       "weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ",
                                       "e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck");
        JweEncryptionProvider jweOut =
            new EcdhDirectKeyJweEncryption(bobPublicKey,
                                           JsonWebKey.EC_CURVE_P256,
                                           "Alice",
                                           "Bob",
                                           ContentAlgorithm.A128GCM);

        String jweOutput = jweOut.encrypt("Hello".getBytes(), null);
        JweDecryptionProvider jweIn =
            new EcdhDirectKeyJweDecryption(bobPrivateKey, ContentAlgorithm.A128GCM);
        assertEquals("Hello", jweIn.decrypt(jweOutput).getContentText());
    }
    
    @Test
    public void testRejectInvalidCurve() throws Exception {
        // Test vectors are provided by Antonio Sanso, test follows a pattern based 
        // on a similar contribution from Antonio to jose4j.
        String receiverJwkJson = "\n{\"kty\":\"EC\",\n"
                + " \"crv\":\"P-256\",\n"
                + " \"x\":\"weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ\",\n"
                + " \"y\":\"e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck\",\n"
                + " \"d\":\"VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw\"\n"
                + "}";
        JsonWebKey receiverJwk = JwkUtils.readJwkKey(receiverJwkJson);
        ECPrivateKey privateKey = JwkUtils.toECPrivateKey(receiverJwk);
        
        //========================= attacking point #1 with order 113 ======================
        //The malicious JWE contains a public key with order 113
        String maliciousJWE1 = "eyJhbGciOiJFQ0RILUVTK0ExMjhLVyIsImVuYyI6IkExMjhDQkMtSFMyNTYiLCJlcGsiOnsia3R5IjoiRU"
            + "MiLCJ4IjoiZ1Rsa"
            + "TY1ZVRRN3otQmgxNDdmZjhLM203azJVaURpRzJMcFlrV0FhRkpDYyIsInkiOiJjTEFuakthNGJ6akQ3REpWUHdhOUVQclJ6TUc3"
            + "ck9OZ3NpVUQta"
            + "2YzMEZzIiwiY3J2IjoiUC0yNTYifX0.qGAdxtEnrV_3zbIxU2ZKrMWcejNltjA_dtefBFnRh9A2z9cNIqYRWg.pEA5kX304PMCOm"
            + "FSKX_cEg.a9f"
            + "wUrx2JXi1OnWEMOmZhXd94-bEGCH9xxRwqcGuG2AMo-AwHoljdsH5C_kcTqlXS5p51OB1tvgQcMwB5rpTxg.72CHiYFecyDvuUa4"
            + "3KKT6w";

        
        JweDecryptionProvider jweIn = JweUtils.createJweDecryptionProvider(privateKey, 
                                                                 KeyAlgorithm.ECDH_ES_A128KW,
                                                                 ContentAlgorithm.A128CBC_HS256);
        try {
            jweIn.decrypt(maliciousJWE1);
            fail("Decryption should have failed due to invalid curve");
        } catch (JweException e) {
            // continue
        }

        //========================= attacking point #2 with order 2447 ======================
        //The malicious JWE contains a public key with order 2447
        String maliciousJWE2 = "eyJhbGciOiJFQ0RILUVTK0ExMjhLVyIsImVuYyI6IkExMjhDQkMtSFMyNTYiLCJlcGsiOnsia3R5IjoiRU"
        + "MiLCJ4IjoiWE9YR1"
        + "E5XzZRQ3ZCZzN1OHZDSS1VZEJ2SUNBRWNOTkJyZnFkN3RHN29RNCIsInkiOiJoUW9XTm90bk56S2x3aUNuZUprTElxRG5UTnc3SXNkQ"
        + "kM1M1ZVcVZ"
        + "qVkpjIiwiY3J2IjoiUC0yNTYifX0.UGb3hX3ePAvtFB9TCdWsNkFTv9QWxSr3MpYNiSBdW630uRXRBT3sxw.6VpU84oMob16DxOR98Y"
        + "TRw.y1Uslv"
        + "tkoWdl9HpugfP0rSAkTw1xhm_LbK1iRXzGdpYqNwIG5VU33UBpKAtKFBoA1Kk_sYtfnHYAvn-aes4FTg.UZPN8h7FcvA5MIOq-Pkj8A";
        JweDecryptionProvider jweIn2 = JweUtils.createJweDecryptionProvider(privateKey, 
                                                                           KeyAlgorithm.ECDH_ES_A128KW,
                                                                           ContentAlgorithm.A128CBC_HS256);
        try {
            jweIn2.decrypt(maliciousJWE2);
            fail("Decryption should have failed due to invalid curve");
        } catch (JweException e) {
            // expected
        }
    }
    @Test
    public void testEncryptDecryptRSA15WrapA128CBCHS256() throws Exception {
        final String specPlainText = "Live long and prosper.";

        RSAPublicKey publicKey = CryptoUtils.getRSAPublicKey(RSA_MODULUS_ENCODED_A1,
                                                             RSA_PUBLIC_EXPONENT_ENCODED_A1);

        KeyEncryptionProvider keyEncryption = new RSAKeyEncryptionAlgorithm(publicKey,
                                                                             KeyAlgorithm.RSA1_5);

        JweEncryptionProvider encryption = new AesCbcHmacJweEncryption(ContentAlgorithm.A128CBC_HS256,
                                                           CONTENT_ENCRYPTION_KEY_A3,
                                                           INIT_VECTOR_A3,
                                                           keyEncryption);
        String jweContent = encryption.encrypt(specPlainText.getBytes(StandardCharsets.UTF_8), null);

        RSAPrivateKey privateKey = CryptoUtils.getRSAPrivateKey(RSA_MODULUS_ENCODED_A1,
                                                                RSA_PRIVATE_EXPONENT_ENCODED_A1);
        KeyDecryptionProvider keyDecryption = new RSAKeyDecryptionAlgorithm(privateKey,
                                                                             KeyAlgorithm.RSA1_5);
        JweDecryptionProvider decryption = new AesCbcHmacJweDecryption(keyDecryption);
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);
    }
    @Test
    public void testEncryptDecryptAesGcmWrapA128CBCHS256() throws Exception {
        //
        // This test fails with the IBM JDK
        //
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }
        final String specPlainText = "Live long and prosper.";

        byte[] cekEncryptionKey = Base64UrlUtility.decode(KEY_ENCRYPTION_KEY_A3);

        AesGcmWrapKeyEncryptionAlgorithm keyEncryption =
            new AesGcmWrapKeyEncryptionAlgorithm(cekEncryptionKey, KeyAlgorithm.A128GCMKW);
        JweEncryptionProvider encryption = new AesCbcHmacJweEncryption(ContentAlgorithm.A128CBC_HS256,
                                                           CONTENT_ENCRYPTION_KEY_A3,
                                                           INIT_VECTOR_A3,
                                                           keyEncryption);
        String jweContent = encryption.encrypt(specPlainText.getBytes(StandardCharsets.UTF_8), null);

        AesGcmWrapKeyDecryptionAlgorithm keyDecryption = new AesGcmWrapKeyDecryptionAlgorithm(cekEncryptionKey);
        JweDecryptionProvider decryption = new AesCbcHmacJweDecryption(keyDecryption);
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);
    }

    @Test
    public void testEncryptDecryptSpecExample() throws Exception {
        final String specPlainText = "The true sign of intelligence is not knowledge but imagination.";
        String jweContent = encryptContent(specPlainText, true);

        decrypt(jweContent, specPlainText, true);
    }

    @Test
    public void testDirectKeyEncryptDecrypt() throws Exception {
        final String specPlainText = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey key = createSecretKey(true);
        String jweContent = encryptContentDirect(key, specPlainText);

        decryptDirect(key, jweContent, specPlainText);
    }

    @Test
    public void testEncryptDecryptJwsToken() throws Exception {
        String jweContent = encryptContent(JwsCompactReaderWriterTest.ENCODED_TOKEN_SIGNED_BY_MAC, false);
        decrypt(jweContent, JwsCompactReaderWriterTest.ENCODED_TOKEN_SIGNED_BY_MAC, false);
    }

    private String encryptContent(String content, boolean createIfException) throws Exception {
        RSAPublicKey publicKey = CryptoUtils.getRSAPublicKey(RSA_MODULUS_ENCODED_A1,
                                                             RSA_PUBLIC_EXPONENT_ENCODED_A1);
        SecretKey key = createSecretKey(createIfException);
        final String jwtKeyName;
        if (key == null) {
            // the encryptor will generate it
            jwtKeyName = ContentAlgorithm.A128GCM.getJwaName();
        } else {
            jwtKeyName = AlgorithmUtils.toJwaName(key.getAlgorithm(), key.getEncoded().length * 8);
        }
        KeyEncryptionProvider keyEncryptionAlgo = new RSAKeyEncryptionAlgorithm(publicKey,
                                                                                 KeyAlgorithm.RSA_OAEP);
        ContentEncryptionProvider contentEncryptionAlgo =
            new AesGcmContentEncryptionAlgorithm(key == null ? null : key.getEncoded(), INIT_VECTOR_A1,
                ContentAlgorithm.getAlgorithm(jwtKeyName));
        JweEncryptionProvider encryptor = new JweEncryption(keyEncryptionAlgo, contentEncryptionAlgo);
        return encryptor.encrypt(content.getBytes(StandardCharsets.UTF_8), null);
    }
    private String encryptContentDirect(SecretKey key, String content) throws Exception {
        JweEncryption encryptor = new JweEncryption(new DirectKeyEncryptionAlgorithm(),
            new AesGcmContentEncryptionAlgorithm(key, INIT_VECTOR_A1, ContentAlgorithm.A128GCM));
        return encryptor.encrypt(content.getBytes(StandardCharsets.UTF_8), null);
    }
    private void decrypt(String jweContent, String plainContent, boolean unwrap) throws Exception {
        RSAPrivateKey privateKey = CryptoUtils.getRSAPrivateKey(RSA_MODULUS_ENCODED_A1,
                                                                RSA_PRIVATE_EXPONENT_ENCODED_A1);
        ContentAlgorithm algo = Cipher.getMaxAllowedKeyLength("AES") > 128
            ? ContentAlgorithm.A256GCM : ContentAlgorithm.A128GCM;
        JweDecryptionProvider decryptor = new JweDecryption(new RSAKeyDecryptionAlgorithm(privateKey),
                                              new AesGcmContentDecryptionAlgorithm(algo));
        String decryptedText = decryptor.decrypt(jweContent).getContentText();
        assertEquals(decryptedText, plainContent);
    }
    private void decryptDirect(SecretKey key, String jweContent, String plainContent) throws Exception {
        JweDecryption decryptor = new JweDecryption(new DirectKeyDecryptionAlgorithm(key),
                                               new AesGcmContentDecryptionAlgorithm(ContentAlgorithm.A128GCM));
        String decryptedText = decryptor.decrypt(jweContent).getContentText();
        assertEquals(decryptedText, plainContent);
    }
    private SecretKey createSecretKey(boolean createIfException) throws Exception {
        SecretKey key = null;
        if (Cipher.getMaxAllowedKeyLength("AES") > 128) {
            key = CryptoUtils.createSecretKeySpec(CONTENT_ENCRYPTION_KEY_A1, "AES");
        } else if (createIfException) {
            key = CryptoUtils.createSecretKeySpec(CryptoUtils.generateSecureRandomBytes(128 / 8), "AES");
        }
        return key;
    }
}
