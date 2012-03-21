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
import java.util.Iterator;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.SecurityToken.State;

public class DefaultInMemoryTokenStore implements STSTokenStore {

    private Cache cache;
    private CacheManager cacheManager = CacheManager.create();
    private boolean autoRemove = true;
    
    public DefaultInMemoryTokenStore() {
        String key = "STS";
        if (!cacheManager.cacheExists(key)) {
            cache = new Cache(key, 5000, false, false, 3600, 3600);
            cacheManager.addCache(cache);
        } else {
            cache = cacheManager.getCache(key);
        }
    }
    
    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            cache.put(new Element(token.getId(), token));
        }
    }

    public void add(SecurityToken token, Integer timeToLiveSeconds) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            cache.put(new Element(token.getId(), token, false, timeToLiveSeconds, timeToLiveSeconds));
        }
    }
    
    public void update(SecurityToken token) {
        if (autoRemove 
            && (token.getState() == State.EXPIRED || token.getState() == State.CANCELLED)) {
            remove(token);
        } else {
            add(token);
        }
    }

    public void remove(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            cache.remove(token.getId());
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getTokenIdentifiers() {
        return cache.getKeys();
    }

    public Collection<SecurityToken> getExpiredTokens() {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    public Collection<SecurityToken> getValidTokens() {
        return cache.getAllWithLoader(cache.getKeysWithExpiryCheck(), null).values();
    }

    public Collection<SecurityToken> getRenewedTokens() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<SecurityToken> getCancelledTokens() {
        // TODO Auto-generated method stub
        return null;
    }

    public SecurityToken getToken(String id) {
        Element element = cache.get(id);
        if (element != null) {
            return (SecurityToken)element.getObjectValue();
        } else {
            return null;
        }
    }

    public SecurityToken getTokenByAssociatedHash(int hashCode) {
        @SuppressWarnings("unchecked")
        Iterator<String> ids = cache.getKeys().iterator();
        while (ids.hasNext()) {
            SecurityToken securityToken = getToken(ids.next());
            if (hashCode == securityToken.getAssociatedHash()) {
                return securityToken;
            }
        }
        return null;
    }

    public void removeExpiredTokens() {
        // TODO Auto-generated method stub
    }

    public void removeCancelledTokens() {
        // TODO Auto-generated method stub
    }

    public void setAutoRemoveTokens(boolean auto) {
        this.autoRemove = auto;
    }

}
