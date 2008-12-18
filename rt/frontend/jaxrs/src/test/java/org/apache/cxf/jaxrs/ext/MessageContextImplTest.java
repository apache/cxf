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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWorkers;

import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class MessageContextImplTest extends Assert {

    @Test
    public void testGetProperty() {
        Message m = new MessageImpl();
        m.put("a", "b");
        MessageContext mc = new MessageContextImpl(m);
        assertEquals("b", mc.get("a"));
        assertNull(mc.get("b"));
    }
    
    @Test
    public void testGetUriInfo() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(UriInfoImpl.class, mc.getUriInfo().getClass());
        assertSame(UriInfoImpl.class, mc.getContext(UriInfo.class).getClass());
    }
    
    @Test
    public void testGetRequest() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(RequestImpl.class, mc.getRequest().getClass());
        assertSame(RequestImpl.class, mc.getContext(Request.class).getClass());
    }
    
    @Test
    public void testGetHttpHeaders() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(HttpHeadersImpl.class, mc.getHttpHeaders().getClass());
        assertSame(HttpHeadersImpl.class, mc.getContext(HttpHeaders.class).getClass());
    }
    
    @Test
    public void testGetSecurityContext() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(SecurityContextImpl.class, mc.getSecurityContext().getClass());
        assertSame(SecurityContextImpl.class, mc.getContext(SecurityContext.class).getClass());
    }
    
    @Test
    public void testProviders() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(ProvidersImpl.class, mc.getProviders().getClass());
        assertSame(ProvidersImpl.class, mc.getContext(MessageBodyWorkers.class).getClass());
    }
    
    @Test
    public void testHttpRequest() {
        Message m = new MessageImpl();
        MessageContext mc = new MessageContextImpl(m);
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, request);
        assertSame(request.getClass(), mc.getHttpServletRequest().getClass());
        assertSame(request.getClass(), mc.getContext(HttpServletRequest.class).getClass());
    }
    
    @Test
    public void testHttpResponse() {
        Message m = new MessageImpl();
        MessageContext mc = new MessageContextImpl(m);
        HttpServletResponse request = EasyMock.createMock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, request);
        assertSame(request.getClass(), mc.getHttpServletResponse().getClass());
        assertSame(request.getClass(), mc.getContext(HttpServletResponse.class).getClass());
    }
    
    @Test
    public void testServletContext() {
        Message m = new MessageImpl();
        MessageContext mc = new MessageContextImpl(m);
        ServletContext request = EasyMock.createMock(ServletContext.class);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, request);
        assertSame(request.getClass(), mc.getServletContext().getClass());
        assertSame(request.getClass(), mc.getContext(ServletContext.class).getClass());
    }
    
    @Test
    public void testServletConfig() {
        Message m = new MessageImpl();
        MessageContext mc = new MessageContextImpl(m);
        ServletConfig request = EasyMock.createMock(ServletConfig.class);
        m.put(AbstractHTTPDestination.HTTP_CONFIG, request);
        assertSame(request.getClass(), mc.getServletConfig().getClass());
        assertSame(request.getClass(), mc.getContext(ServletConfig.class).getClass());
    }
    
    @Test
    public void testNoContext() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertNull(mc.getContext(Message.class));
    }
}
