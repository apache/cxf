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

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class IdTokenReaderTest {

    @Test
    public void testCodeHashIsOptionalByDefaultForTokenEndpointIdToken() {
        IdTokenReader idTokenReader = new StubIdTokenReader(new JwtToken(new JwtClaims()));
        idTokenReader.setRequireAccessTokenHash(false);

        ClientAccessToken accessToken = new ClientAccessToken("Bearer", "access-token");
        accessToken.getParameters().put(OidcUtils.ID_TOKEN, "id-token");

        assertNotNull(idTokenReader.getIdJwtToken(accessToken, "auth-code", new Consumer("client-id")));
    }

    @Test(expected = OAuthServiceException.class)
    public void testCodeHashIsRequiredByDefaultForHybridTokenEndpointIdToken() {
        IdTokenReader idTokenReader = new StubIdTokenReader(new JwtToken(new JwtClaims()));
        idTokenReader.setRequireAccessTokenHash(false);

        ClientAccessToken accessToken = new ClientAccessToken("Bearer", "access-token");
        accessToken.getParameters().put(OidcUtils.ID_TOKEN, "id-token");
        accessToken.getParameters().put(OAuthConstants.RESPONSE_TYPE, OidcUtils.CODE_ID_TOKEN_RESPONSE_TYPE);

        idTokenReader.getIdJwtToken(accessToken, "auth-code", new Consumer("client-id"));
    }

    // The String overload has no access_token/code to check at_hash/c_hash against: if the
    // id_token asserts such a claim while hash validation is required, it must be rejected.
    @Test(expected = OAuthServiceException.class)
    public void testStringOverloadRejectsUnverifiableAtHashByDefault() {
        JwtClaims claims = validClaims();
        claims.setClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM, "some-hash");
        IdTokenReader idTokenReader = new StubJwtParsingIdTokenReader(new JwtToken(claims));
        idTokenReader.setIssuerId("https://idp.example.com");

        idTokenReader.getIdJwtToken("id-token", new Consumer("client-id"));
    }

    @Test(expected = OAuthServiceException.class)
    public void testStringOverloadRejectsUnverifiableCodeHashWhenRequired() {
        JwtClaims claims = validClaims();
        claims.setClaim(IdToken.AUTH_CODE_HASH_CLAIM, "some-hash");
        IdTokenReader idTokenReader = new StubJwtParsingIdTokenReader(new JwtToken(claims));
        idTokenReader.setIssuerId("https://idp.example.com");
        idTokenReader.setRequireAccessTokenHash(false);
        idTokenReader.setRequireCodeHash(true);

        idTokenReader.getIdJwtToken("id-token", new Consumer("client-id"));
    }

    @Test
    public void testStringOverloadAcceptsTokenWithoutHashClaimsByDefault() {
        JwtClaims claims = validClaims();
        IdTokenReader idTokenReader = new StubJwtParsingIdTokenReader(new JwtToken(claims));
        idTokenReader.setIssuerId("https://idp.example.com");

        assertNotNull(idTokenReader.getIdJwtToken("id-token", new Consumer("client-id")));
    }

    @Test
    public void testStringOverloadAcceptsAtHashWhenNotRequired() {
        JwtClaims claims = validClaims();
        claims.setClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM, "some-hash");
        IdTokenReader idTokenReader = new StubJwtParsingIdTokenReader(new JwtToken(claims));
        idTokenReader.setIssuerId("https://idp.example.com");
        idTokenReader.setRequireAccessTokenHash(false);

        assertNotNull(idTokenReader.getIdJwtToken("id-token", new Consumer("client-id")));
    }

    private static JwtClaims validClaims() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://idp.example.com");
        claims.setSubject("subject");
        claims.setAudience("client-id");
        long now = System.currentTimeMillis() / 1000L;
        claims.setIssuedAt(now);
        claims.setExpiryTime(now + 300L);
        return claims;
    }

    private static final class StubIdTokenReader extends IdTokenReader {
        private final JwtToken jwt;

        private StubIdTokenReader(JwtToken jwt) {
            this.jwt = jwt;
        }

        @Override
        public JwtToken getJwtToken(String wrappedJwtToken, String clientSecret) {
            return jwt;
        }

        @Override
        public void validateJwtClaims(JwtClaims claims, String clientId, boolean validateClaimsAlways) {
            // Claims validation is exercised separately in OidcClaimsValidatorTest.
        }
    }

    // Bypasses actual JWS parsing/signature verification so the real getIdJwtToken(String, Consumer)
    // logic (claims validation + hash-claim guard) under test still executes.
    private static final class StubJwtParsingIdTokenReader extends IdTokenReader {
        private final JwtToken jwt;

        private StubJwtParsingIdTokenReader(JwtToken jwt) {
            this.jwt = jwt;
        }

        @Override
        public JwtToken getJwtToken(String wrappedJwtToken, String clientSecret) {
            return jwt;
        }
    }
}
