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

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

public final class JweUtils {
    private JweUtils() {
        
    }
    public static KeyEncryptionAlgorithm getKeyEncryptionAlgorithm(JsonWebKey jwk) {
        return getKeyEncryptionAlgorithm(jwk, null);
    }
    public static KeyEncryptionAlgorithm getKeyEncryptionAlgorithm(JsonWebKey jwk, String defaultAlgorithm) {
        String keyEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        KeyEncryptionAlgorithm keyEncryptionProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            keyEncryptionProvider = new RSAOaepKeyEncryptionAlgorithm(JwkUtils.toRSAPublicKey(jwk), 
                                                                      keyEncryptionAlgo);
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
            SecretKey key = JwkUtils.toSecretKey(jwk);
            if (Algorithm.isAesKeyWrap(keyEncryptionAlgo)) {
                keyEncryptionProvider = new AesWrapKeyEncryptionAlgorithm(key, keyEncryptionAlgo);
            } else if (Algorithm.isAesGcmKeyWrap(keyEncryptionAlgo)) {
                keyEncryptionProvider = new AesGcmWrapKeyEncryptionAlgorithm(key, keyEncryptionAlgo);
            }
        } else {
            // TODO: support elliptic curve keys
        }
        return keyEncryptionProvider;
    }
    public static KeyDecryptionAlgorithm getKeyDecryptionAlgorithm(JsonWebKey jwk) {
        KeyDecryptionAlgorithm keyDecryptionProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            keyDecryptionProvider = new RSAOaepKeyDecryptionAlgorithm(JwkUtils.toRSAPrivateKey(jwk));
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
            SecretKey key = JwkUtils.toSecretKey(jwk);
            if (Algorithm.isAesKeyWrap(jwk.getAlgorithm())) {
                keyDecryptionProvider = new AesWrapKeyDecryptionAlgorithm(key);
            } else if (Algorithm.isAesGcmKeyWrap(jwk.getAlgorithm())) {
                keyDecryptionProvider = new AesGcmWrapKeyDecryptionAlgorithm(key);
            } 
        } else {
            // TODO: support elliptic curve keys
        }
        return keyDecryptionProvider;
    }
    public static ContentEncryptionAlgorithm getContentEncryptionAlgorithm(JsonWebKey jwk) {
        return getContentEncryptionAlgorithm(jwk, null);
    }
    public static ContentEncryptionAlgorithm getContentEncryptionAlgorithm(JsonWebKey jwk, String defaultAlgorithm) {
        String ctEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        ContentEncryptionAlgorithm contentEncryptionProvider = null;
        if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
            SecretKey key = JwkUtils.toSecretKey(jwk);
            if (Algorithm.isAesGcm(ctEncryptionAlgo)) {
                contentEncryptionProvider = new AesGcmContentEncryptionAlgorithm(key, null, ctEncryptionAlgo);
            }
        }
        return contentEncryptionProvider;
    }
}
