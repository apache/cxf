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
package org.apache.cxf.rs.security.oauth2.jwe;

import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.jws.JwsCompactReaderWriterTest;
import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JweCompactReaderWriterTest extends Assert {
    private static final byte[] CONTENT_ENCRYPTION_KEY = {
        (byte)177, (byte)161, (byte)244, (byte)128, 84, (byte)143, (byte)225,
        115, 63, (byte)180, 3, (byte)255, 107, (byte)154, (byte)212, (byte)246,
        (byte)138, 7, 110, 91, 112, 46, 34, 105, 47, 
        (byte)130, (byte)203, 46, 122, (byte)234, 64, (byte)252};
    private static final String RSA_MODULUS_ENCODED = "oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUW"
           + "cJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3S"
           + "psk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2a"
           + "sbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMS"
           + "tPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2dj"
           + "YgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw";
    private static final String RSA_PUBLIC_EXPONENT_ENCODED = "AQAB";
    private static final String RSA_PRIVATE_EXPONENT_ENCODED = 
        "kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5N"
        + "WV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD9"
        + "3Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghk"
        + "qDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vl"
        + "t3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSnd"
        + "VTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ";
    
    private static final byte[] INIT_VECTOR = {(byte)227, (byte)197, 117, (byte)252, 2, (byte)219, 
        (byte)233, 68, (byte)180, (byte)225, 77, (byte)219};
     
    @BeforeClass
    public static void registerBouncyCastleIfNeeded() throws Exception {
        try {
            // Java 8 apparently has it
            Cipher.getInstance(Algorithm.AES_GCM_ALGO_JAVA);
        } catch (Throwable t) {
            // Oracle Java 7
            Security.addProvider(new BouncyCastleProvider());    
        }
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.class.getName());    
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
        RSAPublicKey publicKey = CryptoUtils.getRSAPublicKey(RSA_MODULUS_ENCODED, RSA_PUBLIC_EXPONENT_ENCODED);
        SecretKey key = createSecretKey(createIfException);
        String jwtKeyName = null;
        if (key == null) {
            // the encryptor will generate it
            jwtKeyName = Algorithm.A128GCM.getJwtName();
        } else {
            jwtKeyName = Algorithm.toJwtName(key.getAlgorithm(), key.getEncoded().length * 8);
        }
        RSAJweEncryption encryptor = new RSAJweEncryption(publicKey, 
                                                        key, 
                                                        jwtKeyName, 
                                                        INIT_VECTOR);
        return encryptor.encrypt(content.getBytes("UTF-8"), null);
    }
    private String encryptContentDirect(SecretKey key, String content) throws Exception {
        DirectKeyJweEncryption encryptor = new DirectKeyJweEncryption(key, INIT_VECTOR);
        return encryptor.encrypt(content.getBytes("UTF-8"), null);
    }
    private void decrypt(String jweContent, String plainContent, boolean unwrap) throws Exception {
        RSAPrivateKey privateKey = CryptoUtils.getRSAPrivateKey(RSA_MODULUS_ENCODED, RSA_PRIVATE_EXPONENT_ENCODED);
        RSAJweDecryption decryptor = new RSAJweDecryption(privateKey, unwrap);
        String decryptedText = decryptor.decrypt(jweContent).getContentText();
        assertEquals(decryptedText, plainContent);
    }
    private void decryptDirect(SecretKey key, String jweContent, String plainContent) throws Exception {
        DirectKeyJweDecryption decryptor = new DirectKeyJweDecryption(key);
        String decryptedText = decryptor.decrypt(jweContent).getContentText();
        assertEquals(decryptedText, plainContent);
    }
    private SecretKey createSecretKey(boolean createIfException) throws Exception {
        SecretKey key = null;
        if (Cipher.getMaxAllowedKeyLength("AES") > 128) { 
            key = CryptoUtils.createSecretKeySpec(CONTENT_ENCRYPTION_KEY, "AES");
        } else if (createIfException) {
            key = CryptoUtils.createSecretKeySpec(CryptoUtils.generateSecureRandomBytes(128 / 8), "AES");
        }
        return key;
    }
}

