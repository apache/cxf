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

package org.apache.cxf.rs.security.oauth2.utils.crypto;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;
import org.apache.cxf.security.SecurityContext;


/**
 * Encryption helpers
 */
public final class CryptoUtils {
    public static final String RSSEC_KEY_STORE_TYPE = "rs.security.keystore.type";
    public static final String RSSEC_KEY_STORE_PSWD = "rs.security.keystore.password";
    public static final String RSSEC_KEY_PSWD = "rs.security.key.password";
    public static final String RSSEC_KEY_STORE_ALIAS = "rs.security.keystore.alias";
    public static final String RSSEC_KEY_STORE_FILE = "rs.security.keystore.file";
    public static final String RSSEC_PRINCIPAL_NAME = "rs.security.principal.name";
    public static final String RSSEC_SIG_KEY_PSWD_PROVIDER = "rs.security.signature.key.password.provider";
    public static final String RSSEC_DECRYPT_KEY_PSWD_PROVIDER = "rs.security.decryption.key.password.provider";
    
    private CryptoUtils() {
    }
    
    public static String encodeSecretKey(SecretKey key) throws SecurityException {
        return encodeBytes(key.getEncoded());
    }
    
    public static String encryptSecretKey(SecretKey secretKey, PublicKey publicKey) 
        throws SecurityException {
        KeyProperties props = new KeyProperties(publicKey.getAlgorithm());
        return encryptSecretKey(secretKey, publicKey, props);
    }
    
    public static String encryptSecretKey(SecretKey secretKey, PublicKey publicKey,
        KeyProperties props) throws SecurityException {
        byte[] encryptedBytes = encryptBytes(secretKey.getEncoded(), 
                                             publicKey,
                                             props);
        return encodeBytes(encryptedBytes);
    }
    
    public static byte[] generateSecureRandomBytes(int size) {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[size];
        sr.nextBytes(bytes);
        return bytes;
    }
    
    public static RSAPublicKey getRSAPublicKey(String encodedModulus,
                                               String encodedPublicExponent) {
        try {
            return getRSAPublicKey(Base64UrlUtility.decode(encodedModulus),
                                   Base64UrlUtility.decode(encodedPublicExponent));
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }
    }
    
    public static RSAPublicKey getRSAPublicKey(byte[] modulusBytes,
                                               byte[] publicExponentBytes) {
        try {
            return getRSAPublicKey(KeyFactory.getInstance("RSA"), 
                                   modulusBytes,
                                   publicExponentBytes);
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }         
    }
    
    public static RSAPublicKey getRSAPublicKey(KeyFactory factory,
                                               byte[] modulusBytes,
                                               byte[] publicExponentBytes) {
        BigInteger modulus =  new BigInteger(1, modulusBytes);
        BigInteger publicExponent =  new BigInteger(1, publicExponentBytes);
        try {
            return (RSAPublicKey)factory.generatePublic(
                new RSAPublicKeySpec(modulus, publicExponent));
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }    
    }
    
