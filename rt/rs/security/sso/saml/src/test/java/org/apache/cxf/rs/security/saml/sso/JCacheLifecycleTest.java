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
package org.apache.cxf.rs.security.saml.sso;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

import org.apache.cxf.rs.security.saml.sso.jcache.JCacheTokenReplayCache;
import org.apache.cxf.rs.security.saml.sso.state.ResponseState;
import org.apache.cxf.rs.security.saml.sso.state.jcache.JCacheSPStateManager;
import org.apache.wss4j.common.cache.EHCacheValue;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JCacheLifecycleTest {
    @Test
    public void testReplayAndStateCachesSurviveOtherUsersClosing() throws Exception {
        JCacheTokenReplayCache replay = new JCacheTokenReplayCache();
        JCacheTokenReplayCache sameReplay = new JCacheTokenReplayCache();
        JCacheSPStateManager state = new JCacheSPStateManager();
        JCacheSPStateManager sameState = new JCacheSPStateManager();
        try {
            replay.putId("retained");
            state.setResponseState("context", new ResponseState("assertion", null, "/", null,
                System.currentTimeMillis(), System.currentTimeMillis() + 300000));
            replay.close();
            replay.close();
            assertTrue(sameReplay.contains("retained"));
            assertNotNull(state.getResponseState("context"));
            state.close();
            state.close();
            assertNotNull(sameState.getResponseState("context"));
            assertTrue(sameReplay.contains("retained"));
        } finally {
            replay.close();
            sameReplay.close();
            state.close();
            sameState.close();
        }
    }

    /**
     * Closing the last user closes the manager of the configuration file, which releases its disk
     * store, so that a redeployed application (another class loader) can create it again.
     */
    @Test
    public void testLastCloseReleasesConfiguredManager() throws Exception {
        JCacheTokenReplayCache replay = new JCacheTokenReplayCache();
        JCacheSPStateManager state = new JCacheSPStateManager();
        replay.putId("first");
        replay.close();
        state.close();

        URL config = JCacheTokenReplayCache.class.getResource("/cxf-samlp-jcache.xml");
        ClassLoader redeployed = new URLClassLoader(new URL[0], JCacheTokenReplayCache.class.getClassLoader());
        CacheManager manager = Caching.getCachingProvider().getCacheManager(config.toURI(), redeployed);
        try {
            Cache<String, EHCacheValue> cache = manager.createCache("cxf.samlp.replay.cache",
                new MutableConfiguration<String, EHCacheValue>().setTypes(String.class, EHCacheValue.class));
            cache.put("second", new EHCacheValue("second", Instant.now().plusSeconds(60)));
            assertTrue(cache.containsKey("second"));
        } finally {
            manager.close();
        }
    }
}
