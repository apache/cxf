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

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class OidcClaimsValidatorTest {

    private static final String SELF_ISSUED_ISSUER = "https://self-issued.me";
    private static final String CLIENT_ID = "client-id";

    // EC P-256 public key for self-issued tests
    private static final String EC_256_KEY = "{"
        + "\"kty\": \"EC\","
        + "\"x\": \"CEuRLUISufhcjrj-32N0Bvl3KPMiHH9iSw4ohN9jxrA\","
        + "\"y\": \"EldWz_iXSK3l_S7n4w_t3baxos7o9yqX0IjzG959vHc\","
        + "\"crv\": \"P-256\""
        + "}";

    // --- validateJwtClaims: self-issued path ---

    @Test
    public void testSelfIssuedValidTokenAccepted() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        validator.validateJwtClaims(buildValidSelfIssuedClaims(), CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testSelfIssuedMissingSubjectRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        // sub intentionally absent
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testSelfIssuedMissingAudienceRejectedWhenRequired() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject("key-thumbprint");
        // aud intentionally absent
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testSelfIssuedWrongAudienceRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject("key-thumbprint");
        claims.setAudience("wrong-client-id");
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testSelfIssuedExpiredTokenRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject("key-thumbprint");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now - 600); // expired 10 minutes ago
        claims.setIssuedAt(now - 700);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testSelfIssuedMissingExpiryRejectedWhenRequired() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject("key-thumbprint");
        claims.setAudience(CLIENT_ID);
        // exp intentionally absent
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testSelfIssuedMissingIssuedAtRejectedWhenRequired() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject("key-thumbprint");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        // iat intentionally absent

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testSelfIssuedMissingSubJwkRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject("key-thumbprint");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);
        // sub_jwk intentionally absent

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testSelfIssuedSubJwkThumbprintMismatchRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject("wrong-thumbprint");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);
        claims.setClaim("sub_jwk", EC_256_KEY);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    // --- getInitializedSignatureVerifier: self-issued branch ---

    @Test
    public void testSignatureVerifierUsesSubJwkWhenIssIsSelfIssued() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        String thumbprint = JwkUtils.getThumbprint(EC_256_KEY);

        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject(thumbprint);
        claims.setClaim("sub_jwk", EC_256_KEY);

        JwsHeaders headers = new JwsHeaders();
        headers.setSignatureAlgorithm(SignatureAlgorithm.ES256);

        JwsSignatureVerifier verifier = validator.getInitializedSignatureVerifier(new JwtToken(headers, claims));
        assertNotNull(verifier);
    }

    @Test(expected = SecurityException.class)
    public void testSignatureVerifierRejectsSubJwkWithWrongThumbprint() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setSupportSelfIssuedProvider(true);

        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject("wrong-thumbprint");
        claims.setClaim("sub_jwk", EC_256_KEY);

        JwsHeaders headers = new JwsHeaders();
        headers.setSignatureAlgorithm(SignatureAlgorithm.ES256);

        validator.getInitializedSignatureVerifier(new JwtToken(headers, claims));
    }

    // --- validateJwtClaims: normal issuer path ---

    @Test
    public void testNormalIssuerValidTokenAccepted() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        validator.validateJwtClaims(buildValidNormalIssuerClaims(), CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerMissingIssuerRejectedWhenRequired() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        // issuer intentionally absent
        claims.setSubject("subject-value");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerWrongIssuerRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://wrong-issuer.example.com");
        claims.setSubject("subject-value");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerMissingSubjectRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        // sub intentionally absent
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerMissingAudienceRejectedWhenRequired() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        claims.setSubject("subject-value");
        // aud intentionally absent
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerWrongAudienceRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        claims.setSubject("subject-value");
        claims.setAudience("wrong-client-id");
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerExpiredTokenRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        claims.setSubject("subject-value");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now - 600); // expired 10 minutes ago
        claims.setIssuedAt(now - 700);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerMissingExpiryRejectedWhenRequired() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        claims.setSubject("subject-value");
        claims.setAudience(CLIENT_ID);
        // exp intentionally absent
        claims.setIssuedAt(now - 10);

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerMissingIssuedAtRejectedWhenRequired() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        claims.setSubject("subject-value");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        // iat intentionally absent

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test(expected = OAuthServiceException.class)
    public void testNormalIssuerWrongAzpRejected() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        claims.setSubject("subject-value");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);
        claims.setClaim("azp", "wrong-client-id"); // azp doesn't match clientId

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    @Test
    public void testNormalIssuerValidTokenWithAzpAccepted() {
        OidcClaimsValidator validator = new OidcClaimsValidator();
        validator.setIssuerId("https://issuer.example.com");

        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        claims.setSubject("subject-value");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);
        claims.setClaim("azp", CLIENT_ID); // azp matches clientId

        validator.validateJwtClaims(claims, CLIENT_ID, true);
    }

    // --- helper ---

    private static JwtClaims buildValidNormalIssuerClaims() {
        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://issuer.example.com");
        claims.setSubject("subject-value");
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);
        return claims;
    }

    private static JwtClaims buildValidSelfIssuedClaims() {
        long now = System.currentTimeMillis() / 1000;
        String thumbprint = JwkUtils.getThumbprint(EC_256_KEY);
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(SELF_ISSUED_ISSUER);
        claims.setSubject(thumbprint);
        claims.setAudience(CLIENT_ID);
        claims.setExpiryTime(now + 300);
        claims.setIssuedAt(now - 10);
        claims.setClaim("sub_jwk", EC_256_KEY);
        return claims;
    }
}
