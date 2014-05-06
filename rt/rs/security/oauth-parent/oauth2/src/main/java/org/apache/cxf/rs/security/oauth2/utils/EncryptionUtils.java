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

package org.apache.cxf.rs.security.oauth2.utils;

import java.lang.reflect.Method;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.helpers.IOUtils;


/**
 * Encryption helpers
 */
public final class EncryptionUtils {
    private EncryptionUtils() {
    }
    
    public static String encodeSecretKey(SecretKey key) throws EncryptionException {
        return encodeBytes(key.getEncoded());
    }
    
    public static String encryptSecretKey(SecretKey secretKey, PublicKey publicKey) 
        throws EncryptionException {
        SecretKeyProperties props = new SecretKeyProperties(publicKey.getAlgorithm());
        return encryptSecretKey(secretKey, publicKey, props);
    }
    
    public static String encryptSecretKey(SecretKey secretKey, PublicKey publicKey,
        SecretKeyProperties props) throws EncryptionException {
        byte[] encryptedBytes = encryptBytes(secretKey.getEncoded(), 
                                             publicKey,
                                             props);
        return encodeBytes(encryptedBytes);
    }
    
    public static SecretKey getSecretKey() throws Exception {
        return getSecretKey("AES");
    }
    
    public static SecretKey getSecretKey(String symEncAlgo) throws EncryptionException {
        return getSecretKey(new SecretKeyProperties(symEncAlgo));
    }
    
