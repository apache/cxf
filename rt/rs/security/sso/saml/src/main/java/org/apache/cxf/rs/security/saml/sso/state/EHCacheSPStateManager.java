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
package org.apache.cxf.rs.security.saml.sso.state;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.wss4j.common.util.Loader;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;

/**
 * An in-memory EHCache implementation of the SPStateManager interface.
 * The default TTL is 5 minutes.
 */
public class EHCacheSPStateManager implements SPStateManager {

    public static final String REQUEST_CACHE_KEY = "cxf.samlp.request.state.cache";
    public static final String RESPONSE_CACHE_KEY = "cxf.samlp.response.state.cache";
    private static final String DEFAULT_CONFIG_URL = "/cxf-samlp-ehcache.xml";
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(EHCacheSPStateManager.class);

    private final Cache<String, RequestState> requestCache;
    private final Cache<String, ResponseState> responseCache;
    private final CacheManager requestCacheManager;
    private final CacheManager responseCacheManager;

    public EHCacheSPStateManager() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this(DEFAULT_CONFIG_URL, null);
    }

    public EHCacheSPStateManager(Bus bus)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this(DEFAULT_CONFIG_URL, bus);
    }

    public EHCacheSPStateManager(String configFileURL)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this(configFileURL, null);
    }

    public EHCacheSPStateManager(String configFile, Bus bus)
            throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus(true);
        }

        URL configFileURL = null;
        try {
            configFileURL =
                ResourceUtils.getClasspathResourceURL(configFile, EHCacheSPStateManager.class, bus);
        } catch (Exception ex) {
            // ignore
        }

        XmlConfiguration xmlConfig = new XmlConfiguration(getConfigFileURL(configFileURL));
        CacheConfigurationBuilder<String, RequestState> requestConfigurationBuilder =
                xmlConfig.newCacheConfigurationBuilderFromTemplate(REQUEST_CACHE_KEY,
                        String.class, RequestState.class);

        // Note, we don't require strong random values here
        String diskKey = REQUEST_CACHE_KEY + "-" + Math.abs(new Random().nextInt());
        requestCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache(REQUEST_CACHE_KEY, requestConfigurationBuilder)
                .with(CacheManagerBuilder.persistence(new File(System.getProperty("java.io.tmpdir"), diskKey))).build();

        requestCacheManager.init();
        requestCache = requestCacheManager.getCache(REQUEST_CACHE_KEY, String.class, RequestState.class);

        CacheConfigurationBuilder<String, ResponseState> responseConfigurationBuilder =
                xmlConfig.newCacheConfigurationBuilderFromTemplate(RESPONSE_CACHE_KEY,
                        String.class, ResponseState.class);

        // Note, we don't require strong random values here
        diskKey = RESPONSE_CACHE_KEY + "-" + Math.abs(new Random().nextInt());
        responseCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache(RESPONSE_CACHE_KEY, responseConfigurationBuilder)
                .with(CacheManagerBuilder.persistence(new File(System.getProperty("java.io.tmpdir"), diskKey))).build();

        responseCacheManager.init();
        responseCache = responseCacheManager.getCache(RESPONSE_CACHE_KEY, String.class, ResponseState.class);
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
        if (requestCacheManager.getStatus() == Status.AVAILABLE) {
            requestCacheManager.removeCache(REQUEST_CACHE_KEY);
            requestCacheManager.close();
        }
        if (responseCacheManager.getStatus() == Status.AVAILABLE) {
            responseCacheManager.removeCache(RESPONSE_CACHE_KEY);
            responseCacheManager.close();
        }
    }

}
