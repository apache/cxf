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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServletControllerTest extends Assert {

    private HttpServletRequest req;
    private HttpServletResponse res;
    private DestinationRegistry registry;
    private HttpServlet serviceListGenerator;
    
    @Before
    public void setUp() {
        req = EasyMock.createMock(HttpServletRequest.class);
        res = EasyMock.createMock(HttpServletResponse.class);
        registry = EasyMock.createMock(DestinationRegistry.class);
        serviceListGenerator = EasyMock.createMock(HttpServlet.class);
    }
    
    private void setReq(String pathInfo, String requestUri, String styleSheet, String formatted) {
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn(pathInfo).anyTimes();
        req.getContextPath();
        EasyMock.expectLastCall().andReturn("");
        req.getServletPath();
        EasyMock.expectLastCall().andReturn("");
        req.setAttribute(Message.BASE_PATH, "http://localhost:8080");
        EasyMock.expectLastCall().anyTimes();
        req.getRequestURI();
        EasyMock.expectLastCall().andReturn(requestUri).times(2);
        req.getParameter("stylesheet");
        EasyMock.expectLastCall().andReturn(styleSheet);
        req.getParameter("formatted");
        EasyMock.expectLastCall().andReturn(formatted);
        req.getRequestURL();
        EasyMock.expectLastCall().andReturn(new StringBuffer("http://localhost:8080" + requestUri));
        registry.getDestinationsPaths();
        EasyMock.expectLastCall().andReturn(Collections.emptySet()).atLeastOnce();
        registry.getDestinationForPath("", true);
        EasyMock.expectLastCall().andReturn(null).anyTimes();
    }
    
    private void expectServiceListGeneratorCalled() throws ServletException, IOException {
        serviceListGenerator.service(EasyMock.isA(HttpServletRequest.class), 
                                     EasyMock.isA(HttpServletResponse.class));
        EasyMock.expectLastCall();
    }
    
    private void expectServiceListGeneratorNotCalled() throws ServletException, IOException {
    }
    
    @Test
    public void testGenerateServiceListing() throws Exception {
        setReq(null, "/services", null, "true");
        expectServiceListGeneratorCalled();
        EasyMock.replay(req, registry, serviceListGenerator);
        TestServletController sc = new TestServletController(registry, serviceListGenerator);
        sc.invoke(req, res);
        assertFalse(sc.invokeDestinationCalled());
    }
    
    @Test
    public void testGenerateUnformattedServiceListing() throws Exception {
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        req.getContextPath();
        EasyMock.expectLastCall().andReturn("");
        req.getServletPath();
        EasyMock.expectLastCall().andReturn("");
        req.getRequestURI();
        EasyMock.expectLastCall().andReturn("/services").times(2);
        
        req.getParameter("stylesheet");
        EasyMock.expectLastCall().andReturn(null);
        req.getParameter("formatted");
        EasyMock.expectLastCall().andReturn("false");
        req.getRequestURL();
        EasyMock.expectLastCall().andReturn(new StringBuffer("http://localhost:8080/services"));
        req.setAttribute(Message.BASE_PATH, "http://localhost:8080");
        EasyMock.expectLastCall().anyTimes();
        registry.getDestinationsPaths();
        EasyMock.expectLastCall().andReturn(Collections.emptySet()).atLeastOnce();
        registry.getDestinationForPath("", true);
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        
        expectServiceListGeneratorCalled();
        EasyMock.replay(req, registry, serviceListGenerator);
        
        TestServletController sc = new TestServletController(registry, serviceListGenerator);
        sc.invoke(req, res);
        assertFalse(sc.invokeDestinationCalled());
    }
    
    @Test
    public void testHideServiceListing() throws Exception {
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn(null);
        
        registry.getDestinationForPath("", true);
        EasyMock.expectLastCall().andReturn(null).atLeastOnce();
        AbstractHTTPDestination dest = EasyMock.createMock(AbstractHTTPDestination.class);
        registry.checkRestfulRequest("");
        EasyMock.expectLastCall().andReturn(dest).atLeastOnce();
        dest.getMessageObserver();
        EasyMock.expectLastCall().andReturn(EasyMock.createMock(MessageObserver.class)).atLeastOnce();
        
        expectServiceListGeneratorNotCalled();
        
        EasyMock.replay(req, registry, serviceListGenerator, dest);
        TestServletController sc = new TestServletController(registry, serviceListGenerator);
        sc.setHideServiceList(true);
        sc.invoke(req, res);
        assertTrue(sc.invokeDestinationCalled());
    }
    
    @Test
    public void testDifferentServiceListPath() throws Exception {
        setReq(null, "/listing", null, "true");
        expectServiceListGeneratorCalled();
        EasyMock.replay(req, registry, serviceListGenerator);
        TestServletController sc = new TestServletController(registry, serviceListGenerator);
        sc.setServiceListRelativePath("/listing");
        sc.invoke(req, res);
        assertFalse(sc.invokeDestinationCalled());
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
