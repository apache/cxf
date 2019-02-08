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

package org.apache.cxf.transport.http_undertow;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.endpoint.EndpointResolverRegistry;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.policy.PolicyDataEngine;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.ContinuationProviderFactory;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.JAXWSAConstants;

import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class UndertowHTTPDestinationTest {
    protected static final String AUTH_HEADER = "Authorization";
    protected static final String USER = "copernicus";
    protected static final String PASSWD = "epicycles";
    protected static final String BASIC_AUTH =
        "Basic " + Base64Utility.encode((USER + ":" + PASSWD).getBytes());

    private static final String NOWHERE = "http://nada.nothing.nowhere.null/";
    private static final String PAYLOAD = "message payload";
    private static final String CHALLENGE_HEADER = "WWW-Authenticate";
    private static final String BASIC_CHALLENGE = "Basic realm=terra";
    private static final String DIGEST_CHALLENGE = "Digest realm=luna";
    private static final String CUSTOM_CHALLENGE = "Custom realm=sol";
    private Bus bus;
    private Bus threadDefaultBus;
    private Conduit decoupledBackChannel;
    private EndpointInfo endpointInfo;
    private EndpointReferenceType address;
    private UndertowHTTPServerEngine engine;
    private HTTPServerPolicy policy;
    private UndertowHTTPDestination destination;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Message inMessage;
    private Message outMessage;
    private MessageObserver observer;
    private ServletInputStream is;
    private ServletOutputStream os;
    private HTTPTransportFactory transportFactory;

    /**
     * This class replaces the engine in the Undertow Destination.
     */
    private class EasyMockUndertowHTTPDestination
        extends UndertowHTTPDestination {

        EasyMockUndertowHTTPDestination(Bus bus,
                                            DestinationRegistry registry,
                                            EndpointInfo endpointInfo,
                                            UndertowHTTPServerEngineFactory serverEngineFactory,
                                            UndertowHTTPServerEngine easyMockEngine) throws IOException {
            super(bus, registry, endpointInfo, serverEngineFactory);
            engine = easyMockEngine;
        }

        @Override
        public void retrieveEngine() {
            // Leave engine alone.
        }
    }
    @After
    public void tearDown() {
        if (bus != null) {
            bus.shutdown(true);
        }
        bus = null;
        transportFactory = null;
        decoupledBackChannel = null;
        address = null;
        engine = null;
        request = null;
        response = null;
        inMessage = null;
        outMessage = null;
        is = null;
        os = null;
        destination = null;
        BusFactory.setDefaultBus(null);
    }

    @Test
    public void testGetAddress() throws Exception {
        destination = setUpDestination();
        EndpointReferenceType ref = destination.getAddress();
        assertNotNull("unexpected null address", ref);
        assertEquals("unexpected address",
                     EndpointReferenceUtils.getAddress(ref),
                     StringUtils.addDefaultPortIfMissing(EndpointReferenceUtils.getAddress(address)));
        assertEquals("unexpected service name local part",
                     EndpointReferenceUtils.getServiceName(ref, bus).getLocalPart(),
                     "Service");
        assertEquals("unexpected portName",
                     EndpointReferenceUtils.getPortName(ref),
                     "Port");
    }

    @Test
    public void testRandomPortAllocation() throws Exception {
        bus = BusFactory.getDefaultBus(true);
        transportFactory = new HTTPTransportFactory();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(new QName("bla", "Service"));
        EndpointInfo ei = new EndpointInfo(serviceInfo, "");
        ei.setName(new QName("bla", "Port"));

        Destination d1 = transportFactory.getDestination(ei, bus);
        URL url = new URL(d1.getAddress().getAddress().getValue());
        assertTrue("No random port has been allocated",
                   url.getPort() > 0);

    }

    @Test
    public void testSuspendedException() throws Exception {
        destination = setUpDestination(false, false);
        setUpDoService(false);
        final RuntimeException ex = new RuntimeException();
        observer = new MessageObserver() {
            public void onMessage(Message m) {
                throw new SuspendedInvocationException(ex);
            }
        };
        destination.setMessageObserver(observer);
        try {
            destination.doService(request, response);
            fail("Suspended invocation swallowed");
        } catch (RuntimeException runtimeEx) {
            assertSame("Original exception is not preserved", ex, runtimeEx);
        }
    }


    @Test
    public void testContinuationsIgnored() throws Exception {

        HttpServletRequest httpRequest = EasyMock.createMock(HttpServletRequest.class);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(new QName("bla", "Service"));
        EndpointInfo ei = new EndpointInfo(serviceInfo, "");
        ei.setName(new QName("bla", "Port"));

        final UndertowHTTPServerEngine httpEngine = new UndertowHTTPServerEngine();
        httpEngine.setContinuationsEnabled(false);
        UndertowHTTPServerEngineFactory factory = new UndertowHTTPServerEngineFactory() {
            @Override
            public UndertowHTTPServerEngine retrieveUndertowHTTPServerEngine(int port) {
                return httpEngine;
            }
        };
        Bus b2 = new ExtensionManagerBus();
        transportFactory = new HTTPTransportFactory();
        b2.setExtension(factory, UndertowHTTPServerEngineFactory.class);

        TestUndertowDestination testDestination =
            new TestUndertowDestination(b2,
                                     transportFactory.getRegistry(),
                                     ei,
                                     factory);
        testDestination.finalizeConfig();
        Message mi = testDestination.retrieveFromContinuation(httpRequest);
        assertNull("Continuations must be ignored", mi);
    }

    @Test
    public void testGetMultiple() throws Exception {
        bus = BusFactory.getDefaultBus(true);
        transportFactory = new HTTPTransportFactory();

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(new QName("bla", "Service"));
        EndpointInfo ei = new EndpointInfo(serviceInfo, "");
        ei.setName(new QName("bla", "Port"));
        ei.setAddress("http://foo");
        Destination d1 = transportFactory.getDestination(ei, bus);

        Destination d2 = transportFactory.getDestination(ei, bus);

        // Second get should not generate a new destination. It should just retrieve the existing one
        assertEquals(d1, d2);

        d2.shutdown();

        Destination d3 = transportFactory.getDestination(ei, bus);
        // Now a new destination should have been created
        assertNotSame(d1, d3);
    }


    @Test
    public void testRemoveServant() throws Exception {
        destination = setUpDestination();
        setUpRemoveServant();
        destination.setMessageObserver(null);
    }

    @Test
    public void testDoServiceRedirectURL() throws Exception {
        destination = setUpDestination(false, false);
        setUpDoService(true);
        destination.doService(request, response);

    }

    @Test
    public void testDoService() throws Exception {
        Bus defaultBus = new ExtensionManagerBus();
        assertSame("Default thread bus has not been set",
                   defaultBus, BusFactory.getThreadDefaultBus());
        destination = setUpDestination(false, false);
        setUpDoService(false);
        assertSame("Default thread bus has been unexpectedly reset",
                   defaultBus, BusFactory.getThreadDefaultBus());
        destination.doService(request, response);
        verifyDoService();
        assertSame("Default thread bus has not been reset",
                    defaultBus, BusFactory.getThreadDefaultBus());
    }

    @Test
    public void testDoServiceWithHttpGET() throws Exception {
        destination = setUpDestination(false, false);
        setUpDoService(false,
                       false,
                       false,
                       "GET",
                       "?customerId=abc&cutomerAdd=def",
                       200);
        destination.doService(request, response);

        assertNotNull("unexpected null message", inMessage);
        assertEquals("unexpected method",
                     inMessage.get(Message.HTTP_REQUEST_METHOD),
                     "GET");
        assertEquals("unexpected path",
                     inMessage.get(Message.PATH_INFO),
                     "/bar/foo");
        assertEquals("unexpected query",
                     inMessage.get(Message.QUERY_STRING),
                     "?customerId=abc&cutomerAdd=def");

    }

    @Test
    public void testGetAnonBackChannel() throws Exception {
        destination = setUpDestination(false, false);
        setUpDoService(false);
        destination.doService(request, response);
        setUpInMessage();
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNotNull("expected back channel", backChannel);
        assertEquals("unexpected target",
                     EndpointReferenceUtils.ANONYMOUS_ADDRESS,
                     backChannel.getTarget().getAddress().getValue());
    }

    @Test
    public void testGetBackChannelSend() throws Exception {
        destination = setUpDestination(false, false);
        setUpDoService(false, true);
        destination.doService(request, response);
        setUpInMessage();
        Conduit backChannel =
            destination.getBackChannel(inMessage);
        outMessage = setUpOutMessage();
        backChannel.prepare(outMessage);
        verifyBackChannelSend(backChannel, outMessage, 200);
    }

    @Test
    public void testGetBackChannelSendFault() throws Exception {
        destination = setUpDestination(false, false);
        setUpDoService(false, true, 500);
        destination.doService(request, response);
        setUpInMessage();
        Conduit backChannel =
            destination.getBackChannel(inMessage);
        outMessage = setUpOutMessage();
        backChannel.prepare(outMessage);
        verifyBackChannelSend(backChannel, outMessage, 500);
    }

    @Test
    public void testGetBackChannelSendOneway() throws Exception {
        destination = setUpDestination(false, false);
        setUpDoService(false, true, 500);
        destination.doService(request, response);
        setUpInMessage();
        Conduit backChannel =
            destination.getBackChannel(inMessage);
        outMessage = setUpOutMessage();
        backChannel.prepare(outMessage);
        verifyBackChannelSend(backChannel, outMessage, 500, true);
    }

    @Test
    public void testGetBackChannelSendDecoupled() throws Exception {
        destination = setUpDestination(false, false);
        setUpDoService(false, true, true, 202);
        destination.doService(request, response);
        setUpInMessage();

        Message partialResponse = setUpOutMessage();
        partialResponse.put(Message.PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
        Conduit partialBackChannel =
            destination.getBackChannel(inMessage);
        partialBackChannel.prepare(partialResponse);
        verifyBackChannelSend(partialBackChannel, partialResponse, 202);

        outMessage = setUpOutMessage();
        Conduit fullBackChannel =
            destination.getBackChannel(inMessage);

        fullBackChannel.prepare(outMessage);
    }

    @Test
    public void testServerPolicyInServiceModel()
        throws Exception {
        policy = new HTTPServerPolicy();
        address = getEPR("bar/foo");
        bus = BusFactory.getDefaultBus(true);

        transportFactory = new HTTPTransportFactory();

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(new QName("bla", "Service"));
        endpointInfo = new EndpointInfo(serviceInfo, "");
        endpointInfo.setName(new QName("bla", "Port"));
        endpointInfo.addExtensor(policy);

        engine = EasyMock.createMock(UndertowHTTPServerEngine.class);
        EasyMock.replay();
        endpointInfo.setAddress(NOWHERE + "bar/foo");

        UndertowHTTPDestination dest =
            new EasyMockUndertowHTTPDestination(
                    bus, transportFactory.getRegistry(), endpointInfo, null, engine);
        assertEquals(policy, dest.getServer());
    }

    @Test
    public void testMultiplexGetAddressWithId() throws Exception {
        destination = setUpDestination();
        final String id = "ID2";
        EndpointReferenceType refWithId = destination.getAddressWithId(id);
        assertNotNull(refWithId);
        assertNotNull(refWithId.getReferenceParameters());
        assertNotNull(refWithId.getReferenceParameters().getAny());
        assertTrue("it is an element",
                   refWithId.getReferenceParameters().getAny().get(0) instanceof JAXBElement);
        JAXBElement<?> el = (JAXBElement<?>) refWithId.getReferenceParameters().getAny().get(0);
        assertEquals("match our id", el.getValue(), id);
    }

    @Test
    public void testMultiplexGetAddressWithIdForAddress() throws Exception {
        destination = setUpDestination();
        destination.setMultiplexWithAddress(true);

        final String id = "ID3";
        EndpointReferenceType refWithId = destination.getAddressWithId(id);
        assertNotNull(refWithId);
        assertNull(refWithId.getReferenceParameters());
        assertTrue("match our id", EndpointReferenceUtils.getAddress(refWithId).indexOf(id) != -1);
    }

    @Test
    public void testMultiplexGetIdForAddress() throws Exception {
        destination = setUpDestination();
        destination.setMultiplexWithAddress(true);

        final String id = "ID3";
        EndpointReferenceType refWithId = destination.getAddressWithId(id);
        String pathInfo = EndpointReferenceUtils.getAddress(refWithId);

        Map<String, Object> context = new HashMap<>();
        assertNull("fails with no context", destination.getId(context));

        context.put(Message.PATH_INFO, pathInfo);
        String result = destination.getId(context);
        assertNotNull(result);
        assertEquals("match our id", result, id);
    }

    @Test
    public void testMultiplexGetId() throws Exception {
        destination = setUpDestination();

        final String id = "ID3";
        EndpointReferenceType refWithId = destination.getAddressWithId(id);

        Map<String, Object> context = new HashMap<>();
        assertNull("fails with no context", destination.getId(context));

        AddressingProperties maps = EasyMock.createMock(AddressingProperties.class);
        maps.getToEndpointReference();
        EasyMock.expectLastCall().andReturn(refWithId);
        EasyMock.replay(maps);
        context.put(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND, maps);
        String result = destination.getId(context);
        assertNotNull(result);
        assertEquals("match our id", result, id);
    }

    private UndertowHTTPDestination setUpDestination()
        throws Exception {
        return setUpDestination(false, false);
    };

    private UndertowHTTPDestination setUpDestination(
            boolean contextMatchOnStem, boolean mockedBus)
        throws Exception {
        policy = new HTTPServerPolicy();
        address = getEPR("bar/foo");


        transportFactory = new HTTPTransportFactory();

        final ConduitInitiator ci = new ConduitInitiator() {
            public Conduit getConduit(EndpointInfo targetInfo, Bus b) throws IOException {
                return decoupledBackChannel;
            }

            public Conduit getConduit(EndpointInfo localInfo, EndpointReferenceType target, Bus b)
                throws IOException {
                return decoupledBackChannel;
            }

            public List<String> getTransportIds() {
                return null;
            }

            public Set<String> getUriPrefixes() {
                return new HashSet<>(Collections.singletonList("http"));
            }

        };
        ConduitInitiatorManager mgr = new ConduitInitiatorManager() {
            public void deregisterConduitInitiator(String name) {
            }

            public ConduitInitiator getConduitInitiator(String name) throws BusException {
                return null;
            }

            public ConduitInitiator getConduitInitiatorForUri(String uri) {
                return ci;
            }

            public void registerConduitInitiator(String name, ConduitInitiator factory) {
            }
        };

        if (!mockedBus) {
            bus = new ExtensionManagerBus();
            bus.setExtension(mgr, ConduitInitiatorManager.class);
        } else {
            bus = EasyMock.createMock(Bus.class);
            bus.getExtension(EndpointResolverRegistry.class);
            EasyMock.expectLastCall().andReturn(null);
            bus.getExtension(ContinuationProviderFactory.class);
            EasyMock.expectLastCall().andReturn(null).anyTimes();
            bus.getExtension(PolicyDataEngine.class);
            EasyMock.expectLastCall().andReturn(null).anyTimes();
            bus.hasExtensionByName("org.apache.cxf.ws.policy.PolicyEngine");
            EasyMock.expectLastCall().andReturn(false);
            bus.getExtension(ClassLoader.class);
            EasyMock.expectLastCall().andReturn(this.getClass().getClassLoader());
            EasyMock.replay(bus);
        }


        engine = EasyMock.createNiceMock(UndertowHTTPServerEngine.class);
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(new QName("bla", "Service"));
        endpointInfo = new EndpointInfo(serviceInfo, "");
        endpointInfo.setName(new QName("bla", "Port"));
        endpointInfo.setAddress(NOWHERE + "bar/foo");

        endpointInfo.addExtensor(policy);
        engine.addServant(EasyMock.eq(new URL(NOWHERE + "bar/foo")),
                          EasyMock.isA(UndertowHTTPHandler.class));
        EasyMock.expectLastCall();
        engine.getContinuationsEnabled();
        EasyMock.expectLastCall().andReturn(true);
        EasyMock.replay(engine);

        UndertowHTTPDestination dest = new EasyMockUndertowHTTPDestination(bus,
                                                             transportFactory.getRegistry(),
                                                             endpointInfo,
                                                             null,
                                                             engine);
        dest.retrieveEngine();
        policy = dest.getServer();
        observer = new MessageObserver() {
            public void onMessage(Message m) {
                inMessage = m;
                threadDefaultBus = BusFactory.getThreadDefaultBus();
            }
        };
        dest.setMessageObserver(observer);
        return dest;
    }

    private void setUpRemoveServant() throws Exception {
        EasyMock.reset(engine);
        engine.removeServant(EasyMock.eq(new URL(NOWHERE + "bar/foo")));
        EasyMock.expectLastCall();
        EasyMock.replay(engine);
    }

    private void setUpDoService(boolean setRedirectURL) throws Exception {
        setUpDoService(setRedirectURL, false);
    }

    private void setUpDoService(boolean setRedirectURL,
                                boolean sendResponse) throws Exception {
        setUpDoService(setRedirectURL,
                       sendResponse,
                       false);
    }

    private void setUpDoService(boolean setRedirectURL,
                                boolean sendResponse, int status) throws Exception {
        String method = "POST";
        String query = "?name";
        setUpDoService(setRedirectURL, sendResponse, false, method, query, status);
    }

    private void setUpDoService(boolean setRedirectURL,
                                boolean sendResponse, boolean decoupled, int status) throws Exception {
        String method = "POST";
        String query = "?name";
        setUpDoService(setRedirectURL, sendResponse, decoupled, method, query, status);
    }

    private void setUpDoService(boolean setRedirectURL,
            boolean sendResponse,
            boolean decoupled) throws Exception {
        String method = "POST";
        String query = "?name";
        setUpDoService(setRedirectURL, sendResponse, decoupled, method, query, 200);
    }

    private void setUpDoService(boolean setRedirectURL,
                                boolean sendResponse,
                                boolean decoupled,
                                String method,
                                String query,
                                int status
                                ) throws Exception {

        is = EasyMock.createMock(ServletInputStream.class);
        os = EasyMock.createMock(ServletOutputStream.class);
        request = EasyMock.createMock(HttpServletRequest.class);
        response = EasyMock.createMock(HttpServletResponse.class);
        request.getMethod();
        EasyMock.expectLastCall().andReturn(method).atLeastOnce();
        request.getUserPrincipal();
        EasyMock.expectLastCall().andReturn(null).anyTimes();

        if (setRedirectURL) {
            policy.setRedirectURL(NOWHERE + "foo/bar");
            response.sendRedirect(EasyMock.eq(NOWHERE + "foo/bar"));
            EasyMock.expectLastCall();
            response.flushBuffer();
            EasyMock.expectLastCall();
            EasyMock.expectLastCall();
        } else {
            //getQueryString for if statement
            request.getQueryString();
            EasyMock.expectLastCall().andReturn(query);

            if ("GET".equals(method) && "?wsdl".equals(query)) {
                verifyGetWSDLQuery();
            } else { // test for the post
                EasyMock.expect(request.getAttribute(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE))
                    .andReturn(null);


                EasyMock.expect(request.getInputStream()).andReturn(is);
                EasyMock.expect(request.getContextPath()).andReturn("/bar");
                EasyMock.expect(request.getServletPath()).andReturn("");
                EasyMock.expect(request.getPathInfo()).andReturn("/foo");
                EasyMock.expect(request.getRequestURI()).andReturn("/foo");
                EasyMock.expect(request.getRequestURL())
                    .andReturn(new StringBuffer("http://localhost/foo")).anyTimes();
                EasyMock.expect(request.getCharacterEncoding()).andReturn(StandardCharsets.UTF_8.name());
                EasyMock.expect(request.getQueryString()).andReturn(query);
                EasyMock.expect(request.getHeader("Accept")).andReturn("*/*");
                EasyMock.expect(request.getContentType()).andReturn("text/xml charset=utf8").times(2);
                EasyMock.expect(request.getAttribute("http.service.redirection")).andReturn(null).anyTimes();

                HeaderMap httpFields = new HeaderMap();
                httpFields.add(new HttpString("content-type"), "text/xml");
                httpFields.add(new HttpString("content-type"), "charset=utf8");
                httpFields.put(new HttpString(UndertowHTTPDestinationTest.AUTH_HEADER),
                               UndertowHTTPDestinationTest.BASIC_AUTH);
                List<String> headers = new ArrayList<>();
                for (HttpString header : httpFields.getHeaderNames()) {
                    headers.add(header.toString());
                }
                EasyMock.expect(request.getHeaderNames()).andReturn(Collections.enumeration(headers));
                request.getHeaders("content-type");
                EasyMock.expectLastCall().andReturn(Collections.enumeration(httpFields.get("content-type")));
                request.getHeaders(UndertowHTTPDestinationTest.AUTH_HEADER);
                EasyMock.expectLastCall().andReturn(Collections.enumeration(
                                                    httpFields.get(UndertowHTTPDestinationTest.AUTH_HEADER)));

                EasyMock.expect(request.getInputStream()).andReturn(is);
                EasyMock.expectLastCall();
                response.flushBuffer();
                EasyMock.expectLastCall();
                if (sendResponse) {
                    response.setStatus(status);
                    EasyMock.expectLastCall();
                    response.setContentType("text/xml charset=utf8");
                    EasyMock.expectLastCall();
                    response.addHeader(EasyMock.isA(String.class), EasyMock.isA(String.class));
                    EasyMock.expectLastCall().anyTimes();
                    response.setContentLength(0);
                    EasyMock.expectLastCall().anyTimes();
                    response.getOutputStream();
                    EasyMock.expectLastCall().andReturn(os);
                    response.getStatus();
                    EasyMock.expectLastCall().andReturn(status).anyTimes();
                    response.flushBuffer();
                    EasyMock.expectLastCall();
                }
                request.getAttribute("javax.servlet.request.cipher_suite");
                EasyMock.expectLastCall().andReturn("anythingwilldoreally");
                request.getAttribute("javax.net.ssl.session");
                EasyMock.expectLastCall().andReturn(null);
                request.getAttribute("javax.servlet.request.X509Certificate");
                EasyMock.expectLastCall().andReturn(null);
            }
        }

        if (decoupled) {
            setupDecoupledBackChannel();
        }
        EasyMock.replay(response);
        EasyMock.replay(request);
    }

    private void setupDecoupledBackChannel() throws IOException {
        decoupledBackChannel = EasyMock.createMock(Conduit.class);
        decoupledBackChannel.setMessageObserver(EasyMock.isA(MessageObserver.class));
        decoupledBackChannel.prepare(EasyMock.isA(Message.class));
        EasyMock.expectLastCall();
        EasyMock.replay(decoupledBackChannel);
    }

    private void setUpInMessage() {
        inMessage.setExchange(new ExchangeImpl());
    }

    private Message setUpOutMessage() {
        Message outMsg = new MessageImpl();
        outMsg.putAll(inMessage);
        outMsg.setExchange(new ExchangeImpl());
        outMsg.put(Message.PROTOCOL_HEADERS,
                   new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER));
        return outMsg;
    }

    private void setUpResponseHeaders(Message outMsg) {
        Map<String, List<String>> responseHeaders =
            CastUtils.cast((Map<?, ?>)outMsg.get(Message.PROTOCOL_HEADERS));
        assertNotNull("expected response headers", responseHeaders);
        List<String> challenges = new ArrayList<>();
        challenges.add(BASIC_CHALLENGE);
        challenges.add(DIGEST_CHALLENGE);
        challenges.add(CUSTOM_CHALLENGE);
        responseHeaders.put(CHALLENGE_HEADER, challenges);
    }

    private void verifyGetWSDLQuery() throws Exception {
        EasyMock.reset(bus);
        request.getRequestURL();
        EasyMock.expectLastCall().andReturn(new StringBuffer("http://localhost/bar/foo")).times(2);
        request.getPathInfo();
        EasyMock.expectLastCall().andReturn("/bar/foo");
        request.getCharacterEncoding();
        EasyMock.expectLastCall().andReturn(StandardCharsets.UTF_8.name());
        request.getQueryString();
        EasyMock.expectLastCall().andReturn("wsdl");
        response.setContentType("text/xml");
        EasyMock.expectLastCall();
        response.getOutputStream();
        EasyMock.expectLastCall().andReturn(os).anyTimes();
        EasyMock.expectLastCall();
        EasyMock.replay(bus);
    }

    private void verifyDoService() throws Exception {
        assertSame("Default thread bus has not been set for request",
                    bus, threadDefaultBus);
        assertNotNull("unexpected null message", inMessage);
        assertSame("unexpected HTTP request",
                   inMessage.get(AbstractHTTPDestination.HTTP_REQUEST),
                   request);
        assertSame("unexpected HTTP response",
                   inMessage.get(AbstractHTTPDestination.HTTP_RESPONSE),
                   response);
        assertEquals("unexpected method",
                     inMessage.get(Message.HTTP_REQUEST_METHOD),
                     "POST");
        assertEquals("unexpected path",
                     inMessage.get(Message.PATH_INFO),
                     "/bar/foo");
        assertEquals("unexpected query",
                     inMessage.get(Message.QUERY_STRING),
                     "?name");
        assertNotNull("unexpected query",
                   inMessage.get(TLSSessionInfo.class));
        verifyRequestHeaders();

    }

    private void verifyRequestHeaders() throws Exception {
        Map<String, List<String>> requestHeaders =
            CastUtils.cast((Map<?, ?>)inMessage.get(Message.PROTOCOL_HEADERS));
        assertNotNull("expected request headers",
                      requestHeaders);
        List<String> values = requestHeaders.get("content-type");
        assertNotNull("expected field", values);
        assertEquals("unexpected values", 2, values.size());
        assertTrue("expected value", values.contains("text/xml"));
        assertTrue("expected value", values.contains("charset=utf8"));
        values = requestHeaders.get(AUTH_HEADER);
        assertNotNull("expected field", values);
        assertEquals("unexpected values", 1, values.size());
        assertTrue("expected value", values.contains(BASIC_AUTH));

        AuthorizationPolicy authpolicy =
            inMessage.get(AuthorizationPolicy.class);
        assertNotNull("Expected some auth tokens", policy);
        assertEquals("expected user",
                     USER,
                     authpolicy.getUserName());
        assertEquals("expected passwd",
                     PASSWD,
                     authpolicy.getPassword());
    }

    private void verifyResponseHeaders(Message outMsg) throws Exception {
        Map<String, List<String>> responseHeaders =
            CastUtils.cast((Map<?, ?>)outMsg.get(Message.PROTOCOL_HEADERS));
        assertNotNull("expected response headers",
                      responseHeaders);

    }

    private void verifyBackChannelSend(Conduit backChannel,
                                       Message outMsg,
                                       int status) throws Exception {
        verifyBackChannelSend(backChannel, outMsg, status, false);
    }

    private void verifyBackChannelSend(Conduit backChannel,
                                       Message outMsg,
                                       int status,
                                       boolean oneway) throws Exception {
        outMsg.getExchange().setOneWay(oneway);

        assertTrue("unexpected back channel type",
                   backChannel instanceof UndertowHTTPDestination.BackChannelConduit);
        assertTrue("unexpected content formats",
                   outMsg.getContentFormats().contains(OutputStream.class));
        OutputStream responseOS = outMsg.getContent(OutputStream.class);
        assertNotNull("expected output stream", responseOS);
        assertTrue("unexpected output stream type",
                   responseOS instanceof AbstractWrappedOutputStream);

        outMsg.put(Message.RESPONSE_CODE, status);
        responseOS.write(PAYLOAD.getBytes());

        setUpResponseHeaders(outMsg);

        responseOS.flush();
        assertEquals("unexpected status",
                     status,
                     response.getStatus());

        verifyResponseHeaders(outMsg);

        if (oneway) {
            assertNull("unexpected HTTP response",
                       outMsg.get(AbstractHTTPDestination.HTTP_RESPONSE));
        } else {
            assertNotNull("expected HTTP response",
                           outMsg.get(AbstractHTTPDestination.HTTP_RESPONSE));
            responseOS.close();
        }
    }

    static EndpointReferenceType getEPR(String s) {
        return EndpointReferenceUtils.getEndpointReference(NOWHERE + s);
    }

    private static class TestUndertowDestination extends UndertowHTTPDestination {
        TestUndertowDestination(Bus bus,
                                    DestinationRegistry registry,
                                    EndpointInfo endpointInfo,
                                    UndertowHTTPServerEngineFactory serverEngineFactory) throws IOException {
            super(bus, registry, endpointInfo, serverEngineFactory);
        }

        @Override
        public Message retrieveFromContinuation(HttpServletRequest request) {
            return super.retrieveFromContinuation(request);
        }


    }
}