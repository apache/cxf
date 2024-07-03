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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;




public final class AlgorithmUtils {
    public static final String AES = "AES";

    // Key Encryption
    // JWA
    public static final String RSA_OAEP_ALGO = "RSA-OAEP";
    public static final String RSA_OAEP_256_ALGO = "RSA-OAEP-256";
    public static final String RSA1_5_ALGO = "RSA1_5";
    public static final String A128KW_ALGO = "A128KW";
    public static final String A192KW_ALGO = "A192KW";
    public static final String A256KW_ALGO = "A256KW";
    public static final String A128GCMKW_ALGO = "A128GCMKW";
    public static final String A192GCMKW_ALGO = "A192GCMKW";
    public static final String A256GCMKW_ALGO = "A256GCMKW";
    public static final String ECDH_ES_A128KW_ALGO = "ECDH-ES+A128KW";
    public static final String ECDH_ES_A192KW_ALGO = "ECDH-ES+A192KW";
    public static final String ECDH_ES_A256KW_ALGO = "ECDH-ES+A256KW";
    public static final String PBES2_HS256_A128KW_ALGO = "PBES2-HS256+A128KW";
    public static final String PBES2_HS384_A192KW_ALGO = "PBES2-HS384+A192KW";
    public static final String PBES2_HS512_A256KW_ALGO = "PBES2-HS512+A256KW";
    public static final String ECDH_ES_DIRECT_ALGO = "ECDH-ES";
    // Java
    public static final String RSA_OAEP_ALGO_JAVA = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
    public static final String RSA_OAEP_256_ALGO_JAVA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String RSA_1_5_ALGO_JAVA = "RSA/ECB/PKCS1Padding";
    public static final String AES_WRAP_ALGO_JAVA = AES + "Wrap";
    // Content Encryption
    // JWA
    public static final String A128CBC_HS256_ALGO = "A128CBC-HS256";
    public static final String A192CBC_HS384_ALGO = "A192CBC-HS384";
    public static final String A256CBC_HS512_ALGO = "A256CBC-HS512";
    public static final String A128GCM_ALGO = "A128GCM";
    public static final String A192GCM_ALGO = "A192GCM";
    public static final String A256GCM_ALGO = "A256GCM";
    // Java
    public static final String AES_GCM_ALGO_JAVA = AES + "/GCM/NoPadding";
    public static final String AES_CBC_ALGO_JAVA = AES + "/CBC/PKCS5Padding";
    // Signature
    // JWA
    public static final String HMAC_SHA_256_ALGO = "HS256";
    public static final String HMAC_SHA_384_ALGO = "HS384";
    public static final String HMAC_SHA_512_ALGO = "HS512";
    public static final String RS_SHA_256_ALGO = "RS256";
    public static final String RS_SHA_384_ALGO = "RS384";
    public static final String RS_SHA_512_ALGO = "RS512";
    public static final String PS_SHA_256_ALGO = "PS256";
    public static final String PS_SHA_384_ALGO = "PS384";
    public static final String PS_SHA_512_ALGO = "PS512";
    public static final String ES_SHA_256_ALGO = "ES256";
    public static final String ES_SHA_384_ALGO = "ES384";
    public static final String ES_SHA_512_ALGO = "ES512";
    public static final String NONE_TEXT_ALGO = "none";
    // Java
    public static final String HMAC_SHA_256_JAVA = "HmacSHA256";
    public static final String HMAC_SHA_384_JAVA = "HmacSHA384";
    public static final String HMAC_SHA_512_JAVA = "HmacSHA512";
    public static final String RS_SHA_256_JAVA = "SHA256withRSA";
    public static final String RS_SHA_384_JAVA = "SHA384withRSA";
    public static final String RS_SHA_512_JAVA = "SHA512withRSA";
    public static final String PS_SHA_JAVA = "RSASSA-PSS";
    public static final String ES_SHA_256_JAVA = "SHA256withECDSA";
    public static final String ES_SHA_384_JAVA = "SHA384withECDSA";
    public static final String ES_SHA_512_JAVA = "SHA512withECDSA";

