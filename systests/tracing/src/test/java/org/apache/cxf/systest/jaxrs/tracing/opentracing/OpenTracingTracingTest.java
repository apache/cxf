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
package org.apache.cxf.systest.jaxrs.tracing.opentracing;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.systest.jaxrs.tracing.NullPointerExceptionMapper;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.tracing.opentracing.OpenTracingClientFeature;
import org.apache.cxf.tracing.opentracing.jaxrs.OpenTracingClientProvider;
import org.apache.cxf.tracing.opentracing.jaxrs.OpenTracingFeature;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import io.jaegertracing.internal.JaegerSpanContext;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.cxf.systest.jaxrs.tracing.opentracing.HasSpan.hasSpan;
import static org.apache.cxf.systest.jaxrs.tracing.opentracing.IsLogContaining.hasItem;
import static org.apache.cxf.systest.jaxrs.tracing.opentracing.IsTagContaining.hasItem;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OpenTracingTracingTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(OpenTracingTracingTest.class);

    private static final AtomicLong RANDOM = new AtomicLong();

    private static final InMemoryReporter REPORTER = new InMemoryReporter();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final Tracer tracer = new JaegerTracer.Builder("tracer-jaxrs")
        .withSampler(new ConstSampler(true))
        .withReporter(REPORTER)
        .build();

    public static class OpenTracingServer extends AbstractTestServerBase {

        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final Tracer tracer = new JaegerTracer.Builder("tracer-jaxrs")
                .withSampler(new ConstSampler(true))
                .withReporter(REPORTER)
                .build();

            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore<Scope>()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setProvider(new OpenTracingFeature(tracer));
            sf.setProvider(new NullPointerExceptionMapper());
            server = sf.create();
        }

        @Override
        public void tearDown() throws Exception {
            server.destroy();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(OpenTracingServer.class, true));
    }

    @After
    public void tearDown() {
        REPORTER.clear();
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().toString(), REPORTER.getSpans().size(), equalTo(2));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
        assertThat(REPORTER.getSpans().get(1).getTags(), hasItem(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().size(), equalTo(2));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
    }

    @Test
    public void testThatCurrentSpanIsAnnotatedWithKeyValue() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/book/1"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().size(), equalTo(1));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("GET /bookstore/book/1"));
        assertThat(REPORTER.getSpans().get(0).getTags(), hasItem("book-id", "1"));
    }

    @Test
    public void testThatParallelSpanIsAnnotatedWithTimeline() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/process"), spanId).put("");
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().size(), equalTo(2));
        assertThat(REPORTER.getSpans(), hasSpan("Processing books", hasItem("Processing started")));
        assertThat(REPORTER.getSpans(), hasSpan("PUT /bookstore/process"));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() {
        final Response r = createWebClient("/bookstore/books", new OpenTracingClientProvider(tracer)).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().toString(), REPORTER.getSpans().size(), equalTo(3));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(REPORTER.getSpans().get(0).getReferences(), not(empty()));
    }

    @Test
    public void testThatNewInnerSpanIsCreatedUsingAsyncInvocation() throws InterruptedException {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/async"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        await().atMost(Duration.ofSeconds(1L)).until(()-> REPORTER.getSpans().size() == 2);

        assertThat(REPORTER.getSpans().size(), equalTo(2));
        assertEquals("Processing books", REPORTER.getSpans().get(0).getOperationName());
        assertEquals("GET /bookstore/books/async", REPORTER.getSpans().get(1).getOperationName());
        assertThat(REPORTER.getSpans().get(1).getReferences(), not(empty()));
        assertThat(REPORTER.getSpans().get(1).getReferences().get(0).getSpanContext().getSpanId(),
            equalTo(spanId.getSpanId()));
    }

    @Test
    public void testThatOuterSpanIsCreatedUsingAsyncInvocation() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/async/notrace"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().size(), equalTo(1));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("GET /bookstore/books/async/notrace"));
    }

    @Test
    public void testThatNewSpanIsCreatedUsingAsyncInvocation() throws InterruptedException {
        final Response r = createWebClient("/bookstore/books/async").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        await().atMost(Duration.ofSeconds(1L)).until(()-> REPORTER.getSpans().size() == 2);

        assertThat(REPORTER.getSpans().size(), equalTo(2));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Processing books"));
        assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("GET /bookstore/books/async"));
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvidedUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", new OpenTracingClientProvider(tracer));

        final Response r = client.async().get().get(1L, TimeUnit.SECONDS);
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        await().atMost(Duration.ofSeconds(1L)).until(()-> REPORTER.getSpans().size() == 3);

        assertThat(REPORTER.getSpans().size(), equalTo(3));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
        assertThat(REPORTER.getSpans().get(1).getTags(), hasItem(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER));
        assertThat(REPORTER.getSpans().get(2).getOperationName(), equalTo("GET " + client.getCurrentURI()));
        assertThat(REPORTER.getSpans().get(2).getTags(), hasItem(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT));
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleAsyncClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", new OpenTracingClientProvider(tracer));

        // The intention is to make a calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        IntStream
            .range(0, 4)
            .mapToObj(index -> client.async().get())
            .map(this::get)
            .forEach(r -> assertEquals(Status.OK.getStatusCode(), r.getStatus()));

        assertThat(REPORTER.getSpans().size(), equalTo(12));

        IntStream
            .range(0, 4)
            .map(index -> index * 3)
            .forEach(index -> {
                assertThat(REPORTER.getSpans().get(index).getOperationName(),
                    equalTo("Get Books"));
                assertThat(REPORTER.getSpans().get(index + 1).getOperationName(),
                    equalTo("GET /bookstore/books"));
                assertThat(REPORTER.getSpans().get(index + 2).getOperationName(),
                    equalTo("GET " + client.getCurrentURI()));
            });
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", new OpenTracingClientProvider(tracer));

        // The intention is to make a calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        IntStream
            .range(0, 4)
            .mapToObj(index -> client.get())
            .forEach(r -> assertEquals(Status.OK.getStatusCode(), r.getStatus()));

        assertEquals(REPORTER.getSpans().toString(), 12, REPORTER.getSpans().size());

        IntStream
            .range(0, 4)
            .map(index -> index * 3)
            .forEach(index -> {
                assertThat(REPORTER.getSpans().get(index).getOperationName(),
                    equalTo("Get Books"));
                assertThat(REPORTER.getSpans().get(index + 1).getOperationName(),
                    equalTo("GET /bookstore/books"));
                assertThat(REPORTER.getSpans().get(index + 2).getOperationName(),
                    equalTo("GET " + client.getCurrentURI()));
            });
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws MalformedURLException {
        final WebClient client = createWebClient("/bookstore/books", new OpenTracingClientProvider(tracer));

        final Span span = tracer.buildSpan("test span").start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertEquals(REPORTER.getSpans().toString(), 3, REPORTER.getSpans().size());
            assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
            assertThat(REPORTER.getSpans().get(0).getReferences(), not(empty()));
            assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
            assertThat(REPORTER.getSpans().get(1).getReferences(), not(empty()));
            assertThat(REPORTER.getSpans().get(2).getOperationName(), equalTo("GET " + client.getCurrentURI()));
            assertThat(REPORTER.getSpans().get(2).getReferences(), not(empty()));
        } finally {
            span.finish();
        }

        // Await till flush happens, usually every second
        await().atMost(Duration.ofSeconds(1L)).until(()-> REPORTER.getSpans().size() == 4);

        assertThat(REPORTER.getSpans().size(), equalTo(4));
        assertThat(REPORTER.getSpans().get(3).getOperationName(), equalTo("test span"));
        assertThat(REPORTER.getSpans().get(3).getReferences(), empty());
    }

    @Test
    public void testThatProvidedSpanIsNotDetachedWhenActiveUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", new OpenTracingClientProvider(tracer));

        final Span span = tracer.buildSpan("test span").start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            final Response r = client.async().get().get(1L, TimeUnit.MINUTES);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertThat(tracer.activeSpan().context(), equalTo(span.context()));

            await().atMost(Duration.ofSeconds(1L)).until(()-> REPORTER.getSpans().size() == 3);

            assertThat(REPORTER.getSpans().size(), equalTo(3));
            assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
            assertThat(REPORTER.getSpans().get(0).getReferences(), not(empty()));
            assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
            assertThat(REPORTER.getSpans().get(1).getReferences(), not(empty()));
            assertThat(REPORTER.getSpans().get(2).getOperationName(), equalTo("GET " + client.getCurrentURI()));
            assertThat(REPORTER.getSpans().get(2).getReferences(), not(empty()));
        } finally {
            span.finish();
        }

        // Await till flush happens, usually every second
        await().atMost(Duration.ofSeconds(1L)).until(()-> REPORTER.getSpans().size() == 4);

        assertThat(REPORTER.getSpans().size(), equalTo(4));
        assertThat(REPORTER.getSpans().get(3).getOperationName(), equalTo("test span"));
        assertThat(REPORTER.getSpans().get(3).getReferences(), empty());
    }

    @Test
    public void testThatInnerSpanIsCreatedUsingPseudoAsyncInvocation() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/pseudo-async"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().size(), equalTo(2));
        assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("GET /bookstore/books/pseudo-async"));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Processing books"));
    }

    @Test
    public void testThatNewSpanIsCreatedOnClientTimeout() {
        final WebClient client = WebClient
            .create("http://localhost:" + PORT + "/bookstore/books/long", Collections.emptyList(),
                Arrays.asList(new OpenTracingClientFeature(tracer)), null)
            .accept(MediaType.APPLICATION_JSON);

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(100);
        httpClientPolicy.setReceiveTimeout(100);
        WebClient.getConfig(client).getHttpConduit().setClient(httpClientPolicy);

        expectedException.expect(ProcessingException.class);
        try {
            client.get();
        } finally {
            await().atMost(Duration.ofSeconds(1L)).until(()-> REPORTER.getSpans().size() == 2);
            assertThat(REPORTER.getSpans().toString(), REPORTER.getSpans().size(), equalTo(2));
            assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("GET " + client.getCurrentURI()));
            assertThat(REPORTER.getSpans().get(0).getTags(), hasItem(Tags.ERROR.getKey(), Boolean.TRUE));
            assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("GET /bookstore/books/long"));
        }
    }

    @Test
    public void testThatErrorSpanIsCreatedOnExceptionWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/exception").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().toString(), REPORTER.getSpans().size(), equalTo(1));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("GET /bookstore/books/exception"));
        assertThat(REPORTER.getSpans().get(0).getTags(), hasItem(Tags.HTTP_STATUS.getKey(), 500));
    }
    
    @Test
    public void testThatErrorSpanIsCreatedOnErrorWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/error").get();
        assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().toString(), REPORTER.getSpans().size(), equalTo(1));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("GET /bookstore/books/error"));
        assertThat(REPORTER.getSpans().get(0).getTags(), hasItem(Tags.HTTP_STATUS.getKey(), 503));
    }

    @Test
    public void testThatErrorSpanIsCreatedOnMappedExceptionWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/mapper").get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());

        assertThat(REPORTER.getSpans().toString(), REPORTER.getSpans().size(), equalTo(1));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("GET /bookstore/books/mapper"));
        assertThat(REPORTER.getSpans().get(0).getTags(), hasItem(Tags.HTTP_STATUS.getKey(), 404));
    }

    private static WebClient createWebClient(final String path, final Object ... providers) {
        return WebClient
            .create("http://localhost:" + PORT + path, Arrays.asList(providers))
            .accept(MediaType.APPLICATION_JSON);
    }

    private WebClient withTrace(final WebClient client, final JaegerSpanContext spanContext) {
        tracer.inject(spanContext, Builtin.HTTP_HEADERS, new TextMap() {

            @Override
            public void put(String key, String value) {
                client.header(key, value);
            }

            @Override
            public Iterator<Entry<String, String>> iterator() {
                return null;
            }
        });

        return client;
    }

    private<T> T get(final Future<T> future) {
        try {
            return future.get(1L, TimeUnit.MINUTES);
        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static JaegerSpanContext fromRandom() {
        return new JaegerSpanContext(RANDOM.getAndIncrement() /* traceId hi */,
            RANDOM.getAndIncrement() /* traceId lo */, RANDOM.getAndIncrement() /* spanId */,
            RANDOM.getAndIncrement() /* parentId */, (byte) 1 /* sampled */);
    }
}
