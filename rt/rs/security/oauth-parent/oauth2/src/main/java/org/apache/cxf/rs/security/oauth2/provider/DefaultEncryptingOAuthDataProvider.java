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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.crypto.ModelEncryptionSupport;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.KeyProperties;

public class DefaultEncryptingOAuthDataProvider extends AbstractOAuthDataProvider {
    protected SecretKey key;
    private Set<String> tokens = Collections.synchronizedSet(new HashSet<>());
    private Set<String> refreshTokens = Collections.synchronizedSet(new HashSet<>());
    private ConcurrentHashMap<String, String> clientsMap = new ConcurrentHashMap<>();
    public DefaultEncryptingOAuthDataProvider(String algo, int keySize) {
        this(new KeyProperties(algo, keySize));
    }
    public DefaultEncryptingOAuthDataProvider(KeyProperties props) {
        this(CryptoUtils.getSecretKey(props));
    }
    public DefaultEncryptingOAuthDataProvider(SecretKey key) {
        this.key = key;
    }

    @Override
    public Client doGetClient(String clientId) throws OAuthServiceException {
        return ModelEncryptionSupport.decryptClient(clientsMap.get(clientId), key);
    }

    @Override
    public void setClient(Client client) {
        clientsMap.put(client.getClientId(), ModelEncryptionSupport.encryptClient(client, key));

    }
    @Override
    public void doRemoveClient(Client c) {
        clientsMap.remove(c.getClientId());
    }
    @Override
    public List<Client> getClients(UserSubject resourceOwner) {
        List<Client> clients = new ArrayList<>(clientsMap.size());
        for (String clientKey : clientsMap.keySet()) {
            Client c = getClient(clientKey);
            if (isClientMatched(c, resourceOwner)) {
                clients.add(c);
            }
        }
        return clients;
    }
    @Override
    public List<ServerAccessToken> getAccessTokens(Client c, UserSubject sub) {
        List<ServerAccessToken> list = new ArrayList<>(tokens.size());
        for (String tokenKey : tokens) {
            ServerAccessToken token = getAccessToken(tokenKey);
            if (isTokenMatched(token, c, sub)) {
                list.add(token);
            }
        }
        return list;
    }
    @Override
    public List<RefreshToken> getRefreshTokens(Client c, UserSubject sub) {
        List<RefreshToken> list = new ArrayList<>(refreshTokens.size());
        for (String tokenKey : refreshTokens) {
            RefreshToken token = getRefreshToken(tokenKey);
            if (isTokenMatched(token, c, sub)) {
                list.add(token);
            }
        }
        return list;
    }
    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        try {
            return ModelEncryptionSupport.decryptAccessToken(this, accessToken, key);
        } catch (SecurityException ex) {
            throw new OAuthServiceException(OAuthConstants.ACCESS_DENIED, ex);
        }
    }

    @Override
    protected void saveAccessToken(ServerAccessToken serverToken) {
        encryptAccessToken(serverToken);
    }

    @Override
    protected void doRevokeAccessToken(ServerAccessToken at) {
        tokens.remove(at.getTokenKey());
    }

    @Override
    protected void saveRefreshToken(RefreshToken refreshToken) {
        String encryptedRefreshToken = ModelEncryptionSupport.encryptRefreshToken(refreshToken, key);
        refreshToken.setTokenKey(encryptedRefreshToken);
        refreshTokens.add(encryptedRefreshToken);
    }

    @Override
    protected void doRevokeRefreshToken(RefreshToken rt) {
        refreshTokens.remove(rt.getTokenKey());
    }

    private void encryptAccessToken(ServerAccessToken token) {
        String encryptedToken = ModelEncryptionSupport.encryptAccessToken(token, key);
        tokens.add(encryptedToken);
        token.setTokenKey(encryptedToken);
    }
    @Override
    protected RefreshToken getRefreshToken(String refreshTokenKey) {
        try {
            return ModelEncryptionSupport.decryptRefreshToken(this, refreshTokenKey, key);
        } catch (SecurityException ex) {
            throw new OAuthServiceException(OAuthConstants.ACCESS_DENIED, ex);
        }
    }

}
