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

package org.apache.cxf.xkms.cache;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.CachePersistenceException;
import org.ehcache.PersistentCacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in-memory EHCache implementation of the XKMSClientCache interface.
 */
public class EHCacheXKMSClientCache implements XKMSClientCache, BusLifeCycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(EHCacheXKMSClientCache.class);

    private final Cache<String, XKMSCacheToken> cache;
    private final CacheManager cacheManager;
    private final Bus bus;
    private final String cacheKey;
    private final Path diskstorePath;
    private final boolean persistent;

    public EHCacheXKMSClientCache() throws XKMSClientCacheException {
        this(null);
    }

    public EHCacheXKMSClientCache(Bus cxfBus) throws XKMSClientCacheException {
        this(cxfBus, null, 10L, 5000L, false);
    }

    public EHCacheXKMSClientCache(Bus cxfBus, Path diskstorePath, long diskSize,
                                  long heapEntries, boolean persistent) throws XKMSClientCacheException {
        // Do some checking on the arguments
        if (persistent && diskstorePath == null) {
            throw new NullPointerException();
        }
        if (diskstorePath != null && (diskSize < 5 || diskSize > 10000)) {
            throw new IllegalArgumentException("The diskSize parameter must be between 5 and 10000 (megabytes)");
        }
        if (heapEntries < 100) {
            throw new IllegalArgumentException("The heapEntries parameter must be greater than 100 (entries)");
        }

        if (cxfBus == null) {
            cxfBus = BusFactory.getThreadDefaultBus(true);
        }
        if (cxfBus != null) {
            cxfBus.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }

        this.bus = cxfBus;
        this.diskstorePath = diskstorePath;
        this.persistent = persistent;

        cacheKey = UUID.randomUUID().toString();

        ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(heapEntries, EntryUnit.ENTRIES);
        if (diskstorePath != null) {
            resourcePoolsBuilder = resourcePoolsBuilder.disk(diskSize, MemoryUnit.MB, persistent);
        }

        ExpiryPolicy<Object, Object> expiryPolicy =
                ExpiryPolicyBuilder.timeToLiveExpiration(Duration.of(3600, ChronoUnit.SECONDS));
        CacheConfigurationBuilder<String, XKMSCacheToken> configurationBuilder =
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class, XKMSCacheToken.class, resourcePoolsBuilder)
                        .withExpiry(expiryPolicy);

        if (diskstorePath != null) {
            cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                    .with(CacheManagerBuilder.persistence(diskstorePath.toFile()))
                    .withCache(cacheKey, configurationBuilder)
                    .build();
        } else {
            cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                    .withCache(cacheKey, configurationBuilder)
                    .build();
        }

        cacheManager.init();
        cache = cacheManager.getCache(cacheKey, String.class, XKMSCacheToken.class);

    }

    /**
     * Store an XKMSCacheToken in the Cache using the given key
     */
    public void put(String key, XKMSCacheToken cacheToken) {
        cache.put(key, cacheToken);
    }

    /**
     * Get an XKMSCacheToken from the cache matching the given key. Returns null if there
     * is no such XKMSCacheToken in the cache.
     */
    public XKMSCacheToken get(String key) {
        return cache.get(key);
    }

    public synchronized void close() {
        if (cacheManager.getStatus() == Status.AVAILABLE) {
            cacheManager.removeCache(cacheKey);
            cacheManager.close();

            if (!persistent && cacheManager instanceof PersistentCacheManager) {
                try {
                    ((PersistentCacheManager) cacheManager).destroy();
                } catch (CachePersistenceException e) {
                    LOG.debug("Error in shutting down persistent cache", e);
                }

                // As we're not using a persistent disk store, just delete it - it should be empty after calling
                // destroy above
                if (diskstorePath != null) {
                    File file = diskstorePath.toFile();
                    if (file.exists() && file.canWrite()) {
                        file.delete();
                    }
                }
            }

            if (bus != null) {
                bus.getExtension(BusLifeCycleManager.class).unregisterLifeCycleListener(this);
            }
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
