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
package org.apache.cxf.rs.security.oauth2.grants;

import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;


public class OAuthDataProviderImpl implements OAuthDataProvider {

    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        return new Client("alice", "alice", true);
    }

    @Override
    public ServerAccessToken createAccessToken(AccessTokenRegistration accessToken)
        throws OAuthServiceException {
        return new BearerAccessToken(accessToken.getClient(), 3600);
    }

    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        return null;
    }

    @Override
    public ServerAccessToken getPreauthorizedToken(Client client, List<String> requestedScopes,
                                                   UserSubject subject, String grantType)
        throws OAuthServiceException {
        return null;
    }

    @Override
    public ServerAccessToken refreshAccessToken(Client client, String refreshToken,
                                                List<String> requestedScopes) throws OAuthServiceException {
        return null;
    }

    @Override
    public List<OAuthPermission> convertScopeToPermissions(Client client, List<String> requestedScope) {
        return null;
    }

    @Override
    public void revokeToken(Client client, String token, String tokenTypeHint) throws OAuthServiceException {

    }

    @Override
    public List<ServerAccessToken> getAccessTokens(Client client, UserSubject sub) throws OAuthServiceException {
        return null;
    }

    @Override
    public List<RefreshToken> getRefreshTokens(Client client, UserSubject sub) throws OAuthServiceException {
        return null;
    }

}
