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

import java.util.List;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

public class JPAOAuthDataProvider extends AbstractOAuthDataProvider {
    private static final String CLIENT_TABLE_NAME = Client.class.getSimpleName();
    private static final String BEARER_TOKEN_TABLE_NAME = BearerAccessToken.class.getSimpleName();
    private static final String REFRESH_TOKEN_TABLE_NAME = RefreshToken.class.getSimpleName();
    private EntityManager entityManager;
    
    public JPAOAuthDataProvider() {
    }
    
    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        try {
            return getClientQuery(clientId).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    
    public void setClient(Client client) {
        persistEntityWithPossibleRollback(client.getResourceOwnerSubject());
        persistEntity(client);
    }
    
    @Override
    protected void doRemoveClient(Client c) {
        removeEntity(c);
    }

    @Override
    public List<Client> getClients(UserSubject resourceOwner) {
        return getClientsQuery(resourceOwner).getResultList();
    }

    @Override
    public List<ServerAccessToken> getAccessTokens(Client c, UserSubject sub) {
        return getTokensQuery(c, sub).getResultList();
    }

    @Override
    public List<RefreshToken> getRefreshTokens(Client c, UserSubject sub) {
        return getRefreshTokensQuery(c, sub).getResultList();
    }
    
    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        try {
            return getTokenQuery(accessToken).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    @Override
    protected void doRevokeAccessToken(ServerAccessToken at) {
        removeEntity(at);
    }
    @Override
    protected RefreshToken getRefreshToken(String refreshTokenKey) { 
        try {
            return getRefreshTokenQuery(refreshTokenKey).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    @Override
    protected void doRevokeRefreshToken(RefreshToken rt) { 
        removeEntity(rt);
    }
    
    protected void saveAccessToken(ServerAccessToken serverToken) {
        persistEntity(serverToken);
    }
    
    protected void saveRefreshToken(RefreshToken refreshToken) {
        persistEntity(refreshToken);
    }
    protected void persistEntityWithPossibleRollback(Object entity) {
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(entity);
            entityManager.getTransaction().commit();
        }  catch (EntityExistsException ex) {
            entityManager.getTransaction().rollback();
        }
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
    protected TypedQuery<Client> getClientQuery(String clientId) {
        return entityManager.createQuery(
            "SELECT c FROM " + CLIENT_TABLE_NAME + " c WHERE c.clientId = '" + clientId + "'", Client.class);
    }
    protected TypedQuery<Client> getClientsQuery(UserSubject resourceOwnerSubject) {
        if (resourceOwnerSubject == null) {
            return entityManager.createQuery("SELECT c FROM " + CLIENT_TABLE_NAME + " c", Client.class);
        } else {
            return entityManager.createQuery(
                "SELECT c FROM " + CLIENT_TABLE_NAME + " c JOIN c.resourceOwnerSubject r WHERE r.login = '" 
                + resourceOwnerSubject.getLogin() + "'", Client.class);
        }
    }
    protected TypedQuery<BearerAccessToken> getTokenQuery(String tokenKey) {
        return entityManager.createQuery(
            "SELECT t FROM " + BEARER_TOKEN_TABLE_NAME + " t WHERE t.tokenKey = '" + tokenKey + "'", 
            BearerAccessToken.class);
    }
    protected TypedQuery<ServerAccessToken> getTokensQuery(Client c, UserSubject resourceOwnerSubject) {
        if (c == null && resourceOwnerSubject == null) {
            return entityManager.createQuery("SELECT t FROM " + BEARER_TOKEN_TABLE_NAME + " t", 
                                             ServerAccessToken.class);
        } else if (c == null) {
            return entityManager.createQuery(
                "SELECT t FROM " + BEARER_TOKEN_TABLE_NAME + " t JOIN t.subject s WHERE s.login = '" 
                + resourceOwnerSubject.getLogin() + "'", ServerAccessToken.class);
        } else if (resourceOwnerSubject == null) {
            return entityManager.createQuery(
                "SELECT t FROM " + BEARER_TOKEN_TABLE_NAME + " t JOIN t.client c WHERE c.clientId = '" 
                    + c.getClientId() + "'", ServerAccessToken.class);
        } else {
            return entityManager.createQuery(
                "SELECT t FROM " + BEARER_TOKEN_TABLE_NAME + " t JOIN t.subject s JOIN t.client c WHERE s.login = '" 
                + resourceOwnerSubject.getLogin() + "' AND c.clientId = '" + c.getClientId() + "'",
                ServerAccessToken.class);
        }
    }
    protected TypedQuery<RefreshToken> getRefreshTokenQuery(String tokenKey) {
        return entityManager.createQuery(
            "SELECT t FROM " + REFRESH_TOKEN_TABLE_NAME + " t WHERE t.tokenKey = '" + tokenKey + "'", 
            RefreshToken.class);
    }
    protected TypedQuery<RefreshToken> getRefreshTokensQuery(Client c, UserSubject resourceOwnerSubject) {
        if (c == null && resourceOwnerSubject == null) {
            return entityManager.createQuery("SELECT t FROM " + REFRESH_TOKEN_TABLE_NAME + " t", 
                                             RefreshToken.class);
        } else if (c == null) {
            return entityManager.createQuery(
                "SELECT t FROM " + REFRESH_TOKEN_TABLE_NAME + " t JOIN t.subject s WHERE s.login = '" 
                + resourceOwnerSubject.getLogin() + "'", RefreshToken.class);
        } else if (resourceOwnerSubject == null) {
            return entityManager.createQuery(
                "SELECT t FROM " + REFRESH_TOKEN_TABLE_NAME + " t JOIN t.client c WHERE c.clientId = '" 
                    + c.getClientId() + "'", RefreshToken.class);
        } else {
            return entityManager.createQuery(
                "SELECT t FROM " + REFRESH_TOKEN_TABLE_NAME + " t JOIN t.subject s JOIN t.client c WHERE s.login = '" 
                + resourceOwnerSubject.getLogin() + "' AND c.clientId = '" + c.getClientId() + "'",
                RefreshToken.class);
        }
    }
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    public EntityManager getEntityManager() {
        return entityManager;
    }
    @Override
    public void close() {
        entityManager.close();
    }
}
