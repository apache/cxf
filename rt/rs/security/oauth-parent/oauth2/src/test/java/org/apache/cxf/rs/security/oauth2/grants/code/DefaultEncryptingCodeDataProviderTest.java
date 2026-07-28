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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DefaultEncryptingCodeDataProviderTest {

    private DefaultEncryptingCodeDataProvider provider;

    @Before
    public void setUp() {
        provider = new DefaultEncryptingCodeDataProvider("AES", 128);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Client addClient(String clientId, String userLogin) {
        Client c = new Client();
        c.setClientId(clientId);
        c.setRedirectUris(Collections.singletonList("http://client/redirect"));
        c.setResourceOwnerSubject(new UserSubject(userLogin));
        provider.setClient(c);
        return c;
    }

    private ServerAuthorizationCodeGrant createGrant(Client c) {
        AuthorizationCodeRegistration reg = new AuthorizationCodeRegistration();
        reg.setClient(c);
        reg.setApprovedScope(Collections.singletonList("read"));
        reg.setSubject(c.getResourceOwnerSubject());
        return provider.createCodeGrant(reg);
    }

    // -----------------------------------------------------------------------
    // Basic create / list / remove flow
    // -----------------------------------------------------------------------

    @Test
    public void testCreateAndListGrant() {
        Client c = addClient("c1", "alice");
        createGrant(c);

        List<ServerAuthorizationCodeGrant> listed = provider.getCodeGrants(c, null);
        assertEquals(1, listed.size());
        assertEquals("c1", listed.get(0).getClient().getClientId());
        assertEquals("alice", listed.get(0).getSubject().getLogin());
    }

    @Test
    public void testRemoveCodeGrantReturnsGrant() {
        Client c = addClient("c1", "alice");
        ServerAuthorizationCodeGrant grant = createGrant(c);

        ServerAuthorizationCodeGrant removed = provider.removeCodeGrant(grant.getCode());

        assertNotNull(removed);
        assertEquals("c1", removed.getClient().getClientId());
        assertEquals("alice", removed.getSubject().getLogin());
    }

    @Test
    public void testRemoveCodeGrantDeletesFromListing() {
        Client c = addClient("c1", "alice");
        ServerAuthorizationCodeGrant grant = createGrant(c);

        provider.removeCodeGrant(grant.getCode());

        List<ServerAuthorizationCodeGrant> listed = provider.getCodeGrants(c, null);
        assertEquals(0, listed.size());
    }

    // -----------------------------------------------------------------------
    // CWE-294 replay prevention (the security fix under test)
    // -----------------------------------------------------------------------

    @Test
    public void testRemoveCodeGrantRejectsDuplicateRedemption() {
        Client c = addClient("c1", "alice");
        ServerAuthorizationCodeGrant grant = createGrant(c);
        String code = grant.getCode();

        // first redemption must succeed
        assertNotNull(provider.removeCodeGrant(code));

        // second redemption with the same code must be rejected (replay)
        assertNull("Replayed authorization code must be rejected", provider.removeCodeGrant(code));
    }

    @Test
    public void testRemoveCodeGrantRejectsNeverIssuedCode() {
        // A forged or externally-crafted encrypted string that was never registered
        assertNull(provider.removeCodeGrant("not-a-real-code"));
    }

    // -----------------------------------------------------------------------
    // getCodeGrant (read-only, must not consume the grant)
    // -----------------------------------------------------------------------

    @Test
    public void testGetCodeGrantReturnsGrantWithoutConsuming() {
        Client c = addClient("c1", "alice");
        ServerAuthorizationCodeGrant grant = createGrant(c);
        String encryptedCode = grant.getCode();

        ServerAuthorizationCodeGrant first = provider.getCodeGrant(encryptedCode);
        assertNotNull(first);
        assertEquals("c1", first.getClient().getClientId());

        // calling again must still return the grant (not consumed)
        ServerAuthorizationCodeGrant second = provider.getCodeGrant(encryptedCode);
        assertNotNull("getCodeGrant must not consume the grant", second);
        assertEquals("c1", second.getClient().getClientId());
    }

    @Test
    public void testGetCodeGrantDoesNotRemoveFromListing() {
        Client c = addClient("c1", "alice");
        createGrant(c);

        // listing internally calls getCodeGrant; the grant must survive the call
        List<ServerAuthorizationCodeGrant> afterFirstList = provider.getCodeGrants(c, null);
        assertEquals(1, afterFirstList.size());

        List<ServerAuthorizationCodeGrant> afterSecondList = provider.getCodeGrants(c, null);
        assertEquals("getCodeGrants must not consume grants", 1, afterSecondList.size());
    }

    @Test
    public void testGetCodeGrantReturnsNullForUnknownCode() {
        assertNull(provider.getCodeGrant("not-a-real-code"));
    }

    @Test
    public void testGetCodeGrantReturnsNullAfterRemove() {
        Client c = addClient("c1", "alice");
        ServerAuthorizationCodeGrant grant = createGrant(c);
        String code = grant.getCode();

        provider.removeCodeGrant(code);

        assertNull("getCodeGrant must return null for already-consumed code",
                   provider.getCodeGrant(code));
    }

    // -----------------------------------------------------------------------
    // Listing filters
    // -----------------------------------------------------------------------

    @Test
    public void testGetCodeGrantsFiltersBySubject() {
        Client c = addClient("c1", "alice");
        createGrant(c);

        List<ServerAuthorizationCodeGrant> forAlice =
            provider.getCodeGrants(c, new UserSubject("alice"));
        assertEquals(1, forAlice.size());

        List<ServerAuthorizationCodeGrant> forBob =
            provider.getCodeGrants(c, new UserSubject("bob"));
        assertEquals(0, forBob.size());
    }

    @Test
    public void testGetCodeGrantsFiltersByClient() {
        Client c1 = addClient("c1", "alice");
        Client c2 = addClient("c2", "alice");
        createGrant(c1);
        createGrant(c2);

        List<ServerAuthorizationCodeGrant> forC1 = provider.getCodeGrants(c1, null);
        assertEquals(1, forC1.size());
        assertEquals("c1", forC1.get(0).getClient().getClientId());
    }

    // -----------------------------------------------------------------------
    // removeClient cascades to code grants
    // -----------------------------------------------------------------------

    @Test
    public void testRemoveClientAlsoRemovesCodeGrants() {
        Client c = addClient("c1", "alice");
        createGrant(c);

        assertEquals(1, provider.getCodeGrants(c, null).size());

        provider.removeClient("c1");

        assertEquals(0, provider.getCodeGrants(c, null).size());
    }

    // -----------------------------------------------------------------------
    // Multiple concurrent grants for the same client
    // -----------------------------------------------------------------------

    @Test
    public void testMultipleGrantsForSameClient() {
        Client c = addClient("c1", "alice");
        ServerAuthorizationCodeGrant g1 = createGrant(c);
        ServerAuthorizationCodeGrant g2 = createGrant(c);

        List<ServerAuthorizationCodeGrant> grants = provider.getCodeGrants(c, null);
        assertEquals(2, grants.size());

        provider.removeCodeGrant(g1.getCode());
        assertEquals(1, provider.getCodeGrants(c, null).size());

        provider.removeCodeGrant(g2.getCode());
        assertEquals(0, provider.getCodeGrants(c, null).size());
    }
}
