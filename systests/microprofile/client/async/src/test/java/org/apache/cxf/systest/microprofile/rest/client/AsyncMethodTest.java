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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.apache.cxf.systest.microprofile.rest.client.mock.AsyncClientWithFuture;
import org.apache.cxf.systest.microprofile.rest.client.mock.AsyncClientWithInvocationCallback;
import org.apache.cxf.systest.microprofile.rest.client.mock.AsyncInvocationInterceptorFactoryTestImpl;
import org.apache.cxf.systest.microprofile.rest.client.mock.AsyncInvocationInterceptorFactoryTestImpl2;
import org.apache.cxf.systest.microprofile.rest.client.mock.ThreadLocalClientFilter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.tck.providers.TestClientRequestFilter;
import org.eclipse.microprofile.rest.client.tck.providers.TestClientResponseFilter;
import org.eclipse.microprofile.rest.client.tck.providers.TestMessageBodyReader;
import org.eclipse.microprofile.rest.client.tck.providers.TestMessageBodyWriter;
import org.eclipse.microprofile.rest.client.tck.providers.TestParamConverterProvider;
import org.eclipse.microprofile.rest.client.tck.providers.TestReaderInterceptor;
import org.eclipse.microprofile.rest.client.tck.providers.TestWriterInterceptor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

//CHECKSTYLE:OFF
import static com.github.tomakehurst.wiremock.client.WireMock.*;
//CHECKSTYLE:ON

public class AsyncMethodTest extends Assert {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private static class StringInvocationCallback implements InvocationCallback<String> {
        private final List<Object> results;
        private final CountDownLatch latch;

        StringInvocationCallback(List<Object> results, CountDownLatch latch) {
            this.results = results;
            this.latch = latch;
        }

        /** {@inheritDoc}*/
        @Override
        public void completed(String response) {
            results.add(response);
            results.add(Thread.currentThread().getId());
            latch.countDown();
        }

        /** {@inheritDoc}*/
        @Override
        public void failed(Throwable th) {
            results.add(th);
            results.add(Thread.currentThread().getId());
            latch.countDown();
        }
    }

    private static class ResponseInvocationCallback implements InvocationCallback<Response> {
        final List<String> inboundList = new ArrayList<>();
        Throwable throwable;
        private final CountDownLatch latch;


        ResponseInvocationCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        /** {@inheritDoc}*/
        @Override
        public void completed(Response response) {
            inboundList.addAll(response.getStringHeaders().get("CXFTestResponse"));
            latch.countDown();
        }

        /** {@inheritDoc}*/
        @Override
        public void failed(Throwable th) {
            throwable = th;
            latch.countDown();
        }
    }

