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

package org.apache.cxf.rs.security.common.jcache;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.rs.security.common.TokenReplayCache;
import org.apache.wss4j.common.cache.EHCacheValue;
import org.apache.wss4j.common.util.Loader;

/**
 * A JCache based implementation of the TokenReplayCache interface. The default configuration uses
 * Ehcache as the JCache provider, with a default TTL of 60 minutes and a max TTL of 12 hours.
 */
public class JCacheTokenReplayCache implements TokenReplayCache<String> {

    public static final String CACHE_KEY = "cxf.rs.security.replay.cache";

    private static final Logger LOG = LogUtils.getL7dLogger(JCacheTokenReplayCache.class);
    private static final String DEFAULT_CONFIG_URL = "/cxf-rs-security-replay-jcache.xml";

    private final String cacheKey;
    private final Cache<String, EHCacheValue> cache;
    private final CacheManager cacheManager;

    public JCacheTokenReplayCache() throws URISyntaxException {
        this(DEFAULT_CONFIG_URL, null);
    }

    public JCacheTokenReplayCache(Bus bus) throws URISyntaxException {
        this(DEFAULT_CONFIG_URL, bus);
    }

    public JCacheTokenReplayCache(String configFile) throws URISyntaxException {
        this(configFile, null);
    }

    public JCacheTokenReplayCache(String configFile, Bus bus) throws URISyntaxException {
        this(CACHE_KEY, configFile, DEFAULT_CONFIG_URL, bus);
    }

    /**
     * @param cacheKey The name of the cache
     * @param configFile The JCache provider configuration file
     * @param defaultConfigFile The configuration file to fall back to if configFile cannot be loaded
     * @param bus The Bus used to load the configuration file. Can be null, in which case the thread
     *            default Bus is used.
     */
    protected JCacheTokenReplayCache(String cacheKey, String configFile, String defaultConfigFile, Bus bus)
        throws URISyntaxException {
        this.cacheKey = cacheKey;
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus(true);
        }
        URL configFileURL = null;
        try {
            configFileURL = ResourceUtils.getClasspathResourceURL(configFile, getClass(), bus);
        } catch (Exception ex) {
            // ignore
        }

        final CachingProvider cachingProvider = Caching.getCachingProvider();

        URL resolvedConfigFileURL = getConfigFileURL(configFileURL, defaultConfigFile);
        URI configURI = resolvedConfigFileURL != null
            ? resolvedConfigFileURL.toURI() : cachingProvider.getDefaultURI();
        cacheManager = cachingProvider.getCacheManager(configURI, getClass().getClassLoader());

        cache = getOrCreate(cacheManager, cacheKey, String.class, EHCacheValue.class);
    }

    private static <K, V> Cache<K, V> getOrCreate(CacheManager cacheManager, String name,
            Class<K> kclass, Class<V> vclass) {

        final Cache<K, V> cache = cacheManager.getCache(name, kclass, vclass);
        if (cache != null) {
            return cache;
        }

        final MutableConfiguration<K, V> cacheConfiguration = new MutableConfiguration<>();
        cacheConfiguration.setTypes(kclass, vclass);

        return cacheManager.createCache(name, cacheConfiguration);
    }

    private static URL getConfigFileURL(URL suppliedConfigFileURL, String defaultConfigFile) {
        if (suppliedConfigFileURL == null) {
            //using the default
            try {
                URL configFileURL = Loader.getResource(defaultConfigFile);
                if (configFileURL == null) {
                    configFileURL = new URL(defaultConfigFile);
                }
                return configFileURL;
            } catch (IOException e) {
                // Do nothing
                LOG.log(Level.FINE, e.getMessage(), e);
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
     * @param expiry A custom expiry time for the identifier. Can be null in which case, the default
     *               expiry is used.
     */
    public void putId(String identifier, Instant expiry) {
        if (identifier == null || "".equals(identifier)) {
            return;
        }

        cache.put(identifier, new EHCacheValue(identifier, expiry));
    }

    /**
     * Atomically add the given identifier to the cache if it is not already present
     * @param identifier The identifier to be added
     * @param expiry A custom expiry time for the identifier. Can be null in which case, the default
     *               expiry is used.
     * @return true if the identifier was added, false if it was already present (or is null or empty)
     */
    @Override
    public boolean putIdIfAbsent(String identifier, Instant expiry) {
        if (identifier == null || "".equals(identifier)) {
            return false;
        }

        return cache.putIfAbsent(identifier, new EHCacheValue(identifier, expiry));
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
        if (!cacheManager.isClosed()) {
            cacheManager.destroyCache(cacheKey);
            cacheManager.close();
        }
    }

}
