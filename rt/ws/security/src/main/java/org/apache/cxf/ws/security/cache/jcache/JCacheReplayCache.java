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

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import javax.cache.spi.CachingProvider;

import org.apache.cxf.rt.security.utils.JCacheUtils;
import org.apache.wss4j.common.cache.EHCacheExpiry;
import org.apache.wss4j.common.cache.EHCacheValue;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * An in-memory JCache based implementation of the ReplayCache interface.
 * The default TTL is 60 minutes and the max TTL is 12 hours. With Ehcache (the same copy that CXF sees),
 * each identifier is held until its own expiry, up to the max TTL; with other providers, every identifier
 * is held for the default TTL.
 */
class JCacheReplayCache implements ReplayCache {
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(JCacheReplayCache.class);
    private static final String EHCACHE_PROVIDER = "org.ehcache.jsr107.EhcacheCachingProvider";
    private static final String[] EHCACHE_CLASSES = {
        "org.ehcache.jsr107.Eh107Configuration", "org.ehcache.expiry.ExpiryPolicy"
    };

    private final Cache<String, EHCacheValue> cache;
    private final CacheManager cacheManager;
    private final Clock clock;
    private final boolean entryExpiry;
    private boolean closed;

    JCacheReplayCache(String key) throws WSSecurityException {
        this(key, 10000);
    }

    JCacheReplayCache(String key, long heapEntries) throws WSSecurityException {
        this(key, heapEntries, Clock.systemUTC());
    }

    JCacheReplayCache(String key, long heapEntries, Clock clock) throws WSSecurityException {
        this(key, clock, null);
    }

    /**
     * @param ehcacheExpiry whether to use Ehcache's per-entry expiry, or null to use it if the provider is
     *                      the same copy of Ehcache that CXF sees
     */
    JCacheReplayCache(String key, Clock clock, Boolean ehcacheExpiry) throws WSSecurityException {
        Objects.requireNonNull(key);
        this.clock = Objects.requireNonNull(clock);

        // Do some checking on the arguments
        try {
            final CachingProvider cachingProvider = Caching.getCachingProvider();
            cacheManager = cachingProvider.getCacheManager();
            Cache<String, EHCacheValue> entryExpiryCache = null;
            if (ehcacheExpiry != null ? ehcacheExpiry : isSameEhcache(cachingProvider)) {
                try {
                    entryExpiryCache = JCacheUtils.getOrCreate(cacheManager, key, String.class, EHCacheValue.class,
                        () -> EhcacheReplayExpiry.configuration(clock));
                } catch (LinkageError | IllegalArgumentException | ClassCastException ex) {
                    LOG.warn("Ehcache per-entry expiry is not available for the replay cache, every identifier "
                        + "is held for the default TTL: {}", ex.toString());
                }
            }
            cache = entryExpiryCache != null ? entryExpiryCache : JCacheUtils.getOrCreate(cacheManager, key,
                String.class, EHCacheValue.class, JCacheReplayCache::defaultTtlConfiguration);
            // the cache may already exist with another configuration, so check the one it has
            entryExpiry = entryExpiryCache != null && EhcacheReplayExpiry.isUsedBy(entryExpiryCache);
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

        cache.put(identifier, value(identifier, expiry, clock.instant()));
    }

    /**
     * Add the given identifier to the cache if, and only if, it is not already contained in it, as a
     * single atomic operation. This overrides ReplayCache.addIfAbsent, whose default implementation is
     * only atomic within a single JVM, whereas a JCache provider may be distributed.
     * @param identifier The identifier to be added
     * @param expiry A custom expiry time for the identifier. Can be null in which case, the default
     *               expiry is used.
     * @return true if the identifier was added, false if it was already contained in the cache or is
     *         null or empty
     */
    public boolean addIfAbsent(String identifier, Instant expiry) {
        if (identifier == null || identifier.length() == 0) {
            return false;
        }

        Instant now = clock.instant();
        return cache.invoke(identifier, new AddIfAbsent(), value(identifier, expiry, now), now);
    }

    /**
     * Return true if the given identifier is contained in the cache
     * @param identifier The identifier to check
     */
    public boolean contains(String identifier) {
        return get(identifier) != null;
    }

    /**
     * Return the maximum time for which this cache holds an identifier. WSS4J uses this to reject a
     * SAML OneTimeUse Assertion that is valid for longer, as it could be replayed once its identifier
     * had expired from the cache. With Ehcache's per-entry expiry an identifier is held until its own
     * expiry, up to the max TTL; otherwise it is held for the default TTL. This implements
     * ReplayCache.getMaxTTL() in versions of WSS4J that define it, so it is declared without @Override.
     */
    public java.time.Duration getMaxTTL() {
        return java.time.Duration.ofSeconds(entryExpiry ? EHCacheExpiry.MAX_TTL : EHCacheExpiry.DEFAULT_TTL);
    }

    // Only exposed for testing
    EHCacheValue get(String identifier) {
        return cache.invoke(identifier, new ReadEntry(), clock.instant());
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            JCacheUtils.release(cache);
        }
    }

