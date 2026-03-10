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
package org.apache.cxf.systest.jaxws.tracing.opentelemetry;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.jaxws.tracing.BookStoreService;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.tracing.opentelemetry.OpenTelemetryClientFeature;
import org.apache.cxf.tracing.opentelemetry.OpenTelemetryFeature;
import org.apache.cxf.tracing.opentelemetry.internal.TextMapInjectAdapter;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
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
import org.junit.Test;

import static org.apache.cxf.systest.jaxrs.tracing.opentelemetry.HasAttribute.hasAttribute;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class OpenTelemetryTracingTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(OpenTelemetryTracingTest.class);

    @ClassRule
    public static OpenTelemetryRule otelRule = OpenTelemetryRule.create();

    private static final AtomicLong RANDOM = new AtomicLong();

    @BeforeClass
    public static void startServers() throws Exception {
        // keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(BraveServer.class, true));
    }

    private static BookStoreService createJaxWsService() {
        return createJaxWsService(Collections.emptyMap());
    }

    private static BookStoreService createJaxWsService(final Map<String, List<String>> headers) {
        return createJaxWsService(headers, null);
    }

    private static BookStoreService createJaxWsService(final Map<String, List<String>> headers,
                                                       final Feature feature) {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.setServiceClass(BookStoreService.class);
        factory.setAddress("http://localhost:" + PORT + "/BookStore");

        if (feature != null) {
            factory.getFeatures().add(feature);
        }

        final BookStoreService service = (BookStoreService)factory.create();
        final Client proxy = ClientProxy.getClient(service);
        proxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);

        return service;
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
    public void testThatNewSpanIsCreatedWhenNotProvided() throws Exception {
        final BookStoreService service = createJaxWsService();
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(otelRule.getSpans().size(), equalTo(2));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));

        SpanData serverSpan = otelRule.getSpans().get(1);
        assertThat(serverSpan.getName(), equalTo("POST /BookStore"));
        assertThat(serverSpan.getKind(), equalTo(SpanKind.SERVER));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST"));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_PATH, "/BookStore"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(NetworkAttributes.NETWORK_PEER_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ClientAttributes.CLIENT_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(ClientAttributes.CLIENT_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_SCHEME, "http"));
        String userAgent = serverSpan.getAttributes().get(UserAgentAttributes.USER_AGENT_ORIGINAL);
        assertNotNull(userAgent);
        assertThat(userAgent, containsString("Apache-CXF/"));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() throws Exception {
        final Context parentContext = fromRandom();

        try (Scope parentScope = parentContext.makeCurrent()) {
            final Map<String, List<String>> headers = new HashMap<>();
            GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), headers,
                                                                               TextMapInjectAdapter.get());

            final BookStoreService service = createJaxWsService(headers);
            assertThat(service.getBooks().size(), equalTo(2));

            assertThat(otelRule.getSpans().size(), equalTo(2));
            assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
            assertThat(otelRule.getSpans().get(1).getName(), equalTo("POST /BookStore"));
        }
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTelemetryClientFeature(otelRule
            .getOpenTelemetry(), "jaxws-client-test"));
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(otelRule.getSpans().size(), equalTo(3));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
        assertThat(otelRule.getSpans().get(0).getParentSpanContext().isValid(), equalTo(true));

        SpanData serverSpan = otelRule.getSpans().get(1);
        assertThat(serverSpan.getName(), equalTo("POST /BookStore"));
        assertThat(serverSpan.getKind(), equalTo(SpanKind.SERVER));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST"));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_PATH, "/BookStore"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(NetworkAttributes.NETWORK_PEER_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(ClientAttributes.CLIENT_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(ClientAttributes.CLIENT_PORT));

        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1"));

        SpanData clientSpan = otelRule.getSpans().get(2);
        assertThat(clientSpan.getName(), equalTo("POST http://localhost:" + PORT + "/BookStore"));
        assertThat(clientSpan.getKind(), equalTo(SpanKind.CLIENT));
        assertThat(clientSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L));
        assertThat(clientSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST"));
        assertThat(clientSpan.getAttributes(), hasAttribute(UrlAttributes.URL_FULL,
                "http://localhost:" + PORT + "/BookStore"));
        assertThat(clientSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(clientSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(clientSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "localhost"));
        assertThat(clientSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_PORT,  Long.valueOf(PORT)));
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTelemetryClientFeature(otelRule
            .getOpenTelemetry(), "jaxws-client-test"));

        try (Scope parentScope = Context.root().makeCurrent()) {
            final Span span = otelRule.getOpenTelemetry().getTracer("test").spanBuilder("test span")
                .startSpan();
            try (Scope scope = span.makeCurrent()) {
                assertThat(service.getBooks().size(), equalTo(2));
                assertThat(Span.current(), not(nullValue()));

                assertThat(otelRule.getSpans().size(), equalTo(3));
                assertThat(otelRule.getSpans().get(0).getName(), equalTo("Get Books"));
                assertThat(otelRule.getSpans().get(0).getParentSpanContext().isValid(), equalTo(true));
                assertThat(otelRule.getSpans().get(0).getInstrumentationScopeInfo().getName(),
                           equalTo(BookStore.class.getName()));
                assertThat(otelRule.getSpans().get(1).getName(), equalTo("POST /BookStore"));
                assertThat(otelRule.getSpans().get(1).getParentSpanContext().isValid(), equalTo(true));
                assertThat(otelRule.getSpans().get(1).getInstrumentationScopeInfo().getName(),
                           equalTo("jaxws-server-test"));
                assertThat(otelRule.getSpans().get(2).getName(),
                        equalTo("POST http://localhost:" + PORT + "/BookStore"));
                assertThat(otelRule.getSpans().get(2).getParentSpanContext().isValid(), equalTo(true));
                assertThat(otelRule.getSpans().get(2).getInstrumentationScopeInfo().getName(),
                           equalTo("jaxws-client-test"));
            } finally {
                span.end();
            }

            // Await till flush happens, usually every second
            await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 4);

            assertThat(otelRule.getSpans().size(), equalTo(4));
            assertThat(otelRule.getSpans().get(3).getName(), equalTo("test span"));
            assertThat(otelRule.getSpans().get(3).getParentSpanContext().isValid(), equalTo(false));
        }
    }

    @Test
    public void testThatNewSpanIsCreatedInCaseOfFault() throws Exception {
        final BookStoreService service = createJaxWsService();

        try {
            service.removeBooks();
            fail("Expected SOAPFaultException to be raised");
        } catch (final SOAPFaultException ex) {
            /* expected exception */
        }

        assertThat(otelRule.getSpans().size(), equalTo(1));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("POST /BookStore"));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvidedInCaseOfFault() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTelemetryClientFeature(otelRule
            .getOpenTelemetry(), "jaxws-client-test"));

        try {
            service.removeBooks();
            fail("Expected SOAPFaultException to be raised");
        } catch (final SOAPFaultException ex) {
            /* expected exception */
        }

        assertThat(otelRule.getSpans().size(), equalTo(2));

        SpanData serverSpan = otelRule.getSpans().get(0);
        assertThat(serverSpan.getName(), equalTo("POST /BookStore"));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 500L));
        assertThat(serverSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST"));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_PATH, "/BookStore"));
        assertThat(serverSpan.getAttributes(), hasAttribute(UrlAttributes.URL_SCHEME, "http"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(serverSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(serverSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(NetworkAttributes.NETWORK_PEER_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(ClientAttributes.CLIENT_ADDRESS, "127.0.0.1"));
        assertNotNull(serverSpan.getAttributes().get(ClientAttributes.CLIENT_PORT));
        assertThat(serverSpan.getAttributes(), hasAttribute(ErrorAttributes.ERROR_TYPE,  String.valueOf(500)));

        SpanData clientSpan = otelRule.getSpans().get(1);
        assertThat(clientSpan.getName(), equalTo("POST http://localhost:" + PORT + "/BookStore"));
        assertThat(clientSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 500L));
        assertThat(clientSpan.getAttributes(), hasAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST"));
        assertThat(clientSpan.getAttributes(), hasAttribute(UrlAttributes.URL_FULL,
                "http://localhost:" + PORT + "/BookStore"));
        assertThat(clientSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_ADDRESS, "localhost"));
        assertThat(clientSpan.getAttributes(), hasAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(PORT)));
        assertThat(clientSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, "localhost"));
        assertThat(clientSpan.getAttributes(), hasAttribute(NetworkAttributes.NETWORK_PEER_PORT,  Long.valueOf(PORT)));
        assertThat(clientSpan.getAttributes(), hasAttribute(ErrorAttributes.ERROR_TYPE,  String.valueOf(500)));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvidedAndCustomStatusCodeReturned()
        throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTelemetryClientFeature(otelRule
            .getOpenTelemetry(), "jaxws-client-test"));
        service.addBooks();

        assertThat(otelRule.getSpans().size(), equalTo(1));
        assertThat(otelRule.getSpans().get(0).getName(), equalTo("POST /BookStore"));
        assertThat(otelRule.getSpans().get(0).getAttributes(),
                   hasAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 202L));
    }

    @Test
    public void testThatNewInnerSpanIsCreatedOneway() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTelemetryClientFeature(otelRule
                .getOpenTelemetry(), "jaxws-client-test"));
        service.orderBooks();

        // Await till flush happens, usually every second
        await().atMost(Duration.ofSeconds(1L)).until(() -> otelRule.getSpans().size() == 2);

        assertThat(otelRule.getSpans().get(0).getName(), equalTo("POST /BookStore"));
        assertThat(otelRule.getSpans().get(1).getName(),
                equalTo("POST http://localhost:" + PORT + "/BookStore"));
    }

    private BookStoreService createJaxWsService(final Feature feature) {
        return createJaxWsService(Collections.emptyMap(), feature);
    }

    public static class BraveServer extends AbstractTestServerBase {

        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
            sf.setServiceClass(BookStore.class);
            sf.setAddress("http://localhost:" + PORT);
            sf.getFeatures().add(new OpenTelemetryFeature(otelRule.getOpenTelemetry(), "jaxws-server-test"));
            server = sf.create();
        }

        @Override
        public void tearDown() throws Exception {
            server.destroy();
        }
    }
}
