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
package org.apache.cxf.rs.security.oauth2.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EncryptionUtilsTest extends Assert {
    
    private CustomProvider p;
    
    @Before
    public void setUp() throws Exception {
        p = new CustomProvider();
    }
    
    @After
    public void tearDown() {
        p = null;
    }
    
    @Test
    public void testEncryptDecryptToken() throws Exception {
        AccessTokenRegistration atr = prepareTokenRegistration();
        
        // encrypt
        ServerAccessToken token = p.createAccessToken(atr);
        // decrypt
        ServerAccessToken token2 = p.getAccessToken(token.getTokenKey());
        
        // compare tokens
        compareAccessTokens(token, token2);
    }
    
    private void compareAccessTokens(ServerAccessToken token, ServerAccessToken token2) {
        assertEquals(token.getTokenKey(), token2.getTokenKey());
        assertEquals(token.getTokenType(), token2.getTokenType());
        assertEquals(token.getIssuedAt(), token2.getIssuedAt());
        assertEquals(token.getExpiresIn(), token2.getExpiresIn());
        Client regClient1 = token.getClient();
        Client regClient2 = token2.getClient();
        assertEquals(regClient1.getClientId(), regClient2.getClientId());
        
        UserSubject endUser1 = token.getSubject();
        UserSubject endUser2 = token2.getSubject();
        assertEquals(endUser1.getLogin(), endUser2.getLogin());
        assertEquals(endUser1.getId(), endUser2.getId());
        assertEquals(endUser1.getRoles(), endUser2.getRoles());
        
        assertEquals(token.getRefreshToken(), token2.getRefreshToken());
        assertEquals(token.getAudience(), token2.getAudience());
        assertEquals(token.getGrantType(), token2.getGrantType());
        assertEquals(token.getParameters(), token2.getParameters());
        
        List<OAuthPermission> permissions = token.getScopes();
        List<OAuthPermission> permissions2 = token2.getScopes();
        assertEquals(1, permissions.size());
        assertEquals(1, permissions2.size());
        OAuthPermission perm1 = permissions.get(0);
        OAuthPermission perm2 = permissions2.get(0);
        assertEquals(perm1.getPermission(), perm2.getPermission());
        assertEquals(perm1.getDescription(), perm2.getDescription());
        
        RefreshToken refreshToken = 
            EncryptionUtils.decryptRefreshToken(p, token2.getRefreshToken(), p.tokenKey);
        assertEquals(1200L, refreshToken.getExpiresIn());
    }
    
    private AccessTokenRegistration prepareTokenRegistration() {
        AccessTokenRegistration atr = new AccessTokenRegistration();
        Client regClient = p.getClient("1");
        atr.setClient(regClient);
        atr.setGrantType("code");
        atr.setAudience("http://localhost");
        UserSubject endUser = new UserSubject("Barry", "BarryId");
        atr.setSubject(endUser);
        endUser.setRoles(Collections.singletonList("role1"));
        return atr;
    }
    
    private class CustomProvider implements OAuthDataProvider {

        private Map<String, Client> clients;
        private SecretKey tokenKey;
        
        private Set<String> tokens = new HashSet<String>();
        private Map<String, String> refreshTokens = new HashMap<String, String>();
        
        public CustomProvider() throws Exception {
            tokenKey = EncryptionUtils.getSecretKey();
            clients = Collections.singletonMap("1", new Client("1", "2", true));
        }
        
        @Override
        public Client getClient(String clientId) throws OAuthServiceException {
            return clients.get(clientId);
        }

        @Override
        public ServerAccessToken createAccessToken(AccessTokenRegistration accessTokenReg)
            throws OAuthServiceException {
            
            ServerAccessToken token = createNewToken(accessTokenReg);
            
            String encryptedToken = 
                EncryptionUtils.encryptTokenWithSecretKey(token, tokenKey);
            
            tokens.add(encryptedToken);
            refreshTokens.put(token.getRefreshToken(), encryptedToken);
            token.setTokenKey(encryptedToken);
            return token;
        }
        
        @Override
        public ServerAccessToken getAccessToken(String accessTokenKey) throws OAuthServiceException {
            return EncryptionUtils.decryptToken(this, accessTokenKey, tokenKey);
        }

        @Override
        public ServerAccessToken refreshAccessToken(Client client, String refreshToken,
                                                    List<String> requestedScopes)
            throws OAuthServiceException {
            return null;
        }

        @Override
        public void removeAccessToken(ServerAccessToken accessToken) throws OAuthServiceException {
            tokens.remove(accessToken.getTokenKey());
        }

        @Override
        public void revokeToken(Client client, String token, String tokenTypeHint)
            throws OAuthServiceException {
            // complete
        }

        @Override
        public ServerAccessToken getPreauthorizedToken(Client client, List<String> requestedScopes,
                                                       UserSubject subject, String grantType)
            throws OAuthServiceException {
            return null;
        }
        
        @Override
        public List<OAuthPermission> convertScopeToPermissions(Client client, List<String> requestedScope) {
            return null;
        }
        
        private ServerAccessToken createNewToken(AccessTokenRegistration accessTokenReg) {
            ServerAccessToken token = new BearerAccessToken(accessTokenReg.getClient(), 3600L);
            token.setSubject(accessTokenReg.getSubject());
            
            RefreshToken refreshToken = new RefreshToken(accessTokenReg.getClient(),
                                                         "refresh",
                                                         1200L,
                                                         OAuthUtils.getIssuedAt());
            
            String encryptedRefreshToken = EncryptionUtils.encryptTokenWithSecretKey(refreshToken, tokenKey);
            token.setRefreshToken(encryptedRefreshToken);
            
            token.setGrantType(accessTokenReg.getGrantType());
            token.setAudience(accessTokenReg.getAudience());
            token.setParameters(Collections.singletonMap("param", "value"));
            token.setScopes(Collections.singletonList(
                new OAuthPermission("read", "read permission")));
            return token;
        }
        
    }
}
