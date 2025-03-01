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
package org.apache.cxf.microprofile.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.cxf.microprofile.client.mock.AsyncClient;
import org.apache.cxf.microprofile.client.mock.NotFoundExceptionMapper;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsyncTest {
    private HttpServer server;
    private final Queue<String> queue = new ArrayBlockingQueue<>(2);

    public class SimpleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final String body = queue.poll();
            if (body == null) {
                exchange.sendResponseHeaders(404, 0);
            } else {
                exchange.sendResponseHeaders(200, body.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body.getBytes());
                }
            }
            // See please https://bugs.openjdk.org/browse/JDK-6968351
            exchange.getResponseBody().flush();
        }
    }

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 2);
        server.createContext("/", new SimpleHandler());
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.stop(0);
        queue.clear();
    }

    @Test
    public void testAsyncClient() throws Exception {
        queue.add("Hello");
        queue.add("World");

        URI uri = URI.create("http://localhost:" + server.getAddress().getPort());
        AsyncClient client = RestClientBuilder.newBuilder()
                                              .baseUri(uri)
                                              .connectTimeout(5, TimeUnit.SECONDS)
                                              .readTimeout(5, TimeUnit.SECONDS)
                                              .build(AsyncClient.class);
        assertNotNull(client);

        String combined = client.get().thenCombine(client.get(), (a, b) -> {
            return a + " " + b;
        }).toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertTrue("Hello World".equals(combined) || "World Hello".equals(combined));
    }

    @Test
    public void testAsyncClientCanMapExceptionResponses() throws Exception {
        URI uri = URI.create("http://localhost:" + server.getAddress().getPort());
        AsyncClient client = RestClientBuilder.newBuilder()
                                              .baseUri(uri)
                                              .connectTimeout(5, TimeUnit.SECONDS)
                                              .readTimeout(5, TimeUnit.SECONDS)
                                              .register(NotFoundExceptionMapper.class)
                                              .build(AsyncClient.class);

        CompletionStage<?> cs = client.get().exceptionally(t -> {
            Throwable t2 = t.getCause();
            return t.getClass().getSimpleName() + ":" + (t2 == null ? "null" : t2.getClass().getSimpleName());
        });
        assertEquals("CompletionException:NoSuchEntityException", cs.toCompletableFuture().get(10, TimeUnit.SECONDS));
    }
}
