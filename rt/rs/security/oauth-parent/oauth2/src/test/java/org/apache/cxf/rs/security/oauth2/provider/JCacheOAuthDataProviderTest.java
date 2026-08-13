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

import java.util.Collections;
import java.util.List;

import javax.cache.Cache;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.expiry.Duration;

import org.apache.cxf.BusFactory;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JCacheOAuthDataProviderTest extends AbstractOAuthDataProviderTest {

    @Before
    public void setUp() throws Exception {
        JCacheOAuthDataProvider provider = new JCacheOAuthDataProvider();
        initializeProvider(provider);
        setProvider(provider);
    }

    @Test
    public void testAddGetExpiredAccessToken() throws InterruptedException {
        Client c = addClient("102", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        atr.setSubject(c.getResourceOwnerSubject());

        getProvider().setAccessTokenLifetime(2 /* 2 seconds */); 
        getProvider().createAccessToken(atr);
        List<ServerAccessToken> tokens = getProvider().getAccessTokens(c, null);
        assertNotNull(tokens);
        assertEquals(1, tokens.size());

        Thread.sleep(3000); /* 3 seconds, token definitely expires */
        tokens = getProvider().getAccessTokens(c, null);
        assertEquals(0, tokens.size());

        getProvider().removeClient(c.getClientId());
    }

    // Without an explicitly configured CacheTTLs, the access/refresh token caches stay eternal
    // at the JCache infrastructure level (backwards-compatible default).
    @Test
    public void testCachesAreEternalByDefault() {
        JCacheOAuthDataProvider provider = createIsolatedProvider("eternal", JCacheOAuthDataProvider.CacheTTLs.ETERNAL);

        assertEquals(Duration.ETERNAL, expiryForCreation(rawAccessTokenCache(provider, "eternal")));
        assertEquals(Duration.ETERNAL, expiryForCreation(rawRefreshTokenCache(provider, "eternal")));
    }

    // An access token that is never looked up again must still be reaped by the JCache
    // infrastructure once the configured cache TTL elapses, independently of any
    // application-level expiry check (CWE-400: otherwise abandoned entries never get evicted).
    @Test
    public void testUnaccessedExpiredAccessTokenEvictedFromCacheWhenTTLConfigured() throws InterruptedException {
        JCacheOAuthDataProvider provider = createIsolatedProvider(
            "ttl", new JCacheOAuthDataProvider.CacheTTLs(1 /* access token cache TTL, seconds */, -1));

        Client c = addClient(provider, "103", "bob");
        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Collections.singletonList("a"));
        atr.setSubject(c.getResourceOwnerSubject());
        // long-lived at the app level: proves the eviction is cache-driven, not isExpired()-driven
        provider.setAccessTokenLifetime(3600);
        ServerAccessToken at = provider.createAccessToken(atr);

        Thread.sleep(2000); /* entry is never touched during this window */

        assertNull("entry should have been evicted at the cache-infrastructure level",
                  rawAccessTokenCache(provider, "ttl").get(at.getTokenKey()));
    }

    // the CacheManager and its caches are shared per config file, so each test needs its own
    // cache names; the provider is deliberately not closed, as that would also close the
    // CacheManager still in use by the provider under test
    private static JCacheOAuthDataProvider createIsolatedProvider(String suffix,
                                                                  JCacheOAuthDataProvider.CacheTTLs ttls) {
        JCacheOAuthDataProvider provider = new JCacheOAuthDataProvider(
            JCacheOAuthDataProvider.DEFAULT_CONFIG_URL, BusFactory.getThreadDefaultBus(true),
            JCacheOAuthDataProvider.CLIENT_CACHE_KEY + '.' + suffix,
            JCacheOAuthDataProvider.ACCESS_TOKEN_CACHE_KEY + '.' + suffix,
            JCacheOAuthDataProvider.REFRESH_TOKEN_CACHE_KEY + '.' + suffix,
            false, ttls);
        initializeProvider(provider);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static Duration expiryForCreation(Cache<?, ?> cache) {
        CompleteConfiguration<?, ?> config = cache.getConfiguration(CompleteConfiguration.class);
        return config.getExpiryPolicyFactory().create().getExpiryForCreation();
    }

    private static Cache<String, ServerAccessToken> rawAccessTokenCache(JCacheOAuthDataProvider provider,
                                                                        String suffix) {
        return provider.cacheManager.getCache(
            JCacheOAuthDataProvider.ACCESS_TOKEN_CACHE_KEY + '.' + suffix, String.class, ServerAccessToken.class);
    }

    private static Cache<String, RefreshToken> rawRefreshTokenCache(JCacheOAuthDataProvider provider,
                                                                    String suffix) {
        return provider.cacheManager.getCache(
            JCacheOAuthDataProvider.REFRESH_TOKEN_CACHE_KEY + '.' + suffix, String.class, RefreshToken.class);
    }

    private static Client addClient(JCacheOAuthDataProvider provider, String clientId, String userLogin) {
        Client c = new Client();
        c.setRedirectUris(Collections.singletonList("http://client/redirect"));
        c.setClientId(clientId);
        c.setClientSecret("123");
        c.setResourceOwnerSubject(new org.apache.cxf.rs.security.oauth2.common.UserSubject(userLogin));
        provider.setClient(c);
        return c;
    }
}
