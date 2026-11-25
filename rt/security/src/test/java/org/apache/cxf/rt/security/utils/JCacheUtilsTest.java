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
package org.apache.cxf.rt.security.utils;

import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JCacheUtilsTest {
    @Test
    public void testConfiguredManagerClosedOnLastRelease() throws Exception {
        CachingProvider provider = Caching.getCachingProvider();
        URI config = JCacheUtilsTest.class.getResource("/jcache-utils-test.xml").toURI();
        ClassLoader loader = new URLClassLoader(new URL[0], JCacheUtilsTest.class.getClassLoader());
        CacheManager first = JCacheUtils.getCacheManager(provider, config, loader);
        CacheManager second = JCacheUtils.getCacheManager(provider, config, loader);
        assertSame(first, second);
        JCacheUtils.releaseCacheManager(first);
        assertFalse(second.isClosed());
        JCacheUtils.releaseCacheManager(second);
        assertTrue(second.isClosed());
    }

    @Test
    public void testDefaultManagerIsNeverClosed() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager manager = JCacheUtils.getCacheManager(provider, provider.getDefaultURI(),
            provider.getDefaultClassLoader());
        JCacheUtils.releaseCacheManager(manager);
        assertFalse(manager.isClosed());
    }

    @Test
    public void testCacheDestroyedByOthersIsCreatedAgain() {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        String name = UUID.randomUUID().toString();
        Cache<String, String> destroyed = JCacheUtils.getOrCreate(manager, name, String.class, String.class);
        manager.destroyCache(name);
        Cache<String, String> created = JCacheUtils.getOrCreate(manager, name, String.class, String.class);
        assertNotSame(destroyed, created);
        created.put("key", "value");
        JCacheUtils.release(destroyed);
        assertEquals("value", created.get("key"));
        JCacheUtils.release(created);
        assertNull(manager.getCache(name));
    }

    @Test
    public void testSharedCacheSurvivesUntilLastRelease() {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        String name = UUID.randomUUID().toString();
        Cache<String, String> first = JCacheUtils.getOrCreate(manager, name, String.class, String.class);
        Cache<String, String> second = JCacheUtils.getOrCreate(manager, name, String.class, String.class);
        first.put("key", "value");
        JCacheUtils.release(first);
        try {
            assertEquals("value", second.get("key"));
            assertFalse(manager.isClosed());
        } finally {
            JCacheUtils.release(second);
        }
        assertNull(manager.getCache(name));
        assertFalse(manager.isClosed());
    }

    @Test
    public void testExternallyCreatedCacheRemainsOwnedByCaller() {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        String name = UUID.randomUUID().toString();
        Cache<String, String> external = manager.createCache(name,
            new MutableConfiguration<String, String>().setTypes(String.class, String.class));
        Cache<String, String> acquired = JCacheUtils.getOrCreate(manager, name, String.class, String.class);
        external.put("key", "value");
        JCacheUtils.release(acquired);
        try {
            assertEquals("value", external.get("key"));
            assertFalse(manager.isClosed());
        } finally {
            manager.destroyCache(name);
        }
    }
}
