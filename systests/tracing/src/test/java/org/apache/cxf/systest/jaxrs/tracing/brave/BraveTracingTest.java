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
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import brave.Span;
import brave.Tracer.SpanInScope;
import brave.Tracing;
import brave.sampler.Sampler;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.brave.BraveTestSupport.SpanId;
import org.apache.cxf.systest.brave.TestSpanReporter;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.brave.TraceScope;
import org.apache.cxf.tracing.brave.jaxrs.BraveClientProvider;
import org.apache.cxf.tracing.brave.jaxrs.BraveFeature;
import org.awaitility.Duration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BraveTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(BraveTracingTest.class);

    private Tracing brave;
    private BraveClientProvider braveClientProvider;
    private Random random;

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Tracing brave = Tracing
                    .newBuilder()
                    .spanReporter(new TestSpanReporter())
                    .sampler(Sampler.ALWAYS_SAMPLE)
                    .build();

            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore<TraceScope>()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setProvider(new BraveFeature(brave));
            sf.create();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Before
    public void setUp() {
        TestSpanReporter.clear();

        brave = Tracing
                .newBuilder()
                .spanReporter(new TestSpanReporter())
                .sampler(Sampler.ALWAYS_SAMPLE)
                .build();

        braveClientProvider = new BraveClientProvider(brave);
        random = new Random();
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

        assertThatTraceIsPresent(r, spanId);
    }

    @Test
    public void testThatCurrentSpanIsAnnotatedWithKeyValue() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/book/1"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get /bookstore/book/1"));
        assertThat(TestSpanReporter.getAllSpans().get(0).tags(), hasEntry("book-id", "1"));

        assertThatTraceIsPresent(r, spanId);
    }

    @Test
    public void testThatParallelSpanIsAnnotatedWithTimeline() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/process"), spanId).put("");
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans(), hasSpan("processing books", hasItem("Processing started")));
        assertThat(TestSpanReporter.getAllSpans(), hasSpan("put /bookstore/process"));

        assertThatTraceIsPresent(r, spanId);
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() {
        final Response r = createWebClient("/bookstore/books", braveClientProvider).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(0).parentId(), not(nullValue()));

        assertThatTraceHeadersArePresent(r, false);
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

        assertThatTraceIsPresent(r, spanId);
    }

    @Test
    public void testThatOuterSpanIsCreatedUsingAsyncInvocation() {
        final SpanId spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/async/notrace"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get /bookstore/books/async/notrace"));

        assertThatTraceIsPresent(r, spanId);
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
        final WebClient client = createWebClient("/bookstore/books", braveClientProvider);
        final Future<Response> f = client.async().get();

        final Response r = f.get(1, TimeUnit.SECONDS);
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books"));
        assertThat(TestSpanReporter.getAllSpans().get(2).name(), equalTo("get " + client.getCurrentURI()));

        assertThatTraceHeadersArePresent(r, false);
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleAsyncClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", braveClientProvider);

        // The intention is to make a calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        final Collection<Response> responses = IntStream
            .range(0, 4)
            .mapToObj(index -> client.async().get())
            .map(this::get)
            .collect(Collectors.toList());

        for (final Response r: responses) {
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertThatTraceHeadersArePresent(r, false);
        }

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
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", braveClientProvider);

        // The intention is to make a calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        final Collection<Response> responses = IntStream
            .range(0, 4)
            .mapToObj(index -> client.get())
            .collect(Collectors.toList());

        for (final Response r: responses) {
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertThatTraceHeadersArePresent(r, false);
        }

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
        final WebClient client = createWebClient("/bookstore/books", braveClientProvider);
        final Span span = brave.tracer().nextSpan().name("test span").start();

        try {
            try (SpanInScope scope = brave.tracer().withSpanInScope(span)) {
                final Response r = client.get();
                assertEquals(Status.OK.getStatusCode(), r.getStatus());

                assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
                assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
                assertThat(TestSpanReporter.getAllSpans().get(0).parentId(), not(nullValue()));
                assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books"));
                assertThat(TestSpanReporter.getAllSpans().get(2).name(), equalTo("get " + client.getCurrentURI()));

                assertThatTraceHeadersArePresent(r, true);
            }
        } finally {
            span.finish();
        }

        // Await till flush happens, usually a second is enough
        await().atMost(Duration.ONE_SECOND).until(()-> TestSpanReporter.getAllSpans().size() == 4);

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(4));
        assertThat(TestSpanReporter.getAllSpans().get(3).name(), equalTo("test span"));
    }

    @Test
    public void testThatProvidedSpanIsNotDetachedWhenActiveUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", braveClientProvider);
        final Span span = brave.tracer().nextSpan().name("test span").start();

        try {
            try (SpanInScope scope = brave.tracer().withSpanInScope(span)) {
                final Future<Response> f = client.async().get();

                final Response r = f.get(1, TimeUnit.SECONDS);
                assertEquals(Status.OK.getStatusCode(), r.getStatus());
                assertThat(brave.tracer().currentSpan().context().spanId(), equalTo(span.context().spanId()));

                assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
                assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
                assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("get /bookstore/books"));
                assertThat(TestSpanReporter.getAllSpans().get(2).name(), equalTo("get " + client.getCurrentURI()));

                assertThatTraceHeadersArePresent(r, true);
            }
        } finally {
            span.finish();
        }

        // Await till flush happens, usually a second is enough
        await().atMost(Duration.ONE_SECOND).until(()-> TestSpanReporter.getAllSpans().size() == 4);

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

        assertThatTraceIsPresent(r, spanId);
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
        assertThatTraceHeadersArePresent(r, false);
    }

    protected WebClient createWebClient(final String url, final Object ... providers) {
        return WebClient
            .create("http://localhost:" + PORT + url, Arrays.asList(providers))
            .accept(MediaType.APPLICATION_JSON);
    }

    protected WebClient withTrace(final WebClient client, final SpanId spanId) {
        return client
            .header(SPAN_ID_NAME, spanId.spanId())
            .header(TRACE_ID_NAME, spanId.traceId())
            .header(SAMPLED_NAME, spanId.sampled())
            .header(PARENT_SPAN_ID_NAME, spanId.parentId());
    }

    private void assertThatTraceIsPresent(final Response r, final SpanId spanId) {
        assertThat((String)r.getHeaders().getFirst(SPAN_ID_NAME),
            equalTo(Long.toString(spanId.spanId())));
        assertThat((String)r.getHeaders().getFirst(TRACE_ID_NAME),
            equalTo(Long.toString(spanId.traceId())));
        assertThat((String)r.getHeaders().getFirst(SAMPLED_NAME),
            equalTo(Boolean.toString(spanId.sampled())));
        assertThat((String)r.getHeaders().getFirst(PARENT_SPAN_ID_NAME),
            equalTo(Long.toString(spanId.parentId())));
    }

    private void assertThatTraceHeadersArePresent(final Response r, final boolean expectParent) {
        assertTrue(r.getHeaders().containsKey(SPAN_ID_NAME));
        assertTrue(r.getHeaders().containsKey(TRACE_ID_NAME));
        assertTrue(r.getHeaders().containsKey(SAMPLED_NAME));

        if (expectParent) {
            assertTrue(r.getHeaders().containsKey(PARENT_SPAN_ID_NAME));
        } else {
            assertFalse(r.getHeaders().containsKey(PARENT_SPAN_ID_NAME));
        }
    }

    private<T> T get(final Future<T> future) {
        try {
            return future.get(1, TimeUnit.HOURS);
        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private SpanId fromRandom() {
        return new SpanId()
            .traceId(random.nextLong())
            .parentId(random.nextLong())
            .spanId(random.nextLong())
            .sampled(true);
    }
}
