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
import java.time.Instant;
import java.util.Random;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.wss4j.common.cache.EHCacheReplayCache;
import org.apache.wss4j.common.cache.EHCacheValue;
import org.apache.wss4j.common.util.Loader;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;

/**
 * An in-memory EHCache implementation of the TokenReplayCache interface.
 * The default TTL is 60 minutes and the max TTL is 12 hours.
 */

public class EHCacheTokenReplayCache implements TokenReplayCache<String> {

    public static final String CACHE_KEY = "cxf.samlp.replay.cache";

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(EHCacheReplayCache.class);
    private static final String DEFAULT_CONFIG_URL = "/cxf-samlp-ehcache.xml";

    private final Cache<String, EHCacheValue> cache;
    private final CacheManager cacheManager;

    public EHCacheTokenReplayCache()
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this(DEFAULT_CONFIG_URL, null);
    }

    public EHCacheTokenReplayCache(Bus bus)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this(DEFAULT_CONFIG_URL, bus);
    }

    public EHCacheTokenReplayCache(String configFile)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this(configFile, null);
    }

    public EHCacheTokenReplayCache(String configFile, Bus bus)
            throws IllegalAccessException, ClassNotFoundException, InstantiationException {
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

        XmlConfiguration xmlConfig = new XmlConfiguration(getConfigFileURL(configFileURL));
        CacheConfigurationBuilder<String, EHCacheValue> configurationBuilder =
                xmlConfig.newCacheConfigurationBuilderFromTemplate(CACHE_KEY,
                        String.class, EHCacheValue.class);
        // Note, we don't require strong random values here
        String diskKey = CACHE_KEY + "-" + Math.abs(new Random().nextInt());
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache(CACHE_KEY, configurationBuilder)
                .with(CacheManagerBuilder.persistence(new File(System.getProperty("java.io.tmpdir"), diskKey))).build();

        cacheManager.init();
        cache = cacheManager.getCache(CACHE_KEY, String.class, EHCacheValue.class);

    }

    private URL getConfigFileURL(URL suppliedConfigFileURL) {
        if (suppliedConfigFileURL == null) {
            //using the default
            try {
                URL configFileURL = Loader.getResource(DEFAULT_CONFIG_URL);
                if (configFileURL == null) {
                    configFileURL = new URL(DEFAULT_CONFIG_URL);
                }
                return configFileURL;
            } catch (IOException e) {
                // Do nothing
                LOG.debug(e.getMessage());
            }
        }
        return suppliedConfigFileURL;
    }

    /**
     * Add the given identifier to the cache. It will be cached for a default amount of time.
     * @param identifier The identifier to be added
     */
    public void putId(String identifier) {
        putId(identifier, null);
    }

    /**
     * Add the given identifier to the cache to be cached for the given time
     * @param identifier The identifier to be added
     * @param expiry A custom expiry time for the identifier. Can be null in which case, the default expiry is used.
     */
    public void putId(String identifier, Instant expiry) {
        if (identifier == null || "".equals(identifier)) {
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

    public synchronized void close() {
        if (cacheManager.getStatus() == Status.AVAILABLE) {
            cacheManager.removeCache(CACHE_KEY);
            cacheManager.close();
        }
    }


}
