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

package org.apache.cxf.systest.microprofile.rest.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

@RunWith(Parameterized.class)
public class AsyncThreadingTest {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());
    
    private final ExecutorService executorService;
    private final String prefix;
    private EchoResource echo;
    
    public AsyncThreadingTest(final ExecutorService executorService, final String prefix) {
        this.executorService = executorService;
        this.prefix = prefix;
    }

    @Parameters(name = "Using pool: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] 
        {
            {cachedExecutor(), "mp-async-"}, 
            {null, "ForkJoinPool.commonPool-worker-"}
        });
    }
        
    @Before
    public void setUp() {
        final RestClientBuilder builder = RestClientBuilder
            .newBuilder()
            .register(JsonbJaxrsProvider.class)
            .register(AsyncInvocationInterceptorFactoryImpl.class)
            .baseUri(getBaseUri());
        
        if (executorService == null /* use default one */) {
            echo = builder.build(EchoResource.class);
        } else {
            echo = builder.executorService(executorService).build(EchoResource.class);
        }
    }
    
    @After
    public void tearDown() {
        CONTEXT.remove();
    }

    @Test
    public void testAsynchronousNotFoundCall() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/echo"))
            .willReturn(aResponse()
            .withStatus(404)));

        final CompletableFuture<Echo> future = echo
            .getAsync()
            .toCompletableFuture()
            .handle((r, ex) -> {
                try {
                    Thread.sleep(500);
                    assertThat(Thread.currentThread().getName(), startsWith(prefix));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (ex instanceof CompletionException) {
                    throw (CompletionException)ex;
                } else {
                    return r;
                }
            });

        // Simulate some processing pause
        assertNull(future.getNow(null));
        
        final ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(5L, TimeUnit.SECONDS));
        assertEquals(WebApplicationException.class, ex.getCause().getClass());
    }

    @Test
    public void testAsynchronousCall() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/echo"))
            .willReturn(aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
            .withBody("{ \"message\": \"echo\" }")));

        final CompletableFuture<Echo> future = echo
            .getAsync()
            .toCompletableFuture()
            .thenApply(s -> {
                try {
                    Thread.sleep(500);
                    assertThat(Thread.currentThread().getName(), startsWith(prefix));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                return s;
            });
        
        assertNull(future.getNow(null));
        
        final Echo result = future.get(5L, TimeUnit.SECONDS);
        assertThat(result.getMessage(), equalTo("echo"));
    }

    @Test
    public void testAsynchronousCallAndContextPropagation() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/echo"))
            .willReturn(aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
            .withBody("{ \"message\": \"echo\" }")));

        CONTEXT.set("context-value");
        
        final CompletableFuture<Echo> future = echo
            .getAsync()
            .toCompletableFuture()
            .thenApply(s -> {
                try {
                    Thread.sleep(500);
                    assertThat(Thread.currentThread().getName(), startsWith(prefix));
                    assertThat(CONTEXT.get(), equalTo("context-value"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return s;
            });

        final Echo result = future.get(5L, TimeUnit.SECONDS);
        assertThat(result.getMessage(), equalTo("echo"));
    }

    @Test
    public void testAsynchronousCallMany() throws InterruptedException, ExecutionException, TimeoutException {
        wireMockRule.stubFor(get(urlEqualTo("/echo"))
            .willReturn(aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
            .withBody("{ \"message\": \"echo\" }")));

        final Collection<CompletableFuture<Echo>> futures = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            futures.add(
                echo
                    .getAsync()
                    .toCompletableFuture()
                    .thenApply(s -> {
                        try {
                            Thread.sleep(500);
                            assertThat(Thread.currentThread().getName(), startsWith(prefix));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
    
                        return s;
                    })
            );
        }

        CompletableFuture
            .allOf(futures.toArray(new CompletableFuture[0]))
            .join();
        
        for (final CompletableFuture<Echo> future: futures) {
            assertThat(future.get().getMessage(), equalTo("echo"));
        }
    }
    
    private URI getBaseUri() {
        return URI.create("http://localhost:" + wireMockRule.port() + "/echo");
    }
      
    public static class Echo {
        private String message;
          
        public String getMessage() {
            return message;
        }
          
        public void setMessage(String message) {
            this.message = message;
        }
    }
      
    @Path("/")
    public interface EchoResource {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        CompletionStage<Echo> getAsync();
    }
      
    public static class AsyncInvocationInterceptorFactoryImpl implements AsyncInvocationInterceptorFactory {
        @Override
        public AsyncInvocationInterceptor newInterceptor() {
            return new AsyncInvocationInterceptorImpl();
        }
    }

    public static class AsyncInvocationInterceptorImpl implements AsyncInvocationInterceptor {
        private String context;

        @Override
        public void prepareContext() {
            context = CONTEXT.get();
        }

        @Override
        public void applyContext() {
            CONTEXT.set(context);
        }

        @Override
        public void removeContext() {
            CONTEXT.remove();
        }
    }
    
    private static ExecutorService cachedExecutor() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            private AtomicInteger counter = new AtomicInteger();
             
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "mp-async-" + counter.incrementAndGet());
            }
        });
    }
}
