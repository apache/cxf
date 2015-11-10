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
package org.apache.cxf.rs.security.oidc.utils;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

public final class OidcUtils {
    public static final String ID_TOKEN = "id_token";
    public static final String OPENID_SCOPE = "openid";
    public static final String PROFILE_SCOPE = "profile";
    public static final String EMAIL_SCOPE = "email";
    public static final String ADDRESS_SCOPE = "address";
    public static final String PHONE_SCOPE = "phone";
    public static final List<String> PROFILE_CLAIMS = Arrays.asList(UserInfo.NAME_CLAIM, 
                                                                    UserInfo.PROFILE_CLAIM);
    public static final List<String> EMAIL_CLAIMS = Arrays.asList(UserInfo.EMAIL_CLAIM, 
                                                                  UserInfo.EMAIL_VERIFIED_CLAIM);
    public static final List<String> ADDRESS_CLAIMS = Arrays.asList(UserInfo.ADDRESS_CLAIM);
    public static final List<String> PHONE_CLAIMS = Arrays.asList(UserInfo.PHONE_CLAIM);
    private static final Map<String, List<String>> SCOPES_MAP;
    static {
        SCOPES_MAP = new HashMap<String, List<String>>();
        SCOPES_MAP.put(PHONE_SCOPE, PHONE_CLAIMS);
        SCOPES_MAP.put(EMAIL_SCOPE, EMAIL_CLAIMS);
        SCOPES_MAP.put(ADDRESS_SCOPE, ADDRESS_CLAIMS);
        SCOPES_MAP.put(PROFILE_SCOPE, PROFILE_CLAIMS);
    }
    
    private OidcUtils() {
        
    }
    public static String getOpenIdScope() {
        return OPENID_SCOPE;
    }
    public static String getProfileScope() {
        return getScope(OPENID_SCOPE, PROFILE_SCOPE);
    }
    public static String getEmailScope() {
        return getScope(OPENID_SCOPE, EMAIL_SCOPE);
    }
    public static String getAddressScope() {
        return getScope(OPENID_SCOPE, ADDRESS_SCOPE);
    }
    public static String getPhoneScope() {
        return getScope(OPENID_SCOPE, PHONE_SCOPE);
    }
    public static String getAllScopes() {
        return getScope(OPENID_SCOPE, PROFILE_SCOPE, EMAIL_SCOPE, ADDRESS_SCOPE, PHONE_SCOPE);
    }
    public static List<String> getScopeProperties(String scope) {
        return SCOPES_MAP.get(scope);
    }
    
    private static String getScope(String... scopes) {
        StringBuilder sb = new StringBuilder();
        for (String scope : scopes) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(scope);
        }
        return sb.toString();
    }
    public static void validateAccessTokenHash(ClientAccessToken at, JwtToken jwt) {
        validateAccessTokenHash(at, jwt, true);
    }
    public static void validateAccessTokenHash(ClientAccessToken at, JwtToken jwt, boolean required) {
        if (required) {
            validateHash(at.getTokenKey(),
                         (String)jwt.getClaims().getClaim("at_hash"),
                         jwt.getJwsHeaders().getAlgorithm());
        }
    }
    public static void validateCodeHash(String code, JwtToken jwt) {
        validateCodeHash(code, jwt, true);
    }
    public static void validateCodeHash(String code, JwtToken jwt, boolean required) {
        if (required) {
            validateHash(code,
                         (String)jwt.getClaims().getClaim("c_hash"),
                         jwt.getJwsHeaders().getAlgorithm());
        }
    }
    private static void validateHash(String value, String theHash, String joseAlgo) {
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
