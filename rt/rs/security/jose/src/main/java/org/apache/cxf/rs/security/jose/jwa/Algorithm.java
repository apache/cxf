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

package org.apache.cxf.rs.security.jose.jwa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.rs.security.jose.JoseConstants;



public enum Algorithm {
    // Signature
    HmacSHA256(JoseConstants.HMAC_SHA_256_ALGO, 256),
    HmacSHA384(JoseConstants.HMAC_SHA_384_ALGO, 384),
    HmacSHA512(JoseConstants.HMAC_SHA_512_ALGO, 512),
    
    SHA256withRSA(JoseConstants.RS_SHA_256_ALGO, 256),
    SHA384withRSA(JoseConstants.RS_SHA_384_ALGO, 384),
    SHA512withRSA(JoseConstants.RS_SHA_512_ALGO, 512),
    
    SHA256withECDSA(JoseConstants.ES_SHA_256_ALGO, 256),
    SHA384withECDSA(JoseConstants.ES_SHA_384_ALGO, 384),
    SHA512withECDSA(JoseConstants.ES_SHA_512_ALGO, 512),
    
    // Key Encryption
    RSA_OAEP(JoseConstants.RSA_OAEP_ALGO, "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", -1),
    RSA_OAEP_256(JoseConstants.RSA_OAEP_256_ALGO, "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", -1),
    RSA_1_5(JoseConstants.RSA_1_5_ALGO, "RSA/ECB/PKCS1Padding", -1),
    A128KW(JoseConstants.A128KW_ALGO, "AESWrap", 128),
    A192KW(JoseConstants.A192KW_ALGO, "AESWrap", 192),
    A256KW(JoseConstants.A256KW_ALGO, "AESWrap", 256),
    A128GCMKW(JoseConstants.A128GCMKW_ALGO, "AES/GCM/NoPadding", 128),
    A192GCMKW(JoseConstants.A192GCMKW_ALGO, "AES/GCM/NoPadding", 192),
    A256GCMKW(JoseConstants.A256GCMKW_ALGO, "AES/GCM/NoPadding", 256),
    PBES2_HS256_A128KW(JoseConstants.PBES2_HS256_A128KW_ALGO, "AESWrap", 128),
    PBES2_HS384_A192KW(JoseConstants.PBES2_HS384_A192KW_ALGO, "AESWrap", 192),
    PBES2_HS512_A256KW(JoseConstants.PBES2_HS512_A256KW_ALGO, "AESWrap", 256),
    
    // Content Encryption
    A128GCM(JoseConstants.A128GCM_ALGO, "AES/GCM/NoPadding", 128),
    A192GCM(JoseConstants.A192GCM_ALGO, "AES/GCM/NoPadding", 192),
    A256GCM(JoseConstants.A256GCM_ALGO, "AES/GCM/NoPadding", 256),
    A128CBC_HS256(JoseConstants.A128CBC_HS256_ALGO, "AES/CBC/PKCS7Padding", 128),
    A192CBC_HS384(JoseConstants.A192CBC_HS384_ALGO, "AES/CBC/PKCS7Padding", 192),
    A256CBC_HS512(JoseConstants.A256CBC_HS512_ALGO, "AES/CBC/PKCS7Padding", 256);
    
    public static final String HMAC_SHA_256_JAVA = "HmacSHA256";
    public static final String HMAC_SHA_384_JAVA = "HmacSHA384";
    public static final String HMAC_SHA_512_JAVA = "HmacSHA512";
    public static final String RS_SHA_256_JAVA = "SHA256withRSA";
    public static final String RS_SHA_384_JAVA = "SHA384withRSA";
    public static final String RS_SHA_512_JAVA = "SHA512withRSA";
    public static final String ES_SHA_256_JAVA = "SHA256withECDSA";
    public static final String ES_SHA_384_JAVA = "SHA384withECDSA";
    public static final String ES_SHA_512_JAVA = "SHA512withECDSA";
    public static final String RSA_OAEP_ALGO_JAVA = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
    public static final String RSA_OAEP_256_ALGO_JAVA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String RSA_1_5_ALGO_JAVA = "RSA/ECB/PKCS1Padding";
    public static final String AES_ALGO_JAVA = "AES";
    public static final String AES_WRAP_ALGO_JAVA = "AESWrap";
    public static final String AES_GCM_ALGO_JAVA = "AES/GCM/NoPadding";
    public static final String AES_CBC_ALGO_JAVA = "AES/CBC/PKCS7Padding";
    
