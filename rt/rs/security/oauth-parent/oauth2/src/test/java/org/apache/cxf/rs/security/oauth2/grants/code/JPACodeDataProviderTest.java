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

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JPACodeDataProviderTest extends Assert {
    private EntityManagerFactory emFactory;
    private Connection connection;
    private JPACodeDataProvider provider;
    @Before
    public void setUp() throws Exception {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            connection = DriverManager.getConnection("jdbc:hsqldb:mem:oauth-jpa", "sa", "");
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during HSQL database init.");
        }
        try {
            emFactory = Persistence.createEntityManagerFactory("testUnitHibernate");
            EntityManager em = emFactory.createEntityManager();
            provider = new JPACodeDataProvider();
            provider.setEntityManager(em);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during JPA EntityManager creation.");
        }
    }

    @Test
    public void testAddGetDeleteClient() {
        Client c = addClient("12345", "alice");
        Client c2 = provider.getClient(c.getClientId());
        compareClients(c, c2);
        
        provider.removeClient(c.getClientId());
        Client c3 = provider.getClient(c.getClientId());
        assertNull(c3);
    }
    
    @Test
    public void testAddGetDeleteClients() {
        Client c = addClient("12345", "alice");
        Client c2 = addClient("56789", "alice");
        Client c3 = addClient("09876", "bob");
        
        List<Client> aliceClients = provider.getClients(new UserSubject("alice"));
        assertNotNull(aliceClients);
        assertEquals(2, aliceClients.size());
        compareClients(c, aliceClients.get(0).getClientId().equals("12345") 
                       ? aliceClients.get(0) : aliceClients.get(1));
        compareClients(c2, aliceClients.get(0).getClientId().equals("56789") 
                       ? aliceClients.get(0) : aliceClients.get(1));
        
        List<Client> bobClients = provider.getClients(new UserSubject("bob"));
        assertNotNull(bobClients);
        assertEquals(1, bobClients.size());
        Client bobClient = bobClients.get(0);
        compareClients(c3, bobClient);
        
        List<Client> allClients = provider.getClients(null);
        assertNotNull(allClients);
        assertEquals(3, allClients.size());
        
    }
    
    private Client addClient(String clientId, String userLogin) {
        Client c = new Client();
        c.setRedirectUris(Collections.singletonList("http://client/redirect"));
        c.setClientId(clientId);
        c.setResourceOwnerSubject(new UserSubject(userLogin));
        provider.setClient(c);
        return c;
    }
    private void compareClients(Client c, Client c2) {
        assertNotNull(c2);
        assertEquals(c.getClientId(), c2.getClientId());
        assertEquals(1, c.getRedirectUris().size());
        assertEquals(1, c2.getRedirectUris().size());
        assertEquals("http://client/redirect", c.getRedirectUris().get(0));
        assertEquals(c.getResourceOwnerSubject().getLogin(), c2.getResourceOwnerSubject().getLogin());
    }
    
    @After
    public void tearDown() throws Exception {
        try {
            if (provider != null) {
                provider.close();
            }
            if (emFactory != null) {
                emFactory.close();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();    
        } finally {    
            try {
                connection.createStatement().execute("SHUTDOWN");
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
    
}
