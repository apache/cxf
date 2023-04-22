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

import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServletControllerTest {

    private HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
    private HttpServletResponse res = EasyMock.createMock(HttpServletResponse.class);
    private DestinationRegistry registry = EasyMock.createMock(DestinationRegistry.class);
    private HttpServlet serviceListGenerator = EasyMock.createMock(HttpServlet.class);

    private void setReq(String pathInfo, String requestUri, String styleSheet, String formatted) {
        EasyMock.expect(req.getPathInfo()).andReturn(pathInfo).anyTimes();
        EasyMock.expect(req.getContextPath()).andReturn("").anyTimes();
        EasyMock.expect(req.getServletPath()).andReturn("").anyTimes();
        req.setAttribute(Message.BASE_PATH, "http://localhost:8080");
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(req.getRequestURI()).andReturn(requestUri).times(2);
        EasyMock.expect(req.getParameter("stylesheet")).andReturn(styleSheet);
        EasyMock.expect(req.getParameter("formatted")).andReturn(formatted);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer("http://localhost:8080").append(requestUri));
        EasyMock.expect(registry.getDestinationsPaths()).andReturn(Collections.emptySet()).atLeastOnce();
        EasyMock.expect(registry.getDestinationForPath("", true)).andReturn(null).anyTimes();
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
        EasyMock.expectLastCall().andReturn("").anyTimes();
        req.getServletPath();
        EasyMock.expectLastCall().andReturn("").anyTimes();
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
        dest.getBus();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
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

    @Test
    public void testHealthcheck() throws Exception {
        setReq(null, "/services", null, "true");
        EasyMock.expect(req.getAttribute(ServletController.AUTH_SERVICE_LIST)).andReturn(null);
        EasyMock.expect(req.getMethod()).andReturn("HEAD");

        EasyMock.replay(req, registry);
        TestServletController sc = new TestServletController(registry, new ServiceListGeneratorServlet(registry, null));
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
