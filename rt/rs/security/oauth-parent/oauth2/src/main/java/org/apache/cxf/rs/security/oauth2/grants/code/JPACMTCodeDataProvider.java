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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;

public class JPACMTCodeDataProvider extends JPACodeDataProvider {

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
}
