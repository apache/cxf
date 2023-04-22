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

package org.apache.cxf.sts.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;

public class HazelCastTokenStore implements TokenStore {

    public static final long DEFAULT_TTL = 3600L;
    public static final long MAX_TTL = DEFAULT_TTL * 12L;

    private IMap<Object, Object> cacheMap;

    private long ttl = DEFAULT_TTL;
    private HazelcastInstance hazelcastInstance;
    private String mapName;

    public HazelCastTokenStore(String mapName) {
        this.mapName = mapName;
    }

    /**
     * Get the Hazelcast instance
     * @return Hazelcast instance
     */
    public HazelcastInstance getHazelcastInstance() {
        if (hazelcastInstance == null) {
            hazelcastInstance = Hazelcast.newHazelcastInstance();
        }
        return hazelcastInstance;
    }

    /**
     * Set the Hazelcast instance, otherwise default instance used
     * If you configure Hazelcast instance in spring, you must inject the instance here.
     * @param hazelcastInstance Hazelcast instance
     */
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
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

    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            int parsedTTL = getTTL(token);
            if (parsedTTL > 0) {
                getCacheMap().put(token.getId(), token, parsedTTL, TimeUnit.SECONDS);
            }
        }
    }

    public void add(String identifier, SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(identifier)) {
            int parsedTTL = getTTL(token);
            if (parsedTTL > 0) {
                getCacheMap().put(identifier, token, parsedTTL, TimeUnit.SECONDS);
            }
        }
    }

    public void remove(String identifier) {
        if (!StringUtils.isEmpty(identifier) && getCacheMap().containsKey(identifier)) {
            getCacheMap().remove(identifier);
        }
    }

    public Collection<String> getTokenIdentifiers() {
        return CastUtils.cast((Collection<?>)getCacheMap().keySet());
    }

    public SecurityToken getToken(String identifier) {
        return (SecurityToken)getCacheMap().get(identifier);
    }

    public void destroy() {
        if (hazelcastInstance != null) {
            hazelcastInstance.getLifecycleService().shutdown();
        }
    }

    private int getTTL(SecurityToken token) {
        int parsedTTL;
        if (token.getExpires() != null) {
            Instant expires = token.getExpires();
            Instant now = Instant.now();
            if (expires.isBefore(now)) {
                return 0;
            }

            Duration duration = Duration.between(now, expires);

            parsedTTL = (int)duration.getSeconds();
            if (duration.getSeconds() != parsedTTL || parsedTTL > MAX_TTL) {
                // Default to configured value
                parsedTTL = (int)ttl;
                if (ttl != parsedTTL) {
                    // Fall back to 60 minutes if the default TTL is set incorrectly
                    parsedTTL = 3600;
                }
            }
        } else {
            // Default to configured value
            parsedTTL = (int)ttl;
            if (ttl != parsedTTL) {
                // Fall back to 60 minutes if the default TTL is set incorrectly
                parsedTTL = 3600;
            }
        }
        return parsedTTL;
    }

    private IMap<Object, Object> getCacheMap() {
        if (this.cacheMap == null) {
            this.cacheMap = getHazelcastInstance().getMap(mapName);
        }
        return this.cacheMap;
    }

}