    public static SecretKey getSecretKey(SecretKeyProperties props) throws EncryptionException {
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
            throw new EncryptionException(ex);
        }
    }
    
    public static String decryptSequence(String encodedToken, String encodedSecretKey)
        throws EncryptionException {
        return decryptSequence(encodedToken, encodedSecretKey, new SecretKeyProperties("AES"));
    }
    
    public static String decryptSequence(String encodedData, String encodedSecretKey, 
        SecretKeyProperties props) throws EncryptionException {
        SecretKey key = decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
        return decryptSequence(encodedData, key, props);
    }
    
    public static String decryptSequence(String encodedData, Key secretKey) throws EncryptionException {
        return decryptSequence(encodedData, secretKey, null);
    }
    
    public static String decryptSequence(String encodedData, Key secretKey,
        SecretKeyProperties props) throws EncryptionException {
        byte[] encryptedBytes = decodeSequence(encodedData);
        byte[] bytes = decryptBytes(encryptedBytes, secretKey, props);
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception ex) {
            throw new EncryptionException(ex);
        }
    }
    
    public static String encryptSequence(String sequence, Key secretKey) throws EncryptionException {
        return encryptSequence(sequence, secretKey, null);
    }
    
    public static String encryptSequence(String sequence, Key secretKey,
        SecretKeyProperties keyProps) throws EncryptionException {
        try {
            byte[] bytes = encryptBytes(sequence.getBytes("UTF-8"), secretKey, keyProps);
            return encodeBytes(bytes);
        } catch (Exception ex) {
            throw new EncryptionException(ex);
        }
    }
    
    public static String encodeBytes(byte[] bytes) throws EncryptionException {
        try {
            return Base64UrlUtility.encode(bytes);
        } catch (Exception ex) {
            throw new EncryptionException(ex);
        }
    }
    
    public static byte[] encryptBytes(byte[] bytes, Key secretKey) throws EncryptionException {
        return encryptBytes(bytes, secretKey, null);
    }
    
    public static byte[] encryptBytes(byte[] bytes, Key secretKey,
        SecretKeyProperties keyProps) throws EncryptionException {
        return processBytes(bytes, secretKey, keyProps, Cipher.ENCRYPT_MODE);
    }
    
    public static byte[] decryptBytes(byte[] bytes, Key secretKey) throws EncryptionException {
        return decryptBytes(bytes, secretKey, null);
    }
    
    public static byte[] decryptBytes(byte[] bytes, Key secretKey, 
        SecretKeyProperties keyProps) throws EncryptionException {
        return processBytes(bytes, secretKey, keyProps, Cipher.DECRYPT_MODE);
    }
    
    public static byte[] wrapSecretKey(byte[] keyBytes, 
                                       String keyAlgo,
                                       Key wrapperKey,
                                       String wrapperKeyAlgo)  throws EncryptionException {
        return wrapSecretKey(new SecretKeySpec(keyBytes, keyAlgo), wrapperKey, 
                             new SecretKeyProperties(wrapperKeyAlgo));
    }
    
    public static byte[] wrapSecretKey(SecretKey secretKey,
                                       Key wrapperKey,
                                       SecretKeyProperties keyProps)  throws EncryptionException {
        try {
            Cipher c = initCipher(wrapperKey, keyProps, Cipher.WRAP_MODE);
            return c.wrap(secretKey);
        } catch (Exception ex) {
            throw new EncryptionException(ex);
        }    
    }
    
    public static SecretKey unwrapSecretKey(byte[] wrappedBytes,
                                            String wrappedKeyAlgo,
                                            Key unwrapperKey,
                                            String unwrapperKeyAlgo)  throws EncryptionException {
        return unwrapSecretKey(wrappedBytes, wrappedKeyAlgo, unwrapperKey, 
                               new SecretKeyProperties(unwrapperKeyAlgo));
    }
    
    public static SecretKey unwrapSecretKey(byte[] wrappedBytes,
                                            String wrappedKeyAlgo,
                                            Key unwrapperKey,
                                            SecretKeyProperties keyProps)  throws EncryptionException {
        try {
            Cipher c = initCipher(unwrapperKey, keyProps, Cipher.UNWRAP_MODE);
            return (SecretKey)c.unwrap(wrappedBytes, wrappedKeyAlgo, Cipher.SECRET_KEY);
        } catch (Exception ex) {
            throw new EncryptionException(ex);
        }    
    }
    
    private static byte[] processBytes(byte[] bytes, 
                                      Key secretKey, 
                                      SecretKeyProperties keyProps, 
                                      int mode)  throws EncryptionException {
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
            throw new EncryptionException(ex);
        }
    }
    
    public static Cipher initCipher(Key secretKey, SecretKeyProperties keyProps, int mode)  throws EncryptionException {
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
            throw new EncryptionException(ex);
        }
    }
    
    private static byte[] addToResult(byte[] prefix, byte[] suffix) {
        byte[] result = new byte[prefix.length + suffix.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
        return result;
    }
    
    public static SecretKey decodeSecretKey(String encodedSecretKey) throws EncryptionException {
        return decodeSecretKey(encodedSecretKey, "AES");
    }
    
    public static SecretKey decodeSecretKey(String encodedSecretKey, String secretKeyAlgo) 
        throws EncryptionException {
        byte[] secretKeyBytes = decodeSequence(encodedSecretKey);
        return recreateSecretKey(secretKeyBytes, secretKeyAlgo);
    }
    
    public static SecretKey decryptSecretKey(String encodedEncryptedSecretKey,
                                             PrivateKey privateKey) {
        return decryptSecretKey(encodedEncryptedSecretKey, "AES", privateKey);
    }
    
    
    public static SecretKey decryptSecretKey(String encodedEncryptedSecretKey,
                                             String secretKeyAlgo,
                                             PrivateKey privateKey)
        throws EncryptionException {
        SecretKeyProperties props = new SecretKeyProperties(privateKey.getAlgorithm());
        return decryptSecretKey(encodedEncryptedSecretKey, secretKeyAlgo, props, privateKey);
    }
    
    public static SecretKey decryptSecretKey(String encodedEncryptedSecretKey,
                                             String secretKeyAlgo,
                                             SecretKeyProperties props,
                                             PrivateKey privateKey) throws EncryptionException {
        byte[] encryptedBytes = decodeSequence(encodedEncryptedSecretKey);
        byte[] descryptedBytes = decryptBytes(encryptedBytes, privateKey, props);
        return recreateSecretKey(descryptedBytes, secretKeyAlgo);
    }
    
    public static SecretKey recreateSecretKey(byte[] bytes, String algo) {
        return new SecretKeySpec(bytes, algo);
    }
    
    public static byte[] decodeSequence(String encodedSequence) throws EncryptionException {
        try {
            return Base64UrlUtility.decode(encodedSequence);
        } catch (Exception ex) {
            throw new EncryptionException(ex);
        }
    }
    
}
