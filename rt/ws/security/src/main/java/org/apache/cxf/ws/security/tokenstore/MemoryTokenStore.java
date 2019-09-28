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

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.util.StringUtils;

/**
 * A simple HashMap-based TokenStore. The default TTL is 5 minutes and the max TTL is 1 hour.
 */
public class MemoryTokenStore implements TokenStore {
    public static final long DEFAULT_TTL = 60L * 5L;
    public static final long MAX_TTL = DEFAULT_TTL * 12L;

    private Map<String, CacheEntry> tokens = new ConcurrentHashMap<>();
    private long ttl = DEFAULT_TTL;

    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            tokens.put(token.getId(), createCacheEntry(token));
        }
    }

    public void add(String identifier, SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(identifier)) {
            tokens.put(identifier, createCacheEntry(token));
        }
    }

    /**
     * Set a new (default) TTL value in seconds
     * @param newTtl a new (default) TTL value in seconds
     */
    public void setTTL(long newTtl) {
        ttl = newTtl;
    }

    public void remove(String identifier) {
        if (!StringUtils.isEmpty(identifier) && tokens.containsKey(identifier)) {
            tokens.remove(identifier);
        }
    }

    public Collection<String> getTokenIdentifiers() {
        processTokenExpiry();
        return tokens.keySet();
    }

    public SecurityToken getToken(String id) {
        processTokenExpiry();

        CacheEntry cacheEntry = tokens.get(id);
        if (cacheEntry != null) {
            return cacheEntry.getSecurityToken();
        }
        return null;
    }

    protected void processTokenExpiry() {
        Instant current = Instant.now();
        synchronized (tokens) {
            for (Map.Entry<String, CacheEntry> entry : tokens.entrySet()) {
                if (entry.getValue().getExpiry().isBefore(current)) {
                    tokens.remove(entry.getKey());
                }
            }
        }
    }

    private CacheEntry createCacheEntry(SecurityToken token) {
        Instant expires = Instant.now().plusSeconds(ttl);
        return new CacheEntry(token, expires);
    }

    private static class CacheEntry {

        private final SecurityToken securityToken;
        private final Instant expires;

        CacheEntry(SecurityToken securityToken, Instant expires) {
            this.securityToken = securityToken;
            this.expires = expires;
        }

        /**
         * Get the SecurityToken
         * @return the SecurityToken
         */
        public SecurityToken getSecurityToken() {
            return securityToken;
        }

        /**
         * Get when this CacheEntry is to be removed from the cache
         * @return when this CacheEntry is to be removed from the cache
         */
        public Instant getExpiry() {
            return expires;
        }

    }

}
