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

package org.apache.cxf.rs.security.oauth2.jwt;

import java.util.HashMap;
import java.util.Map;




public enum Algorithm {
    // Signature
    HmacSHA256(JwtConstants.HMAC_SHA_256_ALGO, 256),
    HmacSHA384(JwtConstants.HMAC_SHA_384_ALGO, 384),
    HmacSHA512(JwtConstants.HMAC_SHA_512_ALGO, 512),
    
    SHA256withRSA(JwtConstants.RS_SHA_256_ALGO, 256),
    SHA384withRSA(JwtConstants.RS_SHA_384_ALGO, 384),
    SHA512withRSA(JwtConstants.RS_SHA_512_ALGO, 512),
    
    // Key Encryption
    RSA_OAEP_ALGO(JwtConstants.RSA_OAEP_ALGO, "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", -1),
    // Content Encryption
    A256GCM_ALGO(JwtConstants.A256GCM_ALGO, "AES/GCM/NoPadding", 256);
    
    public static final String HMAC_SHA_256_JAVA = "HmacSHA256";
    public static final String HMAC_SHA_384_JAVA = "HmacSHA384";
    public static final String HMAC_SHA_512_JAVA = "HmacSHA512";
    public static final String RS_SHA_256_JAVA = "SHA256withRSA";
    public static final String RS_SHA_384_JAVA = "SHA384withRSA";
    public static final String RS_SHA_512_JAVA = "SHA512withRSA";
    public static final String RSA_OAEP_ALGO_JAVA = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
    public static final String RSA_OAEP_256_ALGO_JAVA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String RSA_1_5_ALGO_JAVA = "RSA/ECB/PKCS1Padding";
    public static final String AES_ALGO_JAVA = "AES";
    public static final String A256GCM_ALGO_JAVA = "AES/GCM/NoPadding";
    
    private static final Map<String, String> JAVA_TO_JWT_NAMES;
    private static final Map<String, String> JWT_TO_JAVA_NAMES;
    static {
        JAVA_TO_JWT_NAMES = new HashMap<String, String>();
        JAVA_TO_JWT_NAMES.put(HMAC_SHA_256_JAVA, JwtConstants.HMAC_SHA_256_ALGO);
        JAVA_TO_JWT_NAMES.put(HMAC_SHA_384_JAVA, JwtConstants.HMAC_SHA_384_ALGO);
        JAVA_TO_JWT_NAMES.put(HMAC_SHA_512_JAVA, JwtConstants.HMAC_SHA_512_ALGO);
        JAVA_TO_JWT_NAMES.put(RS_SHA_256_JAVA, JwtConstants.RS_SHA_256_ALGO);
        JAVA_TO_JWT_NAMES.put(RS_SHA_384_JAVA, JwtConstants.RS_SHA_384_ALGO);
        JAVA_TO_JWT_NAMES.put(RS_SHA_512_JAVA, JwtConstants.RS_SHA_512_ALGO);
        JAVA_TO_JWT_NAMES.put(RSA_OAEP_ALGO_JAVA, JwtConstants.RSA_OAEP_ALGO);
        JAVA_TO_JWT_NAMES.put(RSA_OAEP_256_ALGO_JAVA, JwtConstants.RSA_OAEP_256_ALGO);
        JAVA_TO_JWT_NAMES.put(RSA_1_5_ALGO_JAVA, JwtConstants.RSA_1_5_ALGO);
        JAVA_TO_JWT_NAMES.put(A256GCM_ALGO_JAVA, JwtConstants.A256GCM_ALGO);
        JAVA_TO_JWT_NAMES.put(AES_ALGO_JAVA, JwtConstants.A256GCM_ALGO);
        JWT_TO_JAVA_NAMES = new HashMap<String, String>();
        JWT_TO_JAVA_NAMES.put(JwtConstants.HMAC_SHA_256_ALGO, HMAC_SHA_256_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.HMAC_SHA_384_ALGO, HMAC_SHA_384_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.HMAC_SHA_512_ALGO, HMAC_SHA_512_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.RS_SHA_256_ALGO, RS_SHA_256_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.RS_SHA_384_ALGO, RS_SHA_384_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.RS_SHA_512_ALGO, RS_SHA_512_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.RSA_OAEP_ALGO, RSA_OAEP_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.RSA_OAEP_256_ALGO, RSA_OAEP_256_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.RSA_1_5_ALGO, RSA_1_5_ALGO_JAVA);
        JWT_TO_JAVA_NAMES.put(JwtConstants.A256GCM_ALGO, A256GCM_ALGO_JAVA);
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
    
    public static String toJwtName(String javaName) {    
        return JAVA_TO_JWT_NAMES.get(javaName);
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
    
}