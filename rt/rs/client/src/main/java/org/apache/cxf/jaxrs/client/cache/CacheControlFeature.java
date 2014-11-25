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
package org.apache.cxf.jaxrs.client.cache;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;



@Priority(Priorities.HEADER_DECORATOR)
public class CacheControlFeature implements Feature {
    private CachingProvider provider;
    private CacheManager manager;
    private Cache<Key, Entry> cache;

    @Override
    public boolean configure(final FeatureContext context) {
        // TODO: read context properties to exclude some patterns?
        final Cache<Key, Entry> entryCache = createCache(context.getConfiguration().getProperties());
        context.register(new CacheControlClientRequestFilter(entryCache));
        context.register(new CacheControlClientReaderInterceptor(entryCache));
        return true;
    }

    @PreDestroy // TODO: check it is called
    public void close() {
        for (final Closeable c : Arrays.asList(cache, manager, provider)) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (final Exception e) {
                // no-op
            }
        }
    }

    private Cache<Key, Entry> createCache(final Map<String, Object> properties) {
        final Properties props = new Properties();
        props.putAll(properties);

        final String prefix = ClientCache.class.getName() + ".";
        final String uri = props.getProperty(prefix + "config-uri");
        final String name = props.getProperty(prefix + "name", ClientCache.class.getName());

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        provider = Caching.getCachingProvider();
        try {
            manager = provider.getCacheManager(
                    uri == null ? provider.getDefaultURI() : new URI(uri),
                    contextClassLoader,
                    props);

            final MutableConfiguration<Key, Entry> configuration = new MutableConfiguration<Key, Entry>()
                .setReadThrough("true".equalsIgnoreCase(props.getProperty(prefix + "readThrough", "false")))
                .setWriteThrough("true".equalsIgnoreCase(props.getProperty(prefix + "writeThrough", "false")))
                .setManagementEnabled(
                    "true".equalsIgnoreCase(props.getProperty(prefix + "managementEnabled", "false")))
                .setStatisticsEnabled(
                    "true".equalsIgnoreCase(props.getProperty(prefix + "statisticsEnabled", "false")))
                .setStoreByValue("true".equalsIgnoreCase(props.getProperty(prefix + "storeByValue", "false")));

            final String loader = props.getProperty(prefix + "loaderFactory");
            if (loader != null) {
                configuration.setCacheLoaderFactory(newInstance(contextClassLoader, loader, Factory.class));
            }
            final String writer = props.getProperty(prefix + "writerFactory");
            if (writer != null) {
                configuration.setCacheWriterFactory(newInstance(contextClassLoader, writer, Factory.class));
            }
            final String expiry = props.getProperty(prefix + "expiryFactory");
            if (expiry != null) {
                configuration.setExpiryPolicyFactory(newInstance(contextClassLoader, expiry, Factory.class));
            }

            cache = manager.createCache(name, configuration);
            return cache;
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static <T> T newInstance(final ClassLoader contextClassLoader, final String clazz, final Class<T> cast) {
        try {
            return (T) contextClassLoader.loadClass(clazz).newInstance();
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
