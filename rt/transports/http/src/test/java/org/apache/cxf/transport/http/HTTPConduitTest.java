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

package org.apache.cxf.transport.http;


import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class HTTPConduitTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() {
    }

    /**
     * Generates a new message.
     */
    private Message getNewMessage() throws Exception {
        Message message = new MessageImpl();
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<String> contentTypes = new ArrayList<>();
        contentTypes.add("text/xml");
        contentTypes.add("charset=utf8");
        headers.put("content-type", contentTypes);
        message.put(Message.PROTOCOL_HEADERS, headers);
        return message;
    }

    private final class TestAuthSupplier implements HttpAuthSupplier {

        public String getAuthorization(AuthorizationPolicy authPolicy, URI currentURI, Message message,
                                       String fullHeader) {
            return "myauth";
        }

        public boolean requiresRequestCaching() {
            return false;
        }
    }

    /**
     * This test verfies that the "getTarget() call returns the correct
     * EndpointReferenceType for the given endpoint address.
     */
    @Test
    public void testGetTarget() throws Exception {
        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new URLConnectionHTTPConduit(bus, ei, null);
        conduit.finalizeConfig();

        EndpointReferenceType target =
            EndpointReferenceUtils.getEndpointReference(
                    "http://nowhere.com/bar/foo");

        // Test call
        EndpointReferenceType ref = conduit.getTarget();

        assertNotNull("unexpected null target", ref);
        assertEquals("unexpected target",
                     EndpointReferenceUtils.getAddress(ref),
                     EndpointReferenceUtils.getAddress(target));

        assertEquals("unexpected address",
                     conduit.getAddress(),
                     "http://nowhere.com/bar/foo");
        assertEquals("unexpected on-demand URL",
                     conduit.getURI().getPath(),
                     "/bar/foo");
    }



    /**
     * Verfies one of the tenents of our interface -- the Conduit sets up
     * an OutputStream on the message after a "prepare".
     */
    @Test
    public void testConduitOutputStream() throws Exception {
        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new URLConnectionHTTPConduit(bus, ei, null);
        conduit.finalizeConfig();

        Message message = getNewMessage();

        // Test call
        conduit.prepare(message);

        assertNotNull("Conduit should always set output stream.",
                        message.getContent(OutputStream.class));
    }

    @Test
    public void testAuthPolicyFromEndpointInfo() throws Exception {
        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        AuthorizationPolicy ap = new AuthorizationPolicy();
        ap.setPassword("password");
        ap.setUserName("testUser");
        ei.addExtensor(ap);
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new URLConnectionHTTPConduit(bus, ei, null);
        conduit.finalizeConfig();
        Message message = getNewMessage();

        // Test call
        conduit.prepare(message);

        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        assertNotNull("Authorization Header should exist",
                headers.get("Authorization"));

        assertEquals("Unexpected Authorization Token",
            DefaultBasicAuthSupplier.getBasicAuthHeader("testUser", "password"),
                headers.get("Authorization").get(0));
    }

    /**
     * This test verifies the precedence of Authorization Information.
     * Setting authorization information on the Message takes precedence
     * over a Basic Auth Supplier with preemptive UserPass, and that
     * followed by setting it directly on the Conduit.
     */
    @Test
    public void testAuthPolicyPrecedence() throws Exception {
        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new URLConnectionHTTPConduit(bus, ei, null);
        conduit.finalizeConfig();

        conduit.getAuthorization().setUserName("Satan");
        conduit.getAuthorization().setPassword("hell");
        Message message = getNewMessage();

        // Test call
        conduit.prepare(message);

        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        assertNotNull("Authorization Header should exist",
                headers.get("Authorization"));

        assertEquals("Unexpected Authorization Token",
            DefaultBasicAuthSupplier.getBasicAuthHeader("Satan", "hell"),
                headers.get("Authorization").get(0));

        // Setting a Basic Auth User Pass should override
        conduit.setAuthSupplier(new TestAuthSupplier());
        message = getNewMessage();

        // Test Call
        conduit.prepare(message);

        headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        List<String> authorization = headers.get("Authorization");
        assertNotNull("Authorization Token must be set", authorization);
        assertEquals("Wrong Authorization Token", "myauth", authorization.get(0));

        conduit.setAuthSupplier(null);
        // Setting authorization policy on the message should override
        // conduit setting
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("Hello");
        authPolicy.setPassword("world");
        authPolicy.setAuthorizationType("Basic");
        message = getNewMessage();
        message.put(AuthorizationPolicy.class, authPolicy);

        conduit.prepare(message);

        headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        assertEquals("Unexpected Authorization Token",
            DefaultBasicAuthSupplier.getBasicAuthHeader("Hello", "world"),
                headers.get("Authorization").get(0));
    }


    @Test
    public void testHandleResponseOnWorkqueueAllowCurrentThread() throws Exception {
        Message m = getNewMessage();
        Exchange exchange = new ExchangeImpl();
        Bus bus = new ExtensionManagerBus();
        exchange.put(Bus.class, bus);

        EndpointInfo endpointInfo = new EndpointInfo();
        Endpoint endpoint = new EndpointImpl(null, null, endpointInfo);
        exchange.put(Endpoint.class, endpoint);

        m.setExchange(exchange);

        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setAsyncExecuteTimeoutRejection(true);
        m.put(HTTPClientPolicy.class, policy);
        exchange.put(Executor.class, new Executor() {

            @Override
            public void execute(Runnable command) {
                // simulates a maxxed-out executor
                // forces us to use current thread
                throw new RejectedExecutionException("expected");
            } });

        HTTPConduit conduit = new MockHTTPConduit(bus, endpointInfo, policy);
        OutputStream os = conduit.createOutputStream(m, false, false, 0);
        assertTrue(os instanceof WrappedOutputStream);
        WrappedOutputStream wos = (WrappedOutputStream) os;

        try {
            wos.handleResponseOnWorkqueue(true, false);
            assertEquals(Thread.currentThread(), m.get(Thread.class));

            try {
                wos.handleResponseOnWorkqueue(false, false);
                fail("Expected RejectedExecutionException not thrown");
            } catch (RejectedExecutionException ex) {
                assertEquals("expected", ex.getMessage());
            }
        } catch (Exception ex) {
            throw ex;
        }
    }
}