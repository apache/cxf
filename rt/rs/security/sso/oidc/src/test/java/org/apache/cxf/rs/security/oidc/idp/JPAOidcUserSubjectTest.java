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
package org.apache.cxf.rs.security.oidc.idp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.JPAOAuthDataProvider;
import org.apache.cxf.rs.security.oidc.common.IdToken;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JPAOidcUserSubjectTest extends Assert {
    private EntityManagerFactory emFactory;
    private Connection connection;
    private JPAOAuthDataProvider provider;
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
            provider = new JPAOAuthDataProvider();
            provider.setEntityManager(em);
            provider.setSupportedScopes(Collections.singletonMap("a", "A Scope"));
            provider.setSupportedScopes(Collections.singletonMap("refreshToken", "RefreshToken"));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during JPA EntityManager creation.");
        }
    }

    
    @Test
    public void testAccessTokenWithOidcUserSubject() {
        Client c = addClient("101", "bob");
        
        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        
        OidcUserSubject oidcSubject = new OidcUserSubject();
        oidcSubject.setLogin("bob");
        IdToken idToken = new IdToken();
        idToken.setAudience(c.getClientId());
        oidcSubject.setIdToken(idToken);
        atr.setSubject(oidcSubject);
        
        ServerAccessToken at = provider.createAccessToken(atr);
        ServerAccessToken at2 = provider.getAccessToken(at.getTokenKey());
        assertEquals(at.getTokenKey(), at2.getTokenKey());
                
        OidcUserSubject oidcSubject2 = (OidcUserSubject)at2.getSubject();
        assertEquals(c.getClientId(), oidcSubject2.getIdToken().getAudience());
        
//        OidcUserSubject oidcSubject3 = new OidcUserSubject();
//        oidcSubject3.setLogin("bob");
//        IdToken idToken2 = new IdToken();
//        idToken2.setAudience(c.getClientId());
//        oidcSubject3.setIdToken(idToken2);
//        atr.setSubject(oidcSubject3);
//        
//        ServerAccessToken at3 = provider.createAccessToken(atr);
//        ServerAccessToken at4 = provider.getAccessToken(at3.getTokenKey());
//        OidcUserSubject oidcSubject4 = (OidcUserSubject)at4.getSubject();
//        assertEquals(c.getClientId(), oidcSubject4.getIdToken().getAudience());
    }
    
    
    private Client addClient(String clientId, String userLogin) {
        Client c = new Client();
        c.setRedirectUris(Collections.singletonList("http://client/redirect"));
        c.setClientId(clientId);
        c.setResourceOwnerSubject(new OidcUserSubject(userLogin));
        provider.setClient(c);
        return c;
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