    public static Certificate loadCertificate(InputStream storeLocation, char[] storePassword, String alias,
                                              String storeType) {
        KeyStore keyStore = loadKeyStore(storeLocation, storePassword, storeType);
        return loadCertificate(keyStore, alias);
    }
    public static Certificate loadCertificate(KeyStore keyStore, String alias) {
        try {
            return keyStore.getCertificate(alias);
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }
    }
    public static PublicKey loadPublicKey(InputStream storeLocation, char[] storePassword, String alias,
                                          String storeType) {
        return loadCertificate(storeLocation, storePassword, alias, storeType).getPublicKey();
    }
    public static PublicKey loadPublicKey(KeyStore keyStore, String alias) {
        return loadCertificate(keyStore, alias).getPublicKey();
    }
    public static PublicKey loadPublicKey(Message m, Properties props) {
        KeyStore keyStore = CryptoUtils.loadPersistKeyStore(m, props);
        return CryptoUtils.loadPublicKey(keyStore, props.getProperty(RSSEC_KEY_STORE_ALIAS));
    }
    public static PublicKey loadPublicKey(Message m, String keyStoreLocProp) {
        return loadPublicKey(m, keyStoreLocProp, null);
    }
    public static PublicKey loadPublicKey(Message m, String keyStoreLocPropPreferred, String keyStoreLocPropDefault) {
        String keyStoreLoc = getMessageProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = ResourceUtils.loadProperties(keyStoreLoc, bus);
            return CryptoUtils.loadPublicKey(m, props);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    private static String getMessageProperty(Message m, String keyStoreLocPropPreferred, 
                                             String keyStoreLocPropDefault) {
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        if (propLoc == null) {
            throw new SecurityException();
        }
        return propLoc;
    }
    public static PrivateKey loadPrivateKey(Properties props, Bus bus, PrivateKeyPasswordProvider provider) {
        KeyStore keyStore = loadKeyStore(props, bus);
        return loadPrivateKey(keyStore, props, bus, provider);
    }
    public static PrivateKey loadPrivateKey(KeyStore keyStore, 
                                            Properties props, 
                                            Bus bus, 
                                            PrivateKeyPasswordProvider provider) {
        
        String keyPswd = props.getProperty(RSSEC_KEY_PSWD);
        String alias = props.getProperty(RSSEC_KEY_STORE_ALIAS);
        char[] keyPswdChars = provider != null ? provider.getPassword(props) 
            : keyPswd != null ? keyPswd.toCharArray() : null;    
        return loadPrivateKey(keyStore, keyPswdChars, alias);
    }
    public static PrivateKey loadPrivateKey(InputStream storeLocation, 
                                            char[] storePassword, 
                                            char[] keyPassword, 
                                            String alias,
                                            String storeType) {
        KeyStore keyStore = loadKeyStore(storeLocation, storePassword, storeType);
        return loadPrivateKey(keyStore, keyPassword, alias);
    }
    
    public static PrivateKey loadPrivateKey(KeyStore keyStore,
                                            char[] keyPassword, 
                                            String alias) {
        try {
            KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry)
                keyStore.getEntry(alias, new KeyStore.PasswordProtection(keyPassword));
            return pkEntry.getPrivateKey();
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }
    }
    public static PrivateKey loadPrivateKey(Message m, String keyStoreLocProp, String passwordProviderProp) {
        return loadPrivateKey(m, keyStoreLocProp, null, passwordProviderProp);
    }
    public static PrivateKey loadPrivateKey(Message m, String keyStoreLocPropPreferred,
                                            String keyStoreLocPropDefault, String passwordProviderProp) {
        String keyStoreLoc = getMessageProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = ResourceUtils.loadProperties(keyStoreLoc, bus);
            return CryptoUtils.loadPrivateKey(m, props, passwordProviderProp);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    public static PrivateKey loadPrivateKey(Message m, Properties props, String passwordProviderProp) {
        Bus bus = m.getExchange().getBus();
        KeyStore keyStore = CryptoUtils.loadPersistKeyStore(m, props);
        PrivateKeyPasswordProvider cb = 
            (PrivateKeyPasswordProvider)m.getContextualProperty(passwordProviderProp);
        if (cb != null && m.getExchange().getInMessage() != null) {
            SecurityContext sc = m.getExchange().getInMessage().get(SecurityContext.class);
            if (sc != null) {
                Principal p = sc.getUserPrincipal();
                if (p != null) {
                    props.setProperty(RSSEC_PRINCIPAL_NAME, p.getName());
                }
            }
        }
        return CryptoUtils.loadPrivateKey(keyStore, props, bus, cb);
    }
    public static KeyStore loadPersistKeyStore(Message m, Properties props) {
        KeyStore keyStore = (KeyStore)m.getExchange().get(props.get(CryptoUtils.RSSEC_KEY_STORE_FILE));
        if (keyStore == null) {
            keyStore = CryptoUtils.loadKeyStore(props, m.getExchange().getBus());
            m.getExchange().put((String)props.get(CryptoUtils.RSSEC_KEY_STORE_FILE), keyStore);
        }
        return keyStore;
    }
    public static KeyStore loadKeyStore(Properties props, Bus bus) {
        String keyStoreType = props.getProperty(RSSEC_KEY_STORE_TYPE);
        String keyStoreLoc = props.getProperty(RSSEC_KEY_STORE_FILE);
        String keyStorePswd = props.getProperty(RSSEC_KEY_STORE_PSWD);
        try {
            InputStream is = ResourceUtils.getResourceStream(keyStoreLoc, bus);
            return loadKeyStore(is, keyStorePswd.toCharArray(), keyStoreType);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static KeyStore loadKeyStore(InputStream storeLocation, char[] storePassword, String type) {
        try {
            KeyStore ks = KeyStore.getInstance(type == null ? KeyStore.getDefaultType() : type);
            ks.load(storeLocation, storePassword);
            return ks;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static RSAPrivateKey getRSAPrivateKey(String encodedModulus,
                                                 String encodedPrivateExponent) {
        try {
            return getRSAPrivateKey(Base64UrlUtility.decode(encodedModulus),
                                    Base64UrlUtility.decode(encodedPrivateExponent));
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }
    }
    public static ECPrivateKey getECPrivateKey(String encodedPrivateKey) {
        try {
            return getECPrivateKey(Base64UrlUtility.decode(encodedPrivateKey));
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }
    }
    public static ECPrivateKey getECPrivateKey(byte[] privateKey) {
        try {
            ECParameterSpec params = getECParameterSpec();

            ECPrivateKeySpec keySpec = new ECPrivateKeySpec(
                                           new BigInteger(1, privateKey), params);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return (ECPrivateKey) kf.generatePrivate(keySpec);

        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }    
    }
    private static ECParameterSpec getECParameterSpec() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec kpgparams = new ECGenParameterSpec("secp256r1");
        kpg.initialize(kpgparams);
        return ((ECPublicKey) kpg.generateKeyPair().getPublic()).getParams();
    }
    
    public static ECPublicKey getECPublicKey(String encodedXPoint, String encodedYPoint) {
        try {
            return getECPublicKey(Base64UrlUtility.decode(encodedXPoint),
                                  Base64UrlUtility.decode(encodedYPoint));
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }
    }
    public static ECPublicKey getECPublicKey(byte[] xPoint, byte[] yPoint) {
        try {
            ECParameterSpec params = getECParameterSpec();

            ECPoint ecPoint = new ECPoint(new BigInteger(1, xPoint),
                                          new BigInteger(1, yPoint));
            ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, params);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return (ECPublicKey) kf.generatePublic(keySpec);

        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }    
    }
    public static AlgorithmParameterSpec getContentEncryptionCipherSpec(int authTagLength, byte[] iv) {
        if (authTagLength > 0) {
            return CryptoUtils.getGCMParameterSpec(authTagLength, iv);
        } else if (iv.length > 0) {
            return new IvParameterSpec(iv);
        } else {
            return null;
        }
    }
    
    public static AlgorithmParameterSpec getGCMParameterSpec(int authTagLength, byte[] iv) {
        return new GCMParameterSpec(authTagLength, iv);
    }
    
    public static byte[] signData(byte[] data, PrivateKey key, String signAlgo) {
        return signData(data, key, signAlgo, null, null);
    }
    
    public static byte[] signData(byte[] data, PrivateKey key, String signAlgo, SecureRandom random,
                           AlgorithmParameterSpec params) {
        try {
            Signature s = getSignature(key, signAlgo, random, params);
            s.update(data);
            return s.sign();
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static Signature getSignature(PrivateKey key, String signAlgo, SecureRandom random,
                                  AlgorithmParameterSpec params) {
        try {
            Signature s = Signature.getInstance(signAlgo);
            if (random == null) {
                s.initSign(key);
            } else {
                s.initSign(key, random);
            }
            if (params != null) {
                s.setParameter(params);
            }
            return s;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static boolean verifySignature(byte[] data, byte[] signature, PublicKey key, String signAlgo) {
        return verifySignature(data, signature, key, signAlgo, null);
    }
    
    public static boolean verifySignature(byte[] data, byte[] signature, PublicKey key, String signAlgo, 
                                AlgorithmParameterSpec params) {
        try {
            Signature s = Signature.getInstance(signAlgo);
            s.initVerify(key);
            if (params != null) {
                s.setParameter(params);
            }
            s.update(data);
            return s.verify(signature);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static RSAPrivateKey getRSAPrivateKey(byte[] modulusBytes,
                                                 byte[] privateExponentBytes) {
        try {
            return getRSAPrivateKey(KeyFactory.getInstance("RSA"), 
                                   modulusBytes,
                                   privateExponentBytes);
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }    
    }
    
    public static RSAPrivateKey getRSAPrivateKey(KeyFactory factory,
                                         byte[] modulusBytes,
                                         byte[] privateExponentBytes) {
        BigInteger modulus =  new BigInteger(1, modulusBytes);
        BigInteger privateExponent =  new BigInteger(1, privateExponentBytes);
        try {
            return (RSAPrivateKey)factory.generatePrivate(
                new RSAPrivateKeySpec(modulus, privateExponent));
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }    
    }
    
    public static SecretKey getSecretKey(String symEncAlgo) throws SecurityException {
        return getSecretKey(new KeyProperties(symEncAlgo));
    }
    
    public static SecretKey getSecretKey(String symEncAlgo, int keySize) throws SecurityException {
        return getSecretKey(new KeyProperties(symEncAlgo, keySize));
    }
    
    public static SecretKey getSecretKey(KeyProperties props) throws SecurityException {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(props.getKeyAlgo());
            AlgorithmParameterSpec algoSpec = props.getAlgoSpec();
            SecureRandom random = props.getSecureRandom();
            if (algoSpec != null) {
                if (random != null) {
                    keyGen.init(algoSpec, random);
                } else {
                    keyGen.init(algoSpec);
                }
            } else {
                int keySize = props.getKeySize();
                if (keySize == -1) {
                    keySize = 128;
                }
                if (random != null) {
                    keyGen.init(keySize, random);
                } else {
                    keyGen.init(keySize);
                }
            }
            
            return keyGen.generateKey();
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static String decryptSequence(String encodedToken, String encodedSecretKey)
        throws SecurityException {
        return decryptSequence(encodedToken, encodedSecretKey, new KeyProperties("AES"));
    }
    
    public static String decryptSequence(String encodedData, String encodedSecretKey, 
        KeyProperties props) throws SecurityException {
        SecretKey key = decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
        return decryptSequence(encodedData, key, props);
    }
    
    public static String decryptSequence(String encodedData, Key secretKey) throws SecurityException {
        return decryptSequence(encodedData, secretKey, null);
    }
    
    public static String decryptSequence(String encodedData, Key secretKey,
        KeyProperties props) throws SecurityException {
        byte[] encryptedBytes = decodeSequence(encodedData);
        byte[] bytes = decryptBytes(encryptedBytes, secretKey, props);
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static String encryptSequence(String sequence, Key secretKey) throws SecurityException {
        return encryptSequence(sequence, secretKey, null);
    }
    
    public static String encryptSequence(String sequence, Key secretKey,
        KeyProperties keyProps) throws SecurityException {
        try {
            byte[] bytes = encryptBytes(sequence.getBytes("UTF-8"), secretKey, keyProps);
            return encodeBytes(bytes);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static String encodeBytes(byte[] bytes) throws SecurityException {
        try {
            return Base64UrlUtility.encode(bytes);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static byte[] encryptBytes(byte[] bytes, Key secretKey) throws SecurityException {
        return encryptBytes(bytes, secretKey, null);
    }
    
    public static byte[] encryptBytes(byte[] bytes, Key secretKey,
        KeyProperties keyProps) throws SecurityException {
        return processBytes(bytes, secretKey, keyProps, Cipher.ENCRYPT_MODE);
    }
    
    public static byte[] decryptBytes(byte[] bytes, Key secretKey) throws SecurityException {
        return decryptBytes(bytes, secretKey, null);
    }
    
    public static byte[] decryptBytes(byte[] bytes, Key secretKey, 
        KeyProperties keyProps) throws SecurityException {
        return processBytes(bytes, secretKey, keyProps, Cipher.DECRYPT_MODE);
    }
    
    public static byte[] wrapSecretKey(byte[] keyBytes, 
                                       String keyAlgo,
                                       Key wrapperKey,
                                       KeyProperties wrapperKeyProps)  throws SecurityException {
        return wrapSecretKey(new SecretKeySpec(keyBytes, convertJCECipherToSecretKeyName(keyAlgo)), 
                             wrapperKey, 
                             wrapperKeyProps);
    }
    
    public static byte[] wrapSecretKey(Key secretKey,
                                       Key wrapperKey,
                                       KeyProperties keyProps)  throws SecurityException {
        try {
            Cipher c = initCipher(wrapperKey, keyProps, Cipher.WRAP_MODE);
            return c.wrap(secretKey);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }    
    }
    
    public static SecretKey unwrapSecretKey(byte[] wrappedBytes,
                                            String wrappedKeyAlgo,
                                            Key unwrapperKey,
                                            String unwrapperKeyAlgo)  throws SecurityException {
        return unwrapSecretKey(wrappedBytes, wrappedKeyAlgo, unwrapperKey, 
                               new KeyProperties(unwrapperKeyAlgo));
    }
    
    public static SecretKey unwrapSecretKey(byte[] wrappedBytes,
                                            String wrappedKeyAlgo,
                                            Key unwrapperKey,
                                            KeyProperties keyProps)  throws SecurityException {
        return (SecretKey)unwrapKey(wrappedBytes, wrappedKeyAlgo, unwrapperKey, keyProps, Cipher.SECRET_KEY);    
    }
    
    public static Key unwrapKey(byte[] wrappedBytes,
                                            String wrappedKeyAlgo,
                                            Key unwrapperKey,
                                            KeyProperties keyProps,
                                            int wrappedKeyType)  throws SecurityException {
        try {
            Cipher c = initCipher(unwrapperKey, keyProps, Cipher.UNWRAP_MODE);
            return c.unwrap(wrappedBytes, wrappedKeyAlgo, wrappedKeyType);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }    
    }
    
    private static byte[] processBytes(byte[] bytes, 
                                      Key secretKey, 
                                      KeyProperties keyProps, 
                                      int mode)  throws SecurityException {
        boolean compressionSupported = keyProps != null && keyProps.isCompressionSupported();
        if (compressionSupported && mode == Cipher.ENCRYPT_MODE) {
            bytes = CompressionUtils.deflate(bytes, false);
        }
        try {
            Cipher c = initCipher(secretKey, keyProps, mode);
            byte[] result = new byte[0];
            int blockSize = keyProps != null ? keyProps.getBlockSize() : -1;
            if (secretKey instanceof SecretKey && blockSize == -1) {
                result = c.doFinal(bytes);
            } else {
                if (blockSize == -1) {
                    blockSize = secretKey instanceof PublicKey ? 117 : 128;
                }
                boolean updateRequired = keyProps != null && keyProps.getAdditionalData() != null;
                int offset = 0;
                for (; offset + blockSize < bytes.length; offset += blockSize) {
                    byte[] next = !updateRequired ? c.doFinal(bytes, offset, blockSize) 
                        : c.update(bytes, offset, blockSize);
                    result = addToResult(result, next);
                }
                if (offset < bytes.length) {
                    result = addToResult(result, c.doFinal(bytes, offset, bytes.length - offset));
                } else {
                    result = addToResult(result, c.doFinal());
                }
            }
            if (compressionSupported && mode == Cipher.DECRYPT_MODE) {
                result = IOUtils.readBytesFromStream(CompressionUtils.inflate(result, false));
            }
            return result;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static Cipher initCipher(Key secretKey, KeyProperties keyProps, int mode)  throws SecurityException {
        try {
            String algorithm = keyProps != null && keyProps.getKeyAlgo() != null 
                ? keyProps.getKeyAlgo() : secretKey.getAlgorithm();
            Cipher c = Cipher.getInstance(algorithm);
            if (keyProps == null || keyProps.getAlgoSpec() == null && keyProps.getSecureRandom() == null) {
                c.init(mode, secretKey);
            } else {
                AlgorithmParameterSpec algoSpec = keyProps.getAlgoSpec();
                SecureRandom random = keyProps.getSecureRandom();
                if (algoSpec == null) {
                    c.init(mode, secretKey, random);
                } else if (random == null) {
                    c.init(mode, secretKey, algoSpec);
                } else {
                    c.init(mode, secretKey, algoSpec, random);
                }
            }
            if (keyProps != null && keyProps.getAdditionalData() != null) {
                c.updateAAD(keyProps.getAdditionalData());
            }
            return c;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    private static byte[] addToResult(byte[] prefix, byte[] suffix) {
        if (suffix == null || suffix.length == 0) {
            return prefix;    
        } else if (prefix.length == 0) {
            return suffix;
        } else {
            byte[] result = new byte[prefix.length + suffix.length];
            System.arraycopy(prefix, 0, result, 0, prefix.length);
            System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
            return result;
        }
    }
    
    public static SecretKey decodeSecretKey(String encodedSecretKey) throws SecurityException {
        return decodeSecretKey(encodedSecretKey, "AES");
    }
    
    public static SecretKey decodeSecretKey(String encodedSecretKey, String secretKeyAlgo) 
        throws SecurityException {
        byte[] secretKeyBytes = decodeSequence(encodedSecretKey);
        return createSecretKeySpec(secretKeyBytes, secretKeyAlgo);
    }
    
    public static SecretKey decryptSecretKey(String encodedEncryptedSecretKey,
                                             PrivateKey privateKey) {
        return decryptSecretKey(encodedEncryptedSecretKey, "AES", privateKey);
    }
    
    
    public static SecretKey decryptSecretKey(String encodedEncryptedSecretKey,
                                             String secretKeyAlgo,
                                             PrivateKey privateKey)
        throws SecurityException {
        KeyProperties props = new KeyProperties(privateKey.getAlgorithm());
        return decryptSecretKey(encodedEncryptedSecretKey, secretKeyAlgo, props, privateKey);
    }
    
    public static SecretKey decryptSecretKey(String encodedEncryptedSecretKey,
                                             String secretKeyAlgo,
                                             KeyProperties props,
                                             PrivateKey privateKey) throws SecurityException {
        byte[] encryptedBytes = decodeSequence(encodedEncryptedSecretKey);
        byte[] descryptedBytes = decryptBytes(encryptedBytes, privateKey, props);
        return createSecretKeySpec(descryptedBytes, secretKeyAlgo);
    }
    
    public static SecretKey createSecretKeySpec(byte[] bytes, String algo) {
        return new SecretKeySpec(bytes, convertJCECipherToSecretKeyName(algo));
    }
    
    public static byte[] decodeSequence(String encodedSequence) throws SecurityException {
        try {
            return Base64UrlUtility.decode(encodedSequence);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    private static String convertJCECipherToSecretKeyName(String jceCipherName) {
        if (jceCipherName != null) {
            if (jceCipherName.startsWith("AES")) {
                return "AES";
            } else if (jceCipherName.startsWith("DESede")) {
                return "DESede";
            } else if (jceCipherName.startsWith("SEED")) {
                return "SEED";
            } else if (jceCipherName.startsWith("Camellia")) {
                return "Camellia";
            }
        }
        return null;
    }
}
