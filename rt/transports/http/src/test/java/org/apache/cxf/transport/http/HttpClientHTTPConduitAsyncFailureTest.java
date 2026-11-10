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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * An asynchronous call to an address that refuses the connection must report the failure to the
 * fault observer, also when the connection fails before the caller closes the request stream.
 */
public class HttpClientHTTPConduitAsyncFailureTest {
    private static final String PORT = TestUtil.getPortNumber(HttpClientHTTPConduitAsyncFailureTest.class);

    private Bus bus;
    private final List<HttpClientHTTPConduit> conduits = new ArrayList<>();

    @Before
    public void setUp() {
        bus = new ExtensionManagerBus();
    }

    @After
    public void tearDown() {
        conduits.forEach(HttpClientHTTPConduit::close);
        bus.shutdown(true);
    }

    @Test
    public void testConnectionRefusedBeforeCloseWithChunking() throws Exception {
        assertRefusedBeforeCloseReportedOnce(true);
    }

    @Test
    public void testConnectionRefusedBeforeCloseWithoutChunking() throws Exception {
        assertRefusedBeforeCloseReportedOnce(false);
    }

    @Test
    public void testConnectionRefusedWhileSendingOnClose() throws Exception {
        HttpClientHTTPConduit conduit = newConduit(refusedAddress(), true);
        Message message = newAsyncMessage();
        // a body under the chunking threshold is sent when the stream is closed: the connection is
        // refused while the caller is still sending it, here before the trust decision is made
        message.put(MessageTrustDecider.class, new MessageTrustDecider() {
            @Override
            public void establishTrust(String conduitName, URLConnectionInfo connectionInfo, Message m) {
                awaitRefused(m);
            }
        });
        conduit.prepare(message);
        OutputStream os = message.getContent(OutputStream.class);

        assertReportedOnce(message, () -> {
            os.write(new byte[8]);
            os.close();
        });
    }

    private void assertRefusedBeforeCloseReportedOnce(boolean allowChunking) throws Exception {
        HttpClientHTTPConduit conduit = newConduit(refusedAddress(), allowChunking);
        Message message = newAsyncMessage();
        conduit.prepare(message);
        OutputStream os = message.getContent(OutputStream.class);

        assertReportedOnce(message, () -> {
            // more than the chunking threshold, so that the request is sent before the stream is closed
            os.write(new byte[1024]);
            awaitRefused(message);
            os.close();
        });
    }

    /**
     * The failure must be reported once: either to the caller, by an exception from the stream, as
     * the out chain then reports it, or else to the fault observer
     */
    private static void assertReportedOnce(Message message, IORunnable send) throws Exception {
        AtomicInteger faults = new AtomicInteger();
        CountDownLatch faultReported = new CountDownLatch(1);
        ((PhaseInterceptorChain)message.getInterceptorChain()).setFaultObserver(m -> {
            faults.incrementAndGet();
            faultReported.countDown();
        });
        int failures = 0;
        try {
            send.run();
        } catch (IOException | RuntimeException ex) {
            failures++;
        }
        if (failures == 0) {
            assertTrue("the failure was not reported", faultReported.await(10, TimeUnit.SECONDS));
        }
        Thread.sleep(500);
        assertEquals("the failure was reported more than once", 1, failures + faults.get());
    }

    /** Waits until the connection of the request has been refused, and its callbacks have run */
    private static void awaitRefused(Message message) {
        HttpClientHTTPConduit.HttpClientWrappedOutputStream os =
            (HttpClientHTTPConduit.HttpClientWrappedOutputStream)message.getContent(OutputStream.class);
        try {
            long deadline = System.currentTimeMillis() + 10_000;
            while (!os.future.isDone() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertTrue("the connection was not refused", os.future.isCompletedExceptionally());
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private interface IORunnable {
        void run() throws IOException;
    }

    private HttpClientHTTPConduit newConduit(String address, boolean allowChunking) throws Exception {
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(address);
        HttpClientHTTPConduit conduit = new HttpClientHTTPConduit(bus, ei, null);
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(3000);
        policy.setReceiveTimeout(3000);
        policy.setAllowChunking(allowChunking);
        policy.setChunkingThreshold(16);
        conduit.setClient(policy);
        conduit.finalizeConfig();
        conduits.add(conduit);
        return conduit;
    }

    private Message newAsyncMessage() {
        Message message = new MessageImpl();
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("content-type", new ArrayList<>(List.of("text/xml")));
        message.put(Message.PROTOCOL_HEADERS, headers);
        message.put(Message.HTTP_REQUEST_METHOD, "POST");

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.setSynchronous(false);
        exchange.setOutMessage(message);
        message.setExchange(exchange);

        PhaseInterceptorChain chain = new PhaseInterceptorChain(new TreeSet<>());
        message.setInterceptorChain(chain);
        // as the out chain does before it closes the stream, so that the failure can unwind it
        chain.doIntercept(message);
        return message;
    }

    /** An address nothing listens on, so that the connection is refused */
    private static String refusedAddress() {
        return "http://localhost:" + PORT + "/echo";
    }
}
