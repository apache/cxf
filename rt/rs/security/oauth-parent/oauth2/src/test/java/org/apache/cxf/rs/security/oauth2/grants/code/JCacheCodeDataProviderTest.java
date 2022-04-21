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
import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JCacheCodeDataProviderTest {
    private JCacheCodeDataProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new JCacheCodeDataProvider();
    }

    @Test
    public void testAddGetDeleteCodeGrants() {
        Client c = addClient("111", "bob");

        AuthorizationCodeRegistration atr = new AuthorizationCodeRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        atr.setSubject(c.getResourceOwnerSubject());

        ServerAuthorizationCodeGrant grant = provider.createCodeGrant(atr);

        List<ServerAuthorizationCodeGrant> grants = provider.getCodeGrants(c, c.getResourceOwnerSubject());
        assertNotNull(grants);
        assertEquals(1, grants.size());
        assertEquals(grant.getCode(), grants.get(0).getCode());

        grants = provider.getCodeGrants(c, null);
        assertNotNull(grants);
        assertEquals(1, grants.size());
        assertEquals(grant.getCode(), grants.get(0).getCode());

        ServerAuthorizationCodeGrant grant2 = provider.removeCodeGrant(grant.getCode());
        assertEquals(grant.getCode(), grant2.getCode());

        grants = provider.getCodeGrants(c, null);
        assertNotNull(grants);
        assertEquals(0, grants.size());
    }

    @Test
    public void testAddGetDeleteCodeGrants2() {
        Client c = addClient("111", "bob");

        AuthorizationCodeRegistration atr = new AuthorizationCodeRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        atr.setSubject(c.getResourceOwnerSubject());

        provider.createCodeGrant(atr);

        List<ServerAuthorizationCodeGrant> grants = provider.getCodeGrants(c, c.getResourceOwnerSubject());
        assertNotNull(grants);
        assertEquals(1, grants.size());
        provider.removeClient(c.getClientId());
        grants = provider.getCodeGrants(c, c.getResourceOwnerSubject());
        assertNotNull(grants);
        assertEquals(0, grants.size());
    }

    private Client addClient(String clientId, String userLogin) {
        Client c = new Client();
        c.setRedirectUris(Collections.singletonList("http://client/redirect"));
        c.setClientId(clientId);
        c.setResourceOwnerSubject(new UserSubject(userLogin));
        provider.setClient(c);
        return c;
    }

    @After
    public void tearDown() throws Exception {
        if (provider != null) {
            provider.close();
        }
    }
}