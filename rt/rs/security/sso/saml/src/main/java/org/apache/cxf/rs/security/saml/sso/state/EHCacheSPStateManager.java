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

import java.io.IOException;
import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.ws.security.util.Loader;

/**
 * An in-memory EHCache implementation of the SPStateManager interface. 
 * The default TTL is 5 minutes.
 */
public class EHCacheSPStateManager implements SPStateManager {

    public static final long DEFAULT_TTL = 60L * 5L;
    private static final String REQUEST_CACHE_KEY = "cxf-samlp-request-state-cache";
    private static final String RESPONSE_CACHE_KEY = "cxf-samlp-response-state-cache";
    private Ehcache requestCache;
    private Ehcache responseCache;
    private CacheManager cacheManager;
    private long ttl = DEFAULT_TTL;
    
    public EHCacheSPStateManager() {
        String defaultConfigFile = "cxf-samlp-ehcache.xml";
        URL configFileURL = Loader.getResource(defaultConfigFile);
        createCaches(configFileURL);
    }
    
    public EHCacheSPStateManager(URL configFileURL) {
        createCaches(configFileURL);
    }
    
    private void createCaches(URL configFileURL) {
        if (configFileURL == null) {
            cacheManager = CacheManager.create();
        } else {
            cacheManager = CacheManager.create(configFileURL);
        }
        
        Ehcache newCache = new Cache(REQUEST_CACHE_KEY, 50000, true, false, DEFAULT_TTL, DEFAULT_TTL);
        requestCache = cacheManager.addCacheIfAbsent(newCache);
        
        newCache = new Cache(RESPONSE_CACHE_KEY, 50000, true, false, DEFAULT_TTL, DEFAULT_TTL);
        responseCache = cacheManager.addCacheIfAbsent(newCache);
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
    
    public ResponseState getResponseState(String securityContextKey) {
        Element element = responseCache.get(securityContextKey);
        if (element != null) {
            if (responseCache.isExpired(element)) {
                responseCache.remove(securityContextKey);
                return null;
            }
            return (ResponseState)element.getObjectValue();
        }
        return null;
    }

    public ResponseState removeResponseState(String securityContextKey) {
        Element element = responseCache.get(securityContextKey);
        if (element != null) {
            responseCache.remove(securityContextKey);
            return (ResponseState)element.getObjectValue();
        }
        return null;
    }

    public void setResponseState(String securityContextKey, ResponseState state) {
        if (securityContextKey == null || "".equals(securityContextKey)) {
            return;
        }
        
        int parsedTTL = (int)ttl;
        if (ttl != (long)parsedTTL) {
            // Fall back to 5 minutes if the default TTL is set incorrectly
            parsedTTL = 60 * 5;
        }
        
        responseCache.put(new Element(securityContextKey, state, false, parsedTTL, parsedTTL));
    }
    
    public void setRequestState(String relayState, RequestState state) {
        if (relayState == null || "".equals(relayState)) {
            return;
        }
        
        int parsedTTL = (int)ttl;
        if (ttl != (long)parsedTTL) {
            // Fall back to 60 minutes if the default TTL is set incorrectly
            parsedTTL = 3600;
        }
        
        requestCache.put(new Element(relayState, state, false, parsedTTL, parsedTTL));
    }

    public RequestState removeRequestState(String relayState) {
        Element element = requestCache.get(relayState);
        if (element != null) {
            requestCache.remove(relayState);
            return (RequestState)element.getObjectValue();
        }
        return null;
    }
    
    public void close() throws IOException {
        if (cacheManager != null) {
            cacheManager.shutdown();
            cacheManager = null;
            requestCache = null;
            responseCache = null;
        }
    }

}
