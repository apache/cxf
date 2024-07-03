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




public enum ContentAlgorithm {
    A128GCM(AlgorithmUtils.A128GCM_ALGO, "AES/GCM/NoPadding", 128),
    A192GCM(AlgorithmUtils.A192GCM_ALGO, "AES/GCM/NoPadding", 192),
    A256GCM(AlgorithmUtils.A256GCM_ALGO, "AES/GCM/NoPadding", 256),
    //TODO: default to "AES/CBC/PKCS5Padding" if Cipher "AES/CBC/PKCS7Padding"
    // can not be initialized, apparently Java 8 has decided to settle on PKCS5Padding only
    A128CBC_HS256(AlgorithmUtils.A128CBC_HS256_ALGO, "AES/CBC/PKCS5Padding", 128),
    A192CBC_HS384(AlgorithmUtils.A192CBC_HS384_ALGO, "AES/CBC/PKCS5Padding", 192),
    A256CBC_HS512(AlgorithmUtils.A256CBC_HS512_ALGO, "AES/CBC/PKCS5Padding", 256);

    private final String jwaName;
    private final String javaName;
    private final int keySizeBits;

    ContentAlgorithm(String jwaName, String javaName, int keySizeBits) {
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

    public static ContentAlgorithm getAlgorithm(String algo) {
        if (algo == null) {
            return null;
        }
        return ContentAlgorithm.valueOf(algo.replace('-', '_')
                                        .replace('+', '_'));

    }

}