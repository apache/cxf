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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.apache.cxf.microprofile.client.mock.AsyncClient;
import org.apache.cxf.microprofile.client.mock.NotFoundExceptionMapper;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsyncTest {
    @SuppressWarnings("unchecked")
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
        .extensions(SimpleTransformer.class).dynamicPort());
    
    public static class SimpleTransformer extends ResponseTransformer {
        private final Queue<String> queue = new ArrayBlockingQueue<>(2);
        
        public SimpleTransformer() {
            queue.add("Hello");
            queue.add("World");
        }
        
        @Override
        public Response transform(Request request, Response response, FileSource fileSource, Parameters parameters) {
            return Response.Builder
                .like(response)
                .but().body(queue.poll())
                .build();
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }

        @Override
        public String getName() {
            return "enqueue-transformer";
        }
    }
    
    @Test
    public void testAsyncClient() throws Exception {
        URI uri = URI.create(wireMockRule.baseUrl());
        AsyncClient client = RestClientBuilder.newBuilder()
                                              .baseUri(uri)
                                              .connectTimeout(5, TimeUnit.SECONDS)
                                              .readTimeout(5, TimeUnit.SECONDS)
                                              .build(AsyncClient.class);
        assertNotNull(client);

        wireMockRule.stubFor(get("/").willReturn(ok().withTransformers("enqueue-transformer")));

        String combined = client.get().thenCombine(client.get(), (a, b) -> {
            return a + " " + b;
        }).toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertTrue("Hello World".equals(combined) || "World Hello".equals(combined));
    }

    @Test
    public void testAsyncClientCanMapExceptionResponses() throws Exception {
        URI uri = URI.create(wireMockRule.baseUrl());
        AsyncClient client = RestClientBuilder.newBuilder()
                                              .baseUri(uri)
                                              .connectTimeout(5, TimeUnit.SECONDS)
                                              .readTimeout(5, TimeUnit.SECONDS)
                                              .register(NotFoundExceptionMapper.class)
                                              .build(AsyncClient.class);
        wireMockRule.stubFor(get("/").willReturn(notFound()));

        CompletionStage<?> cs = client.get().exceptionally(t -> {
            Throwable t2 = t.getCause();
            return t.getClass().getSimpleName() + ":" + (t2 == null ? "null" : t2.getClass().getSimpleName());
        });
        assertEquals("CompletionException:NoSuchEntityException", cs.toCompletableFuture().get(10, TimeUnit.SECONDS));
    }
}
