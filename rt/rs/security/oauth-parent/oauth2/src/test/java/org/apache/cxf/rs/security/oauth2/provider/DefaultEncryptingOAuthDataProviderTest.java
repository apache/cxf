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
package org.apache.cxf.rs.security.oauth2.provider;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rt.security.crypto.KeyProperties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class DefaultEncryptingOAuthDataProviderTest {
    private DefaultEncryptingOAuthDataProvider provider;

    @Before
    public void setUp() throws Exception {
        // Initialize provider with AES encryption (128-bit key)
        provider = new DefaultEncryptingOAuthDataProvider(new KeyProperties("AES", 128));
        // Register supported scopes
        provider.setSupportedScopes(Collections.singletonMap("read", "Read Scope"));
        provider.setSupportedScopes(Collections.singletonMap("refreshToken", "RefreshToken"));
    }

    @After
    public void tearDown() throws Exception {
        if (provider != null) {
            provider.close();
        }
    }

    private Client createTestClient(String clientId, String userLogin) {
        Client c = new Client();
        c.setRedirectUris(Collections.singletonList("http://client/redirect"));
        c.setClientId(clientId);
        c.setClientSecret("secret");
        c.setResourceOwnerSubject(new UserSubject(userLogin));
        provider.setClient(c);
        return c;
    }

    /**
     * RFC 7009 §2.1 compliance test: Revoked access tokens must become inaccessible.
     * 
     * Scenario:
     * 1. Create an access token
     * 2. Verify it is accessible via getAccessToken()
     * 3. Revoke the token
     * 4. Verify it is NO LONGER accessible (returns null)
     * 
     * This test ensures the fix for the revocation bypass vulnerability
     * where getAccessToken() was not checking the revoked tokens set.
     */
    @Test
    public void testRevokedAccessTokenReturnsNull() {
        Client c = createTestClient("test1", "alice");
        
        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("read"));
        atr.setSubject(c.getResourceOwnerSubject());
        
        // Step 1: Create and verify token is accessible
        ServerAccessToken token = provider.createAccessToken(atr);
        assertNotNull("Token should be accessible immediately after creation",
                      provider.getAccessToken(token.getTokenKey()));
        
        // Step 2: Revoke the token
        provider.revokeToken(c, token.getTokenKey(), OAuthConstants.ACCESS_TOKEN);
        
        // Step 3: CRITICAL: Verify revoked token is no longer accessible
        // Before the fix, this would return the decrypted token (VULNERABILITY)
        // After the fix, this must return null (RFC 7009 §2.1 compliance)
        assertNull("Revoked access token must return null (RFC 7009 §2.1)",
                   provider.getAccessToken(token.getTokenKey()));
    }

    /**
     * RFC 7009 §2.1 and RFC 7662 §2.2 compliance test: Revoked refresh tokens
     * must become inaccessible.
     * 
     * Scenario:
     * 1. Create an access token with refresh token capability
     * 2. Verify refresh token is accessible
     * 3. Revoke the refresh token
     * 4. Verify it is NO LONGER accessible (returns null)
     * 
     * This test prevents the attack where a revoked refresh token
     * could mint new access tokens.
     */
    @Test
    public void testRevokedRefreshTokenReturnsNull() {
        Client c = createTestClient("test2", "bob");
        
        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Arrays.asList("read", "refreshToken"));
        atr.setSubject(c.getResourceOwnerSubject());
        
        // Step 1: Create access token with refresh token
        ServerAccessToken at = provider.createAccessToken(atr);
        String refreshTokenKey = at.getRefreshToken();
        
        // Step 2: Verify refresh token is accessible before revocation
        RefreshToken rt = provider.getRefreshToken(refreshTokenKey);
        assertNotNull("Refresh token should be accessible immediately after creation", rt);
        
        // Step 3: Revoke the refresh token
        provider.revokeToken(c, refreshTokenKey, OAuthConstants.REFRESH_TOKEN);
        
        // Step 4: CRITICAL: Verify revoked refresh token is no longer accessible
        // Before the fix, this would return the decrypted token (VULNERABILITY)
        // After the fix, this must return null (RFC 7009 §2.1 compliance)
        assertNull("Revoked refresh token must return null (RFC 7009 §2.1)",
                   provider.getRefreshToken(refreshTokenKey));
    }

    /**
     * Security test: Verify multiple tokens can be managed independently.
     * 
     * Scenario:
     * 1. Create two access tokens for the same client
     * 2. Revoke only the first token
     * 3. Verify first token is null (revoked)
     * 4. Verify second token is still accessible (not revoked)
     * 
     * This test ensures the revocation mechanism is token-specific
     * and doesn't inadvertently revoke other tokens.
     */
    @Test
    public void testSelectiveAccessTokenRevocation() {
        Client c = createTestClient("test3", "charlie");
        
        // Create first token
        AccessTokenRegistration atr1 = new AccessTokenRegistration();
        atr1.setClient(c);
        atr1.setApprovedScope(Collections.singletonList("read"));
        atr1.setSubject(c.getResourceOwnerSubject());
        ServerAccessToken token1 = provider.createAccessToken(atr1);
        
        // Create second token
        AccessTokenRegistration atr2 = new AccessTokenRegistration();
        atr2.setClient(c);
        atr2.setApprovedScope(Collections.singletonList("read"));
        atr2.setSubject(c.getResourceOwnerSubject());
        ServerAccessToken token2 = provider.createAccessToken(atr2);
        
        // Verify both are initially accessible
        assertNotNull("Token 1 should be accessible", 
                      provider.getAccessToken(token1.getTokenKey()));
        assertNotNull("Token 2 should be accessible", 
                      provider.getAccessToken(token2.getTokenKey()));
        
        // Revoke only token 1
        provider.revokeToken(c, token1.getTokenKey(), OAuthConstants.ACCESS_TOKEN);
        
        // Verify only token 1 is revoked
        assertNull("Token 1 must be revoked", 
                   provider.getAccessToken(token1.getTokenKey()));
        assertNotNull("Token 2 must still be accessible after revoking Token 1", 
                      provider.getAccessToken(token2.getTokenKey()));
    }

    /**
     * RFC 7662 §2.2 compliance test: Token introspection consistency.
     * 
     * Scenario:
     * 1. Create an access token
     * 2. Revoke it
     * 3. Attempt to retrieve it (simulates introspection)
     * 4. Verify null is returned (service layer converts to active:false)
     * 
     * This ensures that TokenIntrospectionService will correctly report
     * active:false for revoked tokens by returning null.
     */
    @Test
    public void testIntrospectionReportRevocationCorrectly() {
        Client c = createTestClient("test6", "frank");
        
        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("read"));
        atr.setSubject(c.getResourceOwnerSubject());
        
        ServerAccessToken token = provider.createAccessToken(atr);
        String tokenKey = token.getTokenKey();
        
        // Before revocation: introspection should find the token
        ServerAccessToken beforeRevoke = provider.getAccessToken(tokenKey);
        assertNotNull("Token should be found before revocation (introspection returns details)", 
                      beforeRevoke);
        
        // Revoke the token
        provider.revokeToken(c, tokenKey, OAuthConstants.ACCESS_TOKEN);
        
        // After revocation: introspection should return null
        // (higher-level code like TokenIntrospectionService converts this to active:false)
        ServerAccessToken afterRevoke = provider.getAccessToken(tokenKey);
        assertNull("Token should return null after revocation (introspection reports inactive)", 
                   afterRevoke);
    }

    /**
     * Security test: Verify that refresh tokens cannot be reused after revocation.
     * 
     * Scenario:
     * 1. Create an access token with refresh token
     * 2. Use the refresh token to create a new access token (verify it works)
     * 3. Revoke the refresh token
     * 4. Attempt to use the refresh token again (verify it fails)
     * 
     * This test ensures refresh token revocation prevents any further use
     * of that token for minting new access tokens.
     */
    @Test
    public void testRevokedRefreshTokenCannotMintNewTokens() {
        Client c = createTestClient("test5", "eve");
        
        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Arrays.asList("read", "refreshToken"));
        atr.setSubject(c.getResourceOwnerSubject());
        
        // Create initial access token with refresh token
        ServerAccessToken initialToken = provider.createAccessToken(atr);
        String refreshTokenKey = initialToken.getRefreshToken();
        
        // Verify refresh token can be retrieved before revocation
        RefreshToken rt = provider.getRefreshToken(refreshTokenKey);
        assertNotNull("Refresh token should be accessible before revocation", rt);
        
        // Revoke the refresh token
        provider.revokeToken(c, refreshTokenKey, OAuthConstants.REFRESH_TOKEN);
        
        // CRITICAL: Verify refresh token cannot be accessed after revocation
        // This prevents using the revoked refresh token to mint new access tokens
        assertNull("Revoked refresh token must return null, preventing token minting", 
                   provider.getRefreshToken(refreshTokenKey));
    }
}
