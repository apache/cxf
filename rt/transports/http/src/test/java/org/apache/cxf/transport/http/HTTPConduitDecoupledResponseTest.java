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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class HTTPConduitDecoupledResponseTest {
    private static final String DECOUPLED_DESTINATION =
        "org.apache.cxf.ws.addressing.MAPAggregator.decoupledDestination";
    private static final String ADDRESS = "http://localhost/services/hello";

    private final boolean propagate;
    private Exchange exchange;
    private Message out;
    private Message reply;
    private ClientCallback callback;
    private final List<Message> observed = new ArrayList<>();
    private Endpoint endpoint;
    private Bus bus;
    private EndpointInfo endpointInfo;
    private URLConnectionHTTPConduit conduit;

    public HTTPConduitDecoupledResponseTest(boolean propagate) {
        this.propagate = propagate;
    }

    @Parameters(name = "propagate202={0}")
    public static Object[][] parameters() {
        return new Object[][] {{false}, {true}};
    }

    @Before
    public void setUp() throws Exception {
        bus = new ExtensionManagerBus();
        endpointInfo = new EndpointInfo();
        endpointInfo.setAddress(ADDRESS);
        endpointInfo.setBinding(new BindingInfo(null, "http://schemas.xmlsoap.org/wsdl/soap/"));
        endpointInfo.setProperty(DECOUPLED_DESTINATION, new Object());
        endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(endpointInfo);
        reset();

        conduit = new URLConnectionHTTPConduit(bus, endpointInfo, null);
        conduit.setMessageObserver(message -> {
            observed.add(message);
            // ClientImpl.onMessage() also replaces the response when invoked by the transport.
            message.getExchange().setInMessage(message);
        });
    }

    /** a new call: new exchange, request and reply */
    private void reset() {
        exchange = new ExchangeImpl();
        out = new MessageImpl();
        reply = new MessageImpl();
        callback = new ClientCallback();
        observed.clear();
        exchange.put(Endpoint.class, endpoint);
        exchange.put(ClientCallback.class, callback);
        exchange.setOutMessage(out);
        out.put(Message.PROPAGATE_202_RESPONSE_ONEWAY_OR_PARTIAL, propagate);
        reply.setContent(List.class, List.of("Hello Paul!"));
    }

    @After
    public void tearDown() {
        conduit.close();
        bus.shutdown(true);
    }

    @Test
    public void testReplyBeforeAcknowledgement() throws Exception {
        completeReply();
        respond(202, "");
        assertReplyPreserved();
    }

    @Test
    public void testAcknowledgementBeforeReply() throws Exception {
        respond(202, "");
        // the exchange is not finished, and the callback is left to the reply
        assertNull(exchange.get(ClientImpl.FINISHED));
        assertSame(callback, exchange.get(ClientCallback.class));
        assertFalse(callback.isDone());
        completeReply();
        assertSame(reply, exchange.getInMessage());
        assertEquals(List.of("Hello Paul!"), exchange.getInMessage().getContent(List.class));
    }

    @Test
    public void testAcknowledgementWithUnprocessedBodyAfterReply() throws Exception {
        // a 202 with a body the client does not process (no processOneWayResponse) is an acknowledgement too
        completeReply();
        respond(202, "<soap:Envelope/>");
        assertReplyPreserved();
    }

    @Test
    public void testConcurrentReplyAndAcknowledgementRepeated() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 500; i++) {
                reset();
                CountDownLatch start = new CountDownLatch(1);
                Future<?> acknowledgement = executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    respond(202, "");
                    return null;
                });
                Future<?> delivery = executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    completeReply();
                    return null;
                });
                start.countDown();
                acknowledgement.get(5, TimeUnit.SECONDS);
                delivery.get(5, TimeUnit.SECONDS);
                assertSame("iteration " + i, reply, exchange.getInMessage());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testConcurrentReplyAndAcknowledgement() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<?> acknowledgement = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                respond(202, "");
                return null;
            });
            Future<?> delivery = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                completeReply();
                return null;
            });
            start.countDown();
            acknowledgement.get(5, TimeUnit.SECONDS);
            delivery.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertReplyPreserved();
    }

    @Test
    public void testOneWay200CompletesCallback() throws Exception {
        assertOneWayAcknowledgement(200);
    }

    @Test
    public void testOneWay202CompletesCallback() throws Exception {
        assertOneWayAcknowledgement(202);
    }

    @Test
    public void testNonDecoupled202CompletesCallback() throws Exception {
        endpointInfo.setProperty(DECOUPLED_DESTINATION, null);
        respond(202, "");
        assertAcknowledgementProcessed(202);
    }

    @Test
    public void testNonEmpty200ReachesObserver() throws Exception {
        assertNonEmptyResponse(200);
    }

    @Test
    public void testNonEmpty202ReachesObserver() throws Exception {
        assertNonEmptyResponse(202);
    }

    @Test
    public void testFaultReachesObserver() throws Exception {
        assertNonEmptyResponse(500);
    }

    private void assertOneWayAcknowledgement(int status) throws Exception {
        endpointInfo.setProperty(DECOUPLED_DESTINATION, null);
        exchange.setOneWay(true);
        respond(status, "");
        assertAcknowledgementProcessed(status);
    }

    private void assertAcknowledgementProcessed(int status) {
        assertEquals(status, exchange.getInMessage().get(Message.RESPONSE_CODE));
        assertEquals(Boolean.TRUE, exchange.get("IN_CHAIN_COMPLETE"));
        assertNull(exchange.get(ClientCallback.class));
        assertTrue(callback.isDone());
        assertEquals(propagate ? 1 : 0, observed.size());
    }

    private void assertNonEmptyResponse(int status) throws Exception {
        out.put(Message.PROCESS_ONEWAY_RESPONSE, true);
        respond(status, "SOAP response or fault");
        assertEquals(1, observed.size());
        try (InputStream in = observed.get(0).getContent(InputStream.class)) {
            assertEquals("SOAP response or fault", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertFalse(callback.isDone());
    }

    private void completeReply() {
        // Publish the decoded SOAP reply as ClientImpl.onMessage() does on the decoupled response thread.
        synchronized (exchange) {
            exchange.setInMessage(reply);
            exchange.put(ClientImpl.FINISHED, Boolean.TRUE);
            exchange.notifyAll();
        }
    }

    private void assertReplyPreserved() {
        assertSame("Empty acknowledgement must not replace the SOAP reply", reply, exchange.getInMessage());
        assertEquals(List.of("Hello Paul!"), exchange.getInMessage().getContent(List.class));
        assertEquals(Boolean.TRUE, exchange.get(ClientImpl.FINISHED));
        assertAcknowledgementIgnored();
    }

    private void assertAcknowledgementIgnored() {
        assertNull(exchange.get("IN_CHAIN_COMPLETE"));
        assertSame(callback, exchange.get(ClientCallback.class));
        assertFalse(callback.isDone());
        assertTrue(observed.isEmpty());
    }

    private void respond(int status, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        InputStream in = spy(new ByteArrayInputStream(bytes));
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getURL()).thenReturn(URI.create(ADDRESS).toURL());
        when(connection.getResponseCode()).thenReturn(status);
        when(connection.getContentLength()).thenReturn(bytes.length);
        when(connection.getContentType()).thenReturn("text/xml; charset=UTF-8");
        when(connection.getHeaderFields())
            .thenReturn(Map.of("Content-Length", List.of(Integer.toString(bytes.length))));
        when(connection.getInputStream()).thenReturn(in);
        if (status >= 400) {
            when(connection.getErrorStream()).thenReturn(in);
        }
        conduit.new URLConnectionWrappedOutputStream(out, connection, false, false, 0, "test")
            .handleResponseInternal();
        if (bytes.length == 0) {
            verify(in).close();
        }
    }
}
