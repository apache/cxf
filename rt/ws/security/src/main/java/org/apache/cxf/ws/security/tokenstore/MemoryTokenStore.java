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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken.State;

/**
 * 
 */
public class MemoryTokenStore implements TokenStore {
    boolean autoRemove = true;
    
    Map<String, SecurityToken> tokens = new ConcurrentHashMap<String, SecurityToken>();
    
    /** {@inheritDoc}*/
    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            tokens.put(token.getId(), token);
        }
    }

    /** {@inheritDoc}*/
    public void update(SecurityToken token) {
        if (autoRemove 
            && (token.getState() == State.EXPIRED
                || token.getState() == State.CANCELLED)) {
            remove(token);
        } else {
            add(token);
        }
    }
    public void remove(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            tokens.remove(token.getId());
        }
    }

    public Collection<SecurityToken> getCancelledTokens() {
        return getTokens(SecurityToken.State.CANCELLED);
    }
    public Collection<SecurityToken> getExpiredTokens() {
        return getTokens(SecurityToken.State.EXPIRED);
    }
    public Collection<SecurityToken> getRenewedTokens() {
        return getTokens(SecurityToken.State.RENEWED);
    }
    public Collection<String> getTokenIdentifiers() {
        processTokenExpiry();        
        return tokens.keySet();
    }

    public Collection<SecurityToken> getValidTokens() {
        Collection<SecurityToken> toks = getTokens(SecurityToken.State.ISSUED);
        toks.addAll(getTokens(SecurityToken.State.RENEWED));
        toks.addAll(getTokens(SecurityToken.State.UNKNOWN));
        return toks;
    }

    public SecurityToken getToken(String id) {
        processTokenExpiry();
        
        SecurityToken token = tokens.get(id);
        if (token == null) {
            for (SecurityToken t : tokens.values()) {
                if (id.equals(t.getWsuId())) {
                    return t;
                }
            }
        }
        return token;
    }

    
    protected Collection<SecurityToken> getTokens(SecurityToken.State state) {
        processTokenExpiry();
        List<SecurityToken> t = new ArrayList<SecurityToken>();
        for (SecurityToken token : tokens.values()) {
            if (token.getState() == state) {
                t.add(token);
            }
        }
        return t;
    }

    protected void processTokenExpiry() {
        long time = System.currentTimeMillis();
        for (SecurityToken token : tokens.values()) {
            if (token.getState() == State.EXPIRED
                || token.getState() == State.CANCELLED) {
                if (autoRemove) {
                    remove(token);
                }
            } else if (token.getExpires() != null 
                && token.getExpires().getTimeInMillis() < time) {
                token.setState(SecurityToken.State.EXPIRED);
                if (autoRemove) {
                    remove(token);
                }
            }            
        }
    }


    public void removeCancelledTokens() {
        for (SecurityToken token : tokens.values()) {
            if (token.getState() == State.CANCELLED) {
                remove(token);
            }
        }
    }

    public void removeExpiredTokens() {
        processTokenExpiry();
        for (SecurityToken token : tokens.values()) {
            if (token.getState() == State.EXPIRED) {
                remove(token);
            }
        }
    }

    public void setAutoRemoveTokens(boolean auto) {
        autoRemove = auto;
    }
    
}
