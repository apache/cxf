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

package org.apache.cxf.transport.http_osgi;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.http.AddressType;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class OsgiServletTest extends Assert {

    private final class TestOsgiServletController extends ServletController {
        private boolean invokeDestinationCalled;


        private TestOsgiServletController(ServletConfig config, 
                                          DestinationRegistry destinationRegistry,
                                          HttpServlet serviceListGenerator) {
            super(destinationRegistry, config, serviceListGenerator);
        }

        @Override
        public void invokeDestination(HttpServletRequest req,
                                      HttpServletResponse res,
                                      AbstractHTTPDestination d) throws ServletException {
            invokeDestinationCalled = true;
        }
    }

    private static final String ADDRESS = "http://bar/snafu";
    private static final String ROOT = "http://localhost:8080/";
    private static final QName QNAME = new QName(ADDRESS, "foobar");
    private static final String PATH = "/SoapContext/SoapPort";
    private static final String URI = "/cxf" + PATH;
    private static final String SERVICES = "/cxf/services";
    private static final String QUERY = "wsdl";
    private static final String TEXT = "text/html";
    private static final String TEXT_LIST = "text/html; charset=UTF-8";
    private static final String XML = "text/xml";
    private static final String NO_SERVICE = 
        "<html><body>No service was found.</body></html>";
    private IMocksControl control; 
    private Bus bus;
    private DestinationRegistry registry;
    private AbstractHTTPDestination destination;
    private ServletConfig config;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private MessageObserver observer;
    private AddressType extensor;
    private EndpointInfo endpoint;
    private Set<String> paths;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        registry = control.createMock(DestinationRegistry.class);
        destination = control.createMock(AbstractHTTPDestination.class);
        config = control.createMock(ServletConfig.class);
        request = control.createMock(HttpServletRequest.class);
        response = control.createMock(HttpServletResponse.class);
        observer = control.createMock(MessageObserver.class);
        extensor = control.createMock(AddressType.class);
        endpoint = new EndpointInfo();
        endpoint.setAddress(PATH);
        endpoint.setName(QNAME);
        ServiceInfo service = new ServiceInfo();
        service.setInterface(new InterfaceInfo(service, QNAME));
        endpoint.setService(service);
        
        paths = new TreeSet<String>();
    }

    @After
    public void tearDown() {
        bus = null;
        registry = null;
        destination = null;
        config = null;
        request = null;
        response = null;
        observer = null;
        extensor = null;
    }

    @Test
    public void testInvokeNoDestination() throws Exception {
        setUpRequest(URI, null, -1);
        setUpResponse(404, TEXT, NO_SERVICE);

        control.replay();
        OsgiServlet servlet = new OsgiServlet(registry);
        servlet.init(config);
        servlet.invoke(request, response);
        control.verify();
    }

    @Test
    public void testInvokeGetServices() throws Exception {
        setUpRequest(SERVICES, null, 1);

        HttpServlet serviceListGenerator = control.createMock(HttpServlet.class);
        serviceListGenerator.service(EasyMock.isA(ServletRequest.class), EasyMock.isA(ServletResponse.class));
        EasyMock.expectLastCall();
        
        control.replay();
        OsgiServlet servlet = new OsgiServlet(registry, serviceListGenerator);
        servlet.init(config);
        servlet.invoke(request, response);
        control.verify();
    }

    @Test
    public void testInvokeGetServicesNoService() throws Exception {
        setUpRequest(SERVICES, null, 0);
        setUpResponse(0, TEXT_LIST, 
                      "<span class=\"heading\">No services have been found.</span>");

        control.replay();
        OsgiServlet servlet = new OsgiServlet(registry);
        servlet.init(config);
        servlet.invoke(request, response);
        control.verify();
    }

    @Test
    public void testInvokeWsdlQuery() throws Exception {
        setUpRequest(URI, PATH, -2);
        setUpQuery();

        control.replay();
        OsgiServlet servlet = new OsgiServlet(registry);
        servlet.init(config);
        servlet.invoke(request, response);
        control.verify();
    }

    @Test
    public void testInvokeDestination() throws Exception {
        setUpRequest(URI, PATH, -2);
        HttpServlet serviceListGenerator = control.createMock(HttpServlet.class);
        
        control.replay();
        TestOsgiServletController controller = 
            new TestOsgiServletController(config, registry, serviceListGenerator);
        controller.invoke(request, response);
        control.verify();
        Assert.assertTrue(controller.invokeDestinationCalled);
    }

    @Test
    public void testInvokeRestful() throws Exception {
        setUpRequest(URI, null, -1);
        //EasyMock.expect(request.getContextPath()).andReturn("");
        //EasyMock.expect(request.getServletPath()).andReturn("/cxf");
        paths.add(PATH);
        EasyMock.expect(registry.getDestinationForPath(PATH)).andReturn(destination).anyTimes();
        EasyMock.expect(registry.checkRestfulRequest(EasyMock.isA(String.class))).andReturn(destination);
        EasyMock.expect(destination.getMessageObserver()).andReturn(observer);
        endpoint.addExtensor(extensor);
        //extensor.setLocation(EasyMock.eq(ROOT + URI));
        //EasyMock.expectLastCall();

        control.replay();
        OsgiServlet servlet = new OsgiServlet(registry);
        servlet.init(config);
        servlet.invoke(request, response);
        control.verify();
    }

    private void setUpRequest(String requestURI,
                              String path,
                              int destinationCount) throws Exception {
        EasyMock.expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        StringBuffer url = new StringBuffer(ROOT + requestURI);
        EasyMock.expect(request.getRequestURL()).andReturn(url).anyTimes();
        EasyMock.expect(request.getQueryString()).andReturn(QUERY).anyTimes();
        EasyMock.expect(destination.getEndpointInfo()).andReturn(endpoint).anyTimes();
        EasyMock.expect(destination.getBus()).andReturn(bus).anyTimes();

        EasyMock.expect(request.getPathInfo()).andReturn(path != null 
                                                ? path
                                                : PATH).anyTimes();
        if (path != null) {
            EasyMock.expect(registry.getDestinationForPath(path, true)).andReturn(destination);
        }

        EasyMock.expect(registry.getDestinationsPaths()).andReturn(paths).anyTimes();
        if (destinationCount >= 0) {
            List<AbstractHTTPDestination> destinations =
                new ArrayList<AbstractHTTPDestination>();
            for (int i = 0; i < destinationCount; i++) {
                destinations.add(destination);
            }
            EasyMock.expect(registry.getDestinations()).andReturn(destinations).anyTimes();
            EasyMock.expect(registry.getSortedDestinations()).
                andReturn(destinations.toArray(new AbstractDestination[]{})).anyTimes();
        }
    }

    private void setUpResponse(int status, 
                               String responseType,
                               String ... responseMsgs) throws Exception {
        if (status != 0) {
            response.setStatus(status);
            EasyMock.expectLastCall();
        }
        if (responseType != null) {
            response.setContentType(responseType);
            EasyMock.expectLastCall();
        }
        if (responseMsgs != null) {
            PrintWriter writer = control.createMock(PrintWriter.class);
            EasyMock.expect(response.getWriter()).andReturn(writer).anyTimes();
            for (String msg : responseMsgs) { 
                writer.write(msg);
                EasyMock.expectLastCall();
            }
        }
    }

    private void setUpQuery() throws Exception {
        QueryHandlerRegistry qrh = 
            control.createMock(QueryHandlerRegistry.class);
        EasyMock.expect(bus.getExtension(QueryHandlerRegistry.class)).andReturn(qrh).anyTimes();
        QueryHandler qh = control.createMock(QueryHandler.class);
        List<QueryHandler> handlers = new ArrayList<QueryHandler>();
        handlers.add(qh);
        EasyMock.expect(qrh.getHandlers()).andReturn(handlers);
        String base = ROOT + URI + "?" + QUERY;
        EasyMock.expect(qh.isRecognizedQuery(EasyMock.eq(base),
                                             EasyMock.eq(PATH),
                                             EasyMock.same(endpoint))).andReturn(Boolean.TRUE);
        EasyMock.expect(qh.getResponseContentType(EasyMock.eq(base),
                                                  EasyMock.eq(PATH))).andReturn(XML);
        ServletOutputStream sos = control.createMock(ServletOutputStream.class);
        EasyMock.expect(response.getOutputStream()).andReturn(sos);
        qh.writeResponse(EasyMock.eq(base), EasyMock.eq(PATH), EasyMock.same(endpoint), EasyMock.same(sos)); 
        EasyMock.expectLastCall();
        sos.flush();
        EasyMock.expectLastCall();
    }

}
