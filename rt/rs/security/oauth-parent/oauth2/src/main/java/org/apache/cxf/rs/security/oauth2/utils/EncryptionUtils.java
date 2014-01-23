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

import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/**
 * Encryption helpers
 */
public final class EncryptionUtils {
    private EncryptionUtils() {
    }
    
    public static String getEncodedSecretKey(SecretKey key) throws Exception {
        try {
            return Base64UrlUtility.encode(key.getEncoded());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static SecretKey getSecretKey() throws Exception {
        return getSecretKey("AES");
    }
    
    public static SecretKey getSecretKey(String symEncAlgo) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(symEncAlgo);
        return keyGen.generateKey();
    }
    
    public static SecretKey getSecretKey(SecretKeyProperties props) throws Exception {
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
            if (random != null) {
                keyGen.init(props.getKeySize(), random);
            } else {
                keyGen.init(props.getKeySize());
            }
        }
        
        return keyGen.generateKey();
    }
    
    public static String decryptSequence(String encodedToken, 
                                         String encodedSecretKey) {
        return decryptSequence(encodedToken, encodedSecretKey, new SecretKeyProperties("AES"));
    }
    
    public static String decryptSequence(String encodedData, 
                                         String encodedSecretKey, 
                                         SecretKeyProperties props) {
        try {
            SecretKey key = decodeSecretKey(encodedSecretKey, props.getKeyAlgo());
            return decryptSequence(encodedData, key, props);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static String decryptSequence(String encodedData, Key secretKey) {
        return decryptSequence(encodedData, secretKey, null);
    }
    
    public static String decryptSequence(String encodedData, 
                                              Key secretKey,
                                              SecretKeyProperties props) {
        try {
            byte[] encryptedBytes = decodeSequence(encodedData);
            byte[] bytes = processBytes(encryptedBytes, secretKey, props, Cipher.DECRYPT_MODE);
            return new String(bytes, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static String encryptSequence(String sequence, Key secretKey) {
        return encryptSequence(sequence, secretKey, null);
    }
    
    public static String encryptSequence(String sequence, Key secretKey,
                                         SecretKeyProperties keyProps) {
        try {
            byte[] bytes = processBytes(sequence.getBytes("UTF-8"), 
                                        secretKey,
                                        keyProps,
                                        Cipher.ENCRYPT_MODE);
            return Base64UrlUtility.encode(bytes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static byte[] processBytes(byte[] bytes, Key secretKey, 
                                      SecretKeyProperties keyProps, int mode) {
        try {
            Cipher c = Cipher.getInstance(secretKey.getAlgorithm());
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
            return c.doFinal(bytes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static SecretKey decodeSecretKey(String encodedSecretKey, String algo) {
        try {
            byte[] secretKeyBytes = decodeSequence(encodedSecretKey);
            return new SecretKeySpec(secretKeyBytes, algo);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static byte[] decodeSequence(String encodedSequence) {
        try {
            return Base64UrlUtility.decode(encodedSequence);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
