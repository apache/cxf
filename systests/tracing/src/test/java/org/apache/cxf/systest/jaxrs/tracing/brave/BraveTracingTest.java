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
package org.apache.cxf.systest.jaxrs.tracing.brave;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import brave.Span;
import brave.Tracer.SpanInScope;
import brave.Tracing;
import brave.sampler.Sampler;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.brave.BraveTestSupport.SpanId;
import org.apache.cxf.systest.brave.TestSpanReporter;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.systest.jaxrs.tracing.NullPointerExceptionMapper;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.tracing.brave.BraveClientFeature;
import org.apache.cxf.tracing.brave.TraceScope;
import org.apache.cxf.tracing.brave.jaxrs.BraveClientProvider;
import org.apache.cxf.tracing.brave.jaxrs.BraveFeature;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.cxf.systest.brave.BraveTestSupport.PARENT_SPAN_ID_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.SAMPLED_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.SPAN_ID_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.TRACE_ID_NAME;
import static org.apache.cxf.systest.jaxrs.tracing.brave.HasSpan.hasSpan;
import static org.apache.cxf.systest.jaxrs.tracing.brave.IsAnnotationContaining.hasItem;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BraveTracingTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(BraveTracingTest.class);

    private static final AtomicLong RANDOM = new AtomicLong();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final Tracing brave = Tracing.newBuilder()
        .spanReporter(new TestSpanReporter())
        .build();

    public static class BraveServer extends AbstractTestServerBase {

        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final Tracing brave = Tracing
                    .newBuilder()
                    .spanReporter(new TestSpanReporter())
                    .build();

            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore<TraceScope>()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setProvider(new BraveFeature(brave));
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
        assertTrue("server did not launch correctly", launchServer(BraveServer.class, true));
    }

    @After
    public void tearDown() {
        TestSpanReporter.clear();
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books"));

        assertFalse(r.getHeaders().containsKey(SPAN_ID_NAME));
        assertFalse(r.getHeaders().containsKey(TRACE_ID_NAME));
        assertFalse(r.getHeaders().containsKey(SAMPLED_NAME));
        assertFalse(r.getHeaders().containsKey(PARENT_SPAN_ID_NAME));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books"));
    }

    @Test
    public void testThatCurrentSpanIsAnnotatedWithKeyValue() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/book/1"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get /bookstore/book/1"));
        assertThat(TestSpanReporter.getAllSpans().get(0).tags(), hasEntry("book-id", "1"));
    }

    @Test
    public void testThatParallelSpanIsAnnotatedWithTimeline() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/process"), spanId).put("");
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans(), hasSpan("processing books", hasItem("Processing started")));
        assertThat(TestSpanReporter.getAllSpans(), hasSpan("put /bookstore/process"));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() {
        final Response r = createWebClient("/bookstore/books", new BraveClientProvider(brave)).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(0).parentId(), not(nullValue()));
    }

    @Test
    public void testThatNewInnerSpanIsCreatedUsingAsyncInvocation() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/async"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books/async"));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("processing books"));
        assertThat(TestSpanReporter.getAllSpans().get(0).parentId(), not(nullValue()));
        assertThat(TestSpanReporter.getAllSpans().get(0).parentId(),
            equalTo(TestSpanReporter.getAllSpans().get(1).id()));
    }

    @Test
    public void testThatOuterSpanIsCreatedUsingAsyncInvocation() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/async/notrace"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get /bookstore/books/async/notrace"));
    }

    @Test
    public void testThatNewSpanIsCreatedUsingAsyncInvocation() {
        final Response r = createWebClient("/bookstore/books/async").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books/async"));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("processing books"));
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvidedUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", new BraveClientProvider(brave));
        final Future<Response> f = client.async().get();

        final Response r = f.get(1, TimeUnit.SECONDS);
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books"));
        assertThat(TestSpanReporter.getAllSpans().get(2).name(), equalTo("get " + client.getCurrentURI()));
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleAsyncClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", new BraveClientProvider(brave));

        // The intention is to make a calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        IntStream
            .range(0, 4)
            .mapToObj(index -> client.async().get())
            .map(this::get)
            .forEach(r -> assertEquals(Status.OK.getStatusCode(), r.getStatus()));

        assertThat(TestSpanReporter.getAllSpans().toString(), TestSpanReporter.getAllSpans().size(), equalTo(12));

        IntStream
            .range(0, 4)
            .map(index -> index * 3)
            .forEach(index -> {
                assertThat(TestSpanReporter.getAllSpans().get(index).name(),
                    equalTo("get books"));
                assertThat(TestSpanReporter.getAllSpans().get(index + 1).name(),
                    equalTo("get /bookstore/books"));
                assertThat(TestSpanReporter.getAllSpans().get(index + 2).name(),
                    equalTo("get " + client.getCurrentURI()));
            });
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", new BraveClientProvider(brave));

        // The intention is to make a calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        IntStream
            .range(0, 4)
            .mapToObj(index -> client.get())
            .forEach(r -> assertEquals(Status.OK.getStatusCode(), r.getStatus()));

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(12));

        IntStream
            .range(0, 4)
            .map(index -> index * 3)
            .forEach(index -> {
                assertThat(TestSpanReporter.getAllSpans().get(index).name(),
                    equalTo("get books"));
                assertThat(TestSpanReporter.getAllSpans().get(index + 1).name(),
                    equalTo("get /bookstore/books"));
                assertThat(TestSpanReporter.getAllSpans().get(index + 2).name(),
                    equalTo("get " + client.getCurrentURI()));
            });
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws MalformedURLException {
        final WebClient client = createWebClient("/bookstore/books", new BraveClientProvider(brave));
        final Span span = brave.tracer().nextSpan().name("test span").start();

        try (SpanInScope scope = brave.tracer().withSpanInScope(span)) {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
            assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
            assertThat(TestSpanReporter.getAllSpans().get(0).parentId(), not(nullValue()));
            assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books"));
            assertThat(TestSpanReporter.getAllSpans().get(2).name(), equalTo("get " + client.getCurrentURI()));
        } finally {
            span.finish();
        }

        // Await till flush happens, usually a second is enough
        await().atMost(Duration.ofSeconds(1L)).until(()-> TestSpanReporter.getAllSpans().size() == 4);

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(4));
        assertThat(TestSpanReporter.getAllSpans().get(3).name(), equalTo("test span"));
    }

    @Test
    public void testThatProvidedSpanIsNotDetachedWhenActiveUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", new BraveClientProvider(brave));
        final Span span = brave.tracer().nextSpan().name("test span").start();

        try (SpanInScope scope = brave.tracer().withSpanInScope(span)) {
            final Future<Response> f = client.async().get();

            final Response r = f.get(1, TimeUnit.SECONDS);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertThat(brave.tracer().currentSpan().context().spanId(), equalTo(span.context().spanId()));

            assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
            assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
            assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books"));
            assertThat(TestSpanReporter.getAllSpans().get(2).name(), equalTo("get " + client.getCurrentURI()));
        } finally {
            span.finish();
        }

        // Await till flush happens, usually a second is enough
        await().atMost(Duration.ofSeconds(1L)).until(()-> TestSpanReporter.getAllSpans().size() == 4);

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(4));
        assertThat(TestSpanReporter.getAllSpans().get(3).name(), equalTo("test span"));
    }

    @Test
    public void testThatInnerSpanIsCreatedUsingPseudoAsyncInvocation() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/pseudo-async"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books/pseudo-async"));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("processing books"));
    }

    @Test
    public void testThatNoSpansAreRecordedWhenNotSampled() {
        final Tracing never = Tracing
                .newBuilder()
                .spanReporter(new TestSpanReporter())
                .sampler(Sampler.NEVER_SAMPLE)
                .build();

        final Response r = createWebClient("/bookstore/books", new BraveClientProvider(never)).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(0));
    }

    @Test
    public void testThatNewSpanIsCreatedOnClientTimeout() {
        final WebClient client = WebClient
            .create("http://localhost:" + PORT + "/bookstore/books/long", Collections.emptyList(),
                Arrays.asList(new BraveClientFeature(brave)), null)
            .accept(MediaType.APPLICATION_JSON);

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(100);
        httpClientPolicy.setReceiveTimeout(100);
        WebClient.getConfig(client).getHttpConduit().setClient(httpClientPolicy);

        expectedException.expect(ProcessingException.class);
        try {
            client.get();
        } finally {
            await().atMost(Duration.ofSeconds(1L)).until(()-> TestSpanReporter.getAllSpans().size() == 2);
            assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
            assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get " + client.getCurrentURI()));
            assertThat(TestSpanReporter.getAllSpans().get(0).tags(), hasKey("error"));
            assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books/long"));
        }
    }

    @Test
    public void testThatErrorSpanIsCreatedOnExceptionWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/exception").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get /bookstore/books/exception"));
        assertThat(TestSpanReporter.getAllSpans().get(0).tags(), hasEntry("http.status_code", "500"));

        assertFalse(r.getHeaders().containsKey(SPAN_ID_NAME));
        assertFalse(r.getHeaders().containsKey(TRACE_ID_NAME));
        assertFalse(r.getHeaders().containsKey(SAMPLED_NAME));
        assertFalse(r.getHeaders().containsKey(PARENT_SPAN_ID_NAME));
    }
    
    @Test
    public void testThatErrorSpanIsCreatedOnErrorWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/error").get();
        assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get /bookstore/books/error"));
        assertThat(TestSpanReporter.getAllSpans().get(0).tags(), hasEntry("http.status_code", "503"));

        assertFalse(r.getHeaders().containsKey(SPAN_ID_NAME));
        assertFalse(r.getHeaders().containsKey(TRACE_ID_NAME));
        assertFalse(r.getHeaders().containsKey(SAMPLED_NAME));
        assertFalse(r.getHeaders().containsKey(PARENT_SPAN_ID_NAME));
    }

    @Test
    public void testThatErrorSpanIsCreatedOnMappedExceptionWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/mapper").get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get /bookstore/books/mapper"));
        assertThat(TestSpanReporter.getAllSpans().get(0).tags(), hasEntry("http.status_code", "404"));

        assertFalse(r.getHeaders().containsKey(SPAN_ID_NAME));
        assertFalse(r.getHeaders().containsKey(TRACE_ID_NAME));
        assertFalse(r.getHeaders().containsKey(SAMPLED_NAME));
        assertFalse(r.getHeaders().containsKey(PARENT_SPAN_ID_NAME));
    }

    private static WebClient createWebClient(final String path, final Object ... providers) {
        return WebClient
            .create("http://localhost:" + PORT + path, Arrays.asList(providers))
            .accept(MediaType.APPLICATION_JSON);
    }

    private static WebClient withTrace(final WebClient client, final SpanId spanId) {
        return client
            .header(SPAN_ID_NAME, spanId.spanId())
            .header(TRACE_ID_NAME, spanId.traceId())
            .header(SAMPLED_NAME, spanId.sampled())
            .header(PARENT_SPAN_ID_NAME, spanId.parentId());
    }

    private<T> T get(final Future<T> future) {
        try {
            return future.get(1L, TimeUnit.MINUTES);
        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static SpanId fromRandom() {
        return new SpanId()
            .traceId(RANDOM.getAndIncrement())
            .parentId(RANDOM.getAndIncrement())
            .spanId(RANDOM.getAndIncrement())
            .sampled(true);
    }
}
