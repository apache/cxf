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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;


import static org.junit.Assert.fail;

/**
 * Some tests for JwtUtils
 */
public class JwtUtilsTest {

    @org.junit.Test
    public void testExpiredToken() throws Exception {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");

        // Set the expiry date to be yesterday
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1L);
        claims.setExpiryTime(dateTime.toEpochSecond());

        try {
            JwtUtils.validateJwtExpiry(claims, 0, true);
            fail("Failure expected on an expired token");
        } catch (JwtException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testFutureToken() throws Exception {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");

        // Set the issued date to be in the future
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1L);
        claims.setIssuedAt(dateTime.toEpochSecond());

        try {
            JwtUtils.validateJwtIssuedAt(claims, 300, 0, true);
            fail("Failure expected on a token issued in the future");
        } catch (JwtException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testNearFutureToken() throws Exception {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");

        // Set the issued date to be in the near future
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(30L);
        claims.setIssuedAt(dateTime.toEpochSecond());

        try {
            JwtUtils.validateJwtIssuedAt(claims, 0, 0, true);
            fail("Failure expected on a token issued in the future");
        } catch (JwtException ex) {
            // expected
        }

        // Now set the clock offset
        JwtUtils.validateJwtIssuedAt(claims, 0, 60, true);
    }

    @org.junit.Test
    public void testNotBefore() throws Exception {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");

        // Set the issued date to be in the near future
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(dateTime.toEpochSecond());
        claims.setNotBefore(dateTime.plusSeconds(30L).toEpochSecond());

        try {
            JwtUtils.validateJwtNotBefore(claims, 0, true);
            fail("Failure expected on not before");
        } catch (JwtException ex) {
            // expected
        }

        // Now set the clock offset
        JwtUtils.validateJwtNotBefore(claims, 60, true);
    }

    @org.junit.Test
    public void testIssuedAtTTL() throws Exception {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");

        // Set the issued date to be now
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(dateTime.toEpochSecond());

        // Now test the TTL
        JwtUtils.validateJwtIssuedAt(claims, 60, 0, true);

        // Now create the token 70 seconds ago
        claims.setIssuedAt(dateTime.minusSeconds(70L).toEpochSecond());

        try {
            JwtUtils.validateJwtIssuedAt(claims, 60, 0, true);
            fail("Failure expected on an expired token");
        } catch (JwtException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testExpectedAudience() throws Exception {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");

        // No aud claim should validate OK
        Message message = new MessageImpl();
        JwtUtils.validateJwtAudienceRestriction(claims, message);

        // It should fail when we have an unknown aud claim
        claims.setAudience("Receiver");
        try {
            JwtUtils.validateJwtAudienceRestriction(claims, message);
            fail("Failure expected on an invalid audience");
        } catch (JwtException ex) {
            // expected
        }

        // Here the aud claim matches what is expected
        message.put(JwtConstants.EXPECTED_CLAIM_AUDIENCE, "Receiver");
        JwtUtils.validateJwtAudienceRestriction(claims, message);

        // It should fail when the expected aud claim is not present
        claims.removeProperty(JwtConstants.CLAIM_AUDIENCE);
        try {
            JwtUtils.validateJwtAudienceRestriction(claims, message);
            fail("Failure expected on an invalid audience");
        } catch (JwtException ex) {
            // expected
        }
    }

}
