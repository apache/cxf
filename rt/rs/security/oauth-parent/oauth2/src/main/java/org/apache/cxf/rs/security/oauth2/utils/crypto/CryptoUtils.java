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

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;


/**
 * Encryption helpers
 */
public final class CryptoUtils {
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
    
    public static RSAPrivateKey getRSAPrivateKey(String encodedModulus,
                                                 String encodedPrivateExponent) {
        try {
            return getRSAPrivateKey(Base64UrlUtility.decode(encodedModulus),
                                    Base64UrlUtility.decode(encodedPrivateExponent));
        } catch (Exception ex) { 
            throw new SecurityException(ex);
        }
    }
     
    public static byte[] signData(byte[] data, PrivateKey key, String signAlgo) {
        return signData(data, key, signAlgo, null, null);
    }
    
    public static byte[] signData(byte[] data, PrivateKey key, String signAlgo, SecureRandom random,
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
            s.update(data);
            return s.sign();
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static void verifySignature(byte[] data, byte[] signature, PublicKey key, String signAlgo) {
        verifySignature(data, signature, key, signAlgo, null);
    }
    
    public static void verifySignature(byte[] data, byte[] signature, PublicKey key, String signAlgo, 
                                AlgorithmParameterSpec params) {
        try {
            Signature s = Signature.getInstance(signAlgo);
            s.initVerify(key);
            if (params != null) {
                s.setParameter(params);
            }
            s.update(data);
            s.verify(signature);
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
                                       String wrapperKeyAlgo)  throws SecurityException {
        return wrapSecretKey(new SecretKeySpec(keyBytes, keyAlgo), wrapperKey, 
                             new KeyProperties(wrapperKeyAlgo));
    }
    
    public static byte[] wrapSecretKey(SecretKey secretKey,
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
        try {
            Cipher c = initCipher(unwrapperKey, keyProps, Cipher.UNWRAP_MODE);
            return (SecretKey)c.unwrap(wrappedBytes, wrappedKeyAlgo, Cipher.SECRET_KEY);
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
                int offset = 0;
                for (; offset + blockSize < bytes.length; offset += blockSize) {
                    result = addToResult(result, c.doFinal(bytes, offset, blockSize));
                }
                if (offset < bytes.length) {
                    result = addToResult(result, c.doFinal(bytes, offset, bytes.length - offset));
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
                // TODO: call updateAAD directly after switching to Java7
                Method m = Cipher.class.getMethod("updateAAD", new Class[]{byte[].class});
                m.invoke(c, new Object[]{keyProps.getAdditionalData()});
            }
            return c;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    private static byte[] addToResult(byte[] prefix, byte[] suffix) {
        byte[] result = new byte[prefix.length + suffix.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
        return result;
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
        return new SecretKeySpec(bytes, algo);
    }
    
    public static byte[] decodeSequence(String encodedSequence) throws SecurityException {
        try {
            return Base64UrlUtility.decode(encodedSequence);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
}
