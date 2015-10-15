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

import java.util.Date;

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
    
    public static void validateJwtExpiry(JwtClaims claims, boolean claimRequired) {
        Long expiryTime = claims.getExpiryTime();
        if (expiryTime == null) {
            if (claimRequired) {
                throw new JwtException("The token has expired");
            }
            return;
        }
        
        Date rightNow = new Date();
        Date expiresDate = new Date(expiryTime * 1000L);
        if (expiresDate.before(rightNow)) {
            throw new JwtException("The token has expired");
        }
    }
    
    public static void validateJwtNotBefore(JwtClaims claims, int futureTimeToLive, boolean claimRequired) {
        Long notBeforeTime = claims.getNotBefore();
        
        // If no NotBefore then just use the IssueAt if it exists
        if (notBeforeTime == null && claims.getIssuedAt() != null) {
            notBeforeTime = claims.getIssuedAt();
        }
        
        if (notBeforeTime == null && claimRequired) {
            throw new JwtException("The token cannot be accepted yet");
        }
        
        Date validCreation = new Date();
        long currentTime = validCreation.getTime();
        if (futureTimeToLive > 0) {
            validCreation.setTime(currentTime + (long)futureTimeToLive * 1000L);
        }
        Date createdDate = new Date(notBeforeTime * 1000L);

        // Check to see if the not before time is in the future
        if (createdDate.after(validCreation)) {
            throw new JwtException("The token cannot be accepted yet");
        }
    }
    
    public static void validateJwtTTL(JwtClaims claims, int timeToLive, boolean claimRequired) {
        Long issuedAtInSecs = claims.getIssuedAt();
        if (issuedAtInSecs == null) {
            if (claimRequired) {
                throw new JwtException("Invalid issuedAt");
            }
            return;
        }
        
        Date validCreation = new Date();
        Date createdDate = new Date(issuedAtInSecs * 1000L);
        
        int ttl = timeToLive;
        if (ttl <= 0) {
            ttl = 300;
        }
        
        // Calculate the time that is allowed for the message to travel
        long currentTime = validCreation.getTime();
        currentTime -= (long)ttl * 1000L;
        validCreation.setTime(currentTime);

        // Validate the time it took the message to travel
        if (createdDate.before(validCreation)) {
            throw new JwtException("Invalid issuedAt");
        }
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
