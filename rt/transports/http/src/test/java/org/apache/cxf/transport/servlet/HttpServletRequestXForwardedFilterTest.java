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

package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.util.function.BiConsumer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpServletRequestXForwardedFilterTest {
    private AbstractHTTPServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ServletConfig config;
    
    @Before
    public void setUp() throws ServletException {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(request.getContextPath()).thenReturn("/api");
        when(request.getServletPath()).thenReturn("");
        
        config = mock(ServletConfig.class);
        when(config.getInitParameter(eq("use-x-forwarded-headers"))).thenReturn("true");
    }

    @Test
    public void testNoXForwardedHeadersSpecified() throws Exception {
        servlet = servlet((req, resp) -> {
            assertThat(req.getRequestURI(), equalTo("/test"));
            assertThat(req.getContextPath(), equalTo("/api"));
            assertThat(req.getRequestURL().toString(), equalTo("http://localhost/api/test"));
            assertThat(req.getRemoteAddr(), nullValue());
            assertThat(req.isSecure(), equalTo(false));
        });
        
        servlet.init(config);
        servlet.service(request, response);
    }

    @Test
    public void testAllXForwardedHeadersSpecified() throws Exception {
        servlet = servlet((req, resp) -> {
            assertThat(req.getRequestURI(), equalTo("/forwarded/test"));
            assertThat(req.getContextPath(), equalTo("/forwarded"));
            assertThat(req.getServletPath(), equalTo("/api"));
            assertThat(req.getRequestURL().toString(), equalTo("https://abc:50000/forwarded/api/test"));
            assertThat(req.getRemoteAddr(), equalTo("203.0.113.195"));
            assertThat(req.isSecure(), equalTo(true));
        });
        
        when(request.getHeader(eq("X-Forwarded-Proto"))).thenReturn("https");
        when(request.getHeader(eq("X-Forwarded-For"))).thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");
        when(request.getHeader(eq("X-Forwarded-Prefix"))).thenReturn("/forwarded");
        when(request.getHeader(eq("X-Forwarded-Host"))).thenReturn("abc");
        when(request.getHeader(eq("X-Forwarded-Port"))).thenReturn("50000");

        servlet.init(config);
        servlet.service(request, response);
    }

    @Test
    public void testXForwardedProtoHeaderSpecified() throws Exception {
        servlet = servlet((req, resp) -> {
            assertThat(req.getRequestURI(), equalTo("/test"));
            assertThat(req.getContextPath(), equalTo("/api"));
            assertThat(req.getServletPath(), equalTo(""));
            assertThat(req.getRequestURL().toString(), equalTo("https://localhost/api/test"));
            assertThat(req.getRemoteAddr(), nullValue());
            assertThat(req.isSecure(), equalTo(true));
        });
        
        when(request.getHeader(eq("X-Forwarded-Proto"))).thenReturn("https");
        
        servlet.init(config);
        servlet.service(request, response);
    }
    
    @Test
    public void testXForwardedForHeaderSpecified() throws Exception {
        servlet = servlet((req, resp) -> {
            assertThat(req.getRequestURI(), equalTo("/test"));
            assertThat(req.getContextPath(), equalTo("/api"));
            assertThat(req.getServletPath(), equalTo(""));
            assertThat(req.getRequestURL().toString(), equalTo("http://localhost/api/test"));
            assertThat(req.getRemoteAddr(), equalTo("203.0.113.195"));
            assertThat(req.isSecure(), equalTo(false));
        });
        
        when(request.getHeader(eq("X-Forwarded-For"))).thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");
        
        servlet.init(config);
        servlet.service(request, response);
    }

    @Test
    public void testXForwardedPrefixHeaderSpecified() throws Exception {
        servlet = servlet((req, resp) -> {
            assertThat(req.getRequestURI(), equalTo("/forwarded/test"));
            assertThat(req.getContextPath(), equalTo("/forwarded"));
            assertThat(req.getServletPath(), equalTo("/api"));
            assertThat(req.getRequestURL().toString(), equalTo("http://localhost/forwarded/api/test"));
            assertThat(req.getRemoteAddr(), nullValue());
            assertThat(req.isSecure(), equalTo(false));
        });
        
        when(request.getHeader(eq("X-Forwarded-Prefix"))).thenReturn("/forwarded");

        servlet.init(config);
        servlet.service(request, response);
    }
    
    @Test
    public void testXForwardedHostHeaderSpecified() throws Exception {
        servlet = servlet((req, resp) -> {
            assertThat(req.getRequestURI(), equalTo("/test"));
            assertThat(req.getContextPath(), equalTo("/api"));
            assertThat(req.getServletPath(), equalTo(""));
            assertThat(req.getRequestURL().toString(), equalTo("http://abc/api/test"));
            assertThat(req.getRemoteAddr(), nullValue());
            assertThat(req.isSecure(), equalTo(false));
        });
        
        when(request.getHeader(eq("X-Forwarded-Host"))).thenReturn("abc");

        servlet.init(config);
        servlet.service(request, response);
    }
    
    @Test
    public void testXForwardedPortHeaderSpecified() throws Exception {
        servlet = servlet((req, resp) -> {
            assertThat(req.getRequestURI(), equalTo("/test"));
            assertThat(req.getContextPath(), equalTo("/api"));
            assertThat(req.getServletPath(), equalTo(""));
            assertThat(req.getRequestURL().toString(), equalTo("http://localhost:50000/api/test"));
            assertThat(req.getRemoteAddr(), nullValue());
            assertThat(req.isSecure(), equalTo(false));
        });
        
        when(request.getHeader(eq("X-Forwarded-Port"))).thenReturn("50000");

        servlet.init(config);
        servlet.service(request, response);
    }
    
    private static AbstractHTTPServlet servlet(BiConsumer<HttpServletRequest, HttpServletResponse> assertions) {
        return new AbstractHTTPServlet() {
            private static final long serialVersionUID = -3870709934037062681L;

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
                    throws IOException, ServletException {
            }
            
            @Override
            protected void invoke(HttpServletRequest request, HttpServletResponse response) 
                    throws ServletException {
                assertions.accept(request, response);
            }
            
            @Override
            protected Bus getBus() {
                return BusFactory.getDefaultBus();
            }
        };
    }
    
}
