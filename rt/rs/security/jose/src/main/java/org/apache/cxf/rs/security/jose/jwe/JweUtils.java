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
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.MessageDigestUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
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
    private static final String RSSEC_ENCRYPTION_OUT_PROPS = "rs.security.encryption.out.properties";
    private static final String RSSEC_ENCRYPTION_IN_PROPS = "rs.security.encryption.in.properties";
    private static final String RSSEC_ENCRYPTION_PROPS = "rs.security.encryption.properties";
    private static final String RSSEC_ENCRYPTION_REPORT_KEY_PROP = "rs.security.jwe.report.public.key";
    
    private JweUtils() {
        
    }
    public static String encrypt(RSAPublicKey key, String keyAlgo, String contentAlgo, byte[] content) {
        return encrypt(key, keyAlgo, contentAlgo, content, null);
    }
    public static String encrypt(RSAPublicKey key, String keyAlgo, String contentAlgo, byte[] content, String ct) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getRSAKeyEncryptionAlgorithm(key, keyAlgo);
        return encrypt(keyEncryptionProvider, contentAlgo, content, ct);
    }
    public static String encrypt(SecretKey key, String keyAlgo, String contentAlgo, byte[] content) {
        return encrypt(key, keyAlgo, contentAlgo, content, null);
    }
    public static String encrypt(SecretKey key, String keyAlgo, String contentAlgo, byte[] content, String ct) {
        if (keyAlgo != null) {
            KeyEncryptionAlgorithm keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(key, keyAlgo);
            return encrypt(keyEncryptionProvider, contentAlgo, content, ct);
        } else {
            return encryptDirect(key, contentAlgo, content, ct);
        }
    }
    public static String encrypt(JsonWebKey key, String contentAlgo, byte[] content, String ct) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getKeyEncryptionAlgorithm(key);
        return encrypt(keyEncryptionProvider, contentAlgo, content, ct);
    }
    public static String encryptDirect(SecretKey key, String contentAlgo, byte[] content) {
        return encryptDirect(key, contentAlgo, content, null);
    }
    public static String encryptDirect(SecretKey key, String contentAlgo, byte[] content, String ct) {
        JweEncryptionProvider jwe = getDirectKeyJweEncryption(key, contentAlgo);
        return jwe.encrypt(content, toJweHeaders(ct));
    }
    public static String encryptDirect(JsonWebKey key, byte[] content, String ct) {
        JweEncryptionProvider jwe = getDirectKeyJweEncryption(key);
        return jwe.encrypt(content, toJweHeaders(ct));
    }
    public static byte[] decrypt(RSAPrivateKey key, String keyAlgo, String contentAlgo, String content) {
        KeyDecryptionAlgorithm keyDecryptionProvider = getRSAKeyDecryptionAlgorithm(key, keyAlgo);
        return decrypt(keyDecryptionProvider, contentAlgo, content);
    }
    public static byte[] decrypt(SecretKey key, String keyAlgo, String contentAlgo, String content) {
        if (keyAlgo != null) {
            KeyDecryptionAlgorithm keyDecryptionProvider = getSecretKeyDecryptionAlgorithm(key, keyAlgo);
            return decrypt(keyDecryptionProvider, contentAlgo, content);
        } else {
            return decryptDirect(key, contentAlgo, content);
        }
    }
    public static byte[] decrypt(JsonWebKey key, String contentAlgo, String content) {
        KeyDecryptionAlgorithm keyDecryptionProvider = getKeyDecryptionAlgorithm(key);
        return decrypt(keyDecryptionProvider, contentAlgo, content);
    }
    public static byte[] decryptDirect(SecretKey key, String contentAlgo, String content) {
        JweDecryptionProvider jwe = getDirectKeyJweDecryption(key, contentAlgo);
        return jwe.decrypt(content).getContent();
    }
    public static byte[] decryptDirect(JsonWebKey key, String content) {
        JweDecryptionProvider jwe = getDirectKeyJweDecryption(key);
        return jwe.decrypt(content).getContent();
    }
    public static KeyEncryptionAlgorithm getKeyEncryptionAlgorithm(JsonWebKey jwk) {
        return getKeyEncryptionAlgorithm(jwk, null);
    }
    public static KeyEncryptionAlgorithm getKeyEncryptionAlgorithm(JsonWebKey jwk, String defaultAlgorithm) {
        String keyEncryptionAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        KeyEncryptionAlgorithm keyEncryptionProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            keyEncryptionProvider = getRSAKeyEncryptionAlgorithm(JwkUtils.toRSAPublicKey(jwk, true), 
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
        return new RSAKeyEncryptionAlgorithm(key, algo);
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
        return new RSAKeyDecryptionAlgorithm(key, algo);
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
    public static JweEncryption getDirectKeyJweEncryption(JsonWebKey key) {
        return new JweEncryption(new DirectKeyEncryptionAlgorithm(),
                                 getContentEncryptionAlgorithm(key, key.getAlgorithm()));
    }
    public static JweEncryption getDirectKeyJweEncryption(SecretKey key, String algorithm) {
        if (Algorithm.isAesCbcHmac(algorithm)) {
            return new AesCbcHmacJweEncryption(algorithm, key.getEncoded(), 
                                               null, new DirectKeyEncryptionAlgorithm());
        } else {
            return new JweEncryption(new DirectKeyEncryptionAlgorithm(), 
                                 getContentEncryptionAlgorithm(key, algorithm));
        }
    }
    public static JweDecryption getDirectKeyJweDecryption(SecretKey key, String algorithm) {
        return new JweDecryption(new DirectKeyDecryptionAlgorithm(key), 
                                 getContentDecryptionAlgorithm(algorithm));
    }
    public static JweDecryption getDirectKeyJweDecryption(JsonWebKey key) {
        return new JweDecryption(new DirectKeyDecryptionAlgorithm(JwkUtils.toSecretKey(key)), 
                                 getContentDecryptionAlgorithm(key.getAlgorithm()));
    }
    public static JweEncryptionProvider loadEncryptionProvider(boolean required) {
        return loadEncryptionProvider(null, required);
    }
    public static JweEncryptionProvider loadEncryptionProvider(JweHeaders headers, boolean required) {
        Message m = JAXRSUtils.getCurrentMessage();        
        Properties props = KeyManagementUtils.loadStoreProperties(m, required, 
                                                                  RSSEC_ENCRYPTION_OUT_PROPS, RSSEC_ENCRYPTION_PROPS);
        if (props == null) {
            return null;
        }
        
        boolean reportPublicKey = 
            headers != null && MessageUtils.isTrue(
                MessageUtils.getContextualProperty(m, RSSEC_ENCRYPTION_REPORT_KEY_PROP, 
                                                   KeyManagementUtils.RSSEC_REPORT_KEY_PROP));
        
        KeyEncryptionAlgorithm keyEncryptionProvider = null;
        String keyEncryptionAlgo = getKeyEncryptionAlgo(m, props, null, null);
        String contentEncryptionAlgo = getContentEncryptionAlgo(m, props, null);
        ContentEncryptionAlgorithm ctEncryptionProvider = null;
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_ENCRYPT);
            keyEncryptionAlgo = getKeyEncryptionAlgo(m, props, jwk.getAlgorithm(), 
                                                     getDefaultKeyAlgo(jwk));
            if ("direct".equals(keyEncryptionAlgo)) {
                contentEncryptionAlgo = getContentEncryptionAlgo(m, props, jwk.getAlgorithm());
                ctEncryptionProvider = getContentEncryptionAlgorithm(jwk, contentEncryptionAlgo);
            } else {
                keyEncryptionProvider = getKeyEncryptionAlgorithm(jwk, keyEncryptionAlgo);
                if (reportPublicKey) {
                    JwkUtils.setPublicKeyInfo(jwk, headers, keyEncryptionAlgo);
                }
            }
        } else {
            keyEncryptionProvider = getRSAKeyEncryptionAlgorithm(
                (RSAPublicKey)KeyManagementUtils.loadPublicKey(m, props), 
                keyEncryptionAlgo);
            if (reportPublicKey) {
                headers.setX509Chain(KeyManagementUtils.loadAndEncodeX509CertificateOrChain(m, props));
            }
            
        }
        return createJweEncryptionProvider(keyEncryptionProvider, 
                                    ctEncryptionProvider, 
                                    contentEncryptionAlgo,
                                    props.getProperty(JSON_WEB_ENCRYPTION_ZIP_ALGO_PROP));
    }
    public static JweDecryptionProvider loadDecryptionProvider(boolean required) {
        return loadDecryptionProvider(null, required);
    }
    public static JweDecryptionProvider loadDecryptionProvider(JweHeaders inHeaders, boolean required) {
        Message m = JAXRSUtils.getCurrentMessage();
        Properties props = KeyManagementUtils.loadStoreProperties(m, required, 
                                                                  RSSEC_ENCRYPTION_IN_PROPS, RSSEC_ENCRYPTION_PROPS);
        if (props == null) {
            return null;
        }    
        
        KeyDecryptionAlgorithm keyDecryptionProvider = null;
        String contentEncryptionAlgo = getContentEncryptionAlgo(m, props, null);
        SecretKey ctDecryptionKey = null;
        String keyEncryptionAlgo = getKeyEncryptionAlgo(m, props, null, null);
        if (inHeaders != null && inHeaders.getHeader(JoseConstants.HEADER_X509_CHAIN) != null) {
            //TODO: validate incoming public keys or certificates  
            //TODO: optionally validate inHeaders.getAlgorithm against a property in props
            // Supporting loading a private key via a certificate for now
            List<X509Certificate> chain = KeyManagementUtils.toX509CertificateChain(inHeaders.getX509Chain());
            RSAPrivateKey privateKey = 
                KeyManagementUtils.loadPrivateKey(m, props, chain, JsonWebKey.KEY_OPER_DECRYPT);
            contentEncryptionAlgo = inHeaders.getContentEncryptionAlgorithm();
            keyDecryptionProvider = getRSAKeyDecryptionAlgorithm(privateKey, inHeaders.getKeyEncryptionAlgorithm());
        } else {
            if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
                JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_DECRYPT);
                keyEncryptionAlgo = getKeyEncryptionAlgo(m, props, jwk.getAlgorithm(),
                                                         getDefaultKeyAlgo(jwk));
                if ("direct".equals(keyEncryptionAlgo)) {
                    contentEncryptionAlgo = getContentEncryptionAlgo(m, props, contentEncryptionAlgo);
                    ctDecryptionKey = getContentDecryptionSecretKey(jwk, contentEncryptionAlgo);
                } else {
                    keyDecryptionProvider = getKeyDecryptionAlgorithm(jwk, keyEncryptionAlgo);
                }
            } else {
                keyDecryptionProvider = getRSAKeyDecryptionAlgorithm(
                    (RSAPrivateKey)KeyManagementUtils.loadPrivateKey(
                        m, props, JsonWebKey.KEY_OPER_DECRYPT), keyEncryptionAlgo);
            }
        }
        return createJweDecryptionProvider(keyDecryptionProvider, ctDecryptionKey, contentEncryptionAlgo);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(RSAPublicKey key,
                                                                    String keyAlgo,
                                                                    String contentEncryptionAlgo,
                                                                    String compression) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getRSAKeyEncryptionAlgorithm(key, keyAlgo);
        return createJweEncryptionProvider(keyEncryptionProvider, contentEncryptionAlgo, compression);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(RSAPublicKey key, JweHeaders headers) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getRSAKeyEncryptionAlgorithm(key, 
                                                           headers.getKeyEncryptionAlgorithm());
        return createJweEncryptionProvider(keyEncryptionProvider, headers);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(SecretKey key,
                                                                    String keyAlgo,
                                                                    String contentEncryptionAlgo,
                                                                    String compression) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(key, keyAlgo);
        return createJweEncryptionProvider(keyEncryptionProvider, contentEncryptionAlgo, compression);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(SecretKey key, JweHeaders headers) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getSecretKeyEncryptionAlgorithm(key, 
                                                           headers.getKeyEncryptionAlgorithm());
        return createJweEncryptionProvider(keyEncryptionProvider, headers);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(JsonWebKey key,
                                                                    String contentEncryptionAlgo,
                                                                    String compression) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getKeyEncryptionAlgorithm(key);
        return createJweEncryptionProvider(keyEncryptionProvider, contentEncryptionAlgo, compression);
    }
    public static JweEncryptionProvider createJweEncryptionProvider(JsonWebKey key, JweHeaders headers) {
        KeyEncryptionAlgorithm keyEncryptionProvider = getKeyEncryptionAlgorithm(key);
        return createJweEncryptionProvider(keyEncryptionProvider, headers);
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
            return new AesCbcHmacJweEncryption(contentEncryptionAlgo, keyEncryptionProvider);
        } else {
            return new JweEncryption(keyEncryptionProvider,
                                     getContentEncryptionAlgorithm(contentEncryptionAlgo));
        }
    }
    public static JweDecryptionProvider createJweDecryptionProvider(RSAPrivateKey key,
                                                                    String keyAlgo,
                                                                    String contentDecryptionAlgo) {
        return createJweDecryptionProvider(getRSAKeyDecryptionAlgorithm(key, keyAlgo), contentDecryptionAlgo);
    }
    public static JweDecryptionProvider createJweDecryptionProvider(SecretKey key,
                                                                    String keyAlgo,
                                                                    String contentDecryptionAlgo) {
        return createJweDecryptionProvider(getSecretKeyDecryptionAlgorithm(key, keyAlgo), contentDecryptionAlgo);
    }
    public static JweDecryptionProvider createJweDecryptionProvider(JsonWebKey key,
                                                                    String contentDecryptionAlgo) {
        return createJweDecryptionProvider(getKeyDecryptionAlgorithm(key), contentDecryptionAlgo);
    }
    public static JweDecryptionProvider createJweDecryptionProvider(KeyDecryptionAlgorithm keyDecryptionProvider,
                                                                    String contentDecryptionAlgo) {
        if (Algorithm.isAesCbcHmac(contentDecryptionAlgo)) { 
            return new AesCbcHmacJweDecryption(keyDecryptionProvider, contentDecryptionAlgo);
        } else {
            return new JweDecryption(keyDecryptionProvider, 
                                     getContentDecryptionAlgorithm(contentDecryptionAlgo));
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
            throw new SecurityException();
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
            throw new SecurityException(ex);
        }
    }
    private static byte[] generateKeyZ(ECPrivateKey privateKey, ECPublicKey publicKey) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(privateKey);
            ka.doPhase(publicKey, true);
            return ka.generateSecret();
        } catch (Exception ex) {
            throw new SecurityException(ex);
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
            headers.setAlgorithm(keyEncryptionAlgo);
        }
        headers.setContentEncryptionAlgorithm(contentEncryptionAlgo);
        if (compression != null) {
            headers.setZipAlgorithm(compression);
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
            return new JweEncryption(new DirectKeyEncryptionAlgorithm(), ctEncryptionProvider);
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
    private static String getKeyEncryptionAlgo(Message m, Properties props, 
                                               String algo, String defaultAlgo) {
        if (algo == null) {
            if (defaultAlgo == null) {
                defaultAlgo = JoseConstants.RSA_OAEP_ALGO;
            }
            return KeyManagementUtils.getKeyAlgorithm(m, props, 
                JSON_WEB_ENCRYPTION_KEY_ALGO_PROP, defaultAlgo);
        }
        return algo;
    }
    private static String getDefaultKeyAlgo(JsonWebKey jwk) {
        if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
            return JoseConstants.A128GCMKW_ALGO;
        } else {
            return JoseConstants.RSA_OAEP_ALGO;
        }
    }
    private static String getContentEncryptionAlgo(Message m, Properties props, String algo) {
        if (algo == null) {
            return KeyManagementUtils.getKeyAlgorithm(m, props, 
                JSON_WEB_ENCRYPTION_CEK_ALGO_PROP, JoseConstants.A128GCM_ALGO);
        }
        return algo;
    }
    private static String encrypt(KeyEncryptionAlgorithm keyEncryptionProvider, 
                                  String contentAlgo, byte[] content, String ct) {
        JweEncryptionProvider jwe = createJweEncryptionProvider(keyEncryptionProvider, contentAlgo, null);
        return jwe.encrypt(content, toJweHeaders(ct));
    }
    private static byte[] decrypt(KeyDecryptionAlgorithm keyDecryptionProvider, String contentAlgo, String content) {
        JweDecryptionProvider jwe = createJweDecryptionProvider(keyDecryptionProvider, contentAlgo);
        return jwe.decrypt(content).getContent();
    }
    private static JweHeaders toJweHeaders(String ct) {
        return new JweHeaders(Collections.<String, Object>singletonMap(JoseConstants.HEADER_CONTENT_TYPE, ct));
    }
}
