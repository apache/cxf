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
package org.apache.cxf.systest.brave.jaxws;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import brave.Span;
import brave.Tracer.SpanInScope;
import brave.Tracing;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.brave.BraveTestSupport.SpanId;
import org.apache.cxf.systest.brave.TestSpanHandler;
import org.apache.cxf.systest.jaxws.tracing.BookStoreService;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.Test;

import static org.apache.cxf.systest.brave.BraveTestSupport.PARENT_SPAN_ID_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.SAMPLED_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.SPAN_ID_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.TRACE_ID_NAME;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public abstract class AbstractBraveTracingTest extends AbstractClientServerTestBase {
    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() throws Exception {
        final BookStoreService service = createJaxWsService();
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(TestSpanHandler.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanHandler.getAllSpans().get(0).name(), equalTo("Get Books"));
        assertThat(TestSpanHandler.getAllSpans().get(1).name(), equalTo("POST /BookStore"));

        final Map<String, List<String>> headers = getResponseHeaders(service);
        assertFalse(headers.containsKey(TRACE_ID_NAME));
        assertFalse(headers.containsKey(SAMPLED_NAME));
        assertFalse(headers.containsKey(PARENT_SPAN_ID_NAME));
        assertFalse(headers.containsKey(SPAN_ID_NAME));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() throws Exception {
        final Random random = new Random();

        final SpanId spanId = new SpanId()
            .traceId(random.nextLong())
            .parentId(random.nextLong())
            .spanId(random.nextLong())
            .sampled(true);

        final Map<String, List<String>> headers = new HashMap<>();
        headers.put(SPAN_ID_NAME, Arrays.asList(Long.toString(spanId.spanId())));
        headers.put(TRACE_ID_NAME, Arrays.asList(Long.toString(spanId.traceId())));
        headers.put(SAMPLED_NAME, Arrays.asList(Boolean.toString(spanId.sampled())));
        headers.put(PARENT_SPAN_ID_NAME, Arrays.asList(Long.toString(spanId.parentId())));

        final BookStoreService service = createJaxWsService(headers);
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(TestSpanHandler.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanHandler.getAllSpans().get(0).name(), equalTo("Get Books"));
        assertThat(TestSpanHandler.getAllSpans().get(1).name(), equalTo("POST /BookStore"));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() throws Exception {
        try (Tracing brave = createTracer()) {
            final BookStoreService service = createJaxWsService(getClientFeature(brave));
            assertThat(service.getBooks().size(), equalTo(2));
    
            assertThat(TestSpanHandler.getAllSpans().size(), equalTo(3));
            assertThat(TestSpanHandler.getAllSpans().get(0).name(), equalTo("Get Books"));
            assertThat(TestSpanHandler.getAllSpans().get(0).parentId(), not(nullValue()));
            assertThat(TestSpanHandler.getAllSpans().get(1).name(), equalTo("POST /BookStore"));
            assertThat(TestSpanHandler.getAllSpans().get(2).name(),
                equalTo("POST http://localhost:" + getPort() + "/BookStore"));
        }
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws Exception {
        try (Tracing brave = createTracer()) {
            final BookStoreService service = createJaxWsService(getClientFeature(brave));
    
            final Span span = brave.tracer().nextSpan().name("test span").start();
            try {
                try (SpanInScope scope = brave.tracer().withSpanInScope(span)) {
                    assertThat(service.getBooks().size(), equalTo(2));
                    assertThat(brave.tracer().currentSpan(), not(nullValue()));
    
                    assertThat(TestSpanHandler.getAllSpans().size(), equalTo(3));
                    assertThat(TestSpanHandler.getAllSpans().get(0).name(), equalTo("Get Books"));
                    assertThat(TestSpanHandler.getAllSpans().get(0).parentId(), not(nullValue()));
                    assertThat(TestSpanHandler.getAllSpans().get(1).name(), equalTo("POST /BookStore"));
                    assertThat(TestSpanHandler.getAllSpans().get(2).name(),
                        equalTo("POST http://localhost:" + getPort() + "/BookStore"));
                }
            } finally {
                if (span != null) {
                    span.finish();
                }
            }
    
            assertThat(TestSpanHandler.getAllSpans().size(), equalTo(4));
            assertThat(TestSpanHandler.getAllSpans().get(3).name(), equalTo("test span"));
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

        assertThat(TestSpanHandler.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanHandler.getAllSpans().get(0).name(), equalTo("POST /BookStore"));

        final Map<String, List<String>> headers = getResponseHeaders(service);
        assertFalse(headers.containsKey(TRACE_ID_NAME));
        assertFalse(headers.containsKey(SAMPLED_NAME));
        assertFalse(headers.containsKey(PARENT_SPAN_ID_NAME));
        assertFalse(headers.containsKey(SPAN_ID_NAME));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvidedInCaseOfFault() throws Exception {
        try (Tracing brave = createTracer()) {
            final BookStoreService service = createJaxWsService(getClientFeature(brave));
    
            try {
                service.removeBooks();
                fail("Expected SOAPFaultException to be raised");
            } catch (final SOAPFaultException ex) {
                /* expected exception */
            }
    
            assertThat(TestSpanHandler.getAllSpans().size(), equalTo(2));
            assertThat(TestSpanHandler.getAllSpans().get(0).name(), equalTo("POST /BookStore"));
            assertThat(TestSpanHandler.getAllSpans().get(1).name(),
                equalTo("POST http://localhost:" + getPort() + "/BookStore"));
        }
    }
    
    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvidedAndCustomStatusCodeReturned() throws Exception {
        try (Tracing brave = createTracer()) {
            final BookStoreService service = createJaxWsService(getClientFeature(brave));
            service.addBooks();
    
            assertThat(TestSpanHandler.getAllSpans().size(), equalTo(2));
            assertThat(TestSpanHandler.getAllSpans().get(0).name(), equalTo("POST /BookStore"));
            assertThat(TestSpanHandler.getAllSpans().get(0).tags(), hasEntry("http.status_code", "305"));
            assertThat(TestSpanHandler.getAllSpans().get(1).name(),
                    equalTo("POST http://localhost:" + getPort() + "/BookStore"));
        }
    }

    @Test
    public void testThatNewInnerSpanIsCreatedOneway() throws Exception {
        try (Tracing brave = createTracer()) {
            final BookStoreService service = createJaxWsService(getClientFeature(brave));
            service.orderBooks();
    
            // Await till flush happens, usually every second
            await().atMost(Duration.ofSeconds(1L)).until(() -> TestSpanHandler.getAllSpans().size() == 2);

            assertThat(TestSpanHandler.getAllSpans().get(0).name(), equalTo("POST /BookStore"));
            assertThat(TestSpanHandler.getAllSpans().get(1).name(),
                equalTo("POST http://localhost:" + getPort() + "/BookStore"));
        }
    }

    private BookStoreService createJaxWsService() {
        return createJaxWsService(Collections.emptyMap());
    }

    private BookStoreService createJaxWsService(final Map<String, List<String>> headers) {
        return createJaxWsService(headers, null);
    }

    private BookStoreService createJaxWsService(final Feature feature) {
        return createJaxWsService(Collections.emptyMap(), feature);
    }

    private BookStoreService createJaxWsService(final Map<String, List<String>> headers, final Feature feature) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.setServiceClass(BookStoreService.class);
        factory.setAddress("http://localhost:" + getPort() + "/BookStore");

        if (feature != null) {
            factory.getFeatures().add(feature);
        }

        final BookStoreService service = (BookStoreService) factory.create();
        final Client proxy = ClientProxy.getClient(service);
        proxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);

        return service;
    }

    private static Map<String, List<String>> getResponseHeaders(final BookStoreService service) {
        final Client proxy = ClientProxy.getClient(service);
        return CastUtils.cast((Map<?, ?>)proxy.getResponseContext().get(Message.PROTOCOL_HEADERS));
    }
    
    private static Tracing createTracer() {
        return Tracing.newBuilder()
            .localServiceName("book-store")
            .addSpanHandler(new TestSpanHandler())
            .build();
    }

    protected abstract int getPort();

    protected abstract Feature getClientFeature(Tracing tracing);
}
