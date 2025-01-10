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
package org.apache.cxf.ws.security.utils;

import java.util.function.Function;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;

import org.apache.cxf.common.logging.LogUtils;

public final class JCacheUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JCacheUtils.class);
    private static final boolean JCACHE_INSTALLED;

    static {
        boolean jcacheInstalled = false;
        try {
            final Class<?> caching = Class.forName("javax.cache.Caching");
            if (caching != null) {
                jcacheInstalled = true;
            }
        } catch (Exception e) {
            LOG.fine("No JCache SPIs detected on classpath: " + e.getMessage());
        }
        JCACHE_INSTALLED = jcacheInstalled;
    }

    private JCacheUtils() {
    }
    
    public static <K, V> Cache<K, V> getOrCreate(CacheManager cacheManager, String name, Class<K> kclass,
            Class<V> vclass) {
        return getOrCreate(cacheManager, name, kclass, vclass, Function.identity());
    }

    public static <K, V> Cache<K, V> getOrCreate(CacheManager cacheManager, String name, Class<K> kclass,
            Class<V> vclass, Function<MutableConfiguration<K, V>, MutableConfiguration<K, V>> customizer) {

        final Cache<K, V> cache = cacheManager.getCache(name, kclass, vclass);
        if (cache != null) {
            return cache;
        }

        MutableConfiguration<K, V> cacheConfiguration = new MutableConfiguration<>();
        cacheConfiguration = customizer.apply(cacheConfiguration.setTypes(kclass, vclass));

        return cacheManager.createCache(name, cacheConfiguration);
    }

    public static boolean isJCacheInstalled() {
        return JCACHE_INSTALLED;
    }

}
