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
package org.apache.cxf.systest.jaxrs.tracing.opentelemetry;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

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
import org.apache.cxf.tracing.opentelemetry.OpenTelemetryClientFeature;
import org.apache.cxf.tracing.opentelemetry.jaxrs.OpenTelemetryClientProvider;
import org.apache.cxf.tracing.opentelemetry.jaxrs.OpenTelemetryFeature;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.cxf.systest.jaxrs.tracing.opentelemetry.HasAttribute.hasAttribute;
import static org.apache.cxf.systest.jaxrs.tracing.opentelemetry.HasSpan.hasSpan;
import static org.apache.cxf.systest.jaxrs.tracing.opentelemetry.IsLogContaining.hasItem;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpenTelemetryTracingTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(OpenTelemetryTracingTest.class);

    @ClassRule
    public static OpenTelemetryRule otelRule = OpenTelemetryRule.create();

    private static final AtomicLong RANDOM = new AtomicLong();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void startServers() {
        AbstractResourceInfo.clearAllMaps();
        // keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(OpenTelemetryServer.class, true));
    }

    private static WebClient createWebClient(final String path, final Object... providers) {
        return WebClient.create("http://localhost:" + PORT + path, Arrays.asList(providers))
            .accept(MediaType.APPLICATION_JSON);
    }

    private static Context fromRandom() {
        return Context.root()
            .with(Span.wrap(SpanContext
                .create(TraceId.fromLongs(RANDOM.getAndIncrement(), RANDOM.getAndIncrement()),
                        SpanId.fromLong(RANDOM.getAndIncrement()), TraceFlags.getSampled(),
                        TraceState.getDefault())));
    }

    @After
    public void tearDown() {
        otelRule.clearSpans();
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(otelRule.getSpans().size(), equalTo(2));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
        SpanData serverSpan = otelRule.getSpans().get(1);
        assertThat(serverSpan.getName(), equalTo("GET /bookstore/books"));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET"));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_PATH, "/bookstore/books"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(NetworkAttributes.NETWORK_PEER_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(ClientAttributes.CLIENT_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(ClientAttributes.CLIENT_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_SCHEME, "http"));
        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1"));
        String userAgent = serverSpan.getAttributes().get(UserAgentAttributes.USER_AGENT_ORIGINAL);
        assertNotNull(userAgent);
        assertThat(userAgent, containsString("Apache-CXF/"));
        assertThat(serverSpan.getInstrumentationScopeInfo().getName(), equalTo("jaxrs-server-test"));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() {
        final Context parentContext = fromRandom();

        try (Scope parentScope = parentContext.makeCurrent()) {
            final Response r = withTrace(createWebClient("/bookstore/books")).get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertThat(otelRule.getSpans().size(), equalTo(2));
            assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
            assertThat(otelRule.getSpans().get(1).getName(), equalTo("GET /bookstore/books"));
        }
    }

    @Test
    public void testThatCurrentSpanIsAnnotatedWithKeyValue() {
        final Context parentContext = fromRandom();

        try (Scope parentScope = parentContext.makeCurrent()) {
            final Response r = withTrace(createWebClient("/bookstore/book/1")).get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertThat(otelRule.getSpans().size(), equalTo(1));
            assertThat(otelRule.getSpans().get(0).getName(), equalTo("GET /bookstore/book/1"));
            assertThat(otelRule.getSpans().get(0).getAttributes(), hasAttribute("book-id", "1"));
        }
    }

    @Test
    public void testThatParallelSpanIsAnnotatedWithTimeline() {
        final Context parentContext = fromRandom();

        try (Scope parentScope = parentContext.makeCurrent()) {
            final Response r = withTrace(createWebClient("/bookstore/process")).put("");
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertThat(otelRule.getSpans().size(), equalTo(2));
            assertThat(otelRule.getSpans(), hasSpan("Processing books", hasItem("Processing started")));
            assertThat(otelRule.getSpans(), hasSpan("PUT /bookstore/process"));
        }
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() {
        final Response r = createWebClient("/bookstore/books",
            new OpenTelemetryClientProvider(otelRule.getOpenTelemetry(), "jaxrs-client-test"))
                .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(otelRule.getSpans().toString(), otelRule.getSpans().size(), equalTo(3));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
        assertThat(otelRule.getSpans().get(0).getParentSpanContext().isValid(), equalTo(true));

        // let's check that the parent client span has the custom instrumentation name in scope info
        assertThat(otelRule.getSpans().get(2).getInstrumentationScopeInfo().getName(), equalTo("jaxrs-client-test"));
    }

    @Test
    public void spanShouldHasRequiredAttributes() {
        final Response r = createWebClient("/bookstore/books",
                new OpenTelemetryClientProvider(otelRule.getOpenTelemetry(), "jaxrs-client-test"))
                .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(otelRule.getSpans().toString(), otelRule.getSpans().size(), equalTo(3));

        SpanData clientSpan = otelRule.getSpans().get(2);
        assertThat(clientSpan.getName(), equalTo("GET http://localhost:" + PORT + "/bookstore/books"));
        assertThat(clientSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        assertThat(clientSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET"));
        assertThat(clientSpan.getAttributes(), hasAttribute(UrlAttributes.URL_FULL,
                "http://localhost:" + PORT + "/bookstore/books"));
        assertThat(clientSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(clientSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(clientSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "localhost"));
        assertThat(clientSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_PORT, Long.valueOf(PORT)));

        SpanData serverSpan = otelRule.getSpans().get(1);
        assertThat(serverSpan.getName(), equalTo("GET /bookstore/books"));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET"));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_PATH, "/bookstore/books"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(NetworkAttributes.NETWORK_PEER_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(ClientAttributes.CLIENT_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(ClientAttributes.CLIENT_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_SCHEME, "http"));
        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1"));
        String userAgent = serverSpan.getAttributes().get(UserAgentAttributes.USER_AGENT_ORIGINAL);
        assertNotNull(userAgent);
        assertThat(userAgent, containsString("Apache-CXF/"));
        assertThat(serverSpan.getInstrumentationScopeInfo().getName(), equalTo("jaxrs-server-test"));
    }

    @Test
    public void testThatNewInnerSpanIsCreatedUsingAsyncInvocation() throws InterruptedException {
        final Context parentContext = fromRandom();

        try (Scope parentScope = parentContext.makeCurrent()) {
            final Response r = withTrace(createWebClient("/bookstore/books/async")).get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 2);

            final List<SpanData> spans = getSpansSorted();
            assertThat(spans.size(), equalTo(2));

            assertEquals("Processing books", spans.get(0).getName());
            assertEquals("GET /bookstore/books/async", spans.get(1).getName());
            assertThat(spans.get(1).getParentSpanContext().isValid(), equalTo(true));
            assertThat(spans.get(1).getParentSpanId(), equalTo(Span.current().getSpanContext().getSpanId()));
        }
    }

    @Test
    public void testThatOuterSpanIsCreatedUsingAsyncInvocation() {
        final Context parentContext = fromRandom();

        try (Scope parentScope = parentContext.makeCurrent()) {
            final Response r = withTrace(createWebClient("/bookstore/books/async/notrace")).get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertThat(otelRule.getSpans().size(), equalTo(1));
            assertThat(otelRule.getSpans().get(0).getName(), equalTo("GET /bookstore/books/async/notrace"));
        }
    }

    @Test
    public void testThatNewSpanIsCreatedUsingAsyncInvocation() throws InterruptedException {
        final Response r = createWebClient("/bookstore/books/async").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 2);

        final List<SpanData> spans = getSpansSorted();
        assertThat(spans.size(), equalTo(2));
        assertThat(spans.get(0).getName(), equalTo("Processing books"));
        assertThat(spans.get(1).getName(), equalTo("GET /bookstore/books/async"));
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvidedUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books",
            new OpenTelemetryClientProvider(otelRule.getOpenTelemetry(), "jaxrs-client-test"));

        final Response r = client.async().get().get(1L, TimeUnit.SECONDS);
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 3);

        assertThat(otelRule.getSpans().size(), equalTo(3));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
        assertThat(otelRule.getSpans().get(1).getName(), equalTo("GET /bookstore/books"));
        assertThat(otelRule.getSpans().get(1).getKind(), equalTo(SpanKind.SERVER));
        assertThat(otelRule.getSpans().get(2).getName(), equalTo("GET " + client.getCurrentURI()));
        assertThat(otelRule.getSpans().get(2).getKind(), equalTo(SpanKind.CLIENT));
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleAsyncClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books",
            new OpenTelemetryClientProvider(otelRule.getOpenTelemetry(), "jaxrs-client-test"));

        // The intention is to make multiple calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        IntStream.range(0, 4).mapToObj(index -> client.async().get()).map(this::get)
            .forEach(r -> assertEquals(Status.OK.getStatusCode(), r.getStatus()));

        assertThat(otelRule.getSpans().size(), equalTo(12));

        IntStream.range(0, 4).map(index -> index * 3).forEach(index -> {
            assertThat(otelRule.getSpans().get(index).getName(), equalTo("Get Books"));
            assertThat(otelRule.getSpans().get(index + 1).getName(), equalTo("GET /bookstore/books"));
            assertThat(otelRule.getSpans().get(index + 2).getName(), equalTo("GET " + client.getCurrentURI()));
        });
    }

    @Test
    public void testThatNewSpansAreCreatedWhenNotProvidedUsingMultipleClients() throws Exception {
        final WebClient client = createWebClient("/bookstore/books",
            new OpenTelemetryClientProvider(otelRule.getOpenTelemetry(), "jaxrs-client-test"));

        // The intention is to make multiple calls one after another, not in parallel, to ensure the
        // thread have trace contexts cleared out.
        IntStream.range(0, 4).mapToObj(index -> client.get())
            .forEach(r -> assertEquals(Status.OK.getStatusCode(), r.getStatus()));

        assertEquals(otelRule.getSpans().toString(), 12, otelRule.getSpans().size());

        IntStream.range(0, 4).map(index -> index * 3).forEach(index -> {
            assertThat(otelRule.getSpans().get(index).getName(), equalTo("Get Books"));
            assertThat(otelRule.getSpans().get(index + 1).getName(), equalTo("GET /bookstore/books"));
            assertThat(otelRule.getSpans().get(index + 2).getName(), equalTo("GET " + client.getCurrentURI()));
        });
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws MalformedURLException {
        final WebClient client = createWebClient("/bookstore/books",
            new OpenTelemetryClientProvider(otelRule.getOpenTelemetry(), "jaxrs-client-test"));

        final Span span = otelRule.getOpenTelemetry().getTracer("test").spanBuilder("test span").startSpan();
        try (Scope scope = span.makeCurrent()) {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertEquals(otelRule.getSpans().toString(), 3, otelRule.getSpans().size());
            assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
            assertThat(otelRule.getSpans().get(0).getParentSpanContext().isValid(), equalTo(true));
            assertThat(otelRule.getSpans().get(1).getName(), equalTo("GET /bookstore/books"));
            assertThat(otelRule.getSpans().get(1).getParentSpanContext().isValid(), equalTo(true));
            assertThat(otelRule.getSpans().get(2).getName(), equalTo("GET " + client.getCurrentURI()));
            assertThat(otelRule.getSpans().get(2).getParentSpanContext().isValid(), equalTo(true));
        } finally {
            span.end();
        }

        // Await till flush happens, usually every second
        await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 4);

        assertThat(otelRule.getSpans().size(), equalTo(4));
        assertThat(otelRule.getSpans().get(3).getName(), equalTo("test span"));
        assertThat(otelRule.getSpans().get(3).getParentSpanContext().isValid(), equalTo(false));
    }

    @Test
    public void testThatProvidedSpanIsNotDetachedWhenActiveUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books",
            new OpenTelemetryClientProvider(otelRule.getOpenTelemetry(), "jaxrs-client-test"));

        final Span span = otelRule.getOpenTelemetry().getTracer("test").spanBuilder("test span").startSpan();
        try (Scope scope = span.makeCurrent()) {
            final Response r = client.async().get().get(1L, TimeUnit.MINUTES);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertThat(Span.current().getSpanContext().getSpanId(),
                       equalTo(span.getSpanContext().getSpanId()));

            await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 3);

            assertThat(otelRule.getSpans().size(), equalTo(3));
            assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
            assertThat(otelRule.getSpans().get(0).getParentSpanContext(), notNullValue());
            assertThat(otelRule.getSpans().get(1).getName(), equalTo("GET /bookstore/books"));
            assertThat(otelRule.getSpans().get(1).getParentSpanContext().isValid(), equalTo(true));
            assertThat(otelRule.getSpans().get(2).getName(), equalTo("GET " + client.getCurrentURI()));
            assertThat(otelRule.getSpans().get(2).getParentSpanContext(), notNullValue());
        } finally {
            span.end();
        }

        // Await till flush happens, usually every second
        await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 4);

        assertThat(otelRule.getSpans().size(), equalTo(4));
        assertThat(otelRule.getSpans().get(3).getName(), equalTo("test span"));
        assertThat(otelRule.getSpans().get(3).getParentSpanContext().isValid(), equalTo(false));
    }

    @Test
    public void testThatInnerSpanIsCreatedUsingPseudoAsyncInvocation() {
        final Context parentContext = fromRandom();

        try (Scope parentScope = parentContext.makeCurrent()) {
            final Response r = withTrace(createWebClient("/bookstore/books/pseudo-async")).get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            assertThat(otelRule.getSpans().size(), equalTo(2));
            assertThat(otelRule.getSpans().get(1).getName(), equalTo("GET /bookstore/books/pseudo-async"));
            assertThat(otelRule.getSpans().get(0).getName(), equalTo("Processing books"));
        }
    }

    @Test
    public void testThatNewSpanIsCreatedOnClientTimeout() {
        final WebClient client = WebClient
            .create("http://localhost:" + PORT + "/bookstore/books/long", Collections.emptyList(),
                    Arrays.asList(new OpenTelemetryClientFeature(otelRule.getOpenTelemetry())), null)
            .accept(MediaType.APPLICATION_JSON);

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(100);
        httpClientPolicy.setReceiveTimeout(100);
        WebClient.getConfig(client).getHttpConduit().setClient(httpClientPolicy);

        expectedException.expect(ProcessingException.class);
        try {
            client.get();
        } finally {
            await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 2);
            assertThat(otelRule.getSpans().toString(), otelRule.getSpans().size(), equalTo(2));
            assertThat(otelRule.getSpans().get(0).getName(), equalTo("GET " + client.getCurrentURI()));
            assertThat(otelRule.getSpans().get(0).getStatus().getStatusCode(), equalTo(StatusCode.ERROR));
            assertThat(otelRule.getSpans().get(1).getName(), equalTo("GET /bookstore/books/long"));
        }
    }

    @Test
    public void testThatErrorSpanIsCreatedOnExceptionWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/exception").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());

        assertThat(otelRule.getSpans().toString(), otelRule.getSpans().size(), equalTo(1));
        SpanData serverSpan = otelRule.getSpans().get(0);

        assertThat(serverSpan.getName(), equalTo("GET /bookstore/books/exception"));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 500L));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET"));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_PATH, "/bookstore/books/exception"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(serverSpan.getAttributes(), hasAttribute(ErrorAttributes.ERROR_TYPE,  String.valueOf(500)));
    }

    @Test
    public void testClientSpanAttributesOnException() {
        final Response r = createWebClient("/bookstore/books/exception",
                new OpenTelemetryClientProvider(otelRule.getOpenTelemetry(), "jaxrs-client-test"))
                .get();

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());

        assertThat(otelRule.getSpans().toString(), otelRule.getSpans().size(), equalTo(2));

        SpanData clientSpan = otelRule.getSpans().get(1);
        assertThat(clientSpan.getName(), equalTo("GET http://localhost:" + PORT + "/bookstore/books/exception"));
        assertThat(clientSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET"));
        assertThat(clientSpan.getAttributes(), hasAttribute(UrlAttributes.URL_FULL,
                "http://localhost:" + PORT + "/bookstore/books/exception"));
        assertThat(clientSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(clientSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(clientSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "localhost"));
        assertThat(clientSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_PORT, Long.valueOf(PORT)));
        assertThat(clientSpan.getAttributes(), hasAttribute(ErrorAttributes.ERROR_TYPE,  String.valueOf(500)));
    }

    @Test
    public void testThatErrorSpanIsCreatedOnErrorWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/error").get();
        assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), r.getStatus());

        assertThat(otelRule.getSpans().toString(), otelRule.getSpans().size(), equalTo(1));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("GET /bookstore/books/error"));
        assertThat(otelRule.getSpans().get(0).getAttributes(),
                   hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 503L));
    }

    @Test
    public void testThatErrorSpanIsCreatedOnMappedExceptionWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books/mapper").get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());

        assertThat(otelRule.getSpans().toString(), otelRule.getSpans().size(), equalTo(1));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("GET /bookstore/books/mapper"));
        assertThat(otelRule.getSpans().get(0).getAttributes(),
                   hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 404L));
    }

    private WebClient withTrace(final WebClient client) {
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), client,
            new TextMapSetter<WebClient>() {
                @Override
                public void set(WebClient carrier, String key, String value) {
                    carrier.header(key, value);
                }
            });

        return client;
    }

    private <T> T get(final Future<T> future) {
        try {
            return future.get(1L, TimeUnit.MINUTES);
        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<SpanData> getSpansSorted() {
        final List<SpanData> spans = new ArrayList<>(otelRule.getSpans());
        spans.sort(Comparator
            .comparingLong(SpanData::getStartEpochNanos)
            .thenComparingLong(SpanData::getEndEpochNanos)
            .reversed());
        return spans;
    }

    public static class OpenTelemetryServer extends AbstractTestServerBase {

        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore<Scope>()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setProvider(new OpenTelemetryFeature(otelRule.getOpenTelemetry(), "jaxrs-server-test"));
            sf.setProvider(new NullPointerExceptionMapper());
            server = sf.create();
        }

        @Override
        public void tearDown() throws Exception {
            server.destroy();
        }
    }
}
