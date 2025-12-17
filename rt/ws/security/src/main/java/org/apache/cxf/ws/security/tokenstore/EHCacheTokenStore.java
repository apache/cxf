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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.core.util.ClassLoading;
import org.ehcache.xml.XmlConfiguration;

/**
 * An in-memory EHCache implementation of the TokenStore interface. The default TTL is 60 minutes
 * and the max TTL is 12 hours.
 */
public class EHCacheTokenStore implements TokenStore, Closeable, BusLifeCycleListener {
    private final Bus bus;
    private final Cache<String, SecurityToken> cache;
    private final CacheManager cacheManager;
    private final String key;

    public EHCacheTokenStore(String key, Bus b, URL configFileURL) throws TokenStoreException {
        bus = b;
        if (bus != null) {
            b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }

        this.key = key;
        try {
            // Exclude the endpoint info bit added in TokenStoreUtils when getting the template name
            String template = key;
            if (template.contains("-")) {
                template = key.substring(0, key.lastIndexOf('-'));
            }

            // Set class loader cache of template object to SecurityToken classloader
            Map<String, ClassLoader> cacheClassLoaders = new HashMap<>();
            cacheClassLoaders.put(template, SecurityToken.class.getClassLoader());
            XmlConfiguration xmlConfig = new XmlConfiguration(configFileURL, ClassLoading.getDefaultClassLoader(),
                    cacheClassLoaders);

            CacheConfigurationBuilder<String, SecurityToken> configurationBuilder =
                    xmlConfig.newCacheConfigurationBuilderFromTemplate(template,
                            String.class, SecurityToken.class);

            cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache(key, configurationBuilder).build();

            cacheManager.init();
            cache = cacheManager.getCache(key, String.class, SecurityToken.class);

        } catch (Exception e) {
            throw new TokenStoreException(e);
        }
    }

    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            cache.put(token.getId(), token);
        }
    }

    public void add(String identifier, SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(identifier)) {
            cache.put(identifier, token);
        }
    }

    public void remove(String identifier) {
        if (cache != null && !StringUtils.isEmpty(identifier)) {
            cache.remove(identifier);
        }
    }

    public Collection<String> getTokenIdentifiers() {
        if (cache == null) {
            return null;
        }

        // Not very efficient, but we are only using this method for testing
        Set<String> keys = new HashSet<>();
        for (Cache.Entry<String, SecurityToken> entry : cache) {
            keys.add(entry.getKey());
        }

        return keys;
    }

    public SecurityToken getToken(String identifier) {
        if (cache == null) {
            return null;
        }
        return cache.get(identifier);
    }

    public synchronized void close() {
        if (cacheManager.getStatus() == Status.AVAILABLE) {
            cacheManager.removeCache(key);
            cacheManager.close();
        }

        if (bus != null) {
            bus.getExtension(BusLifeCycleManager.class).unregisterLifeCycleListener(this);
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
