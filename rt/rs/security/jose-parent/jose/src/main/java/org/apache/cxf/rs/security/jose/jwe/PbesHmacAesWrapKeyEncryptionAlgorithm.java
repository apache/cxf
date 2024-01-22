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
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;


public class PbesHmacAesWrapKeyEncryptionAlgorithm implements KeyEncryptionProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(PbesHmacAesWrapKeyEncryptionAlgorithm.class);
    private static final Map<String, Integer> PBES_HMAC_MAP;
    private static final Map<String, Integer> DERIVED_KEY_SIZE_MAP;
    static {
        PBES_HMAC_MAP = new HashMap<>();
        PBES_HMAC_MAP.put(KeyAlgorithm.PBES2_HS256_A128KW.getJwaName(), 256);
        PBES_HMAC_MAP.put(KeyAlgorithm.PBES2_HS384_A192KW.getJwaName(), 384);
        PBES_HMAC_MAP.put(KeyAlgorithm.PBES2_HS512_A256KW.getJwaName(), 512);

        DERIVED_KEY_SIZE_MAP = new HashMap<>();
        DERIVED_KEY_SIZE_MAP.put(KeyAlgorithm.PBES2_HS256_A128KW.getJwaName(), 16);
        DERIVED_KEY_SIZE_MAP.put(KeyAlgorithm.PBES2_HS384_A192KW.getJwaName(), 24);
        DERIVED_KEY_SIZE_MAP.put(KeyAlgorithm.PBES2_HS512_A256KW.getJwaName(), 32);
    }

    private final byte[] password;
    private final int pbesCount;
    private final KeyAlgorithm keyAlgoJwt;

    public PbesHmacAesWrapKeyEncryptionAlgorithm(String password, KeyAlgorithm keyAlgoJwt) {
        this(stringToBytes(password), keyAlgoJwt);
    }
    public PbesHmacAesWrapKeyEncryptionAlgorithm(String password, int pbesCount,
                                                 KeyAlgorithm keyAlgoJwt,
                                                 boolean hashLargePasswords) {
        this(stringToBytes(password), pbesCount, keyAlgoJwt, hashLargePasswords);
    }
    public PbesHmacAesWrapKeyEncryptionAlgorithm(char[] password, KeyAlgorithm keyAlgoJwt) {
        this(password, 4096, keyAlgoJwt, false);
    }
    public PbesHmacAesWrapKeyEncryptionAlgorithm(char[] password, int pbesCount,
                                                 KeyAlgorithm keyAlgoJwt,
                                                 boolean hashLargePasswords) {
        this(charsToBytes(password), pbesCount, keyAlgoJwt, hashLargePasswords);
    }
    public PbesHmacAesWrapKeyEncryptionAlgorithm(byte[] password, KeyAlgorithm keyAlgoJwt) {
        this(password, 4096, keyAlgoJwt, false);
    }
    public PbesHmacAesWrapKeyEncryptionAlgorithm(byte[] password, int pbesCount,
                                                 KeyAlgorithm keyAlgoJwt,
                                                 boolean hashLargePasswords) {
        this.keyAlgoJwt = validateKeyAlgorithm(keyAlgoJwt);
        this.password = validatePassword(password, keyAlgoJwt.getJwaName(), hashLargePasswords);
        this.pbesCount = validatePbesCount(pbesCount);
    }

    static byte[] validatePassword(byte[] p, String keyAlgoJwt, boolean hashLargePasswords) {
        int minLen = DERIVED_KEY_SIZE_MAP.get(keyAlgoJwt);
        if (p.length < minLen || p.length > 128) {
            LOG.warning("Invalid password length: " + p.length);
            throw new JweException(JweException.Error.KEY_ENCRYPTION_FAILURE);
        }
        if (p.length > minLen && hashLargePasswords) {
            try {
                return MessageDigestUtils.createDigest(p, MessageDigestUtils.ALGO_SHA_256);
            } catch (Exception ex) {
                LOG.warning("Password hash calculation error");
                throw new JweException(JweException.Error.KEY_ENCRYPTION_FAILURE, ex);
            }
        }
        return p;
    }
    @Override
    public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] cek) {
        int keySize = getKeySize(keyAlgoJwt.getJwaName());
        byte[] saltInput = CryptoUtils.generateSecureRandomBytes(keySize);
        byte[] derivedKey = createDerivedKey(keyAlgoJwt.getJwaName(),
                                             keySize, password, saltInput, pbesCount);

        headers.setHeader("p2s", Base64UrlUtility.encode(saltInput));
        headers.setIntegerHeader("p2c", pbesCount);

        KeyEncryptionProvider aesWrap = new AesWrapKeyEncryptionAlgorithm(derivedKey, keyAlgoJwt) {
            protected void checkAlgorithms(JweHeaders headers) {
                // complete
            }
            protected String getKeyEncryptionAlgoJava(JweHeaders headers) {
                return AlgorithmUtils.AES_WRAP_ALGO_JAVA;
            }
        };
        return aesWrap.getEncryptedContentEncryptionKey(headers, cek);


    }
    static int getKeySize(String keyAlgoJwt) {
        return DERIVED_KEY_SIZE_MAP.get(keyAlgoJwt);
    }
    static byte[] createDerivedKey(String keyAlgoJwt, int keySize,
                                   byte[] password, byte[] saltInput, int pbesCount) {
        try {
            byte[] saltValue = createSaltValue(keyAlgoJwt, saltInput);
            int macSigSize = PBES_HMAC_MAP.get(keyAlgoJwt);
                
            String algorithm = "PBKDF2WithHmacSHA" + macSigSize;
            PBEKeySpec pbeSpec = new PBEKeySpec(new String(password).toCharArray(), saltValue, pbesCount, keySize * 8);
            SecretKeyFactory keyFact = SecretKeyFactory.getInstance(algorithm); 
            Key sKey = keyFact.generateSecret(pbeSpec);
            byte[] ret = new byte[keySize];
            byte[] key = sKey.getEncoded();
            System.arraycopy(key, 0, ret, 0, keySize);
            return ret;
           
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "cannot create Derived Key", ex);
            return null;
        }
    }


    private static byte[] createSaltValue(String keyAlgoJwt, byte[] saltInput) {
        byte[] algoBytes = stringToBytes(keyAlgoJwt);
        byte[] saltValue = new byte[algoBytes.length + 1 + saltInput.length];
        System.arraycopy(algoBytes, 0, saltValue, 0, algoBytes.length);
        saltValue[algoBytes.length] = 0;
        System.arraycopy(saltInput, 0, saltValue, algoBytes.length + 1, saltInput.length);
        return saltValue;
    }
    static KeyAlgorithm validateKeyAlgorithm(KeyAlgorithm algo) {
        if (!AlgorithmUtils.isPbesHsWrap(algo.getJwaName())) {
            LOG.warning("Invalid key encryption algorithm");
            throw new JweException(JweException.Error.INVALID_KEY_ALGORITHM);
        }
        return algo;
    }
    static int validatePbesCount(int count) {
        if (count < 1000) {
            LOG.warning("Iteration count is too low");
            throw new JweException(JweException.Error.KEY_ENCRYPTION_FAILURE);
        }
        return count;
    }

    static byte[] stringToBytes(String str) {
        return StringUtils.toBytesUTF8(str);
    }
    static byte[] charsToBytes(char[] chars) {
        ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return b;
    }
    @Override
    public KeyAlgorithm getAlgorithm() {
        return keyAlgoJwt;
    }

}