    /**
     * Entry processors enforce the expiry of each identifier atomically. A JCache expiry policy cannot
     * inspect values, so without Ehcache's per-entry expiry every identifier is held for the default TTL
     * from when it was last written (an identifier with a shorter expiry is treated as absent once it has
     * passed), and getMaxTTL reports the default TTL.
     */
    private static Configuration<String, EHCacheValue> defaultTtlConfiguration() {
        return new MutableConfiguration<String, EHCacheValue>()
            .setTypes(String.class, EHCacheValue.class)
            .setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(
                new Duration(TimeUnit.SECONDS, EHCacheExpiry.DEFAULT_TTL)));
    }

    /**
     * Whether the provider is Ehcache, loaded from the same classes as CXF sees: otherwise (an unwired
     * optional OSGi import, or Ehcache in an application with CXF in a shared library) CXF cannot build an
     * Ehcache configuration the provider accepts.
     */
    static boolean isSameEhcache(CachingProvider provider) {
        if (!EHCACHE_PROVIDER.equals(provider.getClass().getName())) {
            return false;
        }
        for (String name : EHCACHE_CLASSES) {
            if (!isSameClass(name, JCacheReplayCache.class.getClassLoader(), provider.getClass().getClassLoader())) {
                return false;
            }
        }
        return true;
    }

    static boolean isSameClass(String name, ClassLoader ours, ClassLoader theirs) {
        try {
            return Class.forName(name, false, ours) == Class.forName(name, false, theirs);
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    private EHCacheValue value(String identifier, Instant expiry, Instant now) {
        // as EHCacheExpiry does, an expiry that is not in the future gets the default TTL
        if (expiry == null || !expiry.isAfter(now) || expiry.isAfter(now.plusSeconds(EHCacheExpiry.MAX_TTL))) {
            expiry = now.plusSeconds(EHCacheExpiry.DEFAULT_TTL);
        }
        return new EHCacheValue(identifier, expiry);
    }

    private static boolean expired(EHCacheValue value, Instant now) {
        return value.getExpiry() != null && !value.getExpiry().isAfter(now);
    }

    private static final class ReadEntry implements EntryProcessor<String, EHCacheValue, EHCacheValue>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public EHCacheValue process(MutableEntry<String, EHCacheValue> entry, Object... arguments) {
            if (!entry.exists()) {
                return null;
            }
            EHCacheValue value = entry.getValue();
            if (expired(value, (Instant)arguments[0])) {
                entry.remove();
                return null;
            }
            return value;
        }
    }

    private static final class AddIfAbsent implements EntryProcessor<String, EHCacheValue, Boolean>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public Boolean process(MutableEntry<String, EHCacheValue> entry, Object... arguments) {
            if (entry.exists() && !expired(entry.getValue(), (Instant)arguments[1])) {
                return false;
            }
            entry.setValue((EHCacheValue)arguments[0]);
            return true;
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
