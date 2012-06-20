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

package org.apache.cxf.rs.security.saml.sso;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.apache.cxf.Bus;
import org.apache.ws.security.util.Loader;

/**
 * An in-memory EHCache implementation of the TokenReplayCache interface. 
 * The default TTL is 60 minutes and the max TTL is 12 hours.
 */
public class EHCacheTokenReplayCache implements TokenReplayCache<String> {
    
    public static final long DEFAULT_TTL = 3600L;
    public static final long MAX_TTL = DEFAULT_TTL * 12L;
    public static final String CACHE_KEY = "cxf-samlp-replay-cache";
    
    private Ehcache cache;
    private CacheManager cacheManager;
    private long ttl = DEFAULT_TTL;
    
    public EHCacheTokenReplayCache() {
        this((Bus)null);
    }
    
    public EHCacheTokenReplayCache(Bus bus) {
        String defaultConfigFile = "cxf-samlp-ehcache.xml";
        URL configFileURL = Loader.getResource(defaultConfigFile);
        createCache(configFileURL, bus);
    }
    
    public EHCacheTokenReplayCache(URL configFileURL) {
        createCache(configFileURL, null);
    }
    
    public EHCacheTokenReplayCache(URL configFileURL, Bus bus) {
        createCache(configFileURL, bus);
    }
    
    private void createCache(URL configFileURL, Bus bus) {
        if (configFileURL == null) {
            cacheManager = CacheManager.create();
        } else {
            Configuration conf = ConfigurationFactory.parseConfiguration(configFileURL);
            
            if (bus != null) {
                conf.setName(bus.getId());
                if ("java.io.tmpdir".equals(conf.getDiskStoreConfiguration().getOriginalPath())) {
                    String path = conf.getDiskStoreConfiguration().getPath() + File.separator
                        + bus.getId();
                    conf.getDiskStoreConfiguration().setPath(path);
                }
            }
            
            cacheManager = CacheManager.create(conf);
        }
        
        CacheConfiguration cc = EHCacheUtil.getCacheConfiguration(CACHE_KEY, cacheManager);
        
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
    
    /**
     * Add the given identifier to the cache. It will be cached for a default amount of time.
     * @param id The identifier to be added
     */
    public void putId(String id) {
        putId(id, ttl);
    }
    
    /**
     * Add the given identifier to the cache.
     * @param identifier The identifier to be added
     * @param timeToLive The length of time to cache the Identifier in seconds
     */
    public void putId(String id, long timeToLive) {
        if (id == null || "".equals(id)) {
            return;
        }
        
        int parsedTTL = (int)timeToLive;
        if (timeToLive != (long)parsedTTL || parsedTTL < 0 || parsedTTL > MAX_TTL) {
            // Default to configured value
            parsedTTL = (int)ttl;
            if (ttl != (long)parsedTTL) {
                // Fall back to 60 minutes if the default TTL is set incorrectly
                parsedTTL = 3600;
            }
        }
        
        cache.put(new Element(id, id, false, parsedTTL, parsedTTL));
    }
    
    /**
     * Return the given identifier if it is contained in the cache, otherwise null.
     * @param id The identifier to check
     */
    public String getId(String id) {
        Element element = cache.get(id);
        if (element != null) {
            if (cache.isExpired(element)) {
                cache.remove(id);
                return null;
            }
            return (String)element.getObjectValue();
        }
        return null;
    }

    public void close() throws IOException {
        if (cacheManager != null) {
            cacheManager.shutdown();
            cacheManager = null;
            cache = null;
        }
    }
    
}
