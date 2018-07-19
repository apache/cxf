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

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;

public class JPAOAuthDataProviderTest extends AbstractOAuthDataProviderTest {
    protected EntityManagerFactory emFactory;

    @Before
    public void setUp() throws Exception {
        try {
            emFactory = Persistence.createEntityManagerFactory(getPersistenceUnitName());
            JPAOAuthDataProvider provider = new JPAOAuthDataProvider();
            provider.setEntityManagerFactory(emFactory);
            initializeProvider(provider);
            setProvider(provider);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during JPA EntityManager creation.");
        }
    }

    protected String getPersistenceUnitName() {
        return "testUnitHibernate";
    }

    @After
    public void tearDown() throws Exception {
        try {
            super.tearDown();
            if (emFactory != null) {
                emFactory.close();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            try {
                //connection.createStatement().execute("SHUTDOWN");
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

}
