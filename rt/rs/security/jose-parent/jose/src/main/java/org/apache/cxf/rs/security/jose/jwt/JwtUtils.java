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

import java.time.Instant;

import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

public final class JwtUtils {
    private JwtUtils() {

    }
    public static String claimsToJson(JwtClaims claims) {
        return claimsToJson(claims, null);
    }
    public static String claimsToJson(JwtClaims claims, JsonMapObjectReaderWriter writer) {
        if (writer == null) {
            writer = new JsonMapObjectReaderWriter();
        }
        return writer.toJson(claims);
    }
    public static JwtClaims jsonToClaims(String json) {
        return new JwtClaims(new JsonMapObjectReaderWriter().fromJson(json));
    }

    public static void validateJwtExpiry(JwtClaims claims, int clockOffset, boolean claimRequired) {
        Long expiryTime = claims.getExpiryTime();
        if (expiryTime == null) {
            if (claimRequired) {
                throw new JwtException("The token has expired");
            }
            return;
        }
        Instant now = Instant.now();
        Instant expires = Instant.ofEpochMilli(expiryTime * 1000L);
        if (clockOffset != 0) {
            expires = expires.plusSeconds(clockOffset);
        }
        if (expires.isBefore(now)) {
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

        Instant validCreation = Instant.now();
        if (clockOffset != 0) {
            validCreation = validCreation.plusSeconds(clockOffset);
        }
        Instant notBeforeDate = Instant.ofEpochMilli(notBeforeTime * 1000L);

        // Check to see if the not before time is in the future
        if (notBeforeDate.isAfter(validCreation)) {
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

        Instant createdDate = Instant.ofEpochMilli(issuedAtInSecs * 1000L);

        Instant validCreation = Instant.now();
        if (clockOffset != 0) {
            validCreation = validCreation.plusSeconds(clockOffset);
        }

        // Check to see if the IssuedAt time is in the future
        if (createdDate.isAfter(validCreation)) {
            throw new JwtException("Invalid issuedAt");
        }

        if (timeToLive > 0) {
            // Calculate the time that is allowed for the message to travel
            validCreation = validCreation.minusSeconds(timeToLive);

            // Validate the time it took the message to travel
            if (createdDate.isBefore(validCreation)) {
                throw new JwtException("Invalid issuedAt");
            }
        }
    }

    public static void validateJwtAudienceRestriction(JwtClaims claims, Message message) {
        // If the expected audience is configured, a matching "aud" must be present
        String expectedAudience = (String)message.getContextualProperty(JwtConstants.EXPECTED_CLAIM_AUDIENCE);
        if (expectedAudience != null) {
            if (claims.getAudiences().contains(expectedAudience)) {
                return;
            }
            throw new JwtException("Invalid audience restriction");
        }

        // Otherwise if we have no aud claims then the token is valid
        if (claims.getAudiences().isEmpty()) {
            return;
        }

        // Otherwise one of the aud claims must match the request URL
        expectedAudience = (String)message.getContextualProperty(Message.REQUEST_URL);
        if (expectedAudience != null && claims.getAudiences().contains(expectedAudience)) {
            return;
        }

        throw new JwtException("Invalid audience restriction");
    }

    public static void validateTokenClaims(JwtClaims claims, int timeToLive, int clockOffset,
                                           boolean validateAudienceRestriction) {
        // If we have no issued time then we need to have an expiry
        boolean expiredRequired = claims.getIssuedAt() == null;
        validateJwtExpiry(claims, clockOffset, expiredRequired);

        validateJwtNotBefore(claims, clockOffset, false);

        // If we have no expiry then we must have an issued at
        boolean issuedAtRequired = claims.getExpiryTime() == null;
        validateJwtIssuedAt(claims, timeToLive, clockOffset, issuedAtRequired);

        if (validateAudienceRestriction) {
            validateJwtAudienceRestriction(claims, PhaseInterceptorChain.getCurrentMessage());
        }
    }

}
