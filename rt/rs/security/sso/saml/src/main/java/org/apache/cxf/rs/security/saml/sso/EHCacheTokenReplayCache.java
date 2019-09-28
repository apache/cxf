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
import net.sf.ehcache.config.DiskStoreConfiguration;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

/**
 * An in-memory EHCache implementation of the TokenReplayCache interface.
 * The default TTL is 60 minutes and the max TTL is 12 hours.
 */
public class EHCacheTokenReplayCache implements TokenReplayCache<String> {

    public static final long DEFAULT_TTL = 3600L;
    public static final long MAX_TTL = DEFAULT_TTL * 12L;
    public static final String CACHE_KEY = "cxf.samlp.replay.cache";
    private static final String DEFAULT_CONFIG_URL = "/cxf-samlp-ehcache.xml";

    private Ehcache cache;
    private CacheManager cacheManager;
    private long ttl = DEFAULT_TTL;

    public EHCacheTokenReplayCache() {
        this(DEFAULT_CONFIG_URL, null);
    }

    public EHCacheTokenReplayCache(Bus bus) {
        this(DEFAULT_CONFIG_URL, bus);
    }

    public EHCacheTokenReplayCache(String configFileURL) {
        this(configFileURL, null);
    }

    public EHCacheTokenReplayCache(String configFileURL, Bus bus) {
        createCache(configFileURL, bus);
    }

    private void createCache(String configFile, Bus bus) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus(true);
        }
        URL configFileURL = null;
        try {
            configFileURL =
                ResourceUtils.getClasspathResourceURL(configFile, EHCacheTokenReplayCache.class, bus);
        } catch (Exception ex) {
            // ignore
        }
        if (configFileURL == null) {
            cacheManager = EHCacheUtil.createCacheManager();
        } else {
            Configuration conf = ConfigurationFactory.parseConfiguration(configFileURL);

            if (bus != null) {
                conf.setName(bus.getId());
                DiskStoreConfiguration dsc = conf.getDiskStoreConfiguration();
                if (dsc != null && "java.io.tmpdir".equals(dsc.getOriginalPath())) {
                    String path = conf.getDiskStoreConfiguration().getPath() + File.separator
                        + bus.getId();
                    conf.getDiskStoreConfiguration().setPath(path);
                }
            }

            cacheManager = EHCacheUtil.createCacheManager(conf);
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
        if (timeToLive != parsedTTL || parsedTTL < 0 || parsedTTL > MAX_TTL) {
            // Default to configured value
            parsedTTL = (int)ttl;
            if (ttl != parsedTTL) {
                // Fall back to 60 minutes if the default TTL is set incorrectly
                parsedTTL = 3600;
            }
        }
        Element element = new Element(id, id, parsedTTL, parsedTTL);
        element.resetAccessStatistics();
        cache.put(element);
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
