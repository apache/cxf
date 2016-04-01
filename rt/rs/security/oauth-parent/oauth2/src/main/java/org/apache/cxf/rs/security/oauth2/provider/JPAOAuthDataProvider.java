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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

public class JPAOAuthDataProvider extends AbstractOAuthDataProvider {
    private static final String CLIENT_TABLE_NAME = Client.class.getSimpleName();
    private EntityManager entityManager;
    
    public JPAOAuthDataProvider() {
    }
    
    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        try {
            TypedQuery<Client> query = entityManager.createQuery(
                "SELECT c FROM " + CLIENT_TABLE_NAME + " c WHERE c.clientId = '" + clientId + "'", Client.class);
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    
    public void setClient(Client client) {
        persistEntity(client);
    }
    
    @Override
    protected void doRemoveClient(Client c) {
        removeEntity(c);
    }

    @Override
    public List<Client> getClients(UserSubject resourceOwner) {
        return null;
    }

    @Override
    public List<ServerAccessToken> getAccessTokens(Client c, UserSubject sub) {
        return Collections.emptyList();
    }

    @Override
    public List<RefreshToken> getRefreshTokens(Client c, UserSubject sub) {
        return Collections.emptyList();
    }
    
    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        return null;
    }
    @Override
    protected void doRevokeAccessToken(ServerAccessToken at) {
    }
    @Override
    protected RefreshToken getRefreshToken(String refreshTokenKey) { 
        return null;
    }
    @Override
    protected void doRevokeRefreshToken(RefreshToken rt) { 
    }
    
    protected void saveAccessToken(ServerAccessToken serverToken) {
        persistEntity(serverToken);
    }
    
    protected void saveRefreshToken(ServerAccessToken at, RefreshToken refreshToken) {
    }
    
    protected void persistEntity(Object entity) {
        entityManager.getTransaction().begin();
        entityManager.persist(entity);
        entityManager.getTransaction().commit();
    }
    protected void removeEntity(Object entity) {
        entityManager.getTransaction().begin();
        entityManager.remove(entity);
        entityManager.getTransaction().commit();
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    @Override
    public void close() {
        entityManager.close();
    }
}
