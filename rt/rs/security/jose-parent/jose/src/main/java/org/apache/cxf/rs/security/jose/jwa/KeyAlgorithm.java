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




public enum KeyAlgorithm {
    RSA_OAEP(AlgorithmUtils.RSA_OAEP_ALGO, "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", -1),
    RSA_OAEP_256(AlgorithmUtils.RSA_OAEP_256_ALGO, "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", -1),
    RSA1_5(AlgorithmUtils.RSA1_5_ALGO, "RSA/ECB/PKCS1Padding", -1),
    A128KW(AlgorithmUtils.A128KW_ALGO, "AESWrap", 128),
    A192KW(AlgorithmUtils.A192KW_ALGO, "AESWrap", 192),
    A256KW(AlgorithmUtils.A256KW_ALGO, "AESWrap", 256),
    A128GCMKW(AlgorithmUtils.A128GCMKW_ALGO, "AES/GCM/NoPadding", 128),
    A192GCMKW(AlgorithmUtils.A192GCMKW_ALGO, "AES/GCM/NoPadding", 192),
    A256GCMKW(AlgorithmUtils.A256GCMKW_ALGO, "AES/GCM/NoPadding", 256),
    PBES2_HS256_A128KW(AlgorithmUtils.PBES2_HS256_A128KW_ALGO, "AESWrap", 128),
    PBES2_HS384_A192KW(AlgorithmUtils.PBES2_HS384_A192KW_ALGO, "AESWrap", 192),
    PBES2_HS512_A256KW(AlgorithmUtils.PBES2_HS512_A256KW_ALGO, "AESWrap", 256),
    ECDH_ES_A128KW(AlgorithmUtils.ECDH_ES_A128KW_ALGO, "AESWrap", 128),
    ECDH_ES_A192KW(AlgorithmUtils.ECDH_ES_A192KW_ALGO, "AESWrap", 192),
    ECDH_ES_A256KW(AlgorithmUtils.ECDH_ES_A256KW_ALGO, "AESWrap", 256),
    ECDH_ES_DIRECT(AlgorithmUtils.ECDH_ES_DIRECT_ALGO, null, -1),

    DIRECT("dir", null, -1);

    private final String jwaName;
    private final String javaName;
    private final int keySizeBits;

    KeyAlgorithm(String jwaName, String javaName, int keySizeBits) {
        this.jwaName = jwaName;
        this.javaName = javaName;
        this.keySizeBits = keySizeBits;
    }

    public String getJwaName() {
        return jwaName;
    }

    public String getJavaName() {
        return javaName == null ? name() : javaName;
    }

    public String getJavaAlgoName() {
        return AlgorithmUtils.stripAlgoProperties(getJavaName());
    }

    public int getKeySizeBits() {
        return keySizeBits;
    }
    public static KeyAlgorithm getAlgorithm(String algo) {
        if (algo == null) {
            return null;
        }
        if (KeyAlgorithm.DIRECT.getJwaName().equals(algo)) {
            return KeyAlgorithm.DIRECT;
        }
        if (KeyAlgorithm.ECDH_ES_DIRECT.getJwaName().equals(algo)) {
            return KeyAlgorithm.ECDH_ES_DIRECT;
        }
        return KeyAlgorithm.valueOf(algo.replace('-', '_')
                                    .replace('+', '_'));

    }

    public static boolean isDirect(KeyAlgorithm algo) {
        return algo == DIRECT || algo == ECDH_ES_DIRECT;
    }

}