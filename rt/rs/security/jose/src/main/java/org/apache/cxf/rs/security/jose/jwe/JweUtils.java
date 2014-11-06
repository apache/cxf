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
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseUtils;
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
    public String encrypt(RSAPublicKey key, String keyAlgo, String contentAlgo, byte[] content) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getRSAKeyEncryptionAlgorithm(key, keyAlgo);
        return encrypt(keyEncryptionProvider, contentAlgo, content);
    }
    public String encrypt(SecretKey key, String keyAlgo, String contentAlgo, byte[] content) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(key, keyAlgo);
        return encrypt(keyEncryptionProvider, contentAlgo, content);
    }
    public String encryptDirect(SecretKey key, String contentAlgo, byte[] content) {
        JweEncryptionProvider jwe = getDirectKeyJweEncryption(key, contentAlgo);
        return jwe.encrypt(content, null);
    }
    public byte[] decrypt(RSAPrivateKey key, String keyAlgo, String contentAlgo, String content) {
        KeyDecryptionAlgorithm keyDecryptionProvider = getRSAKeyDecryptionAlgorithm(key, keyAlgo);
        return decrypt(keyDecryptionProvider, contentAlgo, content);
    }
    public byte[] decrypt(SecretKey key, String keyAlgo, String contentAlgo, String content) {
        KeyDecryptionAlgorithm keyDecryptionProvider = getSecretKeyDecryptionAlgorithm(key, keyAlgo);
        return decrypt(keyDecryptionProvider, contentAlgo, content);
    }
    public byte[] decryptDirect(SecretKey key, String contentAlgo, String content) {
        JweDecryptionProvider jwe = getDirectKeyJweDecryption(key, contentAlgo);
        return jwe.decrypt(content).getContent();
    }
    public static KeyEncryptionAlgorithm getKeyEncryptionAlgorithm(JsonWebKey jwk) {
        return getKeyEncryptionAlgorithm(jwk, null);
    }
    public static KeyEncryptionAlgorithm getKeyEncryptionAlgorithm(JsonWebKey jwk, String defaultAlgorithm) {
        String keyEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        KeyEncryptionAlgorithm keyEncryptionProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            keyEncryptionProvider = getRSAKeyEncryptionAlgorithm(JwkUtils.toRSAPublicKey(jwk), 
                                                                 keyEncryptionAlgo);
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
            keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(JwkUtils.toSecretKey(jwk), 
                                                                    keyEncryptionAlgo);
        } else {
            // TODO: support elliptic curve keys
        }
        return keyEncryptionProvider;
    }
    public static KeyEncryptionAlgorithm getRSAKeyEncryptionAlgorithm(RSAPublicKey key, String algo) {
        return new RSAOaepKeyEncryptionAlgorithm(key, algo);
    }
    public static KeyEncryptionAlgorithm getSecretKeyEncryptionAlgorithm(SecretKey key, String algo) {
        if (Algorithm.isAesKeyWrap(algo)) {
            return new AesWrapKeyEncryptionAlgorithm(key, algo);
        } else if (Algorithm.isAesGcmKeyWrap(algo)) {
            return new AesGcmWrapKeyEncryptionAlgorithm(key, algo);
        }
        return null;
    }
    public static KeyDecryptionAlgorithm getKeyDecryptionAlgorithm(JsonWebKey jwk) {
        return getKeyDecryptionAlgorithm(jwk, null);
    }
    public static KeyDecryptionAlgorithm getKeyDecryptionAlgorithm(JsonWebKey jwk, String defaultAlgorithm) {
        String keyEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        KeyDecryptionAlgorithm keyDecryptionProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            keyDecryptionProvider = getRSAKeyDecryptionAlgorithm(JwkUtils.toRSAPrivateKey(jwk), 
                                                                 keyEncryptionAlgo);
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
            keyDecryptionProvider = getSecretKeyDecryptionAlgorithm(JwkUtils.toSecretKey(jwk),
                                            keyEncryptionAlgo);
        } else {
            // TODO: support elliptic curve keys
        }
        return keyDecryptionProvider;
    }
    public static KeyDecryptionAlgorithm getRSAKeyDecryptionAlgorithm(RSAPrivateKey key, String algo) {
        return new RSAOaepKeyDecryptionAlgorithm(key, algo);
    }
    public static KeyDecryptionAlgorithm getSecretKeyDecryptionAlgorithm(SecretKey key, String algo) {
        if (Algorithm.isAesKeyWrap(algo)) {
            return new AesWrapKeyDecryptionAlgorithm(key, algo);
        } else if (Algorithm.isAesGcmKeyWrap(algo)) {
            return new AesGcmWrapKeyDecryptionAlgorithm(key, algo);
        }
        return null;
    }
    public static ContentEncryptionAlgorithm getContentEncryptionAlgorithm(JsonWebKey jwk) {
        return getContentEncryptionAlgorithm(jwk, null);
    }
    public static ContentEncryptionAlgorithm getContentEncryptionAlgorithm(JsonWebKey jwk, String defaultAlgorithm) {
        String ctEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        ContentEncryptionAlgorithm contentEncryptionProvider = null;
        if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
            return getContentEncryptionAlgorithm(JwkUtils.toSecretKey(jwk),
                                                 ctEncryptionAlgo);
        }
        return contentEncryptionProvider;
    }
    public static ContentEncryptionAlgorithm getContentEncryptionAlgorithm(SecretKey key, String algorithm) {
        if (Algorithm.isAesGcm(algorithm)) {
            return new AesGcmContentEncryptionAlgorithm(key, null, algorithm);
        }
        return null;
    }
    public static ContentEncryptionAlgorithm getContentEncryptionAlgorithm(String algorithm) {
        if (Algorithm.isAesGcm(algorithm)) {
            return new AesGcmContentEncryptionAlgorithm(algorithm);
        }
        return null;
    }
    public static ContentDecryptionAlgorithm getContentDecryptionAlgorithm(String algorithm) {
        if (Algorithm.isAesGcm(algorithm)) {
            return new AesGcmContentDecryptionAlgorithm(algorithm);
        }
        return null;
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
    public static JweEncryptionProvider getDirectKeyJweEncryption(SecretKey key, String algorithm) {
        return new DirectKeyJweEncryption(getContentEncryptionAlgorithm(key, algorithm));
    }
    public static JweDecryptionProvider getDirectKeyJweDecryption(SecretKey key, String algorithm) {
        return new DirectKeyJweDecryption(key, getContentDecryptionAlgorithm(algorithm));
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
                ctEncryptionProvider = getContentEncryptionAlgorithm(jwk, contentEncryptionAlgo);
            } else {
                keyEncryptionProvider = getKeyEncryptionAlgorithm(jwk, keyEncryptionAlgo);
            }
            
        } else {
            keyEncryptionProvider = getRSAKeyEncryptionAlgorithm(
                (RSAPublicKey)KeyManagementUtils.loadPublicKey(m, props), 
                getKeyEncryptionAlgo(props, keyEncryptionAlgo));
        }
        return createJweEncryptionProvider(keyEncryptionProvider, 
                                    ctEncryptionProvider, 
                                    contentEncryptionAlgo,
                                    props.getProperty(JSON_WEB_ENCRYPTION_ZIP_ALGO_PROP));
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
        String keyEncryptionAlgo = getKeyEncryptionAlgo(props, null);
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_ENCRYPT);
            keyEncryptionAlgo = getKeyEncryptionAlgo(props, jwk.getAlgorithm());
            if ("direct".equals(keyEncryptionAlgo)) {
                contentEncryptionAlgo = getContentEncryptionAlgo(props, contentEncryptionAlgo);
                ctDecryptionKey = getContentDecryptionSecretKey(jwk, contentEncryptionAlgo);
            } else {
                keyDecryptionProvider = getKeyDecryptionAlgorithm(jwk, keyEncryptionAlgo);
            }
        } else {
            keyDecryptionProvider = getRSAKeyDecryptionAlgorithm(
                (RSAPrivateKey)KeyManagementUtils.loadPrivateKey(
                    m, props, KeyManagementUtils.RSSEC_DECRYPT_KEY_PSWD_PROVIDER), keyEncryptionAlgo);
        }
        return createJweDecryptionProvider(keyDecryptionProvider, ctDecryptionKey, contentEncryptionAlgo);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(KeyEncryptionAlgorithm keyEncryptionProvider,
                                                                    String contentEncryptionAlgo,
                                                                    String compression) {
        JweHeaders headers = 
            prepareJweHeaders(keyEncryptionProvider != null ? keyEncryptionProvider.getAlgorithm() : null,
                contentEncryptionAlgo, compression);
        return createJweEncryptionProvider(keyEncryptionProvider, headers);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(KeyEncryptionAlgorithm keyEncryptionProvider,
                                                                    JweHeaders headers) {
        String contentEncryptionAlgo = headers.getContentEncryptionAlgorithm();
        if (Algorithm.isAesCbcHmac(contentEncryptionAlgo)) { 
            return new AesCbcHmacJweEncryption(headers, keyEncryptionProvider);
        } else {
            return new WrappedKeyJweEncryption(headers, 
                                               keyEncryptionProvider,
                                               getContentEncryptionAlgorithm(contentEncryptionAlgo));
        }
    }
    public static JweDecryptionProvider createJweDecryptionProvider(KeyDecryptionAlgorithm keyDecryptionProvider,
                                                                    String contentDecryptionAlgo) {
        if (Algorithm.isAesCbcHmac(contentDecryptionAlgo)) { 
            return new AesCbcHmacJweDecryption(keyDecryptionProvider, contentDecryptionAlgo);
        } else {
            return new WrappedKeyJweDecryption(keyDecryptionProvider, 
                                               getContentDecryptionAlgorithm(contentDecryptionAlgo));
        }
    }
    
    public static boolean validateCriticalHeaders(JoseHeaders headers) {
        //TODO: Validate JWE specific constraints
        return JoseUtils.validateCriticalHeaders(headers);
    }
    private static JweHeaders prepareJweHeaders(String keyEncryptionAlgo,
                                                String contentEncryptionAlgo,
                                                String compression) {
        JweHeaders headers = new JweHeaders();
        if (keyEncryptionAlgo != null) {
            headers.setAlgorithm(keyEncryptionAlgo);
            headers.setContentEncryptionAlgorithm(contentEncryptionAlgo);
            if (compression != null) {
                headers.setZipAlgorithm(compression);
            }
        }
        return headers;
    }
    private static JweEncryptionProvider createJweEncryptionProvider(KeyEncryptionAlgorithm keyEncryptionProvider,
                                                                     ContentEncryptionAlgorithm ctEncryptionProvider,
                                                                     String contentEncryptionAlgo,
                                                                     String compression) {
        if (keyEncryptionProvider == null && ctEncryptionProvider == null) {
            throw new SecurityException();
        }
        JweHeaders headers = 
            prepareJweHeaders(keyEncryptionProvider != null ? keyEncryptionProvider.getAlgorithm() : null,
                contentEncryptionAlgo, compression);
        if (keyEncryptionProvider != null) {
            return createJweEncryptionProvider(keyEncryptionProvider, headers);
        } else {
            return new DirectKeyJweEncryption(headers, ctEncryptionProvider);
        }
    }
    private static JweDecryptionProvider createJweDecryptionProvider(KeyDecryptionAlgorithm keyDecryptionProvider,
                                                                    SecretKey ctDecryptionKey,
                                                                    String contentDecryptionAlgo) {
        if (keyDecryptionProvider == null && ctDecryptionKey == null) {
            throw new SecurityException();
        }
        if (keyDecryptionProvider != null) {
            return createJweDecryptionProvider(keyDecryptionProvider, contentDecryptionAlgo);
        } else {
            return getDirectKeyJweDecryption(ctDecryptionKey, contentDecryptionAlgo);
        }
    }
    private static String getKeyEncryptionAlgo(Properties props, String algo) {
        return algo == null ? props.getProperty(JSON_WEB_ENCRYPTION_KEY_ALGO_PROP) : algo;
    }
    private static String getContentEncryptionAlgo(Properties props, String algo) {
        return algo == null ? props.getProperty(JSON_WEB_ENCRYPTION_CEK_ALGO_PROP) : algo;
    }
    private static String encrypt(KeyEncryptionAlgorithm keyEncryptionProvider, String contentAlgo, byte[] content) {
        JweEncryptionProvider jwe = createJweEncryptionProvider(keyEncryptionProvider, contentAlgo, null);
        return jwe.encrypt(content, null);
    }
    private static byte[] decrypt(KeyDecryptionAlgorithm keyDecryptionProvider, String contentAlgo, String content) {
        JweDecryptionProvider jwe = createJweDecryptionProvider(keyDecryptionProvider, contentAlgo);
        return jwe.decrypt(content).getContent();
    }
}
