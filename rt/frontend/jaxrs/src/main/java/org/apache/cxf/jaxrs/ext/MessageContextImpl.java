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
package org.apache.cxf.jaxrs.ext;

import java.lang.reflect.Type;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyWorkers;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class MessageContextImpl implements MessageContext {

    private Message m;
    
    public MessageContextImpl(Message m) {
        this.m = m;
    }
    
    public Object get(Object key) {
        return m.get(key);
    }

    public <T> T getContext(Class<T> contextClass) {
        return getContext(contextClass, contextClass);
    }
    
    protected <T> T getContext(Type genericType, Class<T> clazz) {
        return JAXRSUtils.createContextValue(m, genericType, clazz);
    }
    
    @SuppressWarnings("unchecked")
    public <T> ContextResolver<T> getContextResolver(Class<T> resolveClazz) {
        return getContext(resolveClazz, ContextResolver.class);
    }
    
    public Request getRequest() {
        return getContext(Request.class);
    }
    
    public HttpHeaders getHttpHeaders() {
        return getContext(HttpHeaders.class);
    }

    public MessageBodyWorkers getProviders() {
        return getContext(MessageBodyWorkers.class);
    }

    public SecurityContext getSecurityContext() {
        return getContext(SecurityContext.class);
    }

    public UriInfo getUriInfo() {
        return getContext(UriInfo.class);
    }
    
    public HttpServletRequest getHttpServletRequest() {
        return JAXRSUtils.createServletResourceValue(m, HttpServletRequest.class);
    }

    public HttpServletResponse getHttpServletResponse() {
        return JAXRSUtils.createServletResourceValue(m, HttpServletResponse.class);
    }
    
    public ServletConfig getServletConfig() {
        return JAXRSUtils.createServletResourceValue(m, ServletConfig.class);
    }

    public ServletContext getServletContext() {
        return JAXRSUtils.createServletResourceValue(m, ServletContext.class);
    }

}
