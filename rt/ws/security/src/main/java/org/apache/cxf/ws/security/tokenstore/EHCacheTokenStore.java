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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.security.cache.EHCacheManagerHolder;

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

        cacheManager = EHCacheManagerHolder.getCacheManager(bus, configFileURL);
        // Cannot overflow to disk as SecurityToken Elements can't be serialized
        CacheConfiguration cc = EHCacheManagerHolder.getCacheConfiguration(key, cacheManager);
        cc.overflowToDisk(false); //tokens not writable
        
        Ehcache newCache = new Cache(cc);
        cache = cacheManager.addCacheIfAbsent(newCache);
    }
    
    /**
     * Set a new (default) TTL value in seconds
     * @param newTtl a new (default) TTL value in seconds
     */
    public void setTTL(long newTtl) {
        ttl = newTtl;
    }
    
    /**
     * Get the (default) TTL value in seconds
     * @return the (default) TTL value in seconds
     */
    public long getTTL() {
        return ttl;
    }
    
    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            int parsedTTL = getTTL(token);
            if (parsedTTL > 0) {
                cache.put(new Element(token.getId(), token, false, parsedTTL, parsedTTL));
            }
        }
    }
    
    public void add(String identifier, SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(identifier)) {
            int parsedTTL = getTTL(token);
            if (parsedTTL > 0) {
                cache.put(new Element(identifier, token, false, parsedTTL, parsedTTL));
            }
        }
    }
    
    public void remove(String identifier) {
        if (!StringUtils.isEmpty(identifier) && cache.isKeyInCache(identifier)) {
            cache.remove(identifier);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getTokenIdentifiers() {
        return cache.getKeysWithExpiryCheck();
    }
    
    public Collection<SecurityToken> getExpiredTokens() {
        List<SecurityToken> expiredTokens = new ArrayList<SecurityToken>();
        @SuppressWarnings("unchecked")
        Iterator<String> ids = cache.getKeys().iterator();
        while (ids.hasNext()) {
            Element element = cache.get(ids.next());
            if (cache.isExpired(element)) {
                expiredTokens.add((SecurityToken)element.getObjectValue());
            }
        }
        return expiredTokens;
    }
    
    public SecurityToken getToken(String identifier) {
        Element element = cache.get(identifier);
        if (element != null && !cache.isExpired(element)) {
            return (SecurityToken)element.getObjectValue();
        }
        return null;
    }
    
    private int getTTL(SecurityToken token) {
        int parsedTTL = 0;
        if (token.getExpires() != null) {
            Date expires = token.getExpires();
            Date current = new Date();
            long expiryTime = (expires.getTime() - current.getTime()) / 1000L;
            if (expiryTime < 0) {
                return 0;
            }
            
            parsedTTL = (int)expiryTime;
            if (expiryTime != (long)parsedTTL || parsedTTL > MAX_TTL) {
                // Default to configured value
                parsedTTL = (int)ttl;
                if (ttl != (long)parsedTTL) {
                    // Fall back to 60 minutes if the default TTL is set incorrectly
                    parsedTTL = 3600;
                }
            }
        } else {
            // Default to configured value
            parsedTTL = (int)ttl;
            if (ttl != (long)parsedTTL) {
                // Fall back to 60 minutes if the default TTL is set incorrectly
                parsedTTL = 3600;
            }
        }
        return parsedTTL;
    }

    public void close() {
        if (cacheManager != null) {
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
