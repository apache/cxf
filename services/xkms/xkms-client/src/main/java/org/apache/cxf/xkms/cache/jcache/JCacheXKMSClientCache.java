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
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.spi.CachingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.ws.security.utils.JCacheSupport;
import org.apache.cxf.xkms.cache.XKMSCacheToken;
import org.apache.cxf.xkms.cache.XKMSClientCache;
import org.apache.cxf.xkms.cache.XKMSClientCacheException;

/**
 * An in-memory JCache implementation of the XKMSClientCache interface.
 */
public class JCacheXKMSClientCache implements XKMSClientCache, BusLifeCycleListener {
    private final Cache<String, XKMSCacheToken> cache;
    private final CacheManager cacheManager;
    private final Bus bus;
    private final String cacheKey;

    public JCacheXKMSClientCache() throws XKMSClientCacheException {
        this(null);
    }

    public JCacheXKMSClientCache(Bus cxfBus) throws XKMSClientCacheException {
        if (cxfBus == null) {
            cxfBus = BusFactory.getThreadDefaultBus(true);
        }
        if (cxfBus != null) {
            cxfBus.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }

        this.bus = cxfBus;
        this.cacheKey = UUID.randomUUID().toString();

        final CachingProvider cachingProvider = Caching.getCachingProvider();
        cacheManager = cachingProvider.getCacheManager(); 

        cache = JCacheSupport.getOrCreate(cacheManager, cacheKey, String.class, XKMSCacheToken.class,
            cacheConfiguration -> cacheConfiguration.setExpiryPolicyFactory(() -> new ExpiryPolicy() {
                @Override
                public Duration getExpiryForCreation() {
                    return new Duration(TimeUnit.SECONDS, 3600);
                }

                @Override
                public Duration getExpiryForAccess() {
                    return null;
                }

                @Override
                public Duration getExpiryForUpdate() {
                    return new Duration(TimeUnit.SECONDS, 3600);
                }
            }).setStoreByValue(false));

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
        if (!cacheManager.isClosed()) {
            cacheManager.destroyCache(cacheKey);
            cacheManager.close();

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