    @Test
    public void testInvokesPostOperationWithRegisteredProvidersAsyncInvocationCallback() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/echo/test"))
                                .willReturn(aResponse()
                                .withBody("this is the replaced writer input body will be removed")));
        long mainThreadId = Thread.currentThread().getId();
        String inputBody = "input body will be removed";
        String expectedResponseBody = TestMessageBodyReader.REPLACED_BODY;

        AsyncClientWithInvocationCallback api = RestClientBuilder.newBuilder()
                .register(TestClientRequestFilter.class)
                .register(TestClientResponseFilter.class)
                .register(TestMessageBodyReader.class, 3)
                .register(TestMessageBodyWriter.class)
                .register(TestParamConverterProvider.class)
                .register(TestReaderInterceptor.class)
                .register(TestWriterInterceptor.class)
                .baseUri(getBaseUri())
                .build(AsyncClientWithInvocationCallback.class);

        final List<Object> responseBodyList = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        api.put(new StringInvocationCallback(responseBodyList, latch), inputBody);

        latch.await(20, TimeUnit.SECONDS); // should need <1 second, but 20s timeout in case something goes wrong
        assertEquals(2, responseBodyList.size());

        assertEquals(expectedResponseBody, responseBodyList.get(0));
        assertNotEquals(mainThreadId, responseBodyList.get(1));

        assertEquals(TestClientResponseFilter.getAndResetValue(), 1);
        assertEquals(TestClientRequestFilter.getAndResetValue(), 1);
        assertEquals(TestReaderInterceptor.getAndResetValue(), 1);
    }

    @Test
    public void testInvokesPostOperationWithRegisteredProvidersAsyncInvocationCallbackWithExecutor() throws Exception {
        final String inputBody = "input body will be ignored";
        wireMockRule.stubFor(put(urlEqualTo("/echo/test"))
                                .willReturn(aResponse()
                                .withBody(inputBody)));
        AsyncInvocationInterceptorFactoryTestImpl.INBOUND.remove();
        AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.remove();
        try {
            final String asyncThreadName = "CXF-MPRestClientThread-1";

            AsyncClientWithInvocationCallback api = RestClientBuilder.newBuilder()
                .register(AsyncInvocationInterceptorFactoryTestImpl.class)
                .register(AsyncInvocationInterceptorFactoryTestImpl2.class)
                .register(ThreadLocalClientFilter.class)
                .baseUri(getBaseUri())
                .executorService(Executors.newSingleThreadExecutor(new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, asyncThreadName);
                    }
                }))
                .build(AsyncClientWithInvocationCallback.class);

            final CountDownLatch latch = new CountDownLatch(1);

            ResponseInvocationCallback callback = new ResponseInvocationCallback(latch);
            api.put(callback, inputBody);
            List<String> outboundList = AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.get();
            assertEquals(4, outboundList.size());

            // ensure filters and asyncInvocationInterceptors are executed in the correct order and the correct thread
            // outbound:
            assertEquals(ThreadLocalClientFilter.class.getSimpleName(), outboundList.get(0));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl.class.getSimpleName(), outboundList.get(1));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl2.class.getSimpleName(), outboundList.get(2));
            assertEquals(Thread.currentThread().getName(), outboundList.get(3));

            // inbound:
            latch.await(20, TimeUnit.SECONDS); // should need <1 second, but 20s timeout in case something goes wrong
            assertNull(callback.throwable);
            List<String> responseList = callback.inboundList;
            assertEquals(4, responseList.size());

            assertEquals(asyncThreadName, responseList.get(0));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl2.class.getSimpleName(), responseList.get(1));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl.class.getSimpleName(), responseList.get(2));
            assertEquals(ThreadLocalClientFilter.class.getSimpleName(), responseList.get(3));
        } finally {
            AsyncInvocationInterceptorFactoryTestImpl.INBOUND.remove();
            AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.remove();
        }
    }

    @Test
    public void testInvokesPostOperationWithRegisteredProvidersAsyncInvocationCallbackWithException() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/Missing/test"))
                                .willReturn(status(404)));
        long mainThreadId = Thread.currentThread().getId();
        String inputBody = "input body will be removed";

        AsyncClientWithInvocationCallback api = RestClientBuilder.newBuilder()
                .baseUrl(new URL("http://localhost:" + wireMockRule.port() + "/Missing"))
                .build(AsyncClientWithInvocationCallback.class);

        final List<Object> responseBodyList = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        api.put(new StringInvocationCallback(responseBodyList, latch), inputBody);

        latch.await(20, TimeUnit.SECONDS); // should need <1 second, but 20s timeout in case something goes wrong
        assertEquals(2, responseBodyList.size());
        Object body = responseBodyList.get(0);

        assertTrue(body instanceof WebApplicationException);
        assertNotEquals(mainThreadId, responseBodyList.get(1));
    }

    @Test
    public void testInvokesPostOperationWithRegisteredProvidersAsyncFuture() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/echo/test"))
                                .willReturn(aResponse()
                                .withBody("this is the replaced writer input body will be removed")));
        String inputBody = "input body will be removed";
        String expectedResponseBody = TestMessageBodyReader.REPLACED_BODY;

        AsyncClientWithFuture api = RestClientBuilder.newBuilder()
                .register(TestClientRequestFilter.class)
                .register(TestClientResponseFilter.class)
                .register(TestMessageBodyReader.class, 3)
                .register(TestMessageBodyWriter.class)
                .register(TestParamConverterProvider.class)
                .register(TestReaderInterceptor.class)
                .register(TestWriterInterceptor.class)
                .baseUri(getBaseUri())
                .build(AsyncClientWithFuture.class);

        Future<Response> future = api.put(inputBody);

        // should need <1 second, but 20s timeout in case something goes wrong
        Response response = future.get(20, TimeUnit.SECONDS);
        String actualResponseBody = response.readEntity(String.class);

        assertEquals(expectedResponseBody, actualResponseBody);

        assertEquals(TestClientResponseFilter.getAndResetValue(), 1);
        assertEquals(TestClientRequestFilter.getAndResetValue(), 1);
        assertEquals(TestReaderInterceptor.getAndResetValue(), 1);
    }

    @Test
    public void testInvokesPostOperationWithRegisteredProvidersAsyncFutureWithExecutor() throws Exception {
        final String inputBody = "input body will be ignored";
        wireMockRule.stubFor(put(urlEqualTo("/echo/test"))
                                .willReturn(aResponse()
                                .withBody(inputBody)));
        AsyncInvocationInterceptorFactoryTestImpl.INBOUND.remove();
        AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.remove();
        try {
            final String asyncThreadName = "CXF-MPRestClientThread-2";

            AsyncClientWithFuture api = RestClientBuilder.newBuilder()
                .register(AsyncInvocationInterceptorFactoryTestImpl.class)
                .register(AsyncInvocationInterceptorFactoryTestImpl2.class)
                .register(ThreadLocalClientFilter.class)
                .baseUri(getBaseUri())
                .executorService(Executors.newSingleThreadExecutor(new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, asyncThreadName);
                    }
                }))
                .build(AsyncClientWithFuture.class);

            Future<Response> future = api.put(inputBody);
            List<String> outboundList = AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.get();
            assertEquals(4, outboundList.size());

            // ensure filters and asyncInvocationInterceptors are executed in the correct order and the correct thread
            // outbound:
            assertEquals(ThreadLocalClientFilter.class.getSimpleName(), outboundList.get(0));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl.class.getSimpleName(), outboundList.get(1));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl2.class.getSimpleName(), outboundList.get(2));
            assertEquals(Thread.currentThread().getName(), outboundList.get(3));

            // inbound:
            // should need <1 second, but 20s timeout in case something goes wrong
            Response response = future.get(20, TimeUnit.SECONDS);

            List<String> responseList = response.getStringHeaders().get("CXFTestResponse");
            assertEquals(4, responseList.size());

            assertEquals(asyncThreadName, responseList.get(0));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl2.class.getSimpleName(), responseList.get(1));
            assertEquals(AsyncInvocationInterceptorFactoryTestImpl.class.getSimpleName(), responseList.get(2));
            assertEquals(ThreadLocalClientFilter.class.getSimpleName(), responseList.get(3));
        } finally {
            AsyncInvocationInterceptorFactoryTestImpl.INBOUND.remove();
            AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.remove();
        }
    }

    private URI getBaseUri() {
        return URI.create("http://localhost:" + wireMockRule.port() + "/echo");
    }

    private void fail(Response r, String failureMessage) {
        System.out.println(r.getStatus());
        fail(failureMessage);
    }
}
