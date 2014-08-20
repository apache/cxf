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

public abstract class AbstractOAuthDataProvider implements OAuthDataProvider {
    private long accessTokenLifetime = 3600L;
    private long refreshTokenLifetime = 360000L;
    
    protected AbstractOAuthDataProvider() {
    }
    
    @Override
    public ServerAccessToken createAccessToken(AccessTokenRegistration accessToken)
        throws OAuthServiceException {
        ServerAccessToken serverToken = doCreateAccessToken(accessToken);
        saveAccessToken(serverToken);
        return serverToken;
    }
    
    @Override
    public ServerAccessToken refreshAccessToken(Client client, String refreshTokenKey,
                                                List<String> requestedScopes) throws OAuthServiceException {
        RefreshToken oldRefreshToken = revokeRefreshToken(client, refreshTokenKey);

        ServerAccessToken serverToken = doRefreshAccessToken(client, oldRefreshToken, requestedScopes);
        saveAccessToken(serverToken);
        return serverToken;
    }
    
    @Override
    public void revokeToken(Client client, String tokenKey, String tokenTypeHint) throws OAuthServiceException {
        if (revokeAccessToken(tokenKey)) {
            return;
        }
        RefreshToken oldRefreshToken = revokeRefreshToken(client, tokenKey);
        if (oldRefreshToken != null) {
            for (String accessTokenKey : oldRefreshToken.getAccessTokens()) {
                revokeAccessToken(accessTokenKey);
            }
        }
    }
    
    @Override
    public List<OAuthPermission> convertScopeToPermissions(Client client,
                                                           List<String> requestedScope) { 
        if (requestedScope.isEmpty()) {
            return Collections.emptyList();
        } else {
            throw new OAuthServiceException("Requested scopes can not be mapped to the permissions");
        }
    }
    
    @Override
    public ServerAccessToken getPreauthorizedToken(Client client, List<String> requestedScopes,
                                                   UserSubject subject, String grantType)
        throws OAuthServiceException {
        return null;
    }
    
    protected ServerAccessToken doCreateAccessToken(AccessTokenRegistration accessToken) {
        ServerAccessToken at = createNewAccessToken(accessToken.getClient());
        at.setAudience(accessToken.getAudience());
        at.setGrantType(accessToken.getGrantType());
        List<String> theScopes = accessToken.getApprovedScope();
        if (theScopes.isEmpty()) {
            theScopes = accessToken.getRequestedScope();
        }
        List<OAuthPermission> thePermissions = 
            convertScopeToPermissions(accessToken.getClient(), theScopes);
        at.setScopes(thePermissions);
        at.setSubject(accessToken.getSubject());
        createNewRefreshToken(at);
        return at;
    }
    
    protected ServerAccessToken createNewAccessToken(Client client) {
        return new BearerAccessToken(client, accessTokenLifetime);
    }
     
    protected RefreshToken createNewRefreshToken(ServerAccessToken at) {
        RefreshToken rt = new RefreshToken(at.getClient(), refreshTokenLifetime);
        rt.setAudience(at.getAudience());
        rt.setGrantType(at.getGrantType());
        rt.setScopes(at.getScopes());
        rt.getAccessTokens().add(at.getTokenKey());
        at.setRefreshToken(rt.getTokenKey());
        saveRefreshToken(at, rt);
        return rt;
    }
    
    protected ServerAccessToken doRefreshAccessToken(Client client, 
                                                     RefreshToken oldRefreshToken, 
                                                     List<String> requestedScopes) {
        ServerAccessToken at = createNewAccessToken(client);
        at.setAudience(oldRefreshToken.getAudience());
        at.setGrantType(oldRefreshToken.getGrantType());
        List<OAuthPermission> theNewScopes = convertScopeToPermissions(client, requestedScopes);
        if (theNewScopes.isEmpty()) {
            at.setScopes(oldRefreshToken.getScopes());
        } else if (oldRefreshToken.getScopes().containsAll(theNewScopes)) {
            at.setScopes(theNewScopes);
        } else {
            throw new OAuthServiceException("Invalid scopes");
        }
        createNewRefreshToken(at);
        return at;
    }
    
    public void setAccessTokenLifetime(long accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public void setRefreshTokenLifetime(long refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }
    
    protected abstract void saveAccessToken(ServerAccessToken serverToken);
    protected abstract void saveRefreshToken(ServerAccessToken at, RefreshToken refreshToken);
    protected abstract boolean revokeAccessToken(String accessTokenKey);
    protected abstract RefreshToken revokeRefreshToken(Client client, String refreshTokenKey);
    
}
