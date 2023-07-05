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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.UUID;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;
import jakarta.xml.bind.JAXBContext;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.HttpServletRequestFilter;
import org.apache.cxf.jaxrs.impl.HttpServletResponseFilter;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageContextImplTest {

    @Test
    public void testGetProperty() {
        Message m = new MessageImpl();
        m.put("a", "b");
        MessageContext mc = new MessageContextImpl(m);
        assertEquals("b", mc.get("a"));
        assertNull(mc.get("b"));
    }
    @Test
    public void testGetPropertyFromExchange() {
        Message m = new MessageImpl();
        Exchange ex = new ExchangeImpl();
        ex.put("a", "b");
        ex.setInMessage(m);
        MessageContext mc = new MessageContextImpl(m);
        assertEquals("b", mc.get("a"));
        assertNull(mc.get("b"));
    }
    @Test
    public void testGetPropertyFromOtherMessage() {
        Message m1 = new MessageImpl();
        Message m2 = new MessageImpl();
        m2.put("a", "b");

        Exchange ex = new ExchangeImpl();
        ex.setInMessage(m1);
        ex.setOutMessage(m2);
        MessageContext mc = new MessageContextImpl(m1);
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
        assertSame(ProvidersImpl.class, mc.getContext(Providers.class).getClass());
    }

    @Test
    public void testHttpRequest() {
        Message m = createMessage();
        MessageContext mc = new MessageContextImpl(m);
        HttpServletRequest request = mock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, request);

        assertSame(request.getClass(),
                   ((HttpServletRequestFilter)mc.getHttpServletRequest()).getRequest().getClass());
        assertSame(request.getClass(),
                   ((HttpServletRequestFilter)mc.getContext(HttpServletRequest.class)).getRequest().getClass());
    }

    @Test
    public void testHttpResponse() {
        Message m = createMessage();
        MessageContext mc = new MessageContextImpl(m);
        HttpServletResponse request = mock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, request);
        HttpServletResponseFilter filter = (HttpServletResponseFilter)mc.getHttpServletResponse();
        assertSame(request.getClass(), filter.getResponse().getClass());
        filter = (HttpServletResponseFilter)mc.getContext(HttpServletResponse.class);
        assertSame(request.getClass(), filter.getResponse().getClass());
    }

    @Test
    public void testServletContext() {
        Message m = createMessage();
        MessageContext mc = new MessageContextImpl(m);
        ServletContext request = mock(ServletContext.class);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, request);
        assertSame(request.getClass(), mc.getServletContext().getClass());
        assertSame(request.getClass(), mc.getContext(ServletContext.class).getClass());
    }

    @Test
    public void testServletConfig() {
        Message m = createMessage();
        MessageContext mc = new MessageContextImpl(m);
        ServletConfig request = mock(ServletConfig.class);
        m.put(AbstractHTTPDestination.HTTP_CONFIG, request);
        assertSame(request.getClass(), mc.getServletConfig().getClass());
        assertSame(request.getClass(), mc.getContext(ServletConfig.class).getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testContextResolver() {
        ContextResolver<JAXBContext> resolver = new CustomContextResolver();
        ProviderFactory factory = ServerProviderFactory.getInstance();
        factory.registerUserProvider(resolver);

        Message m = new MessageImpl();
        Exchange ex = new ExchangeImpl();
        m.setExchange(ex);
        ex.setInMessage(m);
        Endpoint e = mock(Endpoint.class);
        when(e.get(ServerProviderFactory.class.getName())).thenReturn(factory);
        ex.put(Endpoint.class, e);
        MessageContext mc = new MessageContextImpl(m);
        ContextResolver<JAXBContext> resolver2 =
            mc.getResolver(ContextResolver.class, JAXBContext.class);
        assertNotNull(resolver2);
        assertSame(resolver2, resolver);
    }

    @Test
    public void testNoContext() {
        MessageContext mc = new MessageContextImpl(createMessage());
        assertNull(mc.getContext(Message.class));
    }
    
    @Test
    public void testAttachments() throws IOException {
        final Message in = createMessage();
        final MessageContext mc = new MessageContextImpl(in);
        
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final Message out = new MessageImpl();
            out.put(Message.CONTENT_TYPE, "image/png");
            out.setContent(OutputStream.class, output);
            out.setInterceptorChain(new PhaseInterceptorChain(Collections.emptySortedSet()));
            in.getExchange().setOutMessage(out);
            
            final Binding binding = in.getExchange().getEndpoint().getBinding();
            when(binding.createMessage(any(Message.class))).thenAnswer(i -> i.getArguments()[0]);
    
            final String id = UUID.randomUUID().toString();
            final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
            // Headers should be case-insensitive
            headers.add("Content-Id", id);
            mc.put(MultipartBody.OUTBOUND_MESSAGE_ATTACHMENTS, 
                Collections.singletonList(new Attachment(headers, new byte[0])));

            output.flush();
            assertThat(new String(output.toByteArray()), containsString("Content-ID: <" + id + ">"));
        }
    }

    private Message createMessage() {
        ProviderFactory factory = ServerProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Binding binding = mock(Binding.class);
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(null);
        when(endpoint.get(Application.class.getName())).thenReturn(null);
        when(endpoint.size()).thenReturn(0);
        when(endpoint.isEmpty()).thenReturn(true);
        when(endpoint.get(ServerProviderFactory.class.getName())).thenReturn(factory);
        when(endpoint.getBinding()).thenReturn(binding);
        e.put(Endpoint.class, endpoint);
        return m;
    }

    public static class CustomContextResolver implements ContextResolver<JAXBContext> {

        public JAXBContext getContext(Class<?> type) {
            return null;
        }

    }
}