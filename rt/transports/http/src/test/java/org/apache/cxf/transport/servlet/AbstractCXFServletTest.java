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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.classextension.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class AbstractCXFServletTest extends Assert {

    @Test
    public void testPost() throws Exception {
        testHttpMethod("POST");
    }
    
    @Test
    public void testGet() throws Exception {
        testHttpMethod("GET");
    }
    
    @Test
    public void testPut() throws Exception {
        testHttpMethod("PUT");
    }
    
    @Test
    public void testDelete() throws Exception {
        testHttpMethod("DELETE");
    }
    
    @Test
    public void testHead() throws Exception {
        testHttpMethod("HEAD");
    }
    
    @Test
    public void testOptions() throws Exception {
        testHttpMethod("OPTIONS");
    }
    
    @Test
    public void testPatch() throws Exception {
        testHttpMethod("PATCH", true);
    }
    
    private void testHttpMethod(String method) throws Exception {
        testHttpMethod(method, false);
    }
    
    private void testHttpMethod(String method, boolean setMethod) throws Exception {
        TestServlet servlet = new TestServlet();
        if (setMethod) {
            servlet.setMethod(method);
        }
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        HttpServletResponse res = EasyMock.createMock(HttpServletResponse.class);
        
        req.getMethod();
        EasyMock.expectLastCall().andReturn(method).times(3);
        EasyMock.replay(req);
        
        servlet.service(req, res);
        assertTrue(servlet.isInvoked());
        assertEquals(method, servlet.getMethod());
    }
    
    private static class TestServlet extends AbstractCXFServlet {

        private boolean invoked;
        private String method;
        @Override
        public void loadBus(ServletConfig servletConfig) throws ServletException {
        }
        
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
            method = "POST";
            super.doPost(request, response);
        }
        
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
            method = "GET";
            super.doGet(request, response);
        }
        
        @Override
        protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
            method = "PUT";
            super.doPut(request, response);
        }
        
        @Override
        protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
            method = "DELETE";
            super.doDelete(request, response);
        }
        
        @Override
        protected void doHead(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
            method = "HEAD";
            super.doHead(request, response);
        }
        
        @Override
        protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
            method = "OPTIONS";
            super.doOptions(request, response);
        }
        
        @Override
        protected void invoke(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
            assertEquals(method, request.getMethod());
            invoked = true;    
        }
        
        public boolean isInvoked() {
            return invoked;
        }
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String m) {
            method = m;
        }
        
    }
    
}
