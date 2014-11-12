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
package org.apache.cxf.rs.security.oidc.rp;

import java.security.NoSuchAlgorithmException;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.MessageDigestUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;

public final class OidcUtils {
    private OidcUtils() {
        
    }
    public static void validateAccessTokenHash(ClientAccessToken at, JwtToken jwt) {
        validateAccessTokenHash(at, jwt, true);
    }
    public static void validateAccessTokenHash(ClientAccessToken at, JwtToken jwt, boolean required) {
        validateHash(at.getTokenKey(),
                     (String)jwt.getClaims().getClaim("at_hash"),
                     jwt.getHeaders().getAlgorithm(),
                     required);
    }
    public static void validateCodeHash(String code, JwtToken jwt) {
        validateCodeHash(code, jwt, true);
    }
    public static void validateCodeHash(String code, JwtToken jwt, boolean required) {
        validateHash(code,
                     (String)jwt.getClaims().getClaim("c_hash"),
                     jwt.getHeaders().getAlgorithm(),
                     required);
    }
    private static void validateHash(String value, String theHash, String joseAlgo, boolean required) {
        String hash = calculateHash(value, joseAlgo);
        if (!hash.equals(theHash)) {
            throw new SecurityException("Invalid hash");
        }
    }
    public static String calculateHash(String value, String joseAlgo) {
        //TODO: map from the JOSE alg to a signature alg, 
        // for example, RS256 -> SHA-256 
        // and calculate the chunk size based on the algo key size
        // for example SHA-256 -> 256/8 = 32 and 32/2 = 16 bytes
        try {
            byte[] atBytes = StringUtils.toBytesASCII(value);
            byte[] digest = MessageDigestUtils.createDigest(atBytes,  MessageDigestUtils.ALGO_SHA_256);
            return Base64UrlUtility.encodeChunk(digest, 0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException(ex);
        }
    }
    
}
