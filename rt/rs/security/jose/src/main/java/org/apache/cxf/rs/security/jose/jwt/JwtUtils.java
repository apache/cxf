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
    
    public static void validateJwtExpiry(JwtClaims claims, int clockOffset, boolean claimRequired) {
        Long expiryTime = claims.getExpiryTime();
        if (expiryTime == null) {
            if (claimRequired) {
                throw new JwtException("The token has expired");
            }
            return;
        }
        Date rightNow = new Date();
        Date expiresDate = new Date(expiryTime * 1000L);
        if (clockOffset != 0) {
            expiresDate.setTime(expiresDate.getTime() + (long)clockOffset * 1000L);
        }
        if (expiresDate.before(rightNow)) {
            throw new JwtException("The token has expired");
        }
    }
    
    public static void validateJwtNotBefore(JwtClaims claims, int clockOffset, boolean claimRequired) {
        Long notBeforeTime = claims.getNotBefore();
        if (notBeforeTime == null) {
            if (claimRequired) {
                throw new JwtException("The token cannot be accepted yet");
            }
            return;
        }
        
        Date validCreation = new Date();
        long currentTime = validCreation.getTime();
        if (clockOffset != 0) {
            validCreation.setTime(currentTime + (long)clockOffset * 1000L);
        }
        Date notBeforeDate = new Date(notBeforeTime * 1000L);

        // Check to see if the not before time is in the future
        if (notBeforeDate.after(validCreation)) {
            throw new JwtException("The token cannot be accepted yet");
        }
    }
    
    public static void validateJwtIssuedAt(JwtClaims claims, int timeToLive, int clockOffset, boolean claimRequired) {
        Long issuedAtInSecs = claims.getIssuedAt();
        if (issuedAtInSecs == null) {
            if (claimRequired) {
                throw new JwtException("Invalid issuedAt");
            }
            return;
        }
        
        Date createdDate = new Date(issuedAtInSecs * 1000L);
        if (clockOffset != 0) {
            // Calculate the time that is allowed for the message to travel
            createdDate.setTime(createdDate.getTime() - (long)clockOffset * 1000L);
        }
        
        Date validCreation = new Date();
        if (timeToLive != 0) {
            long currentTime = validCreation.getTime();
            currentTime -= (long)timeToLive * 1000L;
            validCreation.setTime(currentTime);
        }
        
        if (createdDate.after(validCreation)) {
            throw new JwtException("Invalid issuedAt");
        }
    }
    
}
