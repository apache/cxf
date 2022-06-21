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

import java.util.Collections;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

/**
 * Same as {@link JPACodeDataProvider} (stores Clients and tokens in a rdbms using
 * JPA APIs).
 *
 * The transaction demarcation is handled by the container (be it Spring
 * or Java EE).
 *
 * Sample usage with Spring XML:
 <pre>
 {@code
     <bean id="oauthProvider" class="org.apache.cxf.rs.security.oauth2.grants.code.JPACMTCodeDataProvider"
        init-method="init" destroy-method="close">

         <property name="entityManager" ref="entityManager"/>
         <!-- List of accepted scopes -->
         <property name="supportedScopes" ref="supportedScopes"/>
         <!-- List of required scopes -->
         <!-- commented because bug in Resource Owner Flow
         <property name="requiredScopes" ref="coreScopes"/>
         -->
         <!--
         List of scopes that the consent/authorization form should make
         selected by default. For example, asking a user to do an extra click
         to approve an "oidc" scope is a redundant operation because this scope
         is required anyway.
         -->
         <property name="defaultScopes" ref="coreScopes"/>
         <property name="invisibleToClientScopes" ref="invisibleToClientScopes"/>
     </bean>

     <bean name="entityManager" class="org.springframework.orm.jpa.support.SharedEntityManagerBean">
         <property name="entityManagerFactory" ref="entityManagerFactory"/>
     </bean>
     ...
 }
 </pre>

 * You can also extend this class and inject your own entityManager:
 {@code
    public class MyJPACodeDataProvider extends JPACMTCodeDataProvider {

        @PersistenceContext
        @Override
        public void setEntityManager(EntityManager entityManager) {
            super.setEntityManager(entityManager);
        }
 }
 }
 */
public class JPACMTCodeDataProvider extends JPACodeDataProvider {

    private static final int DEFAULT_PESSIMISTIC_LOCK_TIMEOUT = 10000;

    private int pessimisticLockTimeout = DEFAULT_PESSIMISTIC_LOCK_TIMEOUT;
    private boolean useJpaLockForExistingRefreshToken = true;

    private EntityManager entityManager;

    /**
     * Returns the entityManaged used for the current operation.
     */
    @Override
    protected EntityManager getEntityManager() {
        return this.entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Doesn't do anything, beginning tx is handled by container.
     */
    @Override
    protected EntityTransaction beginIfNeeded(EntityManager em) {
        return null;
    }

    /**
     * Doesn't do anything, commit is handled by container.
     */
    @Override
    protected void commitIfNeeded(EntityManager em) {
    }

    /**
     * Doesn't do anything, em lifecycle is handled by container.
     */
    @Override
    protected void closeIfNeeded(EntityManager em) {
    }

    @Override
    protected RefreshToken updateExistingRefreshToken(RefreshToken rt, ServerAccessToken at) {
        if (useJpaLockForExistingRefreshToken) {
            // lock RT for update
            lockRefreshTokenForUpdate(rt);
            return super.updateRefreshToken(rt, at);
        }
        return super.updateExistingRefreshToken(rt, at);
    }

    protected void lockRefreshTokenForUpdate(final RefreshToken refreshToken) {
        try {
            execute(em -> {
                final Map<String, Object> options;
                if (pessimisticLockTimeout > 0) {
                    options = Collections.singletonMap("jakarta.persistence.lock.timeout", pessimisticLockTimeout);
                } else {
                    options = Collections.emptyMap();
                }
                em.refresh(refreshToken, LockModeType.PESSIMISTIC_WRITE, options);
                return null;
            });
        } catch (IllegalArgumentException e) {
            // entity is not managed yet. ignore
        }
    }

    public void setPessimisticLockTimeout(int pessimisticLockTimeout) {
        this.pessimisticLockTimeout = pessimisticLockTimeout;
    }

    public void setUseJpaLockForExistingRefreshToken(boolean useJpaLockForExistingRefreshToken) {
        this.useJpaLockForExistingRefreshToken = useJpaLockForExistingRefreshToken;
    }
}
