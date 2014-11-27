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
        return claimsToJson(claims);
    }
    public static String claimsToJson(JwtClaims claims, JwtTokenWriter writer) {
        if (writer == null) {
            writer = new JwtTokenReaderWriter();
        }
        return writer.claimsToJson(claims);
    }
    public static JwtClaims jsonToClaims(String json) {
        return jsonToClaims(json, null);
    }
    public static JwtClaims jsonToClaims(String json, JwtTokenReader reader) {
        if (reader == null) {
            reader = new JwtTokenReaderWriter();
        }
        return reader.fromJsonClaims(json);
    }
    public static void validateJwtTimeClaims(JwtClaims claims) {
        Long currentTimeInSecs = System.currentTimeMillis() / 1000;
        Long expiryTimeInSecs = claims.getExpiryTime();
        if (expiryTimeInSecs != null && currentTimeInSecs > expiryTimeInSecs) {
            throw new SecurityException("The token expired");
        }
        Long issuedAtInSecs = claims.getIssuedAt();
        if (issuedAtInSecs != null && issuedAtInSecs > currentTimeInSecs) {
            throw new SecurityException("Invalid issuedAt");
        }
    }
}
