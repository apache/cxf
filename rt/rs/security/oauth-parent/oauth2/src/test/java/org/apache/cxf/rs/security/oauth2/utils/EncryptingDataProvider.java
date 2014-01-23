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

public class EncryptingDataProvider implements OAuthDataProvider {

    SecretKey tokenKey;
    private Map<String, Client> clients;
    
    private Set<String> tokens = new HashSet<String>();
    private Map<String, String> refreshTokens = new HashMap<String, String>();
    
    public EncryptingDataProvider() throws Exception {
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
        
        ServerAccessToken token = createAccessTokenInternal(accessTokenReg);
        
        String encryptedToken = 
            ModelEncryptionSupport.encryptAccessToken(token, tokenKey);
        
        tokens.add(encryptedToken);
        refreshTokens.put(token.getRefreshToken(), encryptedToken);
        token.setTokenKey(encryptedToken);
        return token;
    }
    
    @Override
    public ServerAccessToken getAccessToken(String accessTokenKey) throws OAuthServiceException {
        return ModelEncryptionSupport.decryptAccessToken(this, accessTokenKey, tokenKey);
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
    
    BearerAccessToken createAccessTokenInternal(AccessTokenRegistration accessTokenReg) {
        BearerAccessToken token = new BearerAccessToken(accessTokenReg.getClient(), 3600L);
        token.setSubject(accessTokenReg.getSubject());
        
        RefreshToken refreshToken = new RefreshToken(accessTokenReg.getClient(),
                                                     "refresh",
                                                     1200L,
                                                     OAuthUtils.getIssuedAt());
        
        String encryptedRefreshToken = 
            ModelEncryptionSupport.encryptRefreshToken(refreshToken, tokenKey);
        token.setRefreshToken(encryptedRefreshToken);
        
        token.setGrantType(accessTokenReg.getGrantType());
        token.setAudience(accessTokenReg.getAudience());
        token.setParameters(Collections.singletonMap("param", "value"));
        token.setScopes(Collections.singletonList(
            new OAuthPermission("read", "read permission")));
        return token;
    }
    
}
