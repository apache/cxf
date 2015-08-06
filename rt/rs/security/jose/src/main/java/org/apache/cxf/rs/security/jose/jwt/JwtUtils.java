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
package org.apache.cxf.rs.security.jose.jwt;


public final class JwtUtils {
    private JwtUtils() {
        
    }
    public static String claimsToJson(JwtClaims claims) {
        return claimsToJson(claims, null);
    }
    public static String claimsToJson(JwtClaims claims, JwtTokenReaderWriter writer) {
        if (writer == null) {
            writer = new JwtTokenReaderWriter();
        }
        return writer.claimsToJson(claims);
    }
    public static JwtClaims jsonToClaims(String json) {
        return new JwtTokenReaderWriter().fromJsonClaims(json);
    }
    
    public static void validateJwtTimeClaims(JwtClaims claims, int clockOffset, 
                                             int issuedAtRange, boolean claimsRequired) {
        Long currentTimeInSecs = System.currentTimeMillis() / 1000;
        Long expiryTimeInSecs = claims.getExpiryTime();
        if (expiryTimeInSecs == null && claimsRequired 
            || expiryTimeInSecs != null && currentTimeInSecs > expiryTimeInSecs) {
            throw new JwtException("The token expired");
        }
        Long issuedAtInSecs = claims.getIssuedAt();
        if (clockOffset <= 0) {
            clockOffset = 0;
        }
        if (issuedAtInSecs == null && claimsRequired 
            || issuedAtInSecs != null && (issuedAtInSecs - clockOffset > currentTimeInSecs || issuedAtRange > 0
            && issuedAtInSecs < currentTimeInSecs - issuedAtRange)) {
            throw new JwtException("Invalid issuedAt");
        }
    }
    
    public static void validateJwtTimeClaims(JwtClaims claims) {
        validateJwtTimeClaims(claims, 0, 0, false);
    }
    
}
