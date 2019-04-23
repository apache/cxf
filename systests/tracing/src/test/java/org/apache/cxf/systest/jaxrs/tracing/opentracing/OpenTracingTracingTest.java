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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
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

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaeger.TestSender;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.opentracing.jaxrs.OpenTracingClientProvider;
import org.apache.cxf.tracing.opentracing.jaxrs.OpenTracingFeature;
import org.awaitility.Duration;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.JaegerSpanContext;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sender;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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

public class OpenTracingTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(OpenTracingTracingTest.class);

    private Tracer tracer;
    private OpenTracingClientProvider openTracingClientProvider;
    private final Random random = new Random();

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Tracer tracer = new Configuration("tracer-test-server")
                .withSampler(new SamplerConfiguration().withType(ConstSampler.TYPE).withParam(1))
                .withReporter(new ReporterConfiguration().withSender(
                    new SenderConfiguration() {
                        @Override
                        public Sender getSender() {
                            return new TestSender();
                        }
                    }
                ))
                .getTracer();

            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore<Scope>()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setProvider(new OpenTracingFeature(tracer));
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
        TestSender.clear();

        tracer = new Configuration("tracer-test-client")
            .withSampler(new SamplerConfiguration().withType(ConstSampler.TYPE).withParam(1))
            .withReporter(new ReporterConfiguration().withSender(
                new SenderConfiguration() {
                    @Override
                    public Sender getSender() {
                        return new TestSender();
                    }
                }
            ))
            .getTracer();

        openTracingClientProvider = new OpenTracingClientProvider(tracer);
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(2));
        assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(TestSender.getAllSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(2));
        assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(TestSender.getAllSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
    }

    @Test
    public void testThatCurrentSpanIsAnnotatedWithKeyValue() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/book/1"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(1));
        assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("GET /bookstore/book/1"));
        assertThat(TestSender.getAllSpans().get(0).getTags(), hasItem("book-id", "1"));
    }

    @Test
    public void testThatParallelSpanIsAnnotatedWithTimeline() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/process"), spanId).put("");
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(2));
        assertThat(TestSender.getAllSpans(), hasSpan("Processing books", hasItem("Processing started")));
        assertThat(TestSender.getAllSpans(), hasSpan("PUT /bookstore/process"));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() {
        final Response r = createWebClient("/bookstore/books", openTracingClientProvider).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(3));
        assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(TestSender.getAllSpans().get(0).getReferences(), not(empty()));
    }

    @Test
    public void testThatNewInnerSpanIsCreatedUsingAsyncInvocation() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/async"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(2));
        assertEquals("Processing books", TestSender.getAllSpans().get(0).getOperationName());
        assertEquals("GET /bookstore/books/async", TestSender.getAllSpans().get(1).getOperationName());
        assertThat(TestSender.getAllSpans().get(1).getReferences(), not(empty()));
        assertThat(TestSender.getAllSpans().get(1).getReferences().get(0).getSpanContext().getSpanId(),
            equalTo(spanId.getSpanId()));
    }

    @Test
    public void testThatOuterSpanIsCreatedUsingAsyncInvocation() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/async/notrace"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(1));
        assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("GET /bookstore/books/async/notrace"));
    }

    @Test
    public void testThatNewSpanIsCreatedUsingAsyncInvocation() {
        final Response r = createWebClient("/bookstore/books/async").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(2));
        assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("Processing books"));
        assertThat(TestSender.getAllSpans().get(1).getOperationName(), equalTo("GET /bookstore/books/async"));
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvidedUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", openTracingClientProvider);
        final Future<Response> f = client.async().get();

        final Response r = f.get(1, TimeUnit.SECONDS);
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(3));
        assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(TestSender.getAllSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
        assertThat(TestSender.getAllSpans().get(2).getOperationName(), equalTo("GET " + client.getCurrentURI()));
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleAsyncClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", openTracingClientProvider);

        // The intention is to make a calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        final Collection<Response> responses = IntStream
            .range(0, 4)
            .mapToObj(index -> client.async().get())
            .map(this::get)
            .collect(Collectors.toList());

        for (final Response r: responses) {
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
        }

        assertThat(TestSender.getAllSpans().size(), equalTo(12));

        IntStream
            .range(0, 4)
            .map(index -> index * 3)
            .forEach(index -> {
                assertThat(TestSender.getAllSpans().get(index).getOperationName(),
                    equalTo("Get Books"));
                assertThat(TestSender.getAllSpans().get(index + 1).getOperationName(),
                    equalTo("GET /bookstore/books"));
                assertThat(TestSender.getAllSpans().get(index + 2).getOperationName(),
                    equalTo("GET " + client.getCurrentURI()));
            });
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", openTracingClientProvider);

        // The intention is to make a calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        final Collection<Response> responses = IntStream
            .range(0, 4)
            .mapToObj(index -> client.get())
            .collect(Collectors.toList());

        for (final Response r: responses) {
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
        }

        assertEquals(TestSender.getAllSpans().toString(), 12, TestSender.getAllSpans().size());

        IntStream
            .range(0, 4)
            .map(index -> index * 3)
            .forEach(index -> {
                assertThat(TestSender.getAllSpans().get(index).getOperationName(),
                    equalTo("Get Books"));
                assertThat(TestSender.getAllSpans().get(index + 1).getOperationName(),
                    equalTo("GET /bookstore/books"));
                assertThat(TestSender.getAllSpans().get(index + 2).getOperationName(),
                    equalTo("GET " + client.getCurrentURI()));
            });
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws MalformedURLException {
        final WebClient client = createWebClient("/bookstore/books", openTracingClientProvider);

        try (Scope span = tracer.buildSpan("test span").startActive(true)) {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertEquals(TestSender.getAllSpans().toString(), 3, TestSender.getAllSpans().size());
            assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("Get Books"));
            assertThat(TestSender.getAllSpans().get(0).getReferences(), not(empty()));
            assertThat(TestSender.getAllSpans().get(1).getReferences(), not(empty()));
            assertThat(TestSender.getAllSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
            assertThat(TestSender.getAllSpans().get(2).getOperationName(), equalTo("GET " + client.getCurrentURI()));
            assertThat(TestSender.getAllSpans().get(2).getReferences(), not(empty()));
        }

        // Await till flush happens, usually every second
        await().atMost(Duration.ONE_SECOND).until(()-> TestSender.getAllSpans().size() == 4);

        assertThat(TestSender.getAllSpans().size(), equalTo(4));
        assertThat(TestSender.getAllSpans().get(3).getOperationName(), equalTo("test span"));
        assertThat(TestSender.getAllSpans().get(3).getReferences(), empty());
    }

    @Test
    public void testThatProvidedSpanIsNotDetachedWhenActiveUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", openTracingClientProvider);

        try (Scope scope = tracer.buildSpan("test span").startActive(true)) {
            final Future<Response> f = client.async().get();

            final Response r = f.get(1, TimeUnit.HOURS);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertThat(tracer.activeSpan().context(), equalTo(scope.span().context()));

            assertThat(TestSender.getAllSpans().size(), equalTo(3));
            assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("Get Books"));
            assertThat(TestSender.getAllSpans().get(0).getReferences(), not(empty()));
            assertThat(TestSender.getAllSpans().get(1).getOperationName(), equalTo("GET /bookstore/books"));
            assertThat(TestSender.getAllSpans().get(1).getReferences(), not(empty()));
            assertThat(TestSender.getAllSpans().get(2).getOperationName(), equalTo("GET " + client.getCurrentURI()));
            assertThat(TestSender.getAllSpans().get(2).getReferences(), not(empty()));
        }

        // Await till flush happens, usually every second
        await().atMost(Duration.ONE_SECOND).until(()-> TestSender.getAllSpans().size() == 4);

        assertThat(TestSender.getAllSpans().size(), equalTo(4));
        assertThat(TestSender.getAllSpans().get(3).getOperationName(), equalTo("test span"));
        assertThat(TestSender.getAllSpans().get(3).getReferences(), empty());
    }

    @Test
    public void testThatInnerSpanIsCreatedUsingPseudoAsyncInvocation() {
        final JaegerSpanContext spanId = fromRandom();

        final Response r = withTrace(createWebClient("/bookstore/books/pseudo-async"), spanId).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(TestSender.getAllSpans().size(), equalTo(2));
        assertThat(TestSender.getAllSpans().get(1).getOperationName(), equalTo("GET /bookstore/books/pseudo-async"));
        assertThat(TestSender.getAllSpans().get(0).getOperationName(), equalTo("Processing books"));
    }

    protected WebClient createWebClient(final String url, final Object ... providers) {
        return WebClient
            .create("http://localhost:" + PORT + url, Arrays.asList(providers))
            .accept(MediaType.APPLICATION_JSON);
    }

    protected WebClient withTrace(final WebClient client, final JaegerSpanContext spanContext) {
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
            return future.get(1L, TimeUnit.HOURS);
        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private JaegerSpanContext fromRandom() {
        return new JaegerSpanContext(random.nextLong(), /* traceId */ random.nextLong() /* spanId */,
            random.nextLong() /* parentId */, (byte)1 /* sampled */);
    }
}
