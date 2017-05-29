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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

public class JPACMTCodeDataProvider extends JPACodeDataProvider {

    private static final int DEFAULT_PESSIMISTIC_LOCK_TIMEOUT = 10000;
    
    private int pessimisticLockTimeout = DEFAULT_PESSIMISTIC_LOCK_TIMEOUT;
    private boolean useJpaLockForExistingRefreshToken = true;
    
    @PersistenceContext
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
        } else {
            return super.updateExistingRefreshToken(rt, at);
        }
    }
    
    protected void lockRefreshTokenForUpdate(final RefreshToken refreshToken) {
        try {
            execute(new EntityManagerOperation<Void>() {

                @Override
                public Void execute(EntityManager em) {
                    Map<String, Object> options = null;
                    if (pessimisticLockTimeout > 0) {
                        options = Collections.singletonMap("javax.persistence.lock.timeout", pessimisticLockTimeout);
                    } else {
                        options = Collections.emptyMap();
                    }
                    em.refresh(refreshToken, LockModeType.PESSIMISTIC_WRITE, options);
                    return null;
                }
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
