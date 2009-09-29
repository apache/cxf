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

package org.apache.servicemix.cxf.transport.http_osgi;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.HTTPSession;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.cxf.wsdl.http.AddressType;

import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.classextension.EasyMock.*;

public class OsgiServletTest extends Assert {

    private static final String ADDRESS = "http://bar/snafu";
    private static final String ROOT = "http://localhost:8080/";
    private static final QName QNAME = new QName(ADDRESS, "foobar");
    private static final String PATH = "/SoapContext/SoapPort";
    private static final String URI = "/cxf" + PATH;
    private static final String SERVICES = "/cxf/services";
    private static final String QUERY = "wsdl";
    private static final String VERB = "POST";
    private static final String TEXT = "text/html";
    private static final String XML = "text/xml";
    private static final String ENCODING = "UTF-8";
    private static final String NO_SERVICE = 
        "<html><body>No service was found.</body></html>";
    private static final String HEADER = "<html><body>";
    private static final String SERVICE = "<p> <a href=\"" + ADDRESS + "?wsdl\">";
    private static final String NAME = QNAME.toString() + "</a> </p>";
    private static final String FOOTER = "</body></html>";
    private IMocksControl control; 
    private Bus bus;
    private OsgiDestinationRegistryIntf registry;
    private OsgiDestination destination;
    private ServletConfig config;
    private ServletContext context;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private MessageImpl message;
    private MessageObserver observer;
    private AddressType extensor;
    private ExchangeImpl exchange;
    private EndpointInfo endpoint;
    private Set<String> paths;

    @Before
    public void setUp() {
        control = createNiceControl();
        bus = control.createMock(Bus.class);
        registry = control.createMock(OsgiDestinationRegistryIntf.class);
        destination = control.createMock(OsgiDestination.class);
        context = control.createMock(ServletContext.class);
        config = control.createMock(ServletConfig.class);
        request = control.createMock(HttpServletRequest.class);
        response = control.createMock(HttpServletResponse.class);
        message = control.createMock(MessageImpl.class);
        exchange = control.createMock(ExchangeImpl.class);
        observer = control.createMock(MessageObserver.class);
        extensor = control.createMock(AddressType.class);
        endpoint = new EndpointInfo();
        endpoint.setAddress(ADDRESS);
        endpoint.setName(QNAME);
        paths = new TreeSet<String>();
    }

    @After
    public void tearDown() {
        bus = null;
        registry = null;
        destination = null;
        context = null;
        config = null;
        request = null;
        response = null;
        message = null;
        exchange = null;
        destination = null;
        exchange = null;
        observer = null;
        extensor = null;
    }

    @Test
    public void testInvokeNoDestination() throws Exception {
        setUpRequest(URI, null, -1);
        setUpResponse(404, TEXT, NO_SERVICE);

        control.replay();

        OsgiServlet servlet = setUpServlet();

        servlet.invoke(request, response);

        control.verify();
    }

    @Test
    public void testInvokeGetServices() throws Exception {
        setUpRequest(SERVICES, null, 1);
        setUpResponse(0, TEXT, HEADER, SERVICE, NAME, FOOTER);

        control.replay();

        OsgiServlet servlet = setUpServlet();

        servlet.invoke(request, response);

        control.verify();
    }

    @Test
    public void testInvokeGetServicesNoService() throws Exception {
        setUpRequest(SERVICES, null, 0);
        setUpResponse(0, TEXT, HEADER, FOOTER);

        control.replay();

        OsgiServlet servlet = setUpServlet();

        servlet.invoke(request, response);

        control.verify();
    }

    @Test
    public void testInvokeWsdlQuery() throws Exception {
        setUpRequest(URI, PATH, -2);
        setUpQuery();

        control.replay();

        OsgiServlet servlet = setUpServlet();
        
        servlet.invoke(request, response);

        control.verify();
    }

    @Test
    public void testInvokeDestination() throws Exception {
        setUpRequest(URI, PATH, -2);
        setUpMessage();

        control.replay();

        OsgiServlet servlet = setUpServlet();
        
        servlet.invoke(request, response);

        control.verify();
    }

    @Test
    public void testInvokeRestful() throws Exception {
        setUpRequest(URI, null, -1);
        setUpRestful();
        setUpMessage();

        control.replay();

        OsgiServlet servlet = setUpServlet();

        servlet.invoke(request, response);

        control.verify();
    }

