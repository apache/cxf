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
package org.apache.cxf.systest.jaxws.tracing.opentracing;

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
import org.apache.cxf.tracing.opentracing.OpenTracingClientFeature;
import org.apache.cxf.tracing.opentracing.OpenTracingFeature;
import org.apache.cxf.tracing.opentracing.internal.TextMapInjectAdapter;

import io.jaegertracing.internal.JaegerSpanContext;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.cxf.systest.jaxrs.tracing.opentracing.IsTagContaining.hasItem;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OpenTracingTracingTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(OpenTracingTracingTest.class);

    private static final AtomicLong RANDOM = new AtomicLong();

    private static final InMemoryReporter REPORTER = new InMemoryReporter();

    private final Tracer tracer = new JaegerTracer.Builder("tracer-jaxws")
        .withSampler(new ConstSampler(true))
        .withReporter(REPORTER)
        .build();

    public static class BraveServer extends AbstractTestServerBase {

        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final Tracer tracer = new JaegerTracer.Builder("tracer-jaxws")
                .withSampler(new ConstSampler(true))
                .withReporter(REPORTER)
                .build();
            GlobalTracer.registerIfAbsent(tracer);

            final JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
            sf.setServiceClass(BookStore.class);
            sf.setAddress("http://localhost:" + PORT);
            sf.getFeatures().add(new OpenTracingFeature(tracer));
            server = sf.create();
        }

        @Override
        public void tearDown() throws Exception {
            server.destroy();
            GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(BraveServer.class, true));
    }

    @After
    public void tearDown() {
        REPORTER.clear();
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() throws Exception {
        final BookStoreService service = createJaxWsService();
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(REPORTER.getSpans().size(), equalTo(2));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("POST /BookStore"));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() throws Exception {
        final JaegerSpanContext spanId = fromRandom();

        final Map<String, List<String>> headers = new HashMap<>();
        tracer.inject(spanId, Builtin.HTTP_HEADERS, new TextMapInjectAdapter(headers));

        final BookStoreService service = createJaxWsService(headers);
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(REPORTER.getSpans().size(), equalTo(2));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("POST /BookStore"));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTracingClientFeature(tracer));
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(REPORTER.getSpans().size(), equalTo(3));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
        assertThat(REPORTER.getSpans().get(0).getReferences(), not(empty()));
        assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("POST /BookStore"));
        assertThat(REPORTER.getSpans().get(1).getTags(), hasItem(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER));
        assertThat(REPORTER.getSpans().get(2).getOperationName(),
            equalTo("POST http://localhost:" + PORT + "/BookStore"));
        assertThat(REPORTER.getSpans().get(2).getTags(), hasItem(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT));
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTracingClientFeature(tracer));

        final Span span = tracer.buildSpan("test span").start();
        try (Scope scope = tracer.activateSpan(span)) {
            assertThat(service.getBooks().size(), equalTo(2));
            assertThat(tracer.activeSpan(), not(nullValue()));

            assertThat(REPORTER.getSpans().size(), equalTo(3));
            assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("Get Books"));
            assertThat(REPORTER.getSpans().get(0).getReferences(), not(empty()));
            assertThat(REPORTER.getSpans().get(1).getOperationName(), equalTo("POST /BookStore"));
            assertThat(REPORTER.getSpans().get(1).getReferences(), not(empty()));
            assertThat(REPORTER.getSpans().get(2).getOperationName(),
                equalTo("POST http://localhost:" + PORT + "/BookStore"));
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
    public void testThatNewSpanIsCreatedInCaseOfFault() throws Exception {
        final BookStoreService service = createJaxWsService();

        try {
            service.removeBooks();
            fail("Expected SOAPFaultException to be raised");
        } catch (final SOAPFaultException ex) {
            /* expected exception */
        }

        assertThat(REPORTER.getSpans().size(), equalTo(1));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("POST /BookStore"));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvidedInCaseOfFault() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTracingClientFeature(tracer));

        try {
            service.removeBooks();
            fail("Expected SOAPFaultException to be raised");
        } catch (final SOAPFaultException ex) {
            /* expected exception */
        }

        assertThat(REPORTER.getSpans().size(), equalTo(2));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("POST /BookStore"));
        assertThat(REPORTER.getSpans().get(0).getTags(), hasItem(Tags.HTTP_STATUS.getKey(), 500));
        assertThat(REPORTER.getSpans().get(1).getOperationName(),
            equalTo("POST http://localhost:" + PORT + "/BookStore"));
    }
    
    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvidedAndCustomStatusCodeReturned() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTracingClientFeature(tracer));
        service.addBooks();
        
        assertThat(REPORTER.getSpans().size(), equalTo(1));
        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("POST /BookStore"));
        assertThat(REPORTER.getSpans().get(0).getTags(), hasItem(Tags.HTTP_STATUS.getKey(), 202));
    }

    @Test
    public void testThatNewInnerSpanIsCreatedOneway() throws Exception {
        final BookStoreService service = createJaxWsService(new OpenTracingClientFeature(tracer));
        service.orderBooks();

        // Await till flush happens, usually every second
        await().atMost(Duration.ofSeconds(1L)).until(() -> REPORTER.getSpans().size() == 2);

        assertThat(REPORTER.getSpans().get(0).getOperationName(), equalTo("POST /BookStore"));
        assertThat(REPORTER.getSpans().get(1).getOperationName(),
            equalTo("POST http://localhost:" + PORT + "/BookStore"));
    }

    private static BookStoreService createJaxWsService() {
        return createJaxWsService(Collections.emptyMap());
    }

    private static BookStoreService createJaxWsService(final Map<String, List<String>> headers) {
        return createJaxWsService(headers, null);
    }

    private BookStoreService createJaxWsService(final Feature feature) {
        return createJaxWsService(Collections.emptyMap(), feature);
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

        final BookStoreService service = (BookStoreService) factory.create();
        final Client proxy = ClientProxy.getClient(service);
        proxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);

        return service;
    }

    private static JaegerSpanContext fromRandom() {
        return new JaegerSpanContext(RANDOM.getAndIncrement() /* traceId hi */,
            RANDOM.getAndIncrement() /* traceId lo */, RANDOM.getAndIncrement() /* spanId */,
            RANDOM.getAndIncrement() /* parentId */, (byte) 1 /* sampled */);
    }
}
