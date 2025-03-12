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

package org.apache.cxf.ws.security.cache.jcache;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.spi.CachingProvider;

import org.apache.cxf.ws.security.utils.JCacheSupport;
import org.apache.wss4j.common.cache.EHCacheExpiry;
import org.apache.wss4j.common.cache.EHCacheValue;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * An in-memory JCache based implementation of the ReplayCache interface.
 * The default TTL is 60 minutes and the max TTL is 12 hours.
 */
class JCacheReplayCache implements ReplayCache {
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(JCacheReplayCache.class);

    private final Cache<String, EHCacheValue> cache;
    private final CacheManager cacheManager;
    private final String key;

    JCacheReplayCache(String key) throws WSSecurityException {
        this(key, 10000);
    }

    JCacheReplayCache(String key, long heapEntries) throws WSSecurityException {
        this.key = Objects.requireNonNull(key);

        // Do some checking on the arguments
        try {
            final CachingProvider cachingProvider = Caching.getCachingProvider();
            cacheManager = cachingProvider.getCacheManager(); 
            cache = JCacheSupport.getOrCreate(cacheManager, key, String.class, EHCacheValue.class,
                cacheConfiguration -> cacheConfiguration.setExpiryPolicyFactory(() -> new ExpiryPolicy() {
                    @Override
                    public Duration getExpiryForCreation() {
                        return new Duration(TimeUnit.SECONDS, EHCacheExpiry.DEFAULT_TTL);
                    }
                    
                    @Override
                    public Duration getExpiryForAccess() {
                        return null;
                    }
                    
                    @Override
                    public Duration getExpiryForUpdate() {
                        return null;
                    }
                }));
        } catch (Exception ex) {
            LOG.error("Error configuring JCacheReplayCache: {}", ex.getMessage());
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "replayCacheError");
        }
    }

    /**
     * Add the given identifier to the cache. It will be cached for a default amount of time.
     * @param identifier The identifier to be added
     */
    public void add(String identifier) {
        add(identifier, null);
    }

    /**
     * Add the given identifier to the cache to be cached for the given time
     * @param identifier The identifier to be added
     * @param expiry A custom expiry time for the identifier. Can be null in which case, the default expiry is used.
     */
    public void add(String identifier, Instant expiry) {
        if (identifier == null || identifier.length() == 0) {
            return;
        }

        cache.put(identifier, new EHCacheValue(identifier, expiry));
    }

    /**
     * Return true if the given identifier is contained in the cache
     * @param identifier The identifier to check
     */
    public boolean contains(String identifier) {
        if (cache == null) {
            return false;
        }
        EHCacheValue element = cache.get(identifier);
        return element != null;
    }

    // Only exposed for testing
    EHCacheValue get(String identifier) {
        return cache.get(identifier);
    }

    @Override
    public synchronized void close() {
        if (!cacheManager.isClosed()) {
            cacheManager.destroyCache(key);
            cacheManager.close();
        }
    }

    public void initComplete() {
    }

    public void preShutdown() {
        close();
    }

    public void postShutdown() {
        close();
    }
}
