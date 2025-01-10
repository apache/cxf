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
package org.apache.cxf.rs.security.saml.sso.state.jcache;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.rs.security.saml.sso.state.RequestState;
import org.apache.cxf.rs.security.saml.sso.state.ResponseState;
import org.apache.cxf.rs.security.saml.sso.state.SPStateManager;
import org.apache.wss4j.common.util.Loader;

/**
 * An in-memory JCache based implementation of the SPStateManager interface.
 * The default TTL is 5 minutes.
 */
public class JCacheSPStateManager implements SPStateManager {

    public static final String REQUEST_CACHE_KEY = "cxf.samlp.request.state.cache";
    public static final String RESPONSE_CACHE_KEY = "cxf.samlp.response.state.cache";
    private static final String DEFAULT_CONFIG_URL = "/cxf-samlp-jcache.xml";
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(JCacheSPStateManager.class);

    private final Cache<String, RequestState> requestCache;
    private final Cache<String, ResponseState> responseCache;
    private final CacheManager cacheManager;

    public JCacheSPStateManager() throws URISyntaxException {
        this(DEFAULT_CONFIG_URL, null);
    }

    public JCacheSPStateManager(Bus bus) throws URISyntaxException {
        this(DEFAULT_CONFIG_URL, bus);
    }

    public JCacheSPStateManager(String configFileURL) throws URISyntaxException {
        this(configFileURL, null);
    }

    public JCacheSPStateManager(String configFile, Bus bus) throws  URISyntaxException {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus(true);
        }

        URL configFileURL = null;
        try {
            configFileURL =
                ResourceUtils.getClasspathResourceURL(configFile, JCacheSPStateManager.class, bus);
        } catch (Exception ex) {
            // ignore
        }

        final CachingProvider cachingProvider = Caching.getCachingProvider();

        cacheManager = cachingProvider.getCacheManager(
            getConfigFileURL(configFileURL).toURI(), 
            getClass().getClassLoader()); 

        requestCache = getOrCreate(cacheManager, REQUEST_CACHE_KEY, String.class, RequestState.class);
        responseCache = getOrCreate(cacheManager, RESPONSE_CACHE_KEY, String.class, ResponseState.class);
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

    public ResponseState getResponseState(String securityContextKey) {
        return responseCache.get(securityContextKey);
    }

    public ResponseState removeResponseState(String securityContextKey) {
        ResponseState responseState = getResponseState(securityContextKey);
        if (responseState != null) {
            responseCache.remove(securityContextKey);
        }
        return responseState;
    }

    public void setResponseState(String securityContextKey, ResponseState state) {
        if (securityContextKey == null || "".equals(securityContextKey)) {
            return;
        }

        responseCache.put(securityContextKey, state);
    }

    public void setRequestState(String relayState, RequestState state) {
        if (relayState == null || "".equals(relayState)) {
            return;
        }

        requestCache.put(relayState, state);
    }

    public RequestState removeRequestState(String relayState) {
        RequestState state = requestCache.get(relayState);
        if (state != null) {
            requestCache.remove(relayState);
        }
        return state;
    }

    public synchronized void close() throws IOException {
        if (!cacheManager.isClosed()) {
            cacheManager.destroyCache(REQUEST_CACHE_KEY);
            cacheManager.destroyCache(RESPONSE_CACHE_KEY);
            cacheManager.close();
        }
    }

}
