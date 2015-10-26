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

import java.nio.ByteBuffer;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.common.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyOperation;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

public final class JweUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JweUtils.class);
    
    private JweUtils() {
        
    }
    public static String encrypt(PublicKey key, KeyAlgorithm keyAlgo, ContentAlgorithm contentAlgo, 
                                 byte[] content) {
        return encrypt(key, keyAlgo, contentAlgo, content, null);
    }
    public static String encrypt(PublicKey key, KeyAlgorithm keyAlgo, 
                                 ContentAlgorithm contentAlgo, byte[] content, String ct) {
        KeyEncryptionProvider keyEncryptionProvider = getPublicKeyEncryptionProvider(key, keyAlgo);
        return encrypt(keyEncryptionProvider, contentAlgo, content, ct);
    }
    public static String encrypt(SecretKey key, KeyAlgorithm keyAlgo, ContentAlgorithm contentAlgo, 
                                 byte[] content) {
        return encrypt(key, keyAlgo, contentAlgo, content, null);
    }
    public static String encrypt(SecretKey key, KeyAlgorithm keyAlgo, ContentAlgorithm contentAlgo, 
                                 byte[] content, String ct) {
        if (keyAlgo != null) {
            KeyEncryptionProvider keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(key, keyAlgo);
            return encrypt(keyEncryptionProvider, contentAlgo, content, ct);
        } else {
            return encryptDirect(key, contentAlgo, content, ct);
        }
    }
    public static String encrypt(JsonWebKey key, ContentAlgorithm contentAlgo, byte[] content, String ct) {
        KeyEncryptionProvider keyEncryptionProvider = getKeyEncryptionProvider(key);
        return encrypt(keyEncryptionProvider, contentAlgo, content, ct);
    }
    public static String encryptDirect(SecretKey key, ContentAlgorithm contentAlgo, byte[] content) {
        return encryptDirect(key, contentAlgo, content, null);
    }
    public static String encryptDirect(SecretKey key, ContentAlgorithm contentAlgo, byte[] content, String ct) {
        JweEncryptionProvider jwe = getDirectKeyJweEncryption(key, contentAlgo);
        return jwe.encrypt(content, toJweHeaders(ct));
    }
    public static String encryptDirect(JsonWebKey key, byte[] content, String ct) {
        JweEncryptionProvider jwe = getDirectKeyJweEncryption(key);
        return jwe.encrypt(content, toJweHeaders(ct));
    }
    public static byte[] decrypt(PrivateKey key, KeyAlgorithm keyAlgo, ContentAlgorithm contentAlgo, String content) {
        KeyDecryptionProvider keyDecryptionProvider = getPrivateKeyDecryptionProvider(key, keyAlgo);
        return decrypt(keyDecryptionProvider, contentAlgo, content);
    }
    public static byte[] decrypt(SecretKey key, KeyAlgorithm keyAlgo, ContentAlgorithm contentAlgo, String content) {
        if (keyAlgo != null) {
            KeyDecryptionProvider keyDecryptionProvider = getSecretKeyDecryptionProvider(key, keyAlgo);
            return decrypt(keyDecryptionProvider, contentAlgo, content);
        } else {
            return decryptDirect(key, contentAlgo, content);
        }
    }
    public static byte[] decrypt(JsonWebKey key, ContentAlgorithm contentAlgo, String content) {
        KeyDecryptionProvider keyDecryptionProvider = getKeyDecryptionProvider(key);
        return decrypt(keyDecryptionProvider, contentAlgo, content);
    }
    public static byte[] decryptDirect(SecretKey key, ContentAlgorithm contentAlgo, String content) {
        JweDecryptionProvider jwe = getDirectKeyJweDecryption(key, contentAlgo);
        return jwe.decrypt(content).getContent();
    }
    public static byte[] decryptDirect(JsonWebKey key, String content) {
        JweDecryptionProvider jwe = getDirectKeyJweDecryption(key);
        return jwe.decrypt(content).getContent();
    }
    public static KeyEncryptionProvider getKeyEncryptionProvider(JsonWebKey jwk) {
        return getKeyEncryptionProvider(jwk, null);
    }
    public static KeyEncryptionProvider getKeyEncryptionProvider(JsonWebKey jwk, KeyAlgorithm defaultAlgorithm) {
        KeyAlgorithm keyAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm 
            : KeyAlgorithm.getAlgorithm(jwk.getAlgorithm());
        KeyEncryptionProvider keyEncryptionProvider = null;
        KeyType keyType = jwk.getKeyType();
        if (KeyType.RSA == keyType) {
            keyEncryptionProvider = getPublicKeyEncryptionProvider(JwkUtils.toRSAPublicKey(jwk, true), 
                                                                 keyAlgo);
        } else if (KeyType.OCTET == keyType) {
            keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(JwkUtils.toSecretKey(jwk), 
                                                                    keyAlgo);
        } else {
            keyEncryptionProvider = new EcdhAesWrapKeyEncryptionAlgorithm(JwkUtils.toECPublicKey(jwk),
                                        jwk.getStringProperty(JsonWebKey.EC_CURVE),
                                        keyAlgo);
        }
        return keyEncryptionProvider;
    }
    public static KeyEncryptionProvider getPublicKeyEncryptionProvider(PublicKey key, KeyAlgorithm algo) {
        if (key instanceof RSAPublicKey) {
            return new RSAKeyEncryptionAlgorithm((RSAPublicKey)key, algo);
        } else if (key instanceof ECPublicKey) {
            return new EcdhAesWrapKeyEncryptionAlgorithm((ECPublicKey)key, algo);
        }
        
        return null;
    }
    public static KeyEncryptionProvider getSecretKeyEncryptionAlgorithm(SecretKey key, KeyAlgorithm algo) {
        if (AlgorithmUtils.isAesKeyWrap(algo.getJwaName())) {
            return new AesWrapKeyEncryptionAlgorithm(key, algo);
        } else if (AlgorithmUtils.isAesGcmKeyWrap(algo.getJwaName())) {
            return new AesGcmWrapKeyEncryptionAlgorithm(key, algo);
        }
        return null;
    }
    public static KeyDecryptionProvider getKeyDecryptionProvider(JsonWebKey jwk) {
        return getKeyDecryptionProvider(jwk, null);
    }
    
    public static KeyDecryptionProvider getKeyDecryptionProvider(JsonWebKey jwk, KeyAlgorithm defaultAlgorithm) {
        KeyAlgorithm keyAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm 
            : KeyAlgorithm.getAlgorithm(jwk.getAlgorithm());
        KeyDecryptionProvider keyDecryptionProvider = null;
        KeyType keyType = jwk.getKeyType();
        if (KeyType.RSA == keyType) {
            keyDecryptionProvider = getPrivateKeyDecryptionProvider(JwkUtils.toRSAPrivateKey(jwk), 
                                                                 keyAlgo);
        } else if (KeyType.OCTET == keyType) {
            keyDecryptionProvider = getSecretKeyDecryptionProvider(JwkUtils.toSecretKey(jwk),
                                            keyAlgo);
        } else {
            keyDecryptionProvider = getPrivateKeyDecryptionProvider(JwkUtils.toECPrivateKey(jwk), 
                                                                     keyAlgo);
        }
        return keyDecryptionProvider;
    }
    public static KeyDecryptionProvider getPrivateKeyDecryptionProvider(PrivateKey key, KeyAlgorithm algo) {
        if (key instanceof RSAPrivateKey) {
            return new RSAKeyDecryptionAlgorithm((RSAPrivateKey)key, algo);
        } else if (key instanceof ECPrivateKey) {
            return new EcdhAesWrapKeyDecryptionAlgorithm((ECPrivateKey)key, algo);
        }
        
        return null;
    }
    public static KeyDecryptionProvider getSecretKeyDecryptionProvider(SecretKey key, KeyAlgorithm algo) {
        if (AlgorithmUtils.isAesKeyWrap(algo.getJwaName())) {
            return new AesWrapKeyDecryptionAlgorithm(key, algo);
        } else if (AlgorithmUtils.isAesGcmKeyWrap(algo.getJwaName())) {
            return new AesGcmWrapKeyDecryptionAlgorithm(key, algo);
        }
        return null;
    }
    public static ContentEncryptionProvider getContentEncryptionAlgorithm(JsonWebKey jwk) {
        return getContentEncryptionAlgorithm(jwk, null);
    }
    public static ContentEncryptionProvider getContentEncryptionAlgorithm(JsonWebKey jwk, String defaultAlgorithm) {
        String ctEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        ContentEncryptionProvider contentEncryptionProvider = null;
        KeyType keyType = jwk.getKeyType();
        if (KeyType.OCTET == keyType) {
            return getContentEncryptionAlgorithm(JwkUtils.toSecretKey(jwk),
                                                 getContentAlgo(ctEncryptionAlgo));
        }
        return contentEncryptionProvider;
    }
    public static ContentEncryptionProvider getContentEncryptionAlgorithm(SecretKey key, 
                                                                          ContentAlgorithm algorithm) {
        if (AlgorithmUtils.isAesGcm(algorithm.getJwaName())) {
            return new AesGcmContentEncryptionAlgorithm(key, null, algorithm);
        }
        return null;
    }
    public static ContentEncryptionProvider getContentEncryptionAlgorithm(String algorithm) {
        if (AlgorithmUtils.isAesGcm(algorithm)) {
            return new AesGcmContentEncryptionAlgorithm(getContentAlgo(algorithm));
        }
        return null;
    }
    public static ContentDecryptionProvider getContentDecryptionProvider(ContentAlgorithm algorithm) {
        if (AlgorithmUtils.isAesGcm(algorithm.getJwaName())) {
            return new AesGcmContentDecryptionAlgorithm(algorithm);
        }
        return null;
    }
    public static SecretKey getContentDecryptionSecretKey(JsonWebKey jwk) {
        return getContentDecryptionSecretKey(jwk, null);
    }
    public static SecretKey getContentDecryptionSecretKey(JsonWebKey jwk, String defaultAlgorithm) {
        String ctEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        KeyType keyType = jwk.getKeyType();
        if (KeyType.OCTET == keyType && AlgorithmUtils.isAesGcm(ctEncryptionAlgo)) {
            return JwkUtils.toSecretKey(jwk);
        }
        return null;
    }
    private static ContentAlgorithm getContentAlgo(String algo) {
        return ContentAlgorithm.getAlgorithm(algo);
    }
    public static JweEncryption getDirectKeyJweEncryption(JsonWebKey key) {
        return getDirectKeyJweEncryption(JwkUtils.toSecretKey(key), 
                                         getContentAlgo(key.getAlgorithm()));
    }
    public static JweEncryption getDirectKeyJweEncryption(SecretKey key, ContentAlgorithm algo) {
        if (AlgorithmUtils.isAesCbcHmac(algo.getJwaName())) {
            return new AesCbcHmacJweEncryption(algo, key.getEncoded(), 
                                               null, new DirectKeyEncryptionAlgorithm());
        } else {
            return new JweEncryption(new DirectKeyEncryptionAlgorithm(), 
                                 getContentEncryptionAlgorithm(key, algo));
        }
    }
    public static JweDecryption getDirectKeyJweDecryption(JsonWebKey key) {
        return getDirectKeyJweDecryption(JwkUtils.toSecretKey(key), getContentAlgo(key.getAlgorithm()));
    }
    public static JweDecryption getDirectKeyJweDecryption(SecretKey key, ContentAlgorithm algorithm) {
        if (AlgorithmUtils.isAesCbcHmac(algorithm.getJwaName())) { 
            return new AesCbcHmacJweDecryption(new DirectKeyDecryptionAlgorithm(key), algorithm);
        } else {
            return new JweDecryption(new DirectKeyDecryptionAlgorithm(key), 
                                 getContentDecryptionProvider(algorithm));
        }
    }
    public static JweEncryptionProvider loadEncryptionProvider(boolean required) {
        return loadEncryptionProvider(null, required);
    }
    @SuppressWarnings("deprecation")
    public static JweEncryptionProvider loadEncryptionProvider(JweHeaders headers, boolean required) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        Properties props = KeyManagementUtils.loadStoreProperties(m, required, 
                                                                  JoseConstants.RSSEC_ENCRYPTION_OUT_PROPS, 
                                                                  JoseConstants.RSSEC_ENCRYPTION_PROPS);
        if (props == null) {
            return null;
        }
        
        boolean includeCert = 
            headers != null && MessageUtils.isTrue(
                MessageUtils.getContextualProperty(m, JoseConstants.RSSEC_ENCRYPTION_INCLUDE_CERT, 
                                                   JoseConstants.RSSEC_INCLUDE_CERT));
        boolean includeCertSha1 = headers != null && MessageUtils.isTrue(
                MessageUtils.getContextualProperty(m, JoseConstants.RSSEC_ENCRYPTION_INCLUDE_CERT_SHA1, 
                                                   JoseConstants.RSSEC_INCLUDE_CERT_SHA1));
        
        KeyEncryptionProvider keyEncryptionProvider = null;
        String keyEncryptionAlgo = getKeyEncryptionAlgo(m, props, null, null);
        KeyAlgorithm keyAlgo = KeyAlgorithm.getAlgorithm(keyEncryptionAlgo); 
        String contentEncryptionAlgo = getContentEncryptionAlgo(m, props, null);
        ContentEncryptionProvider ctEncryptionProvider = null;
        if (JoseConstants.HEADER_JSON_WEB_KEY.equals(props.get(JoseConstants.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, KeyOperation.ENCRYPT);
            if ("direct".equals(keyEncryptionAlgo)) {
                contentEncryptionAlgo = getContentEncryptionAlgo(m, props, jwk.getAlgorithm());
                ctEncryptionProvider = getContentEncryptionAlgorithm(jwk, contentEncryptionAlgo);
            } else {
                keyEncryptionAlgo = getKeyEncryptionAlgo(m, props, jwk.getAlgorithm(), 
                                                         getDefaultKeyAlgo(jwk));
                keyEncryptionProvider = getKeyEncryptionProvider(jwk, keyAlgo);
                
                boolean includePublicKey = headers != null && MessageUtils.isTrue(
                    MessageUtils.getContextualProperty(m, JoseConstants.RSSEC_ENCRYPTION_INCLUDE_PUBLIC_KEY,
                                                       JoseConstants.RSSEC_INCLUDE_PUBLIC_KEY));
                boolean includeKeyId = headers != null && MessageUtils.isTrue(
                    MessageUtils.getContextualProperty(m, JoseConstants.RSSEC_ENCRYPTION_INCLUDE_KEY_ID,
                                                       JoseConstants.RSSEC_INCLUDE_KEY_ID));
                
                if (includeCert) {
                    JwkUtils.includeCertChain(jwk, headers, keyEncryptionAlgo);
                }
                if (includeCertSha1 && headers != null) {
                    String digest = KeyManagementUtils.loadDigestAndEncodeX509Certificate(m, props);
                    if (digest != null) {
                        headers.setX509Thumbprint(digest);
                    }
                }
                if (includePublicKey) {
                    JwkUtils.includePublicKey(jwk, headers, keyEncryptionAlgo);
                }
                if (includeKeyId && jwk.getKeyId() != null && headers != null) {
                    headers.setKeyId(jwk.getKeyId());
                }
            }
        } else {
            keyEncryptionProvider = getPublicKeyEncryptionProvider(
                KeyManagementUtils.loadPublicKey(m, props), 
                keyAlgo);
            if (includeCert) {
                headers.setX509Chain(KeyManagementUtils.loadAndEncodeX509CertificateOrChain(m, props));
            }
            if (includeCertSha1 && headers != null) {
                String digest = KeyManagementUtils.loadDigestAndEncodeX509Certificate(m, props);
                if (digest != null) {
                    headers.setX509Thumbprint(digest);
                }
            }
        }
        
        String compression = props.getProperty(JoseConstants.RSSEC_ENCRYPTION_ZIP_ALGORITHM);
        if (compression == null) {
            compression = props.getProperty(JoseConstants.DEPR_RSSEC_ENCRYPTION_ZIP_ALGORITHM);
        }
        return createJweEncryptionProvider(keyEncryptionProvider, 
                                    ctEncryptionProvider, 
                                    contentEncryptionAlgo,
                                    compression);
    }
    public static JweDecryptionProvider loadDecryptionProvider(boolean required) {
        return loadDecryptionProvider(null, required);
    }
    public static JweDecryptionProvider loadDecryptionProvider(JweHeaders inHeaders, boolean required) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        Properties props = KeyManagementUtils.loadStoreProperties(m, required, 
                                                                  JoseConstants.RSSEC_ENCRYPTION_IN_PROPS, 
                                                                  JoseConstants.RSSEC_ENCRYPTION_PROPS);
        if (props == null) {
            return null;
        }    
        
        KeyDecryptionProvider keyDecryptionProvider = null;
        String contentEncryptionAlgo = getContentEncryptionAlgo(m, props, null);
        SecretKey ctDecryptionKey = null;
        String keyEncryptionAlgo = getKeyEncryptionAlgo(m, props, null, null);
        if (inHeaders != null && inHeaders.getHeader(JoseConstants.HEADER_X509_CHAIN) != null) {
            //TODO: optionally validate inHeaders.getAlgorithm against a property in props
            // Supporting loading a private key via a certificate for now
            List<X509Certificate> chain = KeyManagementUtils.toX509CertificateChain(inHeaders.getX509Chain());
            KeyManagementUtils.validateCertificateChain(props, chain);
            PrivateKey privateKey = 
                KeyManagementUtils.loadPrivateKey(m, props, chain, KeyOperation.DECRYPT);
            contentEncryptionAlgo = inHeaders.getContentEncryptionAlgorithm().getJwaName();
            keyDecryptionProvider = getPrivateKeyDecryptionProvider(privateKey, 
                                                                 inHeaders.getKeyEncryptionAlgorithm());
        } else {
            if (JoseConstants.HEADER_JSON_WEB_KEY.equals(props.get(JoseConstants.RSSEC_KEY_STORE_TYPE))) {
                JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, KeyOperation.DECRYPT);
                if ("direct".equals(keyEncryptionAlgo)) {
                    contentEncryptionAlgo = getContentEncryptionAlgo(m, props, jwk.getAlgorithm());
                    ctDecryptionKey = getContentDecryptionSecretKey(jwk, contentEncryptionAlgo);
                } else {
                    keyEncryptionAlgo = getKeyEncryptionAlgo(m, props, jwk.getAlgorithm(),
                                                             getDefaultKeyAlgo(jwk));
                    keyDecryptionProvider = getKeyDecryptionProvider(jwk, 
                                                                      KeyAlgorithm.getAlgorithm(keyEncryptionAlgo));
                }
            } else {
                keyDecryptionProvider = getPrivateKeyDecryptionProvider(
                    KeyManagementUtils.loadPrivateKey(m, props, KeyOperation.DECRYPT), 
                    KeyAlgorithm.getAlgorithm(keyEncryptionAlgo));
            }
        }
        return createJweDecryptionProvider(keyDecryptionProvider, ctDecryptionKey, 
                                           getContentAlgo(contentEncryptionAlgo));
    }
    public static JweEncryptionProvider createJweEncryptionProvider(PublicKey key,
                                                                    KeyAlgorithm keyAlgo,
                                                                    ContentAlgorithm contentEncryptionAlgo,
                                                                    String compression) {
        KeyEncryptionProvider keyEncryptionProvider = getPublicKeyEncryptionProvider(key, keyAlgo);
        return createJweEncryptionProvider(keyEncryptionProvider, contentEncryptionAlgo, compression);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(PublicKey key, JweHeaders headers) {
        KeyEncryptionProvider keyEncryptionProvider = getPublicKeyEncryptionProvider(key, 
                                                           headers.getKeyEncryptionAlgorithm());
        return createJweEncryptionProvider(keyEncryptionProvider, headers);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(SecretKey key,
                                                                    KeyAlgorithm keyAlgo,
                                                                    ContentAlgorithm contentEncryptionAlgo,
                                                                    String compression) {
        KeyEncryptionProvider keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(key, keyAlgo);
        return createJweEncryptionProvider(keyEncryptionProvider, contentEncryptionAlgo, compression);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(SecretKey key, JweHeaders headers) {
        KeyEncryptionProvider keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(key, 
                                                           headers.getKeyEncryptionAlgorithm());
        return createJweEncryptionProvider(keyEncryptionProvider, headers);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(JsonWebKey key,
                                                                    ContentAlgorithm contentEncryptionAlgo,
                                                                    String compression) {
        KeyEncryptionProvider keyEncryptionProvider = getKeyEncryptionProvider(key);
        return createJweEncryptionProvider(keyEncryptionProvider, contentEncryptionAlgo, compression);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(JsonWebKey key, JweHeaders headers) {
        KeyEncryptionProvider keyEncryptionProvider = getKeyEncryptionProvider(key);
        return createJweEncryptionProvider(keyEncryptionProvider, headers);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(KeyEncryptionProvider keyEncryptionProvider,
                                                                    ContentAlgorithm contentEncryptionAlgo,
                                                                    String compression) {
        JweHeaders headers = 
            prepareJweHeaders(keyEncryptionProvider != null ? keyEncryptionProvider.getAlgorithm().getJwaName() : null,
                contentEncryptionAlgo.getJwaName(), compression);
        return createJweEncryptionProvider(keyEncryptionProvider, headers);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(KeyEncryptionProvider keyEncryptionProvider,
                                                                    JweHeaders headers) {
        String contentEncryptionAlgo = headers.getContentEncryptionAlgorithm().getJwaName();
        if (AlgorithmUtils.isAesCbcHmac(contentEncryptionAlgo)) { 
            return new AesCbcHmacJweEncryption(getContentAlgo(contentEncryptionAlgo), keyEncryptionProvider);
        } else {
            return new JweEncryption(keyEncryptionProvider,
                                     getContentEncryptionAlgorithm(contentEncryptionAlgo));
        }
    }
    public static JweDecryptionProvider createJweDecryptionProvider(PrivateKey key,
                                                                    KeyAlgorithm keyAlgo,
                                                                    ContentAlgorithm contentDecryptionAlgo) {
        return createJweDecryptionProvider(getPrivateKeyDecryptionProvider(key, keyAlgo), contentDecryptionAlgo);
    }
    public static JweDecryptionProvider createJweDecryptionProvider(SecretKey key,
                                                                    KeyAlgorithm keyAlgo,
                                                                    ContentAlgorithm contentDecryptionAlgo) {
        return createJweDecryptionProvider(getSecretKeyDecryptionProvider(key, keyAlgo), contentDecryptionAlgo);
    }
    public static JweDecryptionProvider createJweDecryptionProvider(JsonWebKey key,
                                                                    ContentAlgorithm contentDecryptionAlgo) {
        return createJweDecryptionProvider(getKeyDecryptionProvider(key), contentDecryptionAlgo);
    }
    public static JweDecryptionProvider createJweDecryptionProvider(KeyDecryptionProvider keyDecryptionProvider,
                                                                    ContentAlgorithm contentDecryptionAlgo) {
        if (AlgorithmUtils.isAesCbcHmac(contentDecryptionAlgo.getJwaName())) { 
            return new AesCbcHmacJweDecryption(keyDecryptionProvider, contentDecryptionAlgo);
        } else {
            return new JweDecryption(keyDecryptionProvider, 
                                     getContentDecryptionProvider(contentDecryptionAlgo));
        }
    }
    public static boolean validateCriticalHeaders(JoseHeaders headers) {
        //TODO: Validate JWE specific constraints
        return JoseUtils.validateCriticalHeaders(headers);
    }
    public static byte[] getECDHKey(JsonWebKey privateKey, 
                                    JsonWebKey peerPublicKey,
                                    byte[] partyUInfo,
                                    byte[] partyVInfo,
                                    String algoName,
                                    int algoKeyBitLen) { 
        return getECDHKey(JwkUtils.toECPrivateKey(privateKey),
                          JwkUtils.toECPublicKey(peerPublicKey),
                          partyUInfo, partyVInfo, algoName, algoKeyBitLen);
    }
    public static byte[] getECDHKey(ECPrivateKey privateKey, 
                                    ECPublicKey peerPublicKey,
                                    byte[] partyUInfo,
                                    byte[] partyVInfo,
                                    String algoName,
                                    int algoKeyBitLen) { 
        byte[] keyZ = generateKeyZ(privateKey, peerPublicKey);
        return calculateDerivedKey(keyZ, algoName, partyUInfo, partyVInfo, algoKeyBitLen);
    }
    public static byte[] getAdditionalAuthenticationData(String headersJson, byte[] aad) {
        byte[] headersAAD = JweHeaders.toCipherAdditionalAuthData(headersJson);
        if (aad != null) {
            // JWE JSON can provide the extra aad
            byte[] newAAD = Arrays.copyOf(headersAAD, headersAAD.length + 1 + aad.length);
            newAAD[headersAAD.length] = '.';
            System.arraycopy(aad, 0, newAAD, headersAAD.length + 1, aad.length);
            return newAAD;
        } else {
            return headersAAD;
        }
    }
    private static byte[] calculateDerivedKey(byte[] keyZ, 
                                              String algoName,
                                              byte[] apuBytes, 
                                              byte[] apvBytes,
                                              int algoKeyBitLen) {
        final byte[] emptyPartyInfo = new byte[4];
       
        if (apuBytes != null && apvBytes != null && Arrays.equals(apuBytes, apvBytes)) {
            LOG.warning("Derived key calculation problem: apu equals to apv");
            throw new JweException(JweException.Error.KEY_ENCRYPTION_FAILURE);
        }
        byte[] algorithmId = concatenateDatalenAndData(StringUtils.toBytesASCII(algoName));
        byte[] partyUInfo = apuBytes == null ? emptyPartyInfo : concatenateDatalenAndData(apuBytes);
        byte[] partyVInfo = apvBytes == null ? emptyPartyInfo : concatenateDatalenAndData(apvBytes);
        byte[] suppPubInfo = datalenToBytes(algoKeyBitLen);
       
        byte[] otherInfo = new byte[algorithmId.length 
                                   + partyUInfo.length
                                   + partyVInfo.length
                                   + suppPubInfo.length];
        System.arraycopy(algorithmId, 0, otherInfo, 0, algorithmId.length);
        System.arraycopy(partyUInfo, 0, otherInfo, algorithmId.length, partyUInfo.length);
        System.arraycopy(partyVInfo, 0, otherInfo, algorithmId.length + partyUInfo.length, partyVInfo.length);
        System.arraycopy(suppPubInfo, 0, otherInfo, algorithmId.length + partyUInfo.length + partyVInfo.length,
                         suppPubInfo.length);
       
       
        byte[] concatKDF = new byte[36 + otherInfo.length];
        concatKDF[3] = 1;
        System.arraycopy(keyZ, 0, concatKDF, 4, keyZ.length);
        System.arraycopy(otherInfo, 0, concatKDF, 36, otherInfo.length);
        try {
            byte[] round1Hash = MessageDigestUtils.createDigest(concatKDF, MessageDigestUtils.ALGO_SHA_256);
            return Arrays.copyOf(round1Hash, algoKeyBitLen / 8);
        } catch (Exception ex) {
            LOG.warning("Derived key calculation problem: round hash1 error");
            throw new JweException(JweException.Error.KEY_ENCRYPTION_FAILURE);
        }
    }
    private static byte[] generateKeyZ(ECPrivateKey privateKey, ECPublicKey publicKey) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(privateKey);
            ka.doPhase(publicKey, true);
            return ka.generateSecret();
        } catch (Exception ex) {
            LOG.warning("Derived key calculation problem");
            throw new JweException(JweException.Error.KEY_ENCRYPTION_FAILURE);
        }
    }
    private static byte[] concatenateDatalenAndData(byte[] bytesASCII) {
        final byte[] datalen = datalenToBytes(bytesASCII.length);
        byte[] all = new byte[4 + bytesASCII.length];
        System.arraycopy(datalen, 0, all, 0, 4);
        System.arraycopy(bytesASCII, 0, all, 4, bytesASCII.length);
        return all;
    }
    private static byte[] datalenToBytes(int len) {
        ByteBuffer buf = ByteBuffer.allocate(4);
        return buf.putInt(len).array();
    }
    private static JweHeaders prepareJweHeaders(String keyEncryptionAlgo,
                                                String contentEncryptionAlgo,
                                                String compression) {
        JweHeaders headers = new JweHeaders();
        if (keyEncryptionAlgo != null) {
            headers.setKeyEncryptionAlgorithm(KeyAlgorithm.getAlgorithm(keyEncryptionAlgo));
        }
        headers.setContentEncryptionAlgorithm(ContentAlgorithm.getAlgorithm(contentEncryptionAlgo));
        if (compression != null) {
            headers.setZipAlgorithm(compression);
        }
        return headers;
    }
    private static JweEncryptionProvider createJweEncryptionProvider(KeyEncryptionProvider keyEncryptionProvider,
                                                                     ContentEncryptionProvider ctEncryptionProvider,
                                                                     String contentEncryptionAlgo,
                                                                     String compression) {
        if (keyEncryptionProvider == null && ctEncryptionProvider == null) {
            LOG.warning("Key or content encryptor is not available");
            throw new JweException(JweException.Error.NO_ENCRYPTOR);
        }
        JweHeaders headers = 
            prepareJweHeaders(keyEncryptionProvider != null ? keyEncryptionProvider.getAlgorithm().getJwaName() : null,
                contentEncryptionAlgo, compression);
        if (keyEncryptionProvider != null) {
            return createJweEncryptionProvider(keyEncryptionProvider, headers);
        } else {
            return new JweEncryption(new DirectKeyEncryptionAlgorithm(), ctEncryptionProvider);
        }
    }
    private static JweDecryptionProvider createJweDecryptionProvider(KeyDecryptionProvider keyDecryptionProvider,
                                                                    SecretKey ctDecryptionKey,
                                                                    ContentAlgorithm contentDecryptionAlgo) {
        if (keyDecryptionProvider == null && ctDecryptionKey == null) {
            LOG.warning("Key or content encryptor is not available");
            throw new JweException(JweException.Error.NO_ENCRYPTOR);
        }
        if (keyDecryptionProvider != null) {
            return createJweDecryptionProvider(keyDecryptionProvider, contentDecryptionAlgo);
        } else {
            return getDirectKeyJweDecryption(ctDecryptionKey, contentDecryptionAlgo);
        }
    }
    @SuppressWarnings("deprecation")
    private static String getKeyEncryptionAlgo(Message m, Properties props, 
                                               String algo, String defaultAlgo) {
        if (algo == null) {
            if (defaultAlgo == null) {
                defaultAlgo = AlgorithmUtils.RSA_OAEP_ALGO;
            }
            
            // Check for deprecated identifier first
            String encAlgo = props.getProperty(JoseConstants.DEPR_RSSEC_ENCRYPTION_KEY_ALGORITHM);
            if (encAlgo == null) {
                encAlgo = (String)m.getContextualProperty(JoseConstants.DEPR_RSSEC_ENCRYPTION_KEY_ALGORITHM);
            }
            if (encAlgo != null) {
                return encAlgo;
            }
            
            // Otherwise check newer identifier
            return KeyManagementUtils.getKeyAlgorithm(m, props, 
                                                      JoseConstants.RSSEC_ENCRYPTION_KEY_ALGORITHM, defaultAlgo);
        }
        return algo;
    }
    private static String getDefaultKeyAlgo(JsonWebKey jwk) {
        KeyType keyType = jwk.getKeyType();
        if (KeyType.OCTET == keyType) {
            return AlgorithmUtils.A128GCMKW_ALGO;
        } else {
            return AlgorithmUtils.RSA_OAEP_ALGO;
        }
    }
    @SuppressWarnings("deprecation")
    private static String getContentEncryptionAlgo(Message m, Properties props, String algo) {
        if (algo == null) {
            // Check for deprecated identifier first
            String encAlgo = props.getProperty(JoseConstants.DEPR_RSSEC_ENCRYPTION_CONTENT_ALGORITHM);
            if (encAlgo == null) {
                encAlgo = (String)m.getContextualProperty(JoseConstants.DEPR_RSSEC_ENCRYPTION_CONTENT_ALGORITHM);
            }
            if (encAlgo != null) {
                return encAlgo;
            }
            
            // Otherwise check newer identifier
            return KeyManagementUtils.getKeyAlgorithm(m, props, 
                                                      JoseConstants.RSSEC_ENCRYPTION_CONTENT_ALGORITHM, 
                                                      AlgorithmUtils.A128GCM_ALGO);
        }
        return algo;
    }
    private static String encrypt(KeyEncryptionProvider keyEncryptionProvider, 
                                  ContentAlgorithm contentAlgo, byte[] content, String ct) {
        JweEncryptionProvider jwe = createJweEncryptionProvider(keyEncryptionProvider, contentAlgo, null);
        return jwe.encrypt(content, toJweHeaders(ct));
    }
    private static byte[] decrypt(KeyDecryptionProvider keyDecryptionProvider, ContentAlgorithm contentAlgo, 
                                  String content) {
        JweDecryptionProvider jwe = createJweDecryptionProvider(keyDecryptionProvider, contentAlgo);
        return jwe.decrypt(content).getContent();
    }
    private static JweHeaders toJweHeaders(String ct) {
        return new JweHeaders(Collections.<String, Object>singletonMap(JoseConstants.HEADER_CONTENT_TYPE, ct));
    }
    public static void validateJweCertificateChain(List<X509Certificate> certs) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        Properties props = KeyManagementUtils.loadStoreProperties(m, true, 
                                                                  JoseConstants.RSSEC_ENCRYPTION_IN_PROPS, 
                                                                  JoseConstants.RSSEC_ENCRYPTION_PROPS);
        KeyManagementUtils.validateCertificateChain(props, certs);
    }

    public static void checkEncryptionKeySize(Key key) {
        if (key instanceof RSAKey && ((RSAKey)key).getModulus().bitLength() < 2048) {
            LOG.fine("A key of size: " + ((RSAKey)key).getModulus().bitLength()
                     + " was used with an RSA encryption algorithm. 2048 is the minimum size that is accepted");
            throw new JweException(JweException.Error.KEY_DECRYPTION_FAILURE);
        }
    }
}
