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
package org.apache.cxf.rs.security.oauth2.jwe;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

public class PbesHmacAesWrapKeyEncryptionAlgorithm implements KeyEncryptionAlgorithm {
    private static final Set<String> SUPPORTED_ALGORITHMS = new HashSet<String>(
        Arrays.asList(Algorithm.PBES2_HS256_A128KW.getJwtName(),
                      Algorithm.PBES2_HS384_A192KW.getJwtName(),
                      Algorithm.PBES2_HS512_A256KW.getJwtName()));
    private static final Map<String, Integer> PBES_HMAC_MAP;
    private static final Map<String, String> PBES_AES_MAP;
    private static final Map<String, Integer> DERIVED_KEY_SIZE_MAP;
    static {
        PBES_HMAC_MAP = new HashMap<String, Integer>();
        PBES_HMAC_MAP.put(Algorithm.PBES2_HS256_A128KW.getJwtName(), 256);
        PBES_HMAC_MAP.put(Algorithm.PBES2_HS384_A192KW.getJwtName(), 384);
        PBES_HMAC_MAP.put(Algorithm.PBES2_HS512_A256KW.getJwtName(), 512);
        
        PBES_AES_MAP = new HashMap<String, String>();
        PBES_AES_MAP.put(Algorithm.PBES2_HS256_A128KW.getJwtName(), Algorithm.A128KW.getJwtName());
        PBES_AES_MAP.put(Algorithm.PBES2_HS384_A192KW.getJwtName(), Algorithm.A192KW.getJwtName());
        PBES_AES_MAP.put(Algorithm.PBES2_HS512_A256KW.getJwtName(), Algorithm.A256KW.getJwtName());
        
        DERIVED_KEY_SIZE_MAP = new HashMap<String, Integer>();
        DERIVED_KEY_SIZE_MAP.put(Algorithm.PBES2_HS256_A128KW.getJwtName(), 16);
        DERIVED_KEY_SIZE_MAP.put(Algorithm.PBES2_HS384_A192KW.getJwtName(), 24);
        DERIVED_KEY_SIZE_MAP.put(Algorithm.PBES2_HS512_A256KW.getJwtName(), 32);
    }
    
    
    private byte[] password;
    private int pbesCount;
    private String keyAlgoJwt;
    public PbesHmacAesWrapKeyEncryptionAlgorithm(String password, String keyAlgoJwt) {
        this(stringToBytes(password), keyAlgoJwt);
    }
    public PbesHmacAesWrapKeyEncryptionAlgorithm(String password, int pbesCount, String keyAlgoJwt) {
        this(stringToBytes(password), pbesCount, keyAlgoJwt);
    }
    public PbesHmacAesWrapKeyEncryptionAlgorithm(byte[] password, String keyAlgoJwt) {
        this(password, 4096, keyAlgoJwt);
    }
    public PbesHmacAesWrapKeyEncryptionAlgorithm(byte[] password, int pbesCount, String keyAlgoJwt) {
        this.password = password;
        this.keyAlgoJwt = validateKeyAlgorithm(keyAlgoJwt);
        this.pbesCount = validatePbesCount(pbesCount);
    }
    
    @Override
    public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] cek) {
        int keySize = getKeySize(keyAlgoJwt);
        byte[] saltInput = CryptoUtils.generateSecureRandomBytes(keySize);
        byte[] derivedKey = createDerivedKey(keyAlgoJwt, keySize, password, saltInput, pbesCount);
        
        headers.setHeader("p2s", Base64UrlUtility.encode(saltInput));
        headers.setHeader("p2c", Integer.valueOf(pbesCount));
        
        final String aesAlgoJwt = PBES_AES_MAP.get(keyAlgoJwt);
        KeyEncryptionAlgorithm aesWrap = new AesWrapKeyEncryptionAlgorithm(derivedKey, aesAlgoJwt) {
            protected void checkAlgorithms(JweHeaders headers, String defaultAlgo) {
                // complete
            }
            protected String getKeyEncryptionAlgoJava(JweHeaders headers) {
                return Algorithm.AES_WRAP_ALGO_JAVA;
            }
        };
        return aesWrap.getEncryptedContentEncryptionKey(headers, cek);
        
        
    }
    static int getKeySize(String keyAlgoJwt) {
        return DERIVED_KEY_SIZE_MAP.get(keyAlgoJwt);
    }
    static byte[] createDerivedKey(String keyAlgoJwt, int keySize,
                                   byte[] password, byte[] saltInput, int pbesCount) {
        byte[] saltValue = createSaltValue(keyAlgoJwt, saltInput);
        Digest digest = null;
        int macSigSize = PBES_HMAC_MAP.get(keyAlgoJwt);
        if (macSigSize == 256) { 
            digest = new SHA256Digest();
        } else if (macSigSize == 384) {
            digest = new SHA384Digest();
        } else {
            digest = new SHA512Digest();
        }
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(digest);
        gen.init(password, saltValue, pbesCount);
        return ((KeyParameter) gen.generateDerivedParameters(keySize * 8)).getKey();
    }
    
    
    private static byte[] createSaltValue(String keyAlgoJwt, byte[] saltInput) {
        byte[] algoBytes = stringToBytes(keyAlgoJwt);
        byte[] saltValue = new byte[algoBytes.length + 1 + saltInput.length];
        System.arraycopy(algoBytes, 0, saltValue, 0, algoBytes.length);
        saltValue[algoBytes.length] = 0;
        System.arraycopy(saltInput, 0, saltValue, algoBytes.length + 1, saltInput.length);
        return saltValue;
    }
    static String validateKeyAlgorithm(String algo) {
        if (!SUPPORTED_ALGORITHMS.contains(algo)) {
            throw new SecurityException();
        }
        return algo;
    }
    static int validatePbesCount(int count) {
        if (count < 1000) {
            throw new SecurityException();
        }
        return count;
    }    
    
    static byte[] stringToBytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new SecurityException(ex);
        }
    }
    
}
