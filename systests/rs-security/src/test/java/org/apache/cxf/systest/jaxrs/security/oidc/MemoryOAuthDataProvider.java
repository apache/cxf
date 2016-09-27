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
package org.apache.cxf.systest.jaxrs.security.oidc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.AbstractOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

public class MemoryOAuthDataProvider extends AbstractOAuthDataProvider {

    private Map<String, Client> clients = new HashMap<String, Client>();
    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        return clients.get(clientId);
    }

    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ServerAccessToken> getAccessTokens(Client client, UserSubject subject)
        throws OAuthServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<RefreshToken> getRefreshTokens(Client client, UserSubject subject)
        throws OAuthServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setClient(Client client) {
        clients.put(client.getClientId(), client);
    }

    @Override
    public List<Client> getClients(UserSubject resourceOwner) {
        return new ArrayList<Client>(clients.values());
    }

    @Override
    protected void saveAccessToken(ServerAccessToken serverToken) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void saveRefreshToken(RefreshToken refreshToken) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void doRevokeAccessToken(ServerAccessToken accessToken) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void doRevokeRefreshToken(RefreshToken refreshToken) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected RefreshToken getRefreshToken(String refreshTokenKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void doRemoveClient(Client c) {
        clients.remove(c.getClientId());
        
    }

}
