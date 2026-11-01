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

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class HttpClientHTTPConduitCloseTest {
    private static final int THREADS = 4;

    private Bus bus;
    private ExecutorService executor;

    @Before
    public void setUp() {
        bus = new ExtensionManagerBus();
        executor = Executors.newFixedThreadPool(THREADS);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        bus.shutdown(true);
    }

    /**
     * Closing a conduit from several threads at the same time, as two finalizers of clients sharing
     * the conduit may do, must release its shared HttpClient only once, so that the HttpClient
     * stays open for the other conduits that use it.
     */
    @Test
    public void testConcurrentCloseReleasesSharedClientOnce() throws Exception {
        for (int i = 0; i < 1000; i++) {
            HttpClientHTTPConduit other = newConduit();
            HttpClient client = prepare(other);

            HttpClientHTTPConduit closed = newConduit();
            assertSame(client, prepare(closed));

            CyclicBarrier barrier = new CyclicBarrier(THREADS);
            List<Future<?>> closes = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                closes.add(executor.submit(() -> {
                    barrier.await();
                    closed.close();
                    return null;
                }));
            }
            for (Future<?> close : closes) {
                close.get();
            }

            // the HttpClient is still shared, so it was not shut down
            HttpClientHTTPConduit next = newConduit();
            assertSame("Iteration " + i, client, prepare(next));

            next.close();
            other.close();
        }
    }

    private HttpClientHTTPConduit newConduit() throws Exception {
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://localhost/test");
        HttpClientHTTPConduit conduit = new HttpClientHTTPConduit(bus, ei, null);
        conduit.finalizeConfig();
        return conduit;
    }

    private static HttpClient prepare(HttpClientHTTPConduit conduit) throws Exception {
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        conduit.prepare(message);
        HttpClient client = message.get(HttpClient.class);
        assertNotNull(client);
        return client;
    }
}
