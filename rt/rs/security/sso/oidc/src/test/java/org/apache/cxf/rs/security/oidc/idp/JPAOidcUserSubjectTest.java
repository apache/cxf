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

import java.util.Collections;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.JPAOAuthDataProvider;
import org.apache.cxf.rs.security.oidc.common.IdToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JPAOidcUserSubjectTest {
    private EntityManagerFactory emFactory;
    private JPAOAuthDataProvider provider;

    @Before
    public void setUp() throws Exception {
        try {
            emFactory = Persistence.createEntityManagerFactory(getPersistenceUnitName());
            provider = new JPAOAuthDataProvider();
            provider.setEntityManagerFactory(emFactory);
            initializeProvider(provider);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Exception during JPA EntityManager creation.");
        }
    }

    protected JPAOAuthDataProvider getProvider() {
        return provider;
    }

    protected void initializeProvider(JPAOAuthDataProvider oauthDataProvider) {
        oauthDataProvider.setSupportedScopes(Collections.singletonMap("a", "A Scope"));
        oauthDataProvider.setSupportedScopes(Collections.singletonMap("refreshToken", "RefreshToken"));
    }

    protected String getPersistenceUnitName() {
        return "test-hibernate-cxf-rt-rs-security-sso-oidc";
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

        ServerAccessToken at = getProvider().createAccessToken(atr);
        ServerAccessToken at2 = getProvider().getAccessToken(at.getTokenKey());
        assertEquals(at.getTokenKey(), at2.getTokenKey());

        OidcUserSubject oidcSubject2 = (OidcUserSubject)at2.getSubject();
        assertEquals(c.getClientId(), oidcSubject2.getIdToken().getAudience());

        OidcUserSubject oidcSubject3 = new OidcUserSubject();
        oidcSubject3.setLogin("bob");
        IdToken idToken2 = new IdToken();
        idToken2.setAudience(c.getClientId());
        oidcSubject3.setIdToken(idToken2);
        atr.setSubject(oidcSubject3);

        ServerAccessToken at3 = getProvider().createAccessToken(atr);
        ServerAccessToken at4 = getProvider().getAccessToken(at3.getTokenKey());
        OidcUserSubject oidcSubject4 = (OidcUserSubject)at4.getSubject();
        assertEquals(c.getClientId(), oidcSubject4.getIdToken().getAudience());
    }


    private Client addClient(String clientId, String userLogin) {
        Client c = new Client();
        c.setRedirectUris(Collections.singletonList("http://client/redirect"));
        c.setClientId(clientId);
        c.setResourceOwnerSubject(new OidcUserSubject(userLogin));
        getProvider().setClient(c);
        return c;
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (getProvider() != null) {
                getProvider().close();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (getProvider() != null) {
                    getProvider().close();
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
}