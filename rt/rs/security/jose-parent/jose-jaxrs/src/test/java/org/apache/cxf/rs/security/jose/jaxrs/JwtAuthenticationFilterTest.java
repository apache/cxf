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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.lang.reflect.Field;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtException;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class JwtAuthenticationFilterTest {

    private static final String AUDIENCE = "https://service.example.com";

    @Before
    public void setUp() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, AUDIENCE);
        setThreadLocalMessage(message);
    }

    @After
    public void tearDown() throws Exception {
        setThreadLocalMessage(null);
    }

    @Test
    public void testNoIssuerAcceptedWithoutSupportedIssuers() {
        JwtAuthenticationFilter filter = createFilter();
        filter.validateToken(new JwtToken(createClaims(null)));
    }

    @Test
    public void testSupportedIssuerAccepted() {
        JwtAuthenticationFilter filter = createFilter();
        filter.setSupportedIssuers(Collections.singleton("https://issuer.example.com"));
        filter.validateToken(new JwtToken(createClaims("https://issuer.example.com")));
    }

    @Test
    public void testUnsupportedIssuerRejected() {
        JwtAuthenticationFilter filter = createFilter();
        filter.setSupportedIssuers(Collections.singleton("https://issuer.example.com"));
        try {
            filter.validateToken(new JwtToken(createClaims("https://other.example.com")));
            fail("Failure expected on an unsupported issuer");
        } catch (JwtException ex) {
            // expected
        }
    }

    @Test
    public void testMissingIssuerRejectedWithSupportedIssuers() {
        JwtAuthenticationFilter filter = createFilter();
        filter.setSupportedIssuers(Collections.singleton("https://issuer.example.com"));
        try {
            filter.validateToken(new JwtToken(createClaims(null)));
            fail("Failure expected on a missing issuer");
        } catch (JwtException ex) {
            // expected
        }
    }

    @Test
    public void testNoAudienceAcceptedByDefault() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.validateToken(new JwtToken(createClaims(null)));
    }

    @Test
    public void testAudienceAcceptedWithRequireAudience() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.setRequireAudience(true);
        JwtClaims claims = createClaims(null);
        claims.setAudience(AUDIENCE);
        filter.validateToken(new JwtToken(claims));
    }

    @Test
    public void testNoAudienceRejectedWithRequireAudience() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.setRequireAudience(true);
        try {
            filter.validateToken(new JwtToken(createClaims(null)));
            fail("Failure expected on a missing audience");
        } catch (JwtException ex) {
            // expected
        }
    }

    private static JwtAuthenticationFilter createFilter() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.setValidateAudience(false);
        return filter;
    }

    private static JwtClaims createClaims(String issuer) {
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        if (issuer != null) {
            claims.setIssuer(issuer);
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.toEpochSecond());
        claims.setExpiryTime(now.plusMinutes(5L).toEpochSecond());
        return claims;
    }

    private static void setThreadLocalMessage(Message message) throws Exception {
        Field f = PhaseInterceptorChain.class.getDeclaredField("CURRENT_MESSAGE");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Message> tl = (ThreadLocal<Message>) f.get(null);
        if (message == null) {
            tl.remove();
        } else {
            tl.set(message);
        }
    }
}
