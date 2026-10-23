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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.ResourceManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractHTTPServletTest {

    private static final String WEB_XML = "<web-app/>";

    @Test
    public void testStaticResourceUnderWebInfIsNotServed() throws Exception {
        ServletContext sc = mockServletContext("/WEB-INF/web.xml", WEB_XML);
        TestServlet servlet = initServlet(sc, "static-resources-list", "/.*\\.xml");
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream bos = captureOutput(response);

        servlet.doGet(mockRequest("/WEB-INF/web.xml"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(sc, never()).getResourceAsStream(anyString());
        assertEquals("", bos.toString(StandardCharsets.UTF_8));
        assertFalse(servlet.invoked);
    }

    @Test
    public void testStaticResourceUnderMetaInfIsNotServed() throws Exception {
        ServletContext sc = mockServletContext("/meta-inf/context.xml", WEB_XML);
        TestServlet servlet = initServlet(sc, "static-resources-list", "/.*\\.xml");
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream bos = captureOutput(response);

        servlet.doGet(mockRequest("/meta-inf/context.xml"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(sc, never()).getResourceAsStream(anyString());
        assertEquals("", bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void testStaticResourceOutsideWebInfIsServed() throws Exception {
        ServletContext sc = mockServletContext("/index.xml", WEB_XML);
        TestServlet servlet = initServlet(sc, "static-resources-list", "/.*\\.xml");
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream bos = captureOutput(response);

        servlet.doGet(mockRequest("/index.xml"), response);

        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(WEB_XML, bos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void testRedirectUnderWebInfIsNotForwarded() throws Exception {
        ServletContext sc = mock(ServletContext.class);
        RequestDispatcher rd = mock(RequestDispatcher.class);
        when(sc.getRequestDispatcher(anyString())).thenReturn(rd);
        TestServlet servlet = initServlet(sc, "redirects-list", "/.*\\.jsp");
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doGet(mockRequest("/WEB-INF/views/admin.jsp"), response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(sc, never()).getRequestDispatcher(anyString());
        assertFalse(servlet.invoked);
    }

    private static ServletContext mockServletContext(String path, String content) {
        ServletContext sc = mock(ServletContext.class);
        when(sc.getResourceAsStream(path))
            .thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return sc;
    }

    private static TestServlet initServlet(ServletContext sc, String paramName, String paramValue)
        throws ServletException {
        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(sc);
        when(config.getInitParameter(paramName)).thenReturn(paramValue);
        if (!"redirects-list".equals(paramName)) {
            when(config.getInitParameter("redirect-servlet-path")).thenReturn(null);
        } else {
            when(config.getInitParameter("redirect-servlet-path")).thenReturn("/");
        }
        TestServlet servlet = new TestServlet(sc);
        servlet.init(config);
        return servlet;
    }

    private static HttpServletRequest mockRequest(String pathInfo) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getPathInfo()).thenReturn(pathInfo);
        when(request.getMethod()).thenReturn("GET");
        return request;
    }

    private static ByteArrayOutputStream captureOutput(HttpServletResponse response) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int b) {
                bos.write(b);
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
            }
        });
        return bos;
    }

    private static final class TestServlet extends AbstractHTTPServlet {
        private static final long serialVersionUID = 1L;
        private final Bus bus;
        private boolean invoked;

        TestServlet(ServletContext sc) {
            bus = mock(Bus.class);
            when(bus.getExtension(ResourceManager.class))
                .thenReturn(new DefaultResourceManager(new ServletContextResourceResolver(sc)));
        }

        @Override
        protected Bus getBus() {
            return bus;
        }

        @Override
        protected void invoke(HttpServletRequest request, HttpServletResponse response) {
            invoked = true;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        }
    }
}
