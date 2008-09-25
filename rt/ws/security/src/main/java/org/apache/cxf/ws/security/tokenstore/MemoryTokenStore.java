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


import org.w3c.dom.Element;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.token.Reference;

/**
 * 
 */
public class MemoryTokenStore implements TokenStore {

    Map<String, SecurityToken> tokens = new ConcurrentHashMap<String, SecurityToken>();
    
    /** {@inheritDoc}*/
    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            tokens.put(token.getId(), token);
        }
    }

    /** {@inheritDoc}*/
    public void update(SecurityToken token) {
        add(token);
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
                Element elem = t.getAttachedReference();
                if (elem != null && id.equals(getIdFromSTR(elem))) {
                    return t;
                }
                elem = t.getUnattachedReference();
                if (elem != null && id.equals(getIdFromSTR(elem))) {
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
        for (SecurityToken token : tokens.values()) {
            if (token.getExpires() != null 
                && token.getExpires().getTimeInMillis() < System.currentTimeMillis()) {
                token.setState(SecurityToken.State.EXPIRED);
            }            
        }
    }
    
    public static String getIdFromSTR(Element str) {
        Element child = DOMUtils.getFirstElement(str);
        if (child == null) {
            return null;
        }
        
        if ("KeyInfo".equals(child.getLocalName())
            && WSConstants.SIG_NS.equals(child.getNamespaceURI())) {
            return DOMUtils.getContent(child);
        } else if (Reference.TOKEN.getLocalPart().equals(child.getLocalName())
            && Reference.TOKEN.getNamespaceURI().equals(child.getNamespaceURI())) {
            return child.getAttribute("URI").substring(1);
        }
        return null;
    }

    
}
