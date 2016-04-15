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

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
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
        return getEntityManager().find(Client.class, clientId);
    }
    
    public void setClient(Client client) {
        getEntityManager().getTransaction().begin();
        if (client.getResourceOwnerSubject() != null) {
            UserSubject sub = getEntityManager().find(UserSubject.class, client.getResourceOwnerSubject().getLogin());
            if (sub == null) {
                getEntityManager().persist(client.getResourceOwnerSubject());
            } else {
                client.setResourceOwnerSubject(sub);
            }
        }
        getEntityManager().persist(client);
        getEntityManager().getTransaction().commit();
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
        return getEntityManager().find(BearerAccessToken.class, accessToken);
    }
    @Override
    protected void doRevokeAccessToken(ServerAccessToken at) {
        removeEntity(at);
    }
    @Override
    protected RefreshToken getRefreshToken(String refreshTokenKey) { 
        return getEntityManager().find(RefreshToken.class, refreshTokenKey);
    }
    @Override
    protected void doRevokeRefreshToken(RefreshToken rt) { 
        removeEntity(rt);
    }
    
    protected void saveAccessToken(ServerAccessToken serverToken) {
        getEntityManager().getTransaction().begin();
        List<OAuthPermission> perms = new LinkedList<OAuthPermission>();
        for (OAuthPermission perm : serverToken.getScopes()) {
            OAuthPermission permSaved = getEntityManager().find(OAuthPermission.class, perm.getPermission());
            if (permSaved != null) {
                perms.add(permSaved);
            } else {
                getEntityManager().persist(perm);
                perms.add(perm);
            }
        }
        serverToken.setScopes(perms);
        
        UserSubject sub = getEntityManager().find(UserSubject.class, serverToken.getSubject().getLogin());
        if (sub == null) {
            getEntityManager().persist(serverToken.getSubject());
        } else {
            sub = getEntityManager().merge(serverToken.getSubject());
            serverToken.setSubject(sub);
        }
        
        getEntityManager().persist(serverToken);
        getEntityManager().getTransaction().commit();
    }
    
    protected void saveRefreshToken(RefreshToken refreshToken) {
        persistEntity(refreshToken);
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
    protected TypedQuery<Client> getClientsQuery(UserSubject resourceOwnerSubject) {
        if (resourceOwnerSubject == null) {
            return entityManager.createQuery("SELECT c FROM " + CLIENT_TABLE_NAME + " c", Client.class);
        } else {
            return entityManager.createQuery(
                "SELECT c FROM " + CLIENT_TABLE_NAME + " c JOIN c.resourceOwnerSubject r WHERE r.login = '" 
                + resourceOwnerSubject.getLogin() + "'", Client.class);
        }
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
