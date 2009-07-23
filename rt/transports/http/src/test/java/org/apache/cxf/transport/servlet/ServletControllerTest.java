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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.MessageObserver;
import org.easymock.classextension.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServletControllerTest extends Assert {

    private HttpServletRequest req;
    private HttpServletResponse res;
    
    @Before
    public void setUp() {
        req = EasyMock.createMock(HttpServletRequest.class);
        res = EasyMock.createMock(HttpServletResponse.class);
    }
    
    @Test
    public void testGenerateServiceListing() throws Exception {
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn(null);
        req.getRequestURI();
        EasyMock.expectLastCall().andReturn("/services");
        req.getParameter("stylesheet");
        EasyMock.expectLastCall().andReturn(null);
        req.getParameter("formatted");
        EasyMock.expectLastCall().andReturn("true");
        EasyMock.replay(req);
        TestServletController sc = new TestServletController();
        sc.invoke(req, res);
        assertTrue(sc.generateListCalled());
        assertFalse(sc.generateUnformattedCalled());
        assertFalse(sc.invokeDestinationCalled());
    }
    
    @Test
    public void testGenerateUnformattedServiceListing() throws Exception {
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn(null);
        req.getRequestURI();
        EasyMock.expectLastCall().andReturn("/services");
        req.getParameter("stylesheet");
        EasyMock.expectLastCall().andReturn(null);
        req.getParameter("formatted");
        EasyMock.expectLastCall().andReturn("false");
        EasyMock.replay(req);
        TestServletController sc = new TestServletController();
        sc.invoke(req, res);
        assertFalse(sc.generateListCalled());
        assertTrue(sc.generateUnformattedCalled());
        assertFalse(sc.invokeDestinationCalled());
    }
    
    @Test
    public void testHideServiceListing() throws Exception {
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn(null);
        EasyMock.replay(req);
        TestServletController sc = new TestServletController();
        sc.setHideServiceList(true);
        sc.invoke(req, res);
        assertFalse(sc.generateListCalled());
        assertFalse(sc.generateUnformattedCalled());
        assertTrue(sc.invokeDestinationCalled());
    }
    
    @Test
    public void testDifferentServiceListPath() throws Exception {
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn(null);
        req.getRequestURI();
        EasyMock.expectLastCall().andReturn("/listing");
        req.getParameter("stylesheet");
        EasyMock.expectLastCall().andReturn(null);
        req.getParameter("formatted");
        EasyMock.expectLastCall().andReturn("true");
        EasyMock.replay(req);
        TestServletController sc = new TestServletController();
        sc.setServiceListRelativePath("/listing");
        sc.invoke(req, res);
        assertTrue(sc.generateListCalled());
        assertFalse(sc.generateUnformattedCalled());
        assertFalse(sc.invokeDestinationCalled());
    }
    
    @Test
    public void testGetRequestURL() throws Exception {
        req.getRequestURL();
        EasyMock.expectLastCall().andReturn(
            new StringBuffer("http://localhost:8080/services/bar")).times(2);
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn("/bar").anyTimes();
        EasyMock.replay(req);
        String url = new ServletController().getBaseURL(req);
        assertEquals("http://localhost:8080/services", url);
        
    }
    
    @Test
    public void testGetRequestURLSingleMatrixParam() throws Exception {
        req.getRequestURL();
        EasyMock.expectLastCall().andReturn(
            new StringBuffer("http://localhost:8080/services/bar;a=b")).times(2);
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn("/bar").anyTimes();
        EasyMock.replay(req);
        String url = new ServletController().getBaseURL(req);
        assertEquals("http://localhost:8080/services", url);
        
    }
    
    @Test
    public void testGetRequestURLMultipleMatrixParam() throws Exception {
        req.getRequestURL();
        EasyMock.expectLastCall().andReturn(
            new StringBuffer("http://localhost:8080/services/bar;a=b;c=d;e=f")).times(2);        
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn("/bar").anyTimes();
        EasyMock.replay(req);
        String url = new ServletController().getBaseURL(req);
        assertEquals("http://localhost:8080/services", url);
        
    }
    
    @Test
    public void testGetRequestURLMultipleMatrixParam2() throws Exception {
        req.getRequestURL();
        EasyMock.expectLastCall().andReturn(
            new StringBuffer("http://localhost:8080/services/bar;a=b;c=d;e=f")).times(2);        
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn("/bar;a=b;c=d").anyTimes();
        EasyMock.replay(req);
        String url = new ServletController().getBaseURL(req);
        assertEquals("http://localhost:8080/services", url);
        
    }
    
    @Test
    public void testGetRequestURLMultipleMatrixParam3() throws Exception {
        req.getRequestURL();
        EasyMock.expectLastCall().andReturn(
            new StringBuffer("http://localhost:8080/services/bar;a=b;c=d;e=f")).times(2);        
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn("/bar;a=b").anyTimes();
        EasyMock.replay(req);
        String url = new ServletController().getBaseURL(req);
        assertEquals("http://localhost:8080/services", url);
        
    }
    
    @Test
    public void testGetRequestURLMultipleMatrixParam4() throws Exception {
        req.getRequestURL();
        EasyMock.expectLastCall().andReturn(
            new StringBuffer("http://localhost:8080/services/bar;a=b;c=d;e=f;")).times(2);        
        req.getPathInfo();
        EasyMock.expectLastCall().andReturn("/bar;a=b").anyTimes();
        EasyMock.replay(req);
        String url = new ServletController().getBaseURL(req);
        assertEquals("http://localhost:8080/services", url);
        
    }
    
    public static class TestServletController extends ServletController {
        
        private boolean generateListCalled;
        private boolean generateUnformattedCalled;
        private boolean invokeDestinationCalled;
        
        @Override
        protected ServletDestination getDestination(String address) {
            return null;
        }
        
        @Override
        protected void updateDests(HttpServletRequest request) { 
        }

        @Override
        protected ServletDestination checkRestfulRequest(HttpServletRequest request) 
            throws IOException {
            ServletDestination sd = EasyMock.createMock(ServletDestination.class);
            sd.getMessageObserver();
            EasyMock.expectLastCall().andReturn(EasyMock.createMock(MessageObserver.class));
            EasyMock.replay(sd);
            return sd;
        }
        
        @Override
        public void invokeDestination(final HttpServletRequest request, HttpServletResponse response,
                                      ServletDestination d) throws ServletException {
            invokeDestinationCalled = true;
        }
        
        @Override
        protected void generateServiceList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
            generateListCalled = true;
        }
        
        @Override
        protected void generateUnformattedServiceList(HttpServletRequest request, 
                                                      HttpServletResponse response) throws IOException {
            generateUnformattedCalled = true;
        }
        
        public boolean generateListCalled() {
            return generateListCalled;
        }
        
        public boolean generateUnformattedCalled() {
            return generateUnformattedCalled;
        }
        
        public boolean invokeDestinationCalled() {
            return invokeDestinationCalled;
        }
    }
}
