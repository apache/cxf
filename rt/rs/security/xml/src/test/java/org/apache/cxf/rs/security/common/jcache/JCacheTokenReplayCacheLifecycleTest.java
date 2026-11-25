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
package org.apache.cxf.rs.security.common.jcache;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

import org.apache.wss4j.common.cache.EHCacheValue;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JCacheTokenReplayCacheLifecycleTest {
    @Test
    public void testReplayCacheSurvivesOtherUserClosing() throws Exception {
        JCacheTokenReplayCache replay = new JCacheTokenReplayCache();
        JCacheTokenReplayCache sameReplay = new JCacheTokenReplayCache();
        try {
            replay.putId("retained");
            replay.close();
            replay.close();
            assertTrue(sameReplay.contains("retained"));
            assertTrue(sameReplay.putIdIfAbsent("added", null));
            assertTrue(sameReplay.contains("added"));
        } finally {
            replay.close();
            sameReplay.close();
        }
    }

    /**
     * Closing the last user closes the manager of the configuration file, which releases its disk
     * store, so that a redeployed application (another class loader) can create it again.
     */
    @Test
    public void testLastCloseReleasesConfiguredManager() throws Exception {
        JCacheTokenReplayCache replay = new JCacheTokenReplayCache();
        replay.putId("first");
        replay.close();

        URL config = JCacheTokenReplayCache.class.getResource("/cxf-rs-security-replay-jcache.xml");
        ClassLoader redeployed = new URLClassLoader(new URL[0], JCacheTokenReplayCache.class.getClassLoader());
        CacheManager manager = Caching.getCachingProvider().getCacheManager(config.toURI(), redeployed);
        try {
            Cache<String, EHCacheValue> cache = manager.createCache(JCacheTokenReplayCache.CACHE_KEY,
                new MutableConfiguration<String, EHCacheValue>().setTypes(String.class, EHCacheValue.class));
            cache.put("second", new EHCacheValue("second", Instant.now().plusSeconds(60)));
            assertTrue(cache.containsKey("second"));
        } finally {
            manager.close();
        }
    }
}
