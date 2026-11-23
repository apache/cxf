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
package org.apache.cxf.xkms.cache.jcache;

import java.util.UUID;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JCacheXKMSClientCacheLifecycleTest {

    // The XKMS cache is in the provider's default manager, which other caches share, so closing it keeps
    // the manager and their caches open
    @Test
    public void testCloseLeavesDefaultManagerOpen() throws Exception {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        String name = "other-" + UUID.randomUUID();
        Cache<String, String> other =
            manager.createCache(name, new MutableConfiguration<String, String>().setTypes(String.class, String.class));
        try {
            JCacheXKMSClientCache xkmsCache = new JCacheXKMSClientCache();
            xkmsCache.close();
            xkmsCache.close();
            assertFalse(manager.isClosed());
            other.put("key", "value");
            assertEquals("value", other.get("key"));
        } finally {
            manager.destroyCache(name);
        }
    }
}
