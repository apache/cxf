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
import org.apache.cxf.common.classloader.ClassLoaderUtils;

/**
 * An in-memory EHCache implementation of the XKMSClientCache interface. 
 */
public class EHCacheXKMSClientCache implements XKMSClientCache {
    
    public static final String CACHE_KEY = "cxf.xkms.client.cache";
    private static final String DEFAULT_CONFIG_URL = "cxf-xkms-client-ehcache.xml";
    
    private Ehcache cache;
    private CacheManager cacheManager;
    
    public EHCacheXKMSClientCache() {
        this(DEFAULT_CONFIG_URL, null);
    }
    
    public EHCacheXKMSClientCache(Bus bus) {
        this(DEFAULT_CONFIG_URL, bus);
    }
    
    public EHCacheXKMSClientCache(String configFileURL) {
        this(configFileURL, null);
    }
    
    public EHCacheXKMSClientCache(String configFileURL, Bus bus) {
        createCache(configFileURL, bus);
    }
    
    private void createCache(String configFile, Bus bus) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus(true);
        }
        URL configFileURL = null;
        try {
            configFileURL = 
                ClassLoaderUtils.getResource(configFile, EHCacheXKMSClientCache.class);
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
     * Store an XKMSCacheToken in the Cache using the given key
     */
    public void put(String key, XKMSCacheToken cacheToken) {
        cache.put(new Element(key, cacheToken, false));
    }
    
    /**
     * Get an XKMSCacheToken from the cache matching the given key. Returns null if there
     * is no such XKMSCacheToken in the cache, or if the certificate has expired in the cache
     */
    public XKMSCacheToken get(String key) {
        Element element = cache.get(key);
        if (element != null && !element.isExpired()) {
            return (XKMSCacheToken)element.getObjectValue();
        }
        return null;
    }
    
    public void close() throws IOException {
        if (cacheManager != null) {
            if (cache != null) {
                cache.removeAll();
            }
            cacheManager.shutdown();
            cacheManager = null;
            cache = null;
        }
    }
    
}
