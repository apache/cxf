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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.JPAOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

public class JPACodeDataProvider extends JPAOAuthDataProvider implements AuthorizationCodeDataProvider {
    private static final String CODE_TABLE_NAME = ServerAuthorizationCodeGrant.class.getSimpleName();
    private long codeLifetime = 10 * 60;
    @Override
    public ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant = doCreateCodeGrant(reg);
        saveCodeGrant(grant);
        return grant;
    }
    
    protected ServerAuthorizationCodeGrant doCreateCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        return AbstractCodeDataProvider.initCodeGrant(reg, codeLifetime);
    }

    protected void saveCodeGrant(ServerAuthorizationCodeGrant grant) { 
        persistEntity(grant);
    }
    
    @Override
    public ServerAuthorizationCodeGrant removeCodeGrant(String code) throws OAuthServiceException {
        try {
            ServerAuthorizationCodeGrant grant = getCodeQuery(code).getSingleResult();
            removeEntity(grant);
            return grant;
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public List<ServerAuthorizationCodeGrant> getCodeGrants(Client c, UserSubject subject)
        throws OAuthServiceException {
        return getCodesQuery(c, subject).getResultList();
    }
    public void setCodeLifetime(long codeLifetime) {
        this.codeLifetime = codeLifetime;
    }
    protected TypedQuery<ServerAuthorizationCodeGrant> getCodeQuery(String code) {
        return getEntityManager().createQuery(
            "SELECT c FROM " + CODE_TABLE_NAME + " c WHERE c.code = '" + code + "'", 
            ServerAuthorizationCodeGrant.class);
    }
    protected TypedQuery<ServerAuthorizationCodeGrant> getCodesQuery(Client c, UserSubject resourceOwnerSubject) {
        if (c == null && resourceOwnerSubject == null) {
            return getEntityManager().createQuery("SELECT c FROM " + CODE_TABLE_NAME + " c", 
                                             ServerAuthorizationCodeGrant.class);
        } else if (c == null) {
            return getEntityManager().createQuery(
                "SELECT c FROM " + CODE_TABLE_NAME + " c JOIN c.subject s WHERE s.login = '" 
                + resourceOwnerSubject.getLogin() + "'", ServerAuthorizationCodeGrant.class);
        } else if (resourceOwnerSubject == null) {
            return getEntityManager().createQuery(
                "SELECT code FROM " + CODE_TABLE_NAME + " code JOIN code.client c WHERE c.clientId = '" 
                    + c.getClientId() + "'", ServerAuthorizationCodeGrant.class);
        } else {
            return getEntityManager().createQuery(
                "SELECT code FROM " + CODE_TABLE_NAME 
                + " code JOIN code.subject s JOIN code.client c WHERE s.login = '" 
                + resourceOwnerSubject.getLogin() + "' AND c.clientId = '" + c.getClientId() + "'",
                ServerAuthorizationCodeGrant.class);
        }
    }
}
