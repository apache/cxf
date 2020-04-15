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
import java.net.URL;
import java.util.UUID;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.wss4j.common.util.Loader;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;

/**
 * An in-memory EHCache implementation of the XKMSClientCache interface.
 */
public class EHCacheXKMSClientCache implements XKMSClientCache, BusLifeCycleListener {

    public static final String TEMPLATE_KEY = "cxf.xkms.client.cache";
    private static final String DEFAULT_CONFIG_URL = "cxf-xkms-client-ehcache.xml";

    private final Cache<String, XKMSCacheToken> cache;
    private final CacheManager cacheManager;
    private final Bus bus;
    private final String cacheKey;

    public EHCacheXKMSClientCache() throws XKMSClientCacheException {
        this(DEFAULT_CONFIG_URL, null);
    }

    public EHCacheXKMSClientCache(Bus cxfBus) throws XKMSClientCacheException {
        this(DEFAULT_CONFIG_URL, cxfBus);
    }

    public EHCacheXKMSClientCache(String configFileURL) throws XKMSClientCacheException {
        this(configFileURL, null);
    }

    public EHCacheXKMSClientCache(String configFile, Bus cxfBus) throws XKMSClientCacheException {
        if (cxfBus == null) {
            cxfBus = BusFactory.getThreadDefaultBus(true);
        }
        this.bus = cxfBus;
        if (bus != null) {
            bus.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }
        
        URL configFileURL = null;
        try {
            configFileURL =
                ClassLoaderUtils.getResource(configFile, EHCacheXKMSClientCache.class);
        } catch (Exception ex) {
            // ignore
        }
        if (configFileURL == null) {
            configFileURL = Loader.getResource(this.getClass().getClassLoader(), configFile);
        }

        XmlConfiguration xmlConfig = new XmlConfiguration(configFileURL);

        try {
            CacheConfigurationBuilder<String, XKMSCacheToken> configurationBuilder =
                    xmlConfig.newCacheConfigurationBuilderFromTemplate(TEMPLATE_KEY,
                            String.class, XKMSCacheToken.class);

            cacheKey = UUID.randomUUID().toString();

            cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache(cacheKey,
                    configurationBuilder)
                    .with(CacheManagerBuilder.persistence(new File(System.getProperty("java.io.tmpdir"), cacheKey)))
                    .build();

            cacheManager.init();
            cache = cacheManager.getCache(cacheKey, String.class, XKMSCacheToken.class);
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new XKMSClientCacheException(e);
        }
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

    public void close() {
        if (cacheManager.getStatus() == Status.AVAILABLE) {
            cacheManager.removeCache(cacheKey);
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