    private void setUpRequest(String requestURI,
                              String path,
                              int destinationCount) throws Exception {
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        StringBuffer url = new StringBuffer(ROOT + requestURI);
        expect(request.getRequestURL()).andReturn(url).anyTimes();
        expect(request.getQueryString()).andReturn(QUERY).anyTimes();
        expect(destination.getEndpointInfo()).andReturn(endpoint).anyTimes();
        expect(destination.getBus()).andReturn(bus).anyTimes();

        expect(request.getPathInfo()).andReturn(path != null 
                                                ? path
                                                : ADDRESS).anyTimes();
        if (path != null) {
            expect(registry.getDestinationForPath(path)).andReturn(destination);
        }

        if (destinationCount == -1) {
            expect(registry.getDestinationsPaths()).andReturn(paths).anyTimes();
        } else if (destinationCount >= 0) {
            expect(registry.getDestinationsPaths()).andReturn(paths);
            List<OsgiDestination> destinations =
                new ArrayList<OsgiDestination>();
            for (int i = 0; i < destinationCount; i++) {
                destinations.add(destination);
            }
            expect(registry.getDestinations()).andReturn(destinations);
        }
    }

    private void setUpMessage() throws Exception {
        ServletInputStream sis = control.createMock(ServletInputStream.class);
        expect(request.getInputStream()).andReturn(sis);
        message.setContent(eq(InputStream.class), same(sis));
        expectLastCall();
        setUpProperty(AbstractHTTPDestination.HTTP_REQUEST, request);
        setUpProperty(AbstractHTTPDestination.HTTP_RESPONSE, response);
        setUpProperty(AbstractHTTPDestination.HTTP_CONTEXT, context);
        setUpProperty(AbstractHTTPDestination.HTTP_CONFIG, config);
        expect(request.getMethod()).andReturn(VERB);
        setUpProperty(Message.HTTP_REQUEST_METHOD, VERB);
        setUpProperty(Message.REQUEST_URI, URI);
        setUpProperty(Message.QUERY_STRING, QUERY);
        expect(request.getContentType()).andReturn(XML);
        setUpProperty(Message.CONTENT_TYPE, XML);
        expect(request.getHeader("Accept")).andReturn(XML);
        setUpProperty(Message.ACCEPT_CONTENT_TYPE, XML);
        destination.getAddress();
        expectLastCall().andReturn(EndpointReferenceUtils.getEndpointReference(PATH));
        setUpProperty(Message.BASE_PATH, PATH);
        message.put(eq(SecurityContext.class), isA(SecurityContext.class));
        expect(request.getCharacterEncoding()).andReturn(ENCODING);
        setUpProperty(Message.ENCODING, ENCODING);
        exchange.setSession(isA(HTTPSession.class));
        expectLastCall();
    }

    private void setUpProperty(String name, Object value) {
        message.put(eq(name), same(value));
        expectLastCall().andReturn(null).anyTimes();
    }

    private void setUpResponse(int status, 
                               String responseType,
                               String ... responseMsgs) throws Exception {
        if (status != 0) {
            response.setStatus(status);
            expectLastCall();
        }
        if (responseType != null) {
            response.setContentType(responseType);
            expectLastCall();
        }
        if (responseMsgs != null) {
            PrintWriter writer = control.createMock(PrintWriter.class);
            expect(response.getWriter()).andReturn(writer).anyTimes();
            for (String msg : responseMsgs) { 
                writer.write(msg);
                expectLastCall();
            }
        }
    }

    private void setUpQuery() throws Exception {
        QueryHandlerRegistry qrh = 
            control.createMock(QueryHandlerRegistry.class);
        expect(bus.getExtension(QueryHandlerRegistry.class)).andReturn(qrh).anyTimes();
        QueryHandler qh = control.createMock(QueryHandler.class);
        List<QueryHandler> handlers = new ArrayList<QueryHandler>();
        handlers.add(qh);
        expect(qrh.getHandlers()).andReturn(handlers);
        String base = ROOT + URI + "?" + QUERY;
        expect(qh.isRecognizedQuery(eq(base),
                                    eq(PATH),
                                    same(endpoint))).andReturn(Boolean.TRUE);
        expect(qh.getResponseContentType(eq(base), eq(PATH))).andReturn(XML);
        ServletOutputStream sos = control.createMock(ServletOutputStream.class);
        expect(response.getOutputStream()).andReturn(sos);
        qh.writeResponse(eq(base), eq(PATH), same(endpoint), same(sos)); 
        expectLastCall();
        sos.flush();
        expectLastCall();
    }

    private void setUpRestful() {
        paths.add(ADDRESS);
        expect(registry.getDestinationForPath(ADDRESS)).andReturn(null);
        expect(registry.getDestinationForPath(ADDRESS)).andReturn(destination).times(2);
        expect(destination.getMessageObserver()).andReturn(observer);
        endpoint.addExtensor(extensor);
        extensor.setLocation(eq(ROOT + "/cxf/Soap" + ADDRESS));
        expectLastCall();
    }

    private OsgiServlet setUpServlet() { 
        return new OsgiServlet(registry) {
            public ServletContext getServletContext() {
                return context;
            }
            public ServletConfig getServletConfig() {
                return config;
            }
            protected MessageImpl createInMessage() {
                return message;
            }
            protected ExchangeImpl createExchange() {
                return exchange;
            }            
        };
    }
}