    public static final Set<String> HMAC_SIGN_SET = new HashSet<String>(Arrays.asList(JoseConstants.HMAC_SHA_256_ALGO,
                                                                        JoseConstants.HMAC_SHA_384_ALGO,
                                                                        JoseConstants.HMAC_SHA_512_ALGO));
    public static final Set<String> RSA_SHA_SIGN_SET = new HashSet<String>(Arrays.asList(JoseConstants.RS_SHA_256_ALGO,
                                                                        JoseConstants.RS_SHA_384_ALGO,
                                                                        JoseConstants.RS_SHA_512_ALGO));
    public static final Set<String> EC_SHA_SIGN_SET = new HashSet<String>(Arrays.asList(JoseConstants.ES_SHA_256_ALGO,
                                                                         JoseConstants.ES_SHA_384_ALGO,
                                                                         JoseConstants.ES_SHA_512_ALGO));
    public static final Set<String> RSA_OAEP_CEK_SET = new HashSet<String>(Arrays.asList(JoseConstants.RSA_OAEP_ALGO,
                                                                               JoseConstants.RSA_OAEP_256_ALGO));
    public static final Set<String> AES_GCM_CEK_SET = new HashSet<String>(Arrays.asList(JoseConstants.A128GCM_ALGO,
                                                                                        JoseConstants.A192GCM_ALGO,
                                                                                        JoseConstants.A256GCM_ALGO));
    public static final Set<String> AES_GCM_KW_SET = new HashSet<String>(Arrays.asList(JoseConstants.A192GCMKW_ALGO,
                                                                                        JoseConstants.A192GCMKW_ALGO,
                                                                                        JoseConstants.A256GCMKW_ALGO));
    public static final Set<String> AES_KW_SET = new HashSet<String>(Arrays.asList(JoseConstants.A128KW_ALGO,
                                                                                        JoseConstants.A192KW_ALGO,
                                                                                        JoseConstants.A256KW_ALGO));
    public static final Set<String> ACBC_HS_SET = 
        new HashSet<String>(Arrays.asList(JoseConstants.A128CBC_HS256_ALGO,
                                          JoseConstants.A192CBC_HS384_ALGO,
                                          JoseConstants.A256CBC_HS512_ALGO));
    
    private static final Map<String, String> JAVA_TO_JWT_NAMES;
    private static final Map<String, String> JWT_TO_JAVA_NAMES;
    static {
        JAVA_TO_JWT_NAMES = new HashMap<String, String>();
        JAVA_TO_JWT_NAMES.put(HMAC_SHA_256_JAVA, JoseConstants.HMAC_SHA_256_ALGO);
        JAVA_TO_JWT_NAMES.put(HMAC_SHA_384_JAVA, JoseConstants.HMAC_SHA_384_ALGO);
        JAVA_TO_JWT_NAMES.put(HMAC_SHA_512_JAVA, JoseConstants.HMAC_SHA_512_ALGO);
        JAVA_TO_JWT_NAMES.put(RS_SHA_256_JAVA, JoseConstants.RS_SHA_256_ALGO);
        JAVA_TO_JWT_NAMES.put(RS_SHA_384_JAVA, JoseConstants.RS_SHA_384_ALGO);
        JAVA_TO_JWT_NAMES.put(RS_SHA_512_JAVA, JoseConstants.RS_SHA_512_ALGO);
        JAVA_TO_JWT_NAMES.put(ES_SHA_256_JAVA, JoseConstants.ES_SHA_256_ALGO);
        JAVA_TO_JWT_NAMES.put(ES_SHA_384_JAVA, JoseConstants.ES_SHA_384_ALGO);
        JAVA_TO_JWT_NAMES.put(ES_SHA_512_JAVA, JoseConstants.ES_SHA_512_ALGO);
        JAVA_TO_JWT_NAMES.put(RSA_OAEP_ALGO_JAVA, JoseConstants.RSA_OAEP_ALGO);
        JAVA_TO_JWT_NAMES.put(RSA_OAEP_256_ALGO_JAVA, JoseConstants.RSA_OAEP_256_ALGO);
        JAVA_TO_JWT_NAMES.put(RSA_1_5_ALGO_JAVA, JoseConstants.RSA_1_5_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_GCM_ALGO_JAVA, JoseConstants.A256GCM_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_GCM_ALGO_JAVA, JoseConstants.A192GCM_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_GCM_ALGO_JAVA, JoseConstants.A128GCM_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_WRAP_ALGO_JAVA, JoseConstants.A128KW_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_WRAP_ALGO_JAVA, JoseConstants.A192KW_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_WRAP_ALGO_JAVA, JoseConstants.A256KW_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_CBC_ALGO_JAVA, JoseConstants.A128CBC_HS256_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_CBC_ALGO_JAVA, JoseConstants.A192CBC_HS384_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_CBC_ALGO_JAVA, JoseConstants.A256CBC_HS512_ALGO);
        JWT_TO_JAVA_NAMES = new HashMap<String, String>();
        JWT_TO_JAVA_NAMES.put(JoseConstants.HMAC_SHA_256_ALGO, HMAC_SHA_256_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.HMAC_SHA_384_ALGO, HMAC_SHA_384_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.HMAC_SHA_512_ALGO, HMAC_SHA_512_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.RS_SHA_256_ALGO, RS_SHA_256_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.RS_SHA_384_ALGO, RS_SHA_384_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.RS_SHA_512_ALGO, RS_SHA_512_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.ES_SHA_256_ALGO, ES_SHA_256_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.ES_SHA_384_ALGO, ES_SHA_384_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.ES_SHA_512_ALGO, ES_SHA_512_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.RSA_OAEP_ALGO, RSA_OAEP_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.RSA_OAEP_256_ALGO, RSA_OAEP_256_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.RSA_1_5_ALGO, RSA_1_5_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A128KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A192KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A256KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A256GCM_ALGO, AES_GCM_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A192GCM_ALGO, AES_GCM_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A128GCM_ALGO, AES_GCM_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A256GCMKW_ALGO, AES_GCM_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A192GCMKW_ALGO, AES_GCM_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A128GCMKW_ALGO, AES_GCM_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A128CBC_HS256_ALGO, AES_CBC_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A192CBC_HS384_ALGO, AES_CBC_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.A256CBC_HS512_ALGO, AES_CBC_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.PBES2_HS256_A128KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.PBES2_HS384_A192KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JoseConstants.PBES2_HS512_A256KW_ALGO, AES_WRAP_ALGO_JAVA);
    }
    private final String jwtName;
    private final String javaName;
    private final int keySizeBits;
    
