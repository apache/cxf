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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JPAOAuthDataProviderTest extends Assert {
    protected EntityManagerFactory emFactory;
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

    protected String getPersistenceUnitName() {
        return "testUnitHibernate";
    }

    protected void initializeProvider(JPAOAuthDataProvider dataProvider) {
        dataProvider.setSupportedScopes(Collections.singletonMap("a", "A Scope"));
        dataProvider.setSupportedScopes(Collections.singletonMap("refreshToken", "RefreshToken"));
    }

    protected JPAOAuthDataProvider getProvider() {
        return provider;
    }

    @Test
    public void testAddGetDeleteClient() {
        Client c = addClient("12345", "alice");
        Client c2 = getProvider().getClient(c.getClientId());
        compareClients(c, c2);

        c2.setClientSecret("567");
        getProvider().setClient(c2);
        Client c22 = getProvider().getClient(c.getClientId());
        compareClients(c2, c22);

        getProvider().removeClient(c.getClientId());
        Client c3 = getProvider().getClient(c.getClientId());
        assertNull(c3);
    }

    @Test
    public void testAddGetDeleteClients() {
        Client c = addClient("12345", "alice");
        Client c2 = addClient("56789", "alice");
        Client c3 = addClient("09876", "bob");

        List<Client> aliceClients = getProvider().getClients(new UserSubject("alice"));
        assertNotNull(aliceClients);
        assertEquals(2, aliceClients.size());
        compareClients(c, aliceClients.get(0).getClientId().equals("12345")
                       ? aliceClients.get(0) : aliceClients.get(1));
        compareClients(c2, aliceClients.get(0).getClientId().equals("56789")
                       ? aliceClients.get(0) : aliceClients.get(1));

        List<Client> bobClients = getProvider().getClients(new UserSubject("bob"));
        assertNotNull(bobClients);
        assertEquals(1, bobClients.size());
        Client bobClient = bobClients.get(0);
        compareClients(c3, bobClient);

        List<Client> allClients = getProvider().getClients(null);
        assertNotNull(allClients);
        assertEquals(3, allClients.size());
        getProvider().removeClient(c.getClientId());
        getProvider().removeClient(c2.getClientId());
        getProvider().removeClient(c3.getClientId());
        allClients = getProvider().getClients(null);
        assertNotNull(allClients);
        assertEquals(0, allClients.size());
    }

    @Test
    public void testAddGetDeleteAccessToken() {
        Client c = addClient("101", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        atr.setSubject(c.getResourceOwnerSubject());

        ServerAccessToken at = getProvider().createAccessToken(atr);
        ServerAccessToken at2 = getProvider().getAccessToken(at.getTokenKey());
        assertEquals(at.getTokenKey(), at2.getTokenKey());
        List<OAuthPermission> scopes = at2.getScopes();
        assertNotNull(scopes);
        assertEquals(1, scopes.size());
        OAuthPermission perm = scopes.get(0);
        assertEquals("a", perm.getPermission());

        List<ServerAccessToken> tokens = getProvider().getAccessTokens(c, c.getResourceOwnerSubject());
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(at.getTokenKey(), tokens.get(0).getTokenKey());

        tokens = getProvider().getAccessTokens(c, null);
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(at.getTokenKey(), tokens.get(0).getTokenKey());

        tokens = getProvider().getAccessTokens(null, c.getResourceOwnerSubject());
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(at.getTokenKey(), tokens.get(0).getTokenKey());

        tokens = getProvider().getAccessTokens(null, null);
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(at.getTokenKey(), tokens.get(0).getTokenKey());

        getProvider().revokeToken(c, at.getTokenKey(), OAuthConstants.ACCESS_TOKEN);
        assertNull(getProvider().getAccessToken(at.getTokenKey()));
    }

    @Test
    public void testAddGetDeleteAccessToken2() {
        Client c = addClient("102", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        atr.setSubject(c.getResourceOwnerSubject());

        getProvider().createAccessToken(atr);
        List<ServerAccessToken> tokens = getProvider().getAccessTokens(c, null);
        assertNotNull(tokens);
        assertEquals(1, tokens.size());

        getProvider().removeClient(c.getClientId());

        tokens = getProvider().getAccessTokens(c, null);
        assertNotNull(tokens);
        assertEquals(0, tokens.size());
    }

    @Test
    public void testAddGetDeleteRefreshToken() {
        Client c = addClient("101", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Arrays.asList("a", "refreshToken"));
        atr.setSubject(c.getResourceOwnerSubject());

        ServerAccessToken at = getProvider().createAccessToken(atr);
        ServerAccessToken at2 = getProvider().getAccessToken(at.getTokenKey());
        assertEquals(at.getTokenKey(), at2.getTokenKey());
        List<OAuthPermission> scopes = at2.getScopes();
        assertNotNull(scopes);
        assertEquals(2, scopes.size());
        OAuthPermission perm = scopes.get(0);
        assertEquals("a", perm.getPermission());
        OAuthPermission perm2 = scopes.get(1);
        assertEquals("refreshToken", perm2.getPermission());

        RefreshToken rt = getProvider().getRefreshToken(at2.getRefreshToken());
        assertNotNull(rt);
        assertEquals(at2.getTokenKey(), rt.getAccessTokens().get(0));

        List<RefreshToken> tokens = getProvider().getRefreshTokens(c, c.getResourceOwnerSubject());
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(rt.getTokenKey(), tokens.get(0).getTokenKey());

        getProvider().revokeToken(c, rt.getTokenKey(), OAuthConstants.REFRESH_TOKEN);

        assertNull(getProvider().getRefreshToken(rt.getTokenKey()));
    }

    private Client addClient(String clientId, String userLogin) {
        Client c = new Client();
        c.setRedirectUris(Collections.singletonList("http://client/redirect"));
        c.setClientId(clientId);
        c.setClientSecret("123");
        c.setResourceOwnerSubject(new UserSubject(userLogin));
        getProvider().setClient(c);
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
                getProvider().close();
            }
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
