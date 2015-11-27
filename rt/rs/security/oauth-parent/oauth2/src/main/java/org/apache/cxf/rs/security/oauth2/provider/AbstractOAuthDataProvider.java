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

import java.util.Collections;
import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public abstract class AbstractOAuthDataProvider implements OAuthDataProvider {
    private long accessTokenLifetime = 3600L;
    private long refreshTokenLifetime; // refresh tokens are eternal by default
    private boolean recycleRefreshTokens = true;
    
    protected AbstractOAuthDataProvider() {
    }
    
    @Override
    public ServerAccessToken createAccessToken(AccessTokenRegistration reg)
        throws OAuthServiceException {
        ServerAccessToken at = doCreateAccessToken(reg);
        saveAccessToken(at);
        if (isRefreshTokenSupported(reg.getApprovedScope())) {
            createNewRefreshToken(at);
        }
        return at;
    }
    
    protected ServerAccessToken doCreateAccessToken(AccessTokenRegistration accessToken) {
        ServerAccessToken at = createNewAccessToken(accessToken.getClient());
        at.setAudience(accessToken.getAudience());
        at.setGrantType(accessToken.getGrantType());
        List<String> theScopes = accessToken.getApprovedScope();
        List<OAuthPermission> thePermissions = 
            convertScopeToPermissions(accessToken.getClient(), theScopes);
        at.setScopes(thePermissions);
        at.setSubject(accessToken.getSubject());
        at.setClientCodeVerifier(accessToken.getClientCodeVerifier());
        return at;
    }
    
    @Override
    public void removeAccessToken(ServerAccessToken token) throws OAuthServiceException {
        revokeAccessToken(token.getTokenKey());
    }
    
    @Override
    public ServerAccessToken refreshAccessToken(Client client, String refreshTokenKey,
                                                List<String> restrictedScopes) throws OAuthServiceException {
        RefreshToken currentRefreshToken = revokeRefreshAndAccessTokens(client, refreshTokenKey);
        ServerAccessToken at = doRefreshAccessToken(client, currentRefreshToken, restrictedScopes);
        saveAccessToken(at);
        if (recycleRefreshTokens) {
            createNewRefreshToken(at);
        } else {
            updateRefreshToken(currentRefreshToken, at);
        }
        return at;
    }
    
    @Override
    public void revokeToken(Client client, String tokenKey, String tokenTypeHint) throws OAuthServiceException {
        ServerAccessToken accessToken = revokeAccessToken(tokenKey);
        if (accessToken == null) {
            doRevokeRefreshAndAccessTokens(client, tokenKey, true);
        } else {
            if (accessToken.getRefreshToken() != null) {
                RefreshToken rt = getRefreshToken(client, accessToken.getRefreshToken());
                if (rt == null) {
                    return;
                }
                
                List<String> accessTokenKeys = rt.getAccessTokens();
                for (int i = 0; i < accessTokenKeys.size(); i++) {
                    if (accessTokenKeys.get(i).equals(accessToken.getTokenKey())) {
                        accessTokenKeys.remove(i);
                        break;
                    }
                }
                if (rt.getAccessTokens().isEmpty()) {
                    revokeRefreshToken(client, rt.getTokenKey());
                } else {
                    saveRefreshToken(null, rt);
                }
            }
        }
    }
    protected RefreshToken revokeRefreshAndAccessTokens(Client client, String tokenKey) {
        return doRevokeRefreshAndAccessTokens(client, tokenKey, recycleRefreshTokens);
    }
    protected RefreshToken doRevokeRefreshAndAccessTokens(Client client, String tokenKey, boolean recycle) {
        RefreshToken currentRefreshToken = recycle ? revokeRefreshToken(client, tokenKey)
            : getRefreshToken(client, tokenKey);
        if (currentRefreshToken == null 
            || OAuthUtils.isExpired(currentRefreshToken.getIssuedAt(), currentRefreshToken.getExpiresIn())) {
            throw new OAuthServiceException(OAuthConstants.ACCESS_DENIED);
        }
        for (String accessTokenKey : currentRefreshToken.getAccessTokens()) {
            revokeAccessToken(accessTokenKey);
        }
        if (recycle) {
            currentRefreshToken.getAccessTokens().clear();
        }
        return currentRefreshToken;
    }

    

    @Override
    public List<OAuthPermission> convertScopeToPermissions(Client client, List<String> requestedScope) {
        if (requestedScope.isEmpty()) {
            return Collections.emptyList();
        } else {
            throw new OAuthServiceException("Requested scopes can not be mapped");
        }
    }

    @Override
    public ServerAccessToken getPreauthorizedToken(Client client, List<String> requestedScopes,
                                                   UserSubject subject, String grantType)
        throws OAuthServiceException {
        return null;
    }
    
    protected boolean isRefreshTokenSupported(List<String> theScopes) {
        return theScopes.contains(OAuthConstants.REFRESH_TOKEN_SCOPE);
    }

    protected ServerAccessToken createNewAccessToken(Client client) {
        return new BearerAccessToken(client, accessTokenLifetime);
    }
     
    protected RefreshToken updateRefreshToken(RefreshToken rt, ServerAccessToken at) {
        linkRefreshAccessTokens(rt, at);
        saveRefreshToken(at, rt);
        return rt;
    }
    protected RefreshToken createNewRefreshToken(ServerAccessToken at) {
        RefreshToken rt = doCreateNewRefreshToken(at);
        saveRefreshToken(at, rt);
        return rt;
    }
    protected RefreshToken doCreateNewRefreshToken(ServerAccessToken at) {
        RefreshToken rt = new RefreshToken(at.getClient(), refreshTokenLifetime);
        rt.setAudience(at.getAudience());
        rt.setGrantType(at.getGrantType());
        rt.setScopes(at.getScopes());
        rt.setSubject(at.getSubject());
        rt.setClientCodeVerifier(at.getClientCodeVerifier());
        linkRefreshAccessTokens(rt, at);
        return rt;
    }
    
    private void linkRefreshAccessTokens(RefreshToken rt, ServerAccessToken at) {
        rt.getAccessTokens().add(at.getTokenKey());
        at.setRefreshToken(rt.getTokenKey());
    }

    protected ServerAccessToken doRefreshAccessToken(Client client, 
                                                     RefreshToken oldRefreshToken, 
                                                     List<String> restrictedScopes) {
        ServerAccessToken at = createNewAccessToken(client);
        at.setAudience(oldRefreshToken.getAudience());
        at.setGrantType(oldRefreshToken.getGrantType());
        at.setSubject(oldRefreshToken.getSubject());
        if (restrictedScopes.isEmpty()) {
            at.setScopes(oldRefreshToken.getScopes());
        } else {
            List<OAuthPermission> theNewScopes = convertScopeToPermissions(client, restrictedScopes);
            if (oldRefreshToken.getScopes().containsAll(theNewScopes)) {
                at.setScopes(theNewScopes);
            } else {
                throw new OAuthServiceException("Invalid scopes");
            }
        }
        return at;
    }
    
    public void setAccessTokenLifetime(long accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public void setRefreshTokenLifetime(long refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }
    
    public void setRecycleRefreshTokens(boolean recycleRefreshTokens) {
        this.recycleRefreshTokens = recycleRefreshTokens;
    }
    
    protected abstract void saveAccessToken(ServerAccessToken serverToken);
    protected abstract void saveRefreshToken(ServerAccessToken at, RefreshToken refreshToken);
    protected abstract ServerAccessToken revokeAccessToken(String accessTokenKey);
    protected abstract RefreshToken revokeRefreshToken(Client client, String refreshTokenKey);
    protected abstract RefreshToken getRefreshToken(Client client, String refreshTokenKey);
}
