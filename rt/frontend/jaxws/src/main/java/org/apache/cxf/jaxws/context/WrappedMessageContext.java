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

package org.apache.cxf.jaxws.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

public class WrappedMessageContext implements MessageContext {
    public static final String SCOPES = WrappedMessageContext.class.getName() + ".SCOPES";
    
    private final Map<String, Object> contextMap;
    private final Message message;
    private Map<String, Scope> scopes;
    private Scope defaultScope;

    public WrappedMessageContext(Message m) {
        this(m, m, Scope.HANDLER);
    }
    public WrappedMessageContext(Message m, Scope defScope) {
        this(m, m, defScope);
    }
    
    public WrappedMessageContext(Map<String, Object> m, Scope defScope) {
        this(null, m, defScope);
    }
    
    public WrappedMessageContext(Message m, Map<String, Object> map, Scope defScope) {
        message = m;
        contextMap = map;
        defaultScope = defScope;
        scopes = CastUtils.cast((Map<?, ?>)contextMap.get(SCOPES));
        if (scopes == null && message != null && message.getExchange() != null) { 
            if (isRequestor() && !isOutbound() && m.getExchange().getOutMessage() != null) {
                scopes = CastUtils.cast((Map<?, ?>)m.getExchange().getOutMessage().get(SCOPES));
                copyScopedProperties(m.getExchange().getOutMessage());
                m.put(SCOPES, scopes);
            } else if (!isRequestor() && isOutbound() && m.getExchange().getInMessage() != null) {
                scopes = CastUtils.cast((Map<?, ?>)m.getExchange().getInMessage().get(SCOPES));
                copyScopedProperties(m.getExchange().getInMessage());
                m.put(SCOPES, scopes);
            }
        }
        if (scopes == null) {
            scopes = new HashMap<String, Scope>();
            contextMap.put(SCOPES, scopes);
        }
    }
    
    protected final void copyScopedProperties(Message m) {
        for (String k : scopes.keySet()) {
            if (!contextMap.containsKey(k)
                && !MessageContext.MESSAGE_OUTBOUND_PROPERTY.equals(k)) {
                contextMap.put(k, m.get(k));
            }
        }
    }
    protected final boolean isRequestor() {
        return Boolean.TRUE.equals(contextMap.containsKey(Message.REQUESTOR_ROLE));
    }
    protected final boolean isOutbound() {
        Exchange ex = message.getExchange();
        return message != null 
            && (message == ex.getOutMessage()
                || message == ex.getOutFaultMessage());
    }
    
    public final Message getWrappedMessage() {
        return message;
    }
    
    public void clear() {
        contextMap.clear();      
    }

    public final boolean containsKey(Object key) {
        return contextMap.containsKey(key);
    }

    public final boolean containsValue(Object value) {
        return contextMap.containsValue(value);
    }

    public final Set<Entry<String, Object>> entrySet() {
        return contextMap.entrySet();
    }

    public final Object get(Object key) {
        Object ret = contextMap.get(key);
        if (ret == null
            && Message.class.getName().equals(key)) {
            return message;
        }
        return ret;
    }

    public final boolean isEmpty() {
        return contextMap.isEmpty();
    }

    public final Set<String> keySet() {
        return contextMap.keySet();
    }

    public final Object put(String key, Object value) {
        if (!MessageContext.MESSAGE_OUTBOUND_PROPERTY.equals(key)
            && !scopes.containsKey(key)) {
            scopes.put(key, defaultScope);
        }
        return contextMap.put(key, value);
    }
    public final Object put(String key, Object value, Scope scope) {
        if (!MessageContext.MESSAGE_OUTBOUND_PROPERTY.equals(key)) {
            scopes.put(key, scope);
        }
        return contextMap.put(key, value);
    }

    public final void putAll(Map<? extends String, ? extends Object> t) {
        for (Map.Entry<? extends String, ? extends Object> s : t.entrySet()) {
            put(s.getKey(), s.getValue());
        }
    }

    public final Object remove(Object key) {
        scopes.remove(key);
        return contextMap.remove(key);
    }

    public final int size() {
        return contextMap.size();
    }

    public final Collection<Object> values() {
        return contextMap.values();
    }

    public final void setScope(String key, Scope arg1) {
        if (!this.containsKey(key)) {
            throw new IllegalArgumentException("non-existant property-" + key + "is specified");    
        }
        scopes.put(key, arg1);        
    }

    public final Scope getScope(String key) {
        if (containsKey(key)) {
            if (scopes.containsKey(key)) {
                return scopes.get(key);
            } else {
                return defaultScope;
            }
        }
        throw new IllegalArgumentException("non-existant property-" + key + "is specified");
    }
    
    public final Map<String, Scope> getScopes() {
        return scopes;
    }
    
}
