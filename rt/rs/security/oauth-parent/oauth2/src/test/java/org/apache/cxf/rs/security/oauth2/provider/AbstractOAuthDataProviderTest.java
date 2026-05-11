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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.PrivateKeyJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

abstract class AbstractOAuthDataProviderTest {
    private static KeyPair keyPair;
    private AbstractOAuthDataProvider provider;

    static {
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    protected static void initializeProvider(AbstractOAuthDataProvider dataProvider) {
        dataProvider.setSupportedScopes(Collections.singletonMap("a", "A Scope"));
        dataProvider.setSupportedScopes(Collections.singletonMap("refreshToken", "RefreshToken"));

        // Configure the means of signing the issued JWT tokens
        if (dataProvider.isUseJwtFormatForAccessTokens()) {
            final JwsSignatureProvider signatureProvider =
                new PrivateKeyJwsSignatureProvider(keyPair.getPrivate(), SignatureAlgorithm.RS256);

            OAuthJoseJwtProducer jwtAccessTokenProducer = new OAuthJoseJwtProducer();
            jwtAccessTokenProducer.setSignatureProvider(signatureProvider);
            dataProvider.setJwtAccessTokenProducer(jwtAccessTokenProducer);
        }
    }

    protected AbstractOAuthDataProvider getProvider() {
        return provider;
    }

    protected void setProvider(AbstractOAuthDataProvider provider) {
        this.provider = provider;
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
        compareClients(c, "12345".equals(aliceClients.get(0).getClientId())
                       ? aliceClients.get(0) : aliceClients.get(1));
        compareClients(c2, "56789".equals(aliceClients.get(0).getClientId())
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
        validateAccessToken(at);
        ServerAccessToken at2 = getProvider().getAccessToken(at.getTokenKey());
        validateAccessToken(at2);
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
        validateAccessToken(tokens.get(0));

        tokens = getProvider().getAccessTokens(c, null);
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(at.getTokenKey(), tokens.get(0).getTokenKey());
        validateAccessToken(tokens.get(0));

        tokens = getProvider().getAccessTokens(null, c.getResourceOwnerSubject());
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(at.getTokenKey(), tokens.get(0).getTokenKey());
        validateAccessToken(tokens.get(0));

        tokens = getProvider().getAccessTokens(null, null);
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals(at.getTokenKey(), tokens.get(0).getTokenKey());
        validateAccessToken(tokens.get(0));

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
        validateAccessToken(tokens.get(0));

        getProvider().removeClient(c.getClientId());

        tokens = getProvider().getAccessTokens(c, null);
        assertNotNull(tokens);
        assertEquals(0, tokens.size());
    }

    @Test
    public void testAddGetDeleteAccessTokenWithNullSubject() {
        Client c = addClient("102", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        atr.setSubject(null);

        getProvider().createAccessToken(atr);
        List<ServerAccessToken> tokens = getProvider().getAccessTokens(c, null);
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        validateAccessToken(tokens.get(0));

        getProvider().removeClient(c.getClientId());

        tokens = getProvider().getAccessTokens(c, null);
        assertNotNull(tokens);
        assertEquals(0, tokens.size());
    }

    /**
     * Checks that having multiple token each with its own
     * userSubject (but having same login) works.
     */
    @Test
    public void testAddGetDeleteMultipleAccessToken() {
        Client c = addClient("101", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        atr.setSubject(c.getResourceOwnerSubject());
        ServerAccessToken at = getProvider().createAccessToken(atr);
        validateAccessToken(at);
        at = getProvider().getAccessToken(at.getTokenKey());
        validateAccessToken(at);

        AccessTokenRegistration atr2 = new AccessTokenRegistration();
        atr2.setClient(c);
        atr2.setApprovedScope(Collections.singletonList("a"));
        atr2.setSubject(new TestingUserSubject(c.getResourceOwnerSubject().getLogin()));
        ServerAccessToken at2 = getProvider().createAccessToken(atr2);
        validateAccessToken(at2);
        at2 = getProvider().getAccessToken(at2.getTokenKey());
        validateAccessToken(at2);

        assertNotNull(at.getSubject().getId());
        assertTrue(at.getSubject() instanceof UserSubject);
        assertNotNull(at2.getSubject().getId());
        assertTrue(at2.getSubject() instanceof TestingUserSubject);
        assertEquals(at.getSubject().getLogin(), at2.getSubject().getLogin());
        assertNotEquals(at.getSubject().getId(), at2.getSubject().getId());
    }

    @Test
    public void testRevokeAccessTokenIdorAcrossUsersOfSharedClient() {
        // A multi-user shared client (e.g. a public mobile app with a known client_id).
        // Its resourceOwnerSubject is intentionally null — many users share this client.
        Client sharedClient = new Client();
        sharedClient.setRedirectUris(Collections.singletonList("http://client/redirect"));
        sharedClient.setClientId("103");
        sharedClient.setClientSecret("secret");
        getProvider().setClient(sharedClient);

        // Alice obtains an access token via the shared client.
        AccessTokenRegistration aliceReg = new AccessTokenRegistration();
        aliceReg.setClient(sharedClient);
        aliceReg.setApprovedScope(Collections.singletonList("a"));
        aliceReg.setSubject(new UserSubject("alice"));
        ServerAccessToken aliceToken = getProvider().createAccessToken(aliceReg);
        assertNotNull(getProvider().getAccessToken(aliceToken.getTokenKey()));

        // Bob obtains a separate access token via the same shared client.
        AccessTokenRegistration bobReg = new AccessTokenRegistration();
        bobReg.setClient(sharedClient);
        bobReg.setApprovedScope(Collections.singletonList("a"));
        bobReg.setSubject(new UserSubject("bob"));
        ServerAccessToken bobToken = getProvider().createAccessToken(bobReg);
        assertNotNull(getProvider().getAccessToken(bobToken.getTokenKey()));

        // IDOR attempt: Bob (authenticated only as the shared client) submits Alice's token
        // key to the revocation endpoint. The subject-aware revokeToken overload is called
        // with Bob's identity, which must not match Alice's subject.
        // The data provider must reject this with OAuthServiceException; the HTTP layer
        // (TokenRevocationService) swallows it per RFC 7009 and returns 200 anyway.
        try {
            getProvider().revokeToken(sharedClient, new UserSubject("bob"), aliceToken.getTokenKey(),
                                      OAuthConstants.ACCESS_TOKEN);
            // Reaching here means the cross-user revocation was NOT blocked — fail the test.
            fail(
                "IDOR: revokeToken should have thrown OAuthServiceException when Bob tried to "
                + "revoke Alice's token, but it succeeded silently"
            );
        } catch (OAuthServiceException expected) {
            // Expected: the subject mismatch guard correctly rejected the request.
        }

        // Alice's token must still be intact after the blocked revocation attempt.
        assertNotNull(
            "Alice's token must survive a cross-user revocation attempt",
            getProvider().getAccessToken(aliceToken.getTokenKey())
        );

        // Bob's own token must remain unaffected regardless.
        assertNotNull(getProvider().getAccessToken(bobToken.getTokenKey()));
    }

    @Test
    public void testAddGetDeleteRefreshToken() {
        Client c = addClient("101", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Arrays.asList("a", "refreshToken"));
        atr.setSubject(c.getResourceOwnerSubject());

        ServerAccessToken at = getProvider().createAccessToken(atr);
        validateAccessToken(at);
        ServerAccessToken at2 = getProvider().getAccessToken(at.getTokenKey());
        validateAccessToken(at2);
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

    protected Client addClient(String clientId, String userLogin) {
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

    protected void tearDownClient(String clientId) {
        if (getProvider() == null) {
            return;
        }
        Client client = getProvider().getClient(clientId);
        if (client != null) {
            List<RefreshToken> refreshTokens = getProvider().getRefreshTokens(client, null);
            for (RefreshToken refreshToken : refreshTokens) {
                getProvider().revokeToken(client, refreshToken.getTokenKey(), refreshToken.getTokenType());
            }
            List<ServerAccessToken> accessTokens = getProvider().getAccessTokens(client, null);
            for (ServerAccessToken accessToken : accessTokens) {
                getProvider().revokeToken(client, accessToken.getTokenKey(), accessToken.getTokenType());
            }
            getProvider().removeClient(clientId);
        }
    }

    protected void tearDownClients() {
        tearDownClient("101");
        tearDownClient("102");
        tearDownClient("103");
        tearDownClient("12345");
        tearDownClient("56789");
        tearDownClient("09876");
    }

    @After
    public void tearDown() throws Exception {
        tearDownClients();
        if (getProvider() != null) {
            getProvider().close();
        }
    }

    private void validateAccessToken(ServerAccessToken accessToken) {
        if (getProvider().isUseJwtFormatForAccessTokens()) {
            JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(accessToken.getTokenKey());
            JwtToken jwt = jwtConsumer.getJwtToken();

            // Validate claims
            assertNotNull(jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
            assertNotNull(jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));

            assertTrue(jwtConsumer.verifySignatureWith(keyPair.getPublic(), SignatureAlgorithm.RS256));
        }
    }

}
