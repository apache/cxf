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

import java.net.URI;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.provider.JCacheOAuthDataProvider;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JCacheLifecycleTest {
    private static final String CONFIG = "cxf-oauth2-jcache.xml";

    @Test
    public void testSharedCodeAndOAuthProvidersSurviveClose() {
        JCacheCodeDataProvider first = new JCacheCodeDataProvider();
        JCacheCodeDataProvider same = new JCacheCodeDataProvider();
        JCacheOAuthDataProvider oauth = new JCacheOAuthDataProvider();
        try {
            Client client = new Client("shared-client", "secret", true);
            first.setClient(client);
            AuthorizationCodeRegistration registration = new AuthorizationCodeRegistration();
            registration.setClient(client);
            ServerAuthorizationCodeGrant grant = first.createCodeGrant(registration);
            first.close();
            first.close();
            assertNotNull(same.getClient("shared-client"));
            assertNotNull(same.getCodeGrant(grant.getCode()));
            assertNotNull(oauth.getClient("shared-client"));
            same.close();
            assertNotNull(oauth.getClient("shared-client"));
        } finally {
            first.close();
            same.close();
            oauth.close();
        }
    }

    /**
     * A constructor that fails after obtaining the manager of a configuration file releases it, so that it
     * is closed (and its disk store unlocked) rather than kept open.
     */
    @Test
    public void testFailedConstructionReleasesManager() throws Exception {
        CacheManager manager = conflictingCache("lifecycle-refresh");
        assertThrows(RuntimeException.class, () -> new JCacheOAuthDataProvider(CONFIG, null,
            "lifecycle-client", "lifecycle-access", "lifecycle-refresh"));
        assertTrue(manager.isClosed());

        manager = conflictingCache("lifecycle-code");
        assertThrows(RuntimeException.class, () -> new JCacheCodeDataProvider(CONFIG, null,
            "lifecycle-client", "lifecycle-code", "lifecycle-access", "lifecycle-refresh"));
        assertTrue(manager.isClosed());
    }

    // A request still in flight after close fails as on a closed cache, not with a NullPointerException
    @Test
    public void testUseAfterClose() {
        JCacheCodeDataProvider provider = new JCacheCodeDataProvider();
        provider.close();
        assertThrows(IllegalStateException.class, () -> provider.getCodeGrant("code"));
    }

    // a cache of the given name, with other types than the providers use, in their configuration's manager
    private static CacheManager conflictingCache(String name) throws Exception {
        URI uri = JCacheLifecycleTest.class.getResource("/" + CONFIG).toURI();
        CacheManager manager = Caching.getCachingProviders().iterator().next()
            .getCacheManager(uri, Thread.currentThread().getContextClassLoader());
        manager.createCache(name, new MutableConfiguration<Integer, Integer>().setTypes(Integer.class, Integer.class));
        return manager;
    }
}