    public static final Set<String> HMAC_SIGN_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(HMAC_SHA_256_ALGO,
                                                                HMAC_SHA_384_ALGO,
                                                                HMAC_SHA_512_ALGO)));

    public static final Set<String> RSA_SHA_SIGN_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(RS_SHA_256_ALGO,
                                                                RS_SHA_384_ALGO,
                                                                RS_SHA_512_ALGO)));

    public static final Set<String> RSA_SHA_PS_SIGN_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PS_SHA_256_ALGO,
                                                                PS_SHA_384_ALGO,
                                                                PS_SHA_512_ALGO)));

    public static final Set<String> EC_SHA_SIGN_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ES_SHA_256_ALGO,
                                                                ES_SHA_384_ALGO,
                                                                ES_SHA_512_ALGO)));

    public static final Set<String> RSA_CEK_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(RSA_OAEP_ALGO,
                                                                RSA_OAEP_256_ALGO,
                                                                RSA1_5_ALGO)));

    public static final Set<String> AES_GCM_CEK_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(A128GCM_ALGO,
                                                                A192GCM_ALGO,
                                                                A256GCM_ALGO)));

    public static final Set<String> AES_GCM_KW_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(A128GCMKW_ALGO,
                                                                A192GCMKW_ALGO,
                                                                A256GCMKW_ALGO)));

    public static final Set<String> AES_KW_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(A128KW_ALGO,
                                                                A192KW_ALGO,
                                                                A256KW_ALGO)));

    public static final Set<String> ACBC_HS_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(A128CBC_HS256_ALGO,
                                                                A192CBC_HS384_ALGO,
                                                                A256CBC_HS512_ALGO)));

    public static final Set<String> PBES_HS_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PBES2_HS256_A128KW_ALGO,
                                                                PBES2_HS384_A192KW_ALGO,
                                                                PBES2_HS512_A256KW_ALGO)));

    public static final Set<String> ECDH_ES_WRAP_SET =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ECDH_ES_A128KW_ALGO,
                                                                ECDH_ES_A192KW_ALGO,
                                                                ECDH_ES_A256KW_ALGO)));

    private static final Map<String, String> JAVA_TO_JWA_NAMES = new HashMap<>();
    private static final Map<String, String> JWA_TO_JAVA_NAMES = new HashMap<>();
    static {
        putAlgo(HMAC_SHA_256_JAVA, HMAC_SHA_256_ALGO);
        putAlgo(HMAC_SHA_384_JAVA, HMAC_SHA_384_ALGO);
        putAlgo(HMAC_SHA_512_JAVA, HMAC_SHA_512_ALGO);
        putAlgo(RS_SHA_256_JAVA, RS_SHA_256_ALGO);
        putAlgo(RS_SHA_384_JAVA, RS_SHA_384_ALGO);
        putAlgo(RS_SHA_512_JAVA, RS_SHA_512_ALGO);
        putAlgo(PS_SHA_JAVA, PS_SHA_256_ALGO);
        putAlgo(PS_SHA_JAVA, PS_SHA_384_ALGO);
        putAlgo(PS_SHA_JAVA, PS_SHA_512_ALGO);
        putAlgo(ES_SHA_256_JAVA, ES_SHA_256_ALGO);
        putAlgo(ES_SHA_384_JAVA, ES_SHA_384_ALGO);
        putAlgo(ES_SHA_512_JAVA, ES_SHA_512_ALGO);
        putAlgo(RSA_OAEP_ALGO_JAVA, RSA_OAEP_ALGO);
        putAlgo(RSA_OAEP_256_ALGO_JAVA, RSA_OAEP_256_ALGO);
        putAlgo(RSA_1_5_ALGO_JAVA, RSA1_5_ALGO);
        putAlgo(AES_GCM_ALGO_JAVA, A256GCM_ALGO);
        putAlgo(AES_GCM_ALGO_JAVA, A192GCM_ALGO);
        putAlgo(AES_GCM_ALGO_JAVA, A128GCM_ALGO);
        putAlgo(AES_WRAP_ALGO_JAVA, A128KW_ALGO);
        putAlgo(AES_WRAP_ALGO_JAVA, A192KW_ALGO);
        putAlgo(AES_WRAP_ALGO_JAVA, A256KW_ALGO);
        putAlgo(AES_CBC_ALGO_JAVA, A128CBC_HS256_ALGO);
        putAlgo(AES_CBC_ALGO_JAVA, A192CBC_HS384_ALGO);
        putAlgo(AES_CBC_ALGO_JAVA, A256CBC_HS512_ALGO);

        JWA_TO_JAVA_NAMES.put(A256GCMKW_ALGO, AES_GCM_ALGO_JAVA);
        JWA_TO_JAVA_NAMES.put(A192GCMKW_ALGO, AES_GCM_ALGO_JAVA);
        JWA_TO_JAVA_NAMES.put(A128GCMKW_ALGO, AES_GCM_ALGO_JAVA);
        JWA_TO_JAVA_NAMES.put(PBES2_HS256_A128KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWA_TO_JAVA_NAMES.put(PBES2_HS384_A192KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWA_TO_JAVA_NAMES.put(PBES2_HS512_A256KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWA_TO_JAVA_NAMES.put(ECDH_ES_A128KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWA_TO_JAVA_NAMES.put(ECDH_ES_A192KW_ALGO, AES_WRAP_ALGO_JAVA);
        JWA_TO_JAVA_NAMES.put(ECDH_ES_A256KW_ALGO, AES_WRAP_ALGO_JAVA);
    }

    private AlgorithmUtils() {
    }
    public static boolean isRsa(String algo) {
        return isRsaKeyWrap(algo) || isRsaSign(algo);
    }
    public static boolean isEc(String algo) {
        return isEcDsaSign(algo) || isEcdhEsWrap(algo);
    }
    public static boolean isRsaKeyWrap(String algo) {
        return RSA_CEK_SET.contains(algo);
    }
    public static boolean isAesKeyWrap(String algo) {
        return AES_KW_SET.contains(algo);
    }
    public static boolean isAesGcmKeyWrap(String algo) {
        return AES_GCM_KW_SET.contains(algo);
    }
    public static boolean isPbesHsWrap(String algo) {
        return PBES_HS_SET.contains(algo);
    }
    public static boolean isEcdhEsWrap(String algo) {
        return ECDH_ES_WRAP_SET.contains(algo);
    }
    public static boolean isEcdhEsDirect(String algo) {
        return ECDH_ES_DIRECT_ALGO.equals(algo);
    }
    public static boolean isAesGcm(String algo) {
        return AES_GCM_CEK_SET.contains(algo);
    }
    public static boolean isAesCbcHmac(String algo) {
        return ACBC_HS_SET.contains(algo);
    }
    public static boolean isOctet(String algo) {
        return isHmacSign(algo)
            || isAesCbcHmac(algo)
            || isAesGcm(algo)
            || isAesGcmKeyWrap(algo)
            || isAesKeyWrap(algo);
    }
    public static boolean isHmacSign(String algo) {
        return HMAC_SIGN_SET.contains(algo);
    }
    public static boolean isHmacSign(SignatureAlgorithm algo) {
        return isHmacSign(algo.getJwaName());
    }
    public static boolean isRsaSign(String algo) {
        return isRsaShaSign(algo) || isRsaShaPsSign(algo);
    }
    public static boolean isRsaSign(SignatureAlgorithm algo) {
        return isRsaSign(algo.getJwaName());
    }
    public static boolean isRsaShaSign(String algo) {
        return RSA_SHA_SIGN_SET.contains(algo);
    }
    public static boolean isRsaShaSign(SignatureAlgorithm algo) {
        return isRsaShaSign(algo.getJwaName());
    }
    public static boolean isRsaShaPsSign(String algo) {
        return RSA_SHA_PS_SIGN_SET.contains(algo);
    }
    public static boolean isRsaShaPsSign(SignatureAlgorithm algo) {
        return isRsaShaPsSign(algo.getJwaName());
    }
    public static boolean isEcDsaSign(String algo) {
        return EC_SHA_SIGN_SET.contains(algo);
    }
    public static boolean isEcDsaSign(SignatureAlgorithm algo) {
        return isEcDsaSign(algo.getJwaName());
    }

    public static String toJwaName(String javaName, int keyBitSize) {
        //TODO: perhaps a key should be a name+keysize pair
        String name = JAVA_TO_JWA_NAMES.get(javaName);
        if (name == null && javaName.startsWith(AES)) {
            name = "A" + keyBitSize + "GCM";
        }
        return name;
    }
    public static String toJavaName(String jwtName) {
        return JWA_TO_JAVA_NAMES.get(jwtName);
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

    private static void putAlgo(String java, String algo) {
        JAVA_TO_JWA_NAMES.put(java, algo);
        JWA_TO_JAVA_NAMES.put(algo, java);
    }
}