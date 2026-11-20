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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

/**
 * Shared ownership of JCache caches and managers. A cache is destroyed when the last user that
 * obtained it here releases it, if it was created here. A manager obtained here for a configuration
 * URI is closed when its last user releases it; the provider's default manager may be shared with
 * the application, so it is never closed.
 */
public final class JCacheUtils {
    private static final Map<Cache<?, ?>, Reference> CACHES = new IdentityHashMap<>();
    private static final Map<CacheManager, Reference> MANAGERS = new IdentityHashMap<>();

    private JCacheUtils() {
    }

    /** Release the manager once with releaseCacheManager. */
    public static synchronized CacheManager getCacheManager(CachingProvider provider, URI uri,
                                                            ClassLoader classLoader) {
        purgeClosed();
        CacheManager manager = provider.getCacheManager(uri, classLoader);
        MANAGERS.computeIfAbsent(manager, m -> new Reference(m, null, !uri.equals(provider.getDefaultURI())))
            .users++;
        return manager;
    }

    public static synchronized void releaseCacheManager(CacheManager manager) {
        Reference reference = MANAGERS.get(manager);
        if (reference != null && --reference.users == 0) {
            MANAGERS.remove(manager);
            if (reference.owned && !manager.isClosed()) {
                manager.close();
            }
            purgeClosed();
        }
    }

    public static <K, V> Cache<K, V> getOrCreate(CacheManager manager, String name,
                                               Class<K> keyType, Class<V> valueType) {
        return getOrCreate(manager, name, keyType, valueType, Function.identity());
    }

    public static <K, V> Cache<K, V> getOrCreate(CacheManager manager, String name,
            Class<K> keyType, Class<V> valueType,
            Function<MutableConfiguration<K, V>, MutableConfiguration<K, V>> customizer) {
        return getOrCreate(manager, name, keyType, valueType,
            () -> customizer.apply(new MutableConfiguration<K, V>().setTypes(keyType, valueType)));
    }

    /** Release the cache once with release. The configuration is only used to create the cache. */
    public static synchronized <K, V> Cache<K, V> getOrCreate(CacheManager manager, String name,
            Class<K> keyType, Class<V> valueType, Supplier<? extends Configuration<K, V>> configuration) {
        purgeClosed();
        Cache<K, V> cache = manager.getCache(name, keyType, valueType);
        boolean owned = cache == null;
        if (owned) {
            cache = manager.createCache(name, configuration.get());
        }
        CACHES.computeIfAbsent(cache, c -> new Reference(manager, name, owned)).users++;
        return cache;
    }

    public static synchronized void release(Cache<?, ?> cache) {
        Reference reference = CACHES.get(cache);
        if (reference != null && --reference.users == 0) {
            CACHES.remove(cache);
            if (reference.owned && !reference.manager.isClosed() && !cache.isClosed()) {
                reference.manager.destroyCache(reference.name);
            }
        }
    }

    // forget the caches and managers that were closed or destroyed by others
    private static void purgeClosed() {
        CACHES.keySet().removeIf(Cache::isClosed);
        MANAGERS.keySet().removeIf(CacheManager::isClosed);
    }

    private static final class Reference {
        private final CacheManager manager;
        private final String name;
        private final boolean owned;
        private int users;

        Reference(CacheManager manager, String name, boolean owned) {
            this.manager = manager;
            this.name = name;
            this.owned = owned;
        }
    }
}
