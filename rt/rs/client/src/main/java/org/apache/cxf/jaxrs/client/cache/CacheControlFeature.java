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

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheWriter;
import javax.cache.spi.CachingProvider;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;


@Provider
public class CacheControlFeature implements Feature, Closeable {
    private CachingProvider provider;
    private CacheManager manager;
    private Cache<Key, Entry> cache;
    private boolean cacheResponseInputStream;

    @Override
    public boolean configure(final FeatureContext context) {
        // TODO: read context properties to exclude some patterns?
        final Cache<Key, Entry> entryCache = createCache(context.getConfiguration().getProperties());
        context.register(new CacheControlClientRequestFilter(entryCache));
        CacheControlClientReaderInterceptor reader = new CacheControlClientReaderInterceptor(entryCache);
        reader.setCacheResponseInputStream(cacheResponseInputStream);
        context.register(reader);
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

        final String prefix = this.getClass().getName() + ".";
        final String uri = props.getProperty(prefix + "config-uri");
        final String name = props.getProperty(prefix + "name", this.getClass().getName());

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        provider = Caching.getCachingProviders().iterator().next();
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
                @SuppressWarnings("unchecked")
                Factory<? extends CacheLoader<Key, Entry>> f = newInstance(contextClassLoader, loader, Factory.class);
                configuration.setCacheLoaderFactory(f);
            }
            final String writer = props.getProperty(prefix + "writerFactory");
            if (writer != null) {
                @SuppressWarnings("unchecked")
                Factory<? extends CacheWriter<Key, Entry>> f = newInstance(contextClassLoader, writer, Factory.class);
                configuration.setCacheWriterFactory(f);
            }
            final String expiry = props.getProperty(prefix + "expiryFactory");
            if (expiry != null) {
                @SuppressWarnings("unchecked")
                Factory<? extends ExpiryPolicy> f = newInstance(contextClassLoader, expiry, Factory.class);
                configuration.setExpiryPolicyFactory(f);
            }

            cache = manager.createCache(name, configuration);
            return cache;
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(final ClassLoader contextClassLoader, final String clazz, final Class<T> cast) {
        try {
            return (T) contextClassLoader.loadClass(clazz).getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void setCacheResponseInputStream(boolean cacheStream) {
        this.cacheResponseInputStream = cacheStream;
    }
}
