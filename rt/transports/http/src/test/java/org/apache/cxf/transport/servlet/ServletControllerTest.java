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
import java.util.Collections;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServletControllerTest {

    private HttpServletRequest req = mock(HttpServletRequest.class);
    private HttpServletResponse res = mock(HttpServletResponse.class);
    private DestinationRegistry registry = mock(DestinationRegistry.class);
    private HttpServlet serviceListGenerator = mock(HttpServlet.class);

    private void setReq(String pathInfo, String requestUri, String styleSheet, String formatted) {
        when(req.getPathInfo()).thenReturn(pathInfo);
        when(req.getContextPath()).thenReturn("");
        when(req.getServletPath()).thenReturn("");
        when(req.getRequestURI()).thenReturn(requestUri);
        when(req.getParameter("stylesheet")).thenReturn(styleSheet);
        when(req.getParameter("formatted")).thenReturn(formatted);
        when(req.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080").append(requestUri));
        when(registry.getDestinationsPaths()).thenReturn(Collections.emptySet());
        when(registry.getDestinationForPath("", true)).thenReturn(null);
    }

    private void expectServiceListGeneratorCalled() throws ServletException, IOException {
        verify(serviceListGenerator, atLeastOnce()).service(isA(HttpServletRequest.class),
                                     isA(HttpServletResponse.class));
    }

    private void expectServiceListGeneratorNotCalled() throws ServletException, IOException {
    }

    @Test
    public void testGenerateServiceListing() throws Exception {
        setReq(null, "/services", null, "true");
        TestServletController sc = new TestServletController(registry, serviceListGenerator);
        sc.invoke(req, res);
        assertFalse(sc.invokeDestinationCalled());
        verify(req, atLeastOnce()).setAttribute(Message.BASE_PATH, "http://localhost:8080");
        verify(req, times(1)).getRequestURI();
        expectServiceListGeneratorCalled();
    }

    @Test
    public void testGenerateUnformattedServiceListing() throws Exception {
        when(req.getPathInfo()).thenReturn(null);
        when(req.getContextPath()).thenReturn("");
        when(req.getServletPath()).thenReturn("");
        when(req.getRequestURI()).thenReturn("/services");

        when(req.getParameter("stylesheet")).thenReturn(null);
        when(req.getParameter("formatted")).thenReturn("false");
        when(req.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/services"));
        when(registry.getDestinationsPaths()).thenReturn(Collections.emptySet());
        when(registry.getDestinationForPath("", true)).thenReturn(null);

        TestServletController sc = new TestServletController(registry, serviceListGenerator);
        sc.invoke(req, res);
        assertFalse(sc.invokeDestinationCalled());

        verify(req, atLeastOnce()).setAttribute(Message.BASE_PATH, "http://localhost:8080");
        verify(req, times(1)).getRequestURI();
        expectServiceListGeneratorCalled();
    }

    @Test
    public void testHideServiceListing() throws Exception {
        when(req.getPathInfo()).thenReturn(null);

        when(registry.getDestinationForPath("", true)).thenReturn(null);
        AbstractHTTPDestination dest = mock(AbstractHTTPDestination.class);
        when(registry.checkRestfulRequest("")).thenReturn(dest);
        when(dest.getBus()).thenReturn(null);
        when(dest.getMessageObserver()).thenReturn(mock(MessageObserver.class));

        TestServletController sc = new TestServletController(registry, serviceListGenerator);
        sc.setHideServiceList(true);
        sc.invoke(req, res);
        assertTrue(sc.invokeDestinationCalled());

        expectServiceListGeneratorNotCalled();
    }

    @Test
    public void testDifferentServiceListPath() throws Exception {
        setReq(null, "/listing", null, "true");
        TestServletController sc = new TestServletController(registry, serviceListGenerator);
        sc.setServiceListRelativePath("/listing");
        sc.invoke(req, res);
        assertFalse(sc.invokeDestinationCalled());
        
        verify(req, atLeastOnce()).setAttribute(Message.BASE_PATH, "http://localhost:8080");
        verify(req, times(1)).getRequestURI();
        expectServiceListGeneratorCalled();
    }

    @Test
    public void testHealthcheck() throws Exception {
        setReq(null, "/services", null, "true");
        when(req.getAttribute(ServletController.AUTH_SERVICE_LIST)).thenReturn(null);
        when(req.getMethod()).thenReturn("HEAD");

        TestServletController sc = new TestServletController(registry, new ServiceListGeneratorServlet(registry, null));
        sc.invoke(req, res);
        assertFalse(sc.invokeDestinationCalled());
        
        verify(req, atLeastOnce()).setAttribute(Message.BASE_PATH, "http://localhost:8080");
        verify(req, times(1)).getRequestURI();
    }


    public static class TestServletController extends ServletController {
        private boolean invokeDestinationCalled;

        public TestServletController(DestinationRegistry destinationRegistry,
                                     HttpServlet serviceListGenerator) {
            super(destinationRegistry, null, serviceListGenerator);
        }

        public void setHideServiceList(boolean b) {
            this.isHideServiceList = b;
        }

        @Override
        protected void updateDestination(HttpServletRequest request, AbstractHTTPDestination d) {
        }

        @Override
        public void invokeDestination(final HttpServletRequest request, HttpServletResponse response,
                                      AbstractHTTPDestination d) throws ServletException {
            invokeDestinationCalled = true;
        }

        public boolean invokeDestinationCalled() {
            return invokeDestinationCalled;
        }
    }
}
