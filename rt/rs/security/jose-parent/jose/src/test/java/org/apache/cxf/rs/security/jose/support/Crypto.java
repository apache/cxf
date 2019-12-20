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
package org.apache.cxf.rs.security.jose.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweJsonConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweJsonEncryptionEntry;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public final class Crypto {

    private static final Map<String, String> AES_HMAC_MAP;

    private static final Map<String, Integer> AES_CEK_SIZE_MAP;
    
    static {
        AES_HMAC_MAP = new HashMap<>();
        AES_HMAC_MAP.put(ContentAlgorithm.A128CBC_HS256.getJwaName(), AlgorithmUtils.HMAC_SHA_256_JAVA);
        AES_HMAC_MAP.put(ContentAlgorithm.A192CBC_HS384.getJwaName(), AlgorithmUtils.HMAC_SHA_384_JAVA);
        AES_HMAC_MAP.put(ContentAlgorithm.A256CBC_HS512.getJwaName(), AlgorithmUtils.HMAC_SHA_512_JAVA);

        AES_CEK_SIZE_MAP = new HashMap<>();
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A128CBC_HS256.getJwaName(), 32);
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A192CBC_HS384.getJwaName(), 48);
        AES_CEK_SIZE_MAP.put(ContentAlgorithm.A256CBC_HS512.getJwaName(), 64);
    }
    
    private Crypto() {
        
    }

    public static SecretKey generateKey(String algo, int size) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(algo);
            keyGenerator.init(size);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Illegal algorithm", e);
        }
    }

    public static KeyPair generateKeyPair(String algo, int size) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algo);
            kpg.initialize(size);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Illegal algorithm", e);
        }
    }

    public static SecretKey generateCek(ContentAlgorithm algo) {
        if (!AES_CEK_SIZE_MAP.containsKey(algo.getJwaName())) {
            throw new IllegalArgumentException("Content algorithm [" + algo.getJwaName() + "] not supported");
        }
        return CryptoUtils.getSecretKey(algo.getJavaAlgoName(), AES_CEK_SIZE_MAP.get(algo.getJwaName()) * 8);
    }

    public static KeyAlgorithm findKeyAlgorithm(JweJsonConsumer jweConsumer, JweJsonEncryptionEntry encryptionBlock) {
        KeyAlgorithm algo = jweConsumer.getProtectedHeader() != null
            ? jweConsumer.getProtectedHeader().getKeyEncryptionAlgorithm()
            : null;
        if (algo == null) {
            algo = jweConsumer.getSharedUnprotectedHeader() != null
                ? jweConsumer.getSharedUnprotectedHeader().getKeyEncryptionAlgorithm()
                : null;
            if (algo == null) {
                algo = encryptionBlock.getUnprotectedHeader() != null
                    ? encryptionBlock.getUnprotectedHeader().getKeyEncryptionAlgorithm()
                    : null;
            }
        }
        return algo;
    }

    public static ContentAlgorithm findContentAlgorithm(JweJsonConsumer jweConsumer,
        JweJsonEncryptionEntry encryptionBlock) {
        ContentAlgorithm algo = jweConsumer.getProtectedHeader() != null
            ? jweConsumer.getProtectedHeader().getContentEncryptionAlgorithm()
            : null;
        if (algo == null) {
            algo = jweConsumer.getSharedUnprotectedHeader() != null
                ? jweConsumer.getSharedUnprotectedHeader().getContentEncryptionAlgorithm()
                : null;
            if (algo == null) {
                algo = encryptionBlock.getUnprotectedHeader() != null
                    ? encryptionBlock.getUnprotectedHeader().getContentEncryptionAlgorithm()
                    : null;
            }
        }
        return algo;
    }

    public static String findKeyId(JweJsonConsumer jweConsumer, JweJsonEncryptionEntry encryptionBlock) {
        String kid = jweConsumer.getProtectedHeader() != null ? jweConsumer.getProtectedHeader().getKeyId() : null;
        if (kid == null) {
            kid = jweConsumer.getSharedUnprotectedHeader() != null
                ? jweConsumer.getSharedUnprotectedHeader().getKeyId()
                : null;
            if (kid == null) {
                kid = encryptionBlock.getUnprotectedHeader() != null
                    ? encryptionBlock.getUnprotectedHeader().getKeyId()
                    : null;
            }
        }
        return kid;
    }

}
