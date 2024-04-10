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

package org.apache.cxf.transport.http.netty.server.servlet;


import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import org.apache.cxf.transport.http.netty.server.util.Utils;

public class NettyHttpSession implements HttpSession {

    public static final String SESSION_ID_KEY = "JSESSIONID";

    private String id;

    private long creationTime;

    private long lastAccessedTime;

    private int maxInactiveInterval = -1;

    private Map<String, Object> attributes;

    public NettyHttpSession(String id) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getAttributeNames() {
        return Utils.enumerationFromKeys(attributes);
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        // TODO do we need to support this
        return null;
    }

    @Override
    public void invalidate() {
        if (attributes != null) {
            attributes.clear();
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (attributes != null) {
            Object value = attributes.get(name);
            if (value instanceof HttpSessionBindingListener) {
                ((HttpSessionBindingListener) value)
                        .valueUnbound(new HttpSessionBindingEvent(this, name,
                                value));
            }
            attributes.remove(name);
        }
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new ConcurrentHashMap<>();
        }
        attributes.put(name, value);

        if (value instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener) value)
                    .valueBound(new HttpSessionBindingEvent(this, name, value));
        }

    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;

    }

    public void touch() {
        this.lastAccessedTime = System.currentTimeMillis();
    }

    @Override
    public boolean isNew() {
        throw new IllegalStateException("Method 'isNew' not yet implemented!");
    }
}
