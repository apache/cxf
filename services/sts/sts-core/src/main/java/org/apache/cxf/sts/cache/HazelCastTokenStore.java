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

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;

public class HazelCastTokenStore implements TokenStore {
    
    public static final long DEFAULT_TTL = 3600L;
    public static final long MAX_TTL = DEFAULT_TTL * 12L;

    private IMap<Object, Object> cacheMap;
    private long ttl = DEFAULT_TTL;
    
    public HazelCastTokenStore(String mapName) {
        cacheMap = Hazelcast.getDefaultInstance().getMap(mapName);
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
            cacheMap.put(token.getId(), token, parsedTTL, TimeUnit.SECONDS);
        }
    }
    
    public void add(String identifier, SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(identifier)) {
            int parsedTTL = getTTL(token);
            cacheMap.put(identifier, token, parsedTTL, TimeUnit.SECONDS);
        }
    }
    
    public void remove(String identifier) {
        if (!StringUtils.isEmpty(identifier) && cacheMap.containsKey(identifier)) {
            cacheMap.remove(identifier);
        }
    }
    
    public Collection<String> getTokenIdentifiers() {
        return CastUtils.cast((Collection<?>)cacheMap.keySet());
    }

    public Collection<SecurityToken> getExpiredTokens() {
        // TODO Auto-generated method stub
        return null;
    }

    public SecurityToken getToken(String identifier) {
        return (SecurityToken)cacheMap.get(identifier);
    }

    private int getTTL(SecurityToken token) {
        int parsedTTL = 0;
        if (token.getExpires() != null) {
            Date expires = token.getExpires();
            Date current = new Date();
            long expiryTime = (expires.getTime() - current.getTime()) / 1000L;
            
            parsedTTL = (int)expiryTime;
            if (expiryTime != (long)parsedTTL || parsedTTL < 0 || parsedTTL > MAX_TTL) {
                // Default to configured value
                parsedTTL = (int)ttl;
                if (ttl != (long)parsedTTL) {
                    // Fall back to 60 minutes if the default TTL is set incorrectly
                    parsedTTL = 3600;
                }
            }
        } else {
            // Default to configured value
            parsedTTL = (int)ttl;
            if (ttl != (long)parsedTTL) {
                // Fall back to 60 minutes if the default TTL is set incorrectly
                parsedTTL = 3600;
            }
        }
        return parsedTTL;
    }
    
}
