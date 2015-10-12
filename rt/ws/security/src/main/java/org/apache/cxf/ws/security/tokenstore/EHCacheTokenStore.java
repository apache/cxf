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

package org.apache.cxf.ws.security.tokenstore;

import java.io.Closeable;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.security.cache.EHCacheUtils;
import org.apache.wss4j.common.cache.EHCacheManagerHolder;

/**
 * An in-memory EHCache implementation of the TokenStore interface. The default TTL is 60 minutes
 * and the max TTL is 12 hours.
 */
public class EHCacheTokenStore implements TokenStore, Closeable, BusLifeCycleListener {
    public static final long DEFAULT_TTL = 3600L;
    public static final long MAX_TTL = DEFAULT_TTL * 12L;
    
    private Ehcache cache;
    private Bus bus;
    private CacheManager cacheManager;
    private long ttl = DEFAULT_TTL;
    
    public EHCacheTokenStore(String key, Bus b, URL configFileURL) {
        bus = b;
        if (bus != null) {
            b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }
        cacheManager = EHCacheUtils.getCacheManager(bus, configFileURL);
        // Cannot overflow to disk as SecurityToken Elements can't be serialized
        @SuppressWarnings("deprecation")
        CacheConfiguration cc = EHCacheManagerHolder.getCacheConfiguration(key, cacheManager)
            .overflowToDisk(false); //tokens not writable
        
        Cache newCache = new RefCountCache(cc);
        cache = cacheManager.addCacheIfAbsent(newCache);
        synchronized (cache) {
            if (cache.getStatus() != Status.STATUS_ALIVE) {
                cache = cacheManager.addCacheIfAbsent(newCache);
            }
            if (cache instanceof RefCountCache) {
                ((RefCountCache)cache).incrementAndGet();
            }
        }
        
        // Set the TimeToLive value from the CacheConfiguration
        ttl = cc.getTimeToLiveSeconds();
    }
    
    private static class RefCountCache extends Cache {
        AtomicInteger count = new AtomicInteger();
        RefCountCache(CacheConfiguration cc) {
            super(cc);
        }
        public int incrementAndGet() {
            return count.incrementAndGet();
        }
        public int decrementAndGet() {
            return count.decrementAndGet();
        }
    }
    
    /**
     * Set a new (default) TTL value in seconds
     * @param newTtl a new (default) TTL value in seconds
     */
    public void setTTL(long newTtl) {
        ttl = newTtl;
    }
    
    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            Element element = new Element(token.getId(), token, getTTL(), getTTL());
            element.resetAccessStatistics();
            cache.put(element);
        }
    }
    
    public void add(String identifier, SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(identifier)) {
            Element element = new Element(identifier, token, getTTL(), getTTL());
            element.resetAccessStatistics();
            cache.put(element);
        }
    }
    
    public void remove(String identifier) {
        if (cache != null && !StringUtils.isEmpty(identifier) && cache.isKeyInCache(identifier)) {
            cache.remove(identifier);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getTokenIdentifiers() {
        if (cache == null) {
            return null;
        }
        return cache.getKeysWithExpiryCheck();
    }
    
    public SecurityToken getToken(String identifier) {
        if (cache == null) {
            return null;
        }
        Element element = cache.get(identifier);
        if (element != null && !cache.isExpired(element)) {
            return (SecurityToken)element.getObjectValue();
        }
        return null;
    }
    
    private int getTTL() {
        int parsedTTL = (int)ttl;
        if (ttl != (long)parsedTTL) {
             // Fall back to 60 minutes if the default TTL is set incorrectly
            parsedTTL = 3600;
        }
        return parsedTTL;
    }

    public void close() {
        if (cacheManager != null) {
            // this step is especially important for global shared cache manager
            if (cache != null) {
                synchronized (cache) {
                    if (cache instanceof RefCountCache
                        && ((RefCountCache)cache).decrementAndGet() == 0) {
                        cacheManager.removeCache(cache.getName());
                    }
                }                
            }
            
            EHCacheManagerHolder.releaseCacheManger(cacheManager);
            cacheManager = null;
            cache = null;
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
