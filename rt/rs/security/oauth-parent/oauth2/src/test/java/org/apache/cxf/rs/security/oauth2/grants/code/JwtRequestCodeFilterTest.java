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
package org.apache.cxf.rs.security.oauth2.grants.code;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that security-sensitive outer request parameters (code_challenge,
 * code_challenge_method, nonce, state) cannot be silently overridden by
 * claims inside a JAR (JWT Authorization Request) — even when the JWT
 * carries a valid signature — per RFC 9101 §2.2.
 */
public class JwtRequestCodeFilterTest {

    private static final String CLIENT_ID = "test-client";
    /**
     * Signing key used for HMAC-SHA256; length ≥ 32 bytes satisfies HS256
     * security requirements for unit-test purposes.
     */
    private static final String SIGNING_KEY = "jwt-request-code-filter-test-key";

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static JwtRequestCodeFilter buildFilter() {
        JwtRequestCodeFilter filter = new JwtRequestCodeFilter();
        // Inject a verifier directly so the filter can validate the JWT
        // without loading key material from system properties or a keystore.
        filter.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));
        return filter;
    }

    private static Client buildClient() {
        return new Client(CLIENT_ID, "secret", true);
    }

    /** Returns baseline JWT claims that satisfy the filter's own issuer check. */
    private static JwtClaims baselineClaims() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(CLIENT_ID);
        return claims;
    }

    private static String buildSignedJwt(JwtClaims claims) {
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(claims);
        return producer.signWith(
                new HmacJwsSignatureProvider(SIGNING_KEY, SignatureAlgorithm.HS256));
    }

    // ------------------------------------------------------------------
    // Vulnerability tests (currently FAIL — demonstrate CWE-20 / RFC 9101)
    // ------------------------------------------------------------------

    /**
     * A validly-signed request JWT carrying {@code code_challenge} and
     * {@code code_challenge_method} must not replace the values supplied
     * in the outer HTTP request.  Allowing the override lets an attacker
     * who controls the JWT payload substitute the client's PKCE challenge
     * with one of their choosing (CVSS 3.1 ~4.1 MEDIUM).
     */
    @Test
    public void testCodeChallengeNotOverriddenByJwtClaim() {
        JwtRequestCodeFilter filter = buildFilter();

        JwtClaims claims = baselineClaims();
        claims.setClaim(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE, "EVIL-CHALLENGE");
        claims.setClaim(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE_METHOD, "S256");

        MultivaluedMap<String, String> params = new MetadataMap<>();
        params.putSingle(OAuthConstants.RESPONSE_TYPE, OAuthConstants.CODE_RESPONSE_TYPE);
        params.putSingle(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE, "legit-client-challenge");
        params.putSingle(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE_METHOD, "plain");
        params.putSingle("request", buildSignedJwt(claims));

        MultivaluedMap<String, String> result = filter.process(params, null, buildClient());

        assertEquals("JWT must not override the outer code_challenge",
                "legit-client-challenge",
                result.getFirst(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE));
        assertEquals("JWT must not override the outer code_challenge_method",
                "plain",
                result.getFirst(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE_METHOD));
    }

    /**
     * {@code nonce} is an anti-replay parameter established by the client
     * in the outer request.  A JWT must not be allowed to replace it, as
     * doing so undermines OpenID Connect replay protection.
     */
    @Test
    public void testNonceNotOverriddenByJwtClaim() {
        JwtRequestCodeFilter filter = buildFilter();

        JwtClaims claims = baselineClaims();
        claims.setClaim(OAuthConstants.NONCE, "EVIL-NONCE");

        MultivaluedMap<String, String> params = new MetadataMap<>();
        params.putSingle(OAuthConstants.RESPONSE_TYPE, OAuthConstants.CODE_RESPONSE_TYPE);
        params.putSingle(OAuthConstants.NONCE, "outer-nonce");
        params.putSingle("request", buildSignedJwt(claims));

        MultivaluedMap<String, String> result = filter.process(params, null, buildClient());

        assertEquals("JWT must not override the outer nonce",
                "outer-nonce",
                result.getFirst(OAuthConstants.NONCE));
    }

    /**
     * {@code state} is the client's CSRF token.  A JWT must not be allowed
     * to replace it, as doing so can redirect the authorization response
     * to an attacker-controlled state.
     */
    @Test
    public void testStateNotOverriddenByJwtClaim() {
        JwtRequestCodeFilter filter = buildFilter();

        JwtClaims claims = baselineClaims();
        claims.setClaim(OAuthConstants.STATE, "EVIL-STATE");

        MultivaluedMap<String, String> params = new MetadataMap<>();
        params.putSingle(OAuthConstants.RESPONSE_TYPE, OAuthConstants.CODE_RESPONSE_TYPE);
        params.putSingle(OAuthConstants.STATE, "outer-state");
        params.putSingle("request", buildSignedJwt(claims));

        MultivaluedMap<String, String> result = filter.process(params, null, buildClient());

        assertEquals("JWT must not override the outer state",
                "outer-state",
                result.getFirst(OAuthConstants.STATE));
    }

    /**
     * Non-sensitive claims present in the JWT (e.g. {@code scope}) that are
     * absent from the outer request should still be applied to the resulting
     * parameter map — the fix must not strip all JWT claims.
     */
    @Test
    public void testNonSensitiveJwtClaimsAreApplied() {
        JwtRequestCodeFilter filter = buildFilter();

        JwtClaims claims = baselineClaims();
        claims.setClaim(OAuthConstants.SCOPE, "read write");

        MultivaluedMap<String, String> params = new MetadataMap<>();
        params.putSingle(OAuthConstants.RESPONSE_TYPE, OAuthConstants.CODE_RESPONSE_TYPE);
        params.putSingle("request", buildSignedJwt(claims));

        MultivaluedMap<String, String> result = filter.process(params, null, buildClient());

        assertEquals("Scope from JWT should be applied when absent from outer request",
                "read write",
                result.getFirst(OAuthConstants.SCOPE));
    }

    /**
     * When no {@code request} parameter is present the filter must return
     * the original params unchanged.
     */
    @Test
    public void testNoRequestParamReturnsOriginalParams() {
        JwtRequestCodeFilter filter = buildFilter();

        MultivaluedMap<String, String> params = new MetadataMap<>();
        params.putSingle(OAuthConstants.RESPONSE_TYPE, OAuthConstants.CODE_RESPONSE_TYPE);
        params.putSingle(OAuthConstants.STATE, "original-state");

        MultivaluedMap<String, String> result = filter.process(params, null, buildClient());

        assertEquals("original-state", result.getFirst(OAuthConstants.STATE));
    }
}
