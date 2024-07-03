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




public enum SignatureAlgorithm {
    HS256(AlgorithmUtils.HMAC_SHA_256_ALGO, AlgorithmUtils.HMAC_SHA_256_JAVA, 256),
    HS384(AlgorithmUtils.HMAC_SHA_384_ALGO, AlgorithmUtils.HMAC_SHA_384_JAVA, 384),
    HS512(AlgorithmUtils.HMAC_SHA_512_ALGO, AlgorithmUtils.HMAC_SHA_512_JAVA, 512),

    RS256(AlgorithmUtils.RS_SHA_256_ALGO, AlgorithmUtils.RS_SHA_256_JAVA, -1),
    RS384(AlgorithmUtils.RS_SHA_384_ALGO, AlgorithmUtils.RS_SHA_384_JAVA, -1),
    RS512(AlgorithmUtils.RS_SHA_512_ALGO, AlgorithmUtils.RS_SHA_512_JAVA, -1),

    PS256(AlgorithmUtils.PS_SHA_256_ALGO, AlgorithmUtils.PS_SHA_JAVA, -1),
    PS384(AlgorithmUtils.PS_SHA_384_ALGO, AlgorithmUtils.PS_SHA_JAVA, -1),
    PS512(AlgorithmUtils.PS_SHA_512_ALGO, AlgorithmUtils.PS_SHA_JAVA, -1),

    ES256(AlgorithmUtils.ES_SHA_256_ALGO, AlgorithmUtils.ES_SHA_256_JAVA, -1),
    ES384(AlgorithmUtils.ES_SHA_384_ALGO, AlgorithmUtils.ES_SHA_384_JAVA, -1),
    ES512(AlgorithmUtils.ES_SHA_512_ALGO, AlgorithmUtils.ES_SHA_512_JAVA, -1),

    NONE(AlgorithmUtils.NONE_TEXT_ALGO, null, -1);


    private final String jwaName;
    private final String javaName;
    private final int keySizeBits;

    SignatureAlgorithm(String jwaName, String javaName, int keySizeBits) {
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

    public static SignatureAlgorithm getAlgorithm(String algo) {
        if (algo == null) {
            return null;
        }
        if (AlgorithmUtils.NONE_TEXT_ALGO.equals(algo)) {
            return NONE;
        }
        return SignatureAlgorithm.valueOf(algo.replace('-', '_')
                                        .replace('+', '_'));

    }

    public static boolean isPublicKeyAlgorithm(SignatureAlgorithm sigAlgorithm) {
        if (sigAlgorithm == null || sigAlgorithm.getJwaName() == null) {
            return false;
        }

        return sigAlgorithm.getJwaName().startsWith("RS") || sigAlgorithm.getJwaName().startsWith("PS")
            || sigAlgorithm.getJwaName().startsWith("ES");
    }

}