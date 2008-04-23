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

package org.apache.cxf.message;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.transport.Destination;

/**
 * A base class to build your own message implementations on.
 * 
 * @author Dan
 */
public abstract class AbstractWrappedMessage implements Message {
    
    private Message message;

    protected AbstractWrappedMessage(Message msg) {
        this.message = msg;
    }

    public void clear() {
        message.clear();
    }

    public boolean containsKey(Object key) {
        return message.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return message.containsValue(value);
    }

    public Set<Entry<String, Object>> entrySet() {
        return message.entrySet();
    }

    public boolean equals(Object o) {
        return message.equals(o);
    }

    public Object get(Object key) {
        return message.get(key);
    }

    public Collection<Attachment> getAttachments() {
        return message.getAttachments();
    }

    public void setAttachments(Collection<Attachment> attachments) {
        message.setAttachments(attachments);
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
    
    public Destination getDestination() {
        return message.getDestination();
    }

    public Exchange getExchange() {
        return message.getExchange();
    }

    public final void setExchange(Exchange exchange) {
        message.setExchange(exchange);
    }

    public String getId() {
        return message.getId();
    }

    public void setId(String id) {
        message.setId(id);
    }
    
    public InterceptorChain getInterceptorChain() {
        return message.getInterceptorChain();
    }
    public void setInterceptorChain(InterceptorChain chain) {
        message.setInterceptorChain(chain);
    }

    public <T> T getContent(Class<T> format) {
        return message.getContent(format);
    }
    public <T> void removeContent(Class<T> format) {
        message.removeContent(format);
    }

    public Set<Class<?>> getContentFormats() {
        return message.getContentFormats();
    }

    public int hashCode() {
        return message.hashCode();
    }

    public boolean isEmpty() {
        return message.isEmpty();
    }

    public Set<String> keySet() {
        return message.keySet();
    }

    public Object put(String key, Object value) {
        return message.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        message.putAll(t);
    }

    public Object remove(Object key) {
        return message.remove(key);
    }

    public <T> void setContent(Class<T> format, Object content) {
        message.setContent(format, content);
    }

    public int size() {
        return message.size();
    }

    public Collection<Object> values() {
        return message.values();
    }
    
    public <T> T get(Class<T> key) {
        return message.get(key);
    }
    public <T> void put(Class<T> key, T value) {
        message.put(key, value);
    }

    public Object getContextualProperty(String key) {
        return message.getContextualProperty(key);
    }       
}
