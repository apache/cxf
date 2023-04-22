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

package org.apache.cxf.jaxrs.impl.tl;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class ThreadLocalMessageContext extends AbstractThreadLocalProxy<MessageContext>
    implements MessageContext {

    public Object get(Object key) {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.get(key) : null;
    }

    public <T> T getContext(Class<T> contextClass) {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getContext(contextClass) : null;
    }

    public HttpHeaders getHttpHeaders() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getHttpHeaders() : null;
    }

    public HttpServletRequest getHttpServletRequest() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getHttpServletRequest() : null;
    }

    public HttpServletResponse getHttpServletResponse() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getHttpServletResponse() : null;
    }

    public Providers getProviders() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getProviders() : null;
    }

    public SecurityContext getSecurityContext() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getSecurityContext() : null;
    }

    public ServletConfig getServletConfig() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getServletConfig() : null;
    }

    public ServletContext getServletContext() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getServletContext() : null;
    }

    public UriInfo getUriInfo() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getUriInfo() : null;
    }

    public Request getRequest() {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getRequest() : null;
    }

    public void put(Object key, Object value) {
        MessageContext mc = getCurrentMessageContext();
        if (mc != null) {
            mc.put(key, value);
        }
    }

    public <T, E> T getResolver(Class<T> resolverClass, Class<E> resolveClazz) {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getResolver(resolverClass, resolveClazz) : null;
    }

    public <T> T getContent(Class<T> format) {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getContent(format) : null;
    }

    public Object getContextualProperty(Object key) {
        MessageContext mc = getCurrentMessageContext();
        return mc != null ? mc.getContextualProperty(key) : null;
    }
    private MessageContext getCurrentMessageContext() {
        MessageContext mc = get();
        return mc != null ? mc : getMessageContextImpl();
    }
    private MessageContext getMessageContextImpl() {
        Message m = JAXRSUtils.getCurrentMessage();
        return m != null ? new MessageContextImpl(m) : null;
    }
}
