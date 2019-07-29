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

import java.net.URI;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.cxf.microprofile.client.mock.AsyncClient;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsyncTest {

    @Test
    public void testAsyncClient() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        URI uri = mockWebServer.url("/").uri();
        AsyncClient client = RestClientBuilder.newBuilder()
                                              .baseUri(uri)
                                              .connectTimeout(5, TimeUnit.SECONDS)
                                              .readTimeout(5, TimeUnit.SECONDS)
                                              .build(AsyncClient.class);
        assertNotNull(client);

        mockWebServer.enqueue(new MockResponse().setBody("Hello"));
        mockWebServer.enqueue(new MockResponse().setBody("World"));

        String combined = client.get().thenCombine(client.get(), (a, b) -> {
            return a + " " + b;
        }).toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertTrue("Hello World".equals(combined) || "World Hello".equals(combined));
    }
}
