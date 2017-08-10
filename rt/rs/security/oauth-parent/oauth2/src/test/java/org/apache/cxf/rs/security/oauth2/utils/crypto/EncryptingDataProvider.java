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
package org.apache.cxf.rs.security.oauth2.utils.crypto;

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
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class EncryptingDataProvider implements OAuthDataProvider {

    SecretKey key;
    private Map<String, String> clients;
    private Set<String> tokens = new HashSet<>();
    private Map<String, String> refreshTokens = new HashMap<>();

    public EncryptingDataProvider() throws Exception {
        key = CryptoUtils.getSecretKey("AES");
        String encryptedClient = ModelEncryptionSupport.encryptClient(new Client("1", "2", true), key);
        clients = Collections.singletonMap("1", encryptedClient);
    }

    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        return ModelEncryptionSupport.decryptClient(clients.get(clientId), key);
    }

    @Override
    public ServerAccessToken createAccessToken(AccessTokenRegistration accessTokenReg)
        throws OAuthServiceException {

        ServerAccessToken token = createAccessTokenInternal(accessTokenReg);
        encryptAccessToken(token);
        return token;
    }

    @Override
    public ServerAccessToken getAccessToken(String accessTokenKey) throws OAuthServiceException {
        return ModelEncryptionSupport.decryptAccessToken(this, accessTokenKey, key);
    }

    @Override
    public ServerAccessToken refreshAccessToken(Client client, String refreshToken,
                                                List<String> requestedScopes)
        throws OAuthServiceException {
        String encrypted = refreshTokens.remove(refreshToken);
        ServerAccessToken token = ModelEncryptionSupport.decryptAccessToken(this, encrypted, key);
        tokens.remove(token.getTokenKey());

        // create a new refresh token
        createRefreshToken(token);
        // possibly update other token properties
        encryptAccessToken(token);

        return token;
    }

    @Override
    public void revokeToken(Client client, String token, String tokenTypeHint)
        throws OAuthServiceException {
        // the fast way: if it is the refresh token then there will be a matching value for it
        String accessToken = refreshTokens.remove(token);
        // if no matching value then the token parameter is access token key
        tokens.remove(accessToken == null ? token : accessToken);
    }

    @Override
    public List<OAuthPermission> convertScopeToPermissions(Client client, List<String> requestedScope) {
        // assuming that no specific scopes is documented/supported
        return Collections.emptyList();
    }

    @Override
    public ServerAccessToken getPreauthorizedToken(Client client, List<String> requestedScopes,
                                                   UserSubject subject, String grantType)
        throws OAuthServiceException {
        // This is an optimization useful in cases where a client requests an authorization code:
        // if a user has already provided a given client with a pre-authorized token then challenging
        // a user with yet another form asking for the authorization is redundant
        return null;
    }

    BearerAccessToken createAccessTokenInternal(AccessTokenRegistration accessTokenReg) {
        BearerAccessToken token = new BearerAccessToken(accessTokenReg.getClient(), 3600L);
        token.setSubject(accessTokenReg.getSubject());

        createRefreshToken(token);

        token.setGrantType(accessTokenReg.getGrantType());
        token.setAudiences(accessTokenReg.getAudiences());
        token.setParameters(Collections.singletonMap("param", "value"));
        token.setScopes(Collections.singletonList(
            new OAuthPermission("read", "read permission")));
        return token;
    }

    private void encryptAccessToken(ServerAccessToken token) {
        String encryptedToken = ModelEncryptionSupport.encryptAccessToken(token, key);
        tokens.add(encryptedToken);
        refreshTokens.put(token.getRefreshToken(), encryptedToken);
        token.setTokenKey(encryptedToken);
    }

    private void createRefreshToken(ServerAccessToken token) {
        RefreshToken refreshToken = new RefreshToken(token.getClient(),
                                                     "refresh",
                                                     1200L,
                                                     OAuthUtils.getIssuedAt());

        String encryptedRefreshToken = ModelEncryptionSupport.encryptRefreshToken(refreshToken, key);
        token.setRefreshToken(encryptedRefreshToken);
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
