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

package org.apache.cxf.ws.security.cache;

import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.ws.security.cache.ReplayCache;

/**
 * An in-memory EHCache implementation of the ReplayCache interface.
 */
public class EHCacheReplayCache implements ReplayCache {
    
    public static final long DEFAULT_TTL = 3600L;
    private Cache cache;
    private CacheManager cacheManager;
    
    public EHCacheReplayCache(String key, URL configFileURL) {
        if (cacheManager == null) {
            if (configFileURL == null) {
                cacheManager = CacheManager.create();
            } else {
                cacheManager = CacheManager.create(configFileURL);
            }
        }
        if (!cacheManager.cacheExists(key)) {
            cache = new Cache(key, 50000, true, false, DEFAULT_TTL, DEFAULT_TTL);
            cacheManager.addCache(cache);
        } else {
            cache = cacheManager.getCache(key);
        }
    }
    
    /**
     * Add the given identifier to the cache. It will be cached for a default amount of time.
     * @param identifier The identifier to be added
     */
    public void add(String identifier) {
        add(identifier, DEFAULT_TTL);
    }
    
    /**
     * Add the given identifier to the cache to be cached for the given time
     * @param identifier The identifier to be added
     * @param timeToLive The length of time to cache the Identifier in seconds
     */
    public void add(String identifier, long timeToLive) {
        if (identifier == null || "".equals(identifier)) {
            return;
        }
        
        int ttl = (int)timeToLive;
        if (timeToLive != (long)ttl) {
            // Default to 60 minutes
            ttl = 3600;
        }
        
        cache.put(new Element(identifier, identifier, false, ttl, ttl));
    }
    
    /**
     * Return true if the given identifier is contained in the cache
     * @param identifier The identifier to check
     */
    public boolean contains(String identifier) {
        Element element = cache.get(identifier);
        if (element != null) {
            if (cache.isExpired(element)) {
                cache.remove(identifier);
                return false;
            }
            return true;
        }
        return false;
    }
    
}
