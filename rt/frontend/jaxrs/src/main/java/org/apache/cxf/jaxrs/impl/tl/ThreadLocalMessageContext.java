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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.jaxrs.ext.MessageContext;

public class ThreadLocalMessageContext extends AbstractThreadLocalProxy<MessageContext> 
    implements MessageContext {

    public Object get(Object key) {
        return get().get(key);
    }

    public <T> T getContext(Class<T> contextClass) {
        return get().getContext(contextClass);
    }

    public HttpHeaders getHttpHeaders() {
        return get().getHttpHeaders();
    }

    public HttpServletRequest getHttpServletRequest() {
        return get().getHttpServletRequest();
    }

    public HttpServletResponse getHttpServletResponse() {
        return get().getHttpServletResponse();
    }

    public Providers getProviders() {
        return get().getProviders();
    }

    public SecurityContext getSecurityContext() {
        return get().getSecurityContext();
    }

    public ServletConfig getServletConfig() {
        return get().getServletConfig();
    }

    public ServletContext getServletContext() {
        return get().getServletContext();
    }

    public UriInfo getUriInfo() {
        return get().getUriInfo();
    }

    public Request getRequest() {
        return get().getRequest();
    }

}