    private Algorithm(String jwtName, int keySizeBits) {
        this(jwtName, null, keySizeBits);
    }
    private Algorithm(String jwtName, String javaName, int keySizeBits) {
        this.jwtName = jwtName;
        this.javaName = javaName;
        this.keySizeBits = keySizeBits;
    }

    public String getJwtName() {
        return jwtName;
    }

    public String getJavaName() {
        return javaName == null ? name() : javaName;
    }
    
    public String getJavaAlgoName() {
        return stripAlgoProperties(getJavaName());
    }

    public int getKeySizeBits() {
        return keySizeBits;
    }
    
    public static String toJwtName(String javaName, int keyBitSize) {
        //TODO: perhaps a key should be a name+keysize pair
        String name = JAVA_TO_JWT_NAMES.get(javaName);
        if (name == null && javaName.startsWith(AES_ALGO_JAVA)) {
            name = "A" + keyBitSize + "GCM";
        } 
        return name;
    }
    public static String toJavaName(String jwtName) {    
        return JWT_TO_JAVA_NAMES.get(jwtName);
    }
    public static String toJavaAlgoNameOnly(String jwtName) {    
        return stripAlgoProperties(toJavaName(jwtName));
    }
    public static String stripAlgoProperties(String javaName) {    
        if (javaName != null) {
            int index = javaName.indexOf('/');
            if (index != -1) {
                javaName = javaName.substring(0, index);
            }
        }
        return javaName;
    }
    public static boolean isRsa(String algo) {
        return isRsaOaep(algo) || isRsaShaSign(algo);
    }
    public static boolean isRsaOaep(String algo) {
        return RSA_OAEP_CEK_SET.contains(algo);
    }
    public static boolean isAesKeyWrap(String algo) {
        return AES_KW_SET.contains(algo);
    }
    public static boolean isAesGcmKeyWrap(String algo) {
        return AES_GCM_KW_SET.contains(algo);
    }
    public static boolean isAesGcm(String algo) {
        return AES_GCM_CEK_SET.contains(algo);
    }
    public static boolean isAesCbcHmac(String algo) {
        return ACBC_HS_SET.contains(algo); 
    }
    public static boolean isHmacSign(String algo) {
        return HMAC_SIGN_SET.contains(algo); 
    }
    public static boolean isOctet(String algo) {
        return isHmacSign(algo)
            || isAesCbcHmac(algo)
            || isAesGcm(algo)
            || isAesGcmKeyWrap(algo)
            || isAesKeyWrap(algo); 
    } 
    public static boolean isRsaShaSign(String algo) {
        return RSA_SHA_SIGN_SET.contains(algo); 
    }
    public static boolean isEcDsaSign(String algo) {
        return EC_SHA_SIGN_SET.contains(algo); 
    }
}