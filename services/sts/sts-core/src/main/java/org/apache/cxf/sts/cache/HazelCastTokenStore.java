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
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.SecurityToken.State;

public class HazelCastTokenStore implements STSTokenStore {

    IMap<Object, Object> cacheMap;
    boolean autoRemove = true;
    
    public HazelCastTokenStore(String mapName) {
        cacheMap = Hazelcast.getDefaultInstance().getMap(mapName);
        
    }
    
    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            cacheMap.put(token.getId(), token);
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
            cacheMap.remove(token.getId());
        }
    }
    
    public Collection<String> getTokenIdentifiers() {
        return CastUtils.cast((Collection<?>)cacheMap.keySet());
    }

    public Collection<SecurityToken> getExpiredTokens() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public Collection<SecurityToken> getValidTokens() {
        return CastUtils.cast((Collection<?>)cacheMap.keySet());
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
        return (SecurityToken)cacheMap.get(id);
    }

    public SecurityToken getTokenByAssociatedHash(int hashCode) {
        Iterator<Object> ids = cacheMap.keySet().iterator();
        while (ids.hasNext()) {
            SecurityToken securityToken = getToken((String)ids.next());
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

    
    public void add(SecurityToken token, Integer timeToLiveSeconds) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            cacheMap.put(token.getId(), token, timeToLiveSeconds, TimeUnit.SECONDS);
        }
    }

}
