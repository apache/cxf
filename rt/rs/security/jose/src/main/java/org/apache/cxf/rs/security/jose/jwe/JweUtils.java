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

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jaxrs.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

public final class JweUtils {
    private static final String JSON_WEB_ENCRYPTION_CEK_ALGO_PROP = "rs.security.jwe.content.encryption.algorithm";
    private static final String JSON_WEB_ENCRYPTION_KEY_ALGO_PROP = "rs.security.jwe.key.encryption.algorithm";
    private static final String JSON_WEB_ENCRYPTION_ZIP_ALGO_PROP = "rs.security.jwe.zip.algorithm";
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
        return getKeyDecryptionAlgorithm(jwk, null);
    }
    public static KeyDecryptionAlgorithm getKeyDecryptionAlgorithm(JsonWebKey jwk, String defaultAlgorithm) {
        String keyEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        KeyDecryptionAlgorithm keyDecryptionProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            keyDecryptionProvider = new RSAOaepKeyDecryptionAlgorithm(JwkUtils.toRSAPrivateKey(jwk), 
                                                                      keyEncryptionAlgo);
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
            SecretKey key = JwkUtils.toSecretKey(jwk);
            if (Algorithm.isAesKeyWrap(jwk.getAlgorithm())) {
                keyDecryptionProvider = new AesWrapKeyDecryptionAlgorithm(key, keyEncryptionAlgo);
            } else if (Algorithm.isAesGcmKeyWrap(jwk.getAlgorithm())) {
                keyDecryptionProvider = new AesGcmWrapKeyDecryptionAlgorithm(key, keyEncryptionAlgo);
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
    public static SecretKey getContentDecryptionSecretKey(JsonWebKey jwk) {
        return getContentDecryptionSecretKey(jwk, null);
    }
    public static SecretKey getContentDecryptionSecretKey(JsonWebKey jwk, String defaultAlgorithm) {
        String ctEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType()) && Algorithm.isAesGcm(ctEncryptionAlgo)) {
            return JwkUtils.toSecretKey(jwk);
        }
        return null;
    }
    public static JweEncryptionProvider loadEncryptionProvider(String propLoc, Message m) {
        KeyEncryptionAlgorithm keyEncryptionProvider = null;
        String keyEncryptionAlgo = null;
        Properties props = null;
        try {
            props = ResourceUtils.loadProperties(propLoc, m.getExchange().getBus());
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
        
        String contentEncryptionAlgo = props.getProperty(JSON_WEB_ENCRYPTION_CEK_ALGO_PROP);
        ContentEncryptionAlgorithm ctEncryptionProvider = null;
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_ENCRYPT);
            keyEncryptionAlgo = getKeyEncryptionAlgo(props, jwk.getAlgorithm());
            if ("direct".equals(keyEncryptionAlgo)) {
                contentEncryptionAlgo = getContentEncryptionAlgo(props, jwk.getAlgorithm());
                ctEncryptionProvider = JweUtils.getContentEncryptionAlgorithm(jwk, contentEncryptionAlgo);
            } else {
                keyEncryptionProvider = JweUtils.getKeyEncryptionAlgorithm(jwk, keyEncryptionAlgo);
            }
            
        } else {
            keyEncryptionProvider = new RSAOaepKeyEncryptionAlgorithm(
                (RSAPublicKey)KeyManagementUtils.loadPublicKey(m, props), 
                getKeyEncryptionAlgo(props, keyEncryptionAlgo));
        }
        if (keyEncryptionProvider == null && ctEncryptionProvider == null) {
            throw new SecurityException();
        }
        
        
        JweHeaders headers = new JweHeaders(getKeyEncryptionAlgo(props, keyEncryptionAlgo), 
                                            contentEncryptionAlgo);
        String compression = props.getProperty(JSON_WEB_ENCRYPTION_ZIP_ALGO_PROP);
        if (compression != null) {
            headers.setZipAlgorithm(compression);
        }
        if (keyEncryptionProvider != null) {
            if (Algorithm.isAesCbcHmac(contentEncryptionAlgo)) { 
                return new AesCbcHmacJweEncryption(contentEncryptionAlgo, keyEncryptionProvider);
            } else {
                return new WrappedKeyJweEncryption(headers, 
                                                   keyEncryptionProvider,
                                                   new AesGcmContentEncryptionAlgorithm(contentEncryptionAlgo));
            }
        } else {
            return new DirectKeyJweEncryption(ctEncryptionProvider);
        }
    }
    public static JweDecryptionProvider loadDecryptionProvider(String propLoc, Message m) {
        KeyDecryptionAlgorithm keyDecryptionProvider = null;
        Properties props = null;
        try {
            props = ResourceUtils.loadProperties(propLoc, m.getExchange().getBus());
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }    
        String contentEncryptionAlgo = props.getProperty(JSON_WEB_ENCRYPTION_CEK_ALGO_PROP);
        SecretKey ctDecryptionKey = null;
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_ENCRYPT);
            String keyEncryptionAlgo = getKeyEncryptionAlgo(props, jwk.getAlgorithm());
            if ("direct".equals(keyEncryptionAlgo)) {
                contentEncryptionAlgo = getContentEncryptionAlgo(props, contentEncryptionAlgo);
                ctDecryptionKey = JweUtils.getContentDecryptionSecretKey(jwk, contentEncryptionAlgo);
            } else {
                keyDecryptionProvider = JweUtils.getKeyDecryptionAlgorithm(jwk, keyEncryptionAlgo);
            }
        } else {
            keyDecryptionProvider = new RSAOaepKeyDecryptionAlgorithm(
                (RSAPrivateKey)KeyManagementUtils.loadPrivateKey(
                    m, props, KeyManagementUtils.RSSEC_DECRYPT_KEY_PSWD_PROVIDER));
        }
        if (keyDecryptionProvider == null && ctDecryptionKey == null) {
            throw new SecurityException();
        }
        if (keyDecryptionProvider != null) {
            if (Algorithm.isAesCbcHmac(contentEncryptionAlgo)) { 
                return new AesCbcHmacJweDecryption(keyDecryptionProvider, contentEncryptionAlgo);
            } else {
                return new WrappedKeyJweDecryption(keyDecryptionProvider, 
                                                   new AesGcmContentDecryptionAlgorithm(contentEncryptionAlgo));
            }
        } else {
            return new DirectKeyJweDecryption(ctDecryptionKey, 
                                              new AesGcmContentDecryptionAlgorithm(contentEncryptionAlgo));
        }
    }
    private static String getKeyEncryptionAlgo(Properties props, String algo) {
        return algo == null ? props.getProperty(JSON_WEB_ENCRYPTION_KEY_ALGO_PROP) : algo;
    }
    private static String getContentEncryptionAlgo(Properties props, String algo) {
        return algo == null ? props.getProperty(JSON_WEB_ENCRYPTION_CEK_ALGO_PROP) : algo;
    }
}
