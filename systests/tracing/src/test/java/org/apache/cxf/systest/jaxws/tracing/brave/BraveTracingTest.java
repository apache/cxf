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
package org.apache.cxf.systest.jaxws.tracing.brave;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.ws.soap.SOAPFaultException;

import brave.Span;
import brave.Tracer.SpanInScope;
import brave.Tracing;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.brave.BraveTestSupport.SpanId;
import org.apache.cxf.systest.brave.TestSpanReporter;
import org.apache.cxf.systest.jaxws.tracing.BookStoreService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.brave.BraveClientFeature;
import org.apache.cxf.tracing.brave.BraveFeature;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.cxf.systest.brave.BraveTestSupport.PARENT_SPAN_ID_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.SAMPLED_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.SPAN_ID_NAME;
import static org.apache.cxf.systest.brave.BraveTestSupport.TRACE_ID_NAME;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class BraveTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(BraveTracingTest.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Tracing brave = Tracing.newBuilder()
                .localServiceName("book-store")
                .spanReporter(new TestSpanReporter())
                .build();

            final JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
            sf.setServiceClass(BookStore.class);
            sf.setAddress("http://localhost:" + PORT);
            sf.getFeatures().add(new BraveFeature(brave));
            sf.create();
        }
    }

    private interface Configurator {
        void configure(JaxWsProxyFactoryBean factory);
    }

    @BeforeClass
    public static void startServers() throws Exception {
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Before
    public void setUp() {
        TestSpanReporter.clear();
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() throws MalformedURLException {
        final BookStoreService service = createJaxWsService();
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("post /bookstore"));

        final Map<String, List<String>> headers = getResponseHeaders(service);
        assertFalse(headers.containsKey(TRACE_ID_NAME));
        assertFalse(headers.containsKey(SAMPLED_NAME));
        assertFalse(headers.containsKey(PARENT_SPAN_ID_NAME));
        assertFalse(headers.containsKey(SPAN_ID_NAME));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() throws MalformedURLException {
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

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("post /bookstore"));

        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThatTraceIsPresent(response, spanId);
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() throws MalformedURLException {
        final Tracing brave = Tracing.newBuilder()
            .localServiceName("book-store")
            .spanReporter(new TestSpanReporter())
            .build();

        final BookStoreService service = createJaxWsService(new Configurator() {
            @Override
            public void configure(final JaxWsProxyFactoryBean factory) {
                factory.getFeatures().add(new BraveClientFeature(brave));
            }
        });
        assertThat(service.getBooks().size(), equalTo(2));

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(0).parentId(), not(nullValue()));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("post /bookstore"));
        assertThat(TestSpanReporter.getAllSpans().get(2).name(),
            equalTo("post http://localhost:" + PORT + "/bookstore"));

        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThatTraceHeadersArePresent(response, false);
    }

    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws MalformedURLException {
        final Tracing brave = Tracing.newBuilder()
            .localServiceName("book-store")
            .spanReporter(new TestSpanReporter())
            .build();

        final BookStoreService service = createJaxWsService(new Configurator() {
            @Override
            public void configure(final JaxWsProxyFactoryBean factory) {
                factory.getFeatures().add(new BraveClientFeature(brave));
            }
        });

        final Span span = brave.tracer().nextSpan().name("test span").start();
        try {
            try (SpanInScope scope = brave.tracer().withSpanInScope(span)) {
                assertThat(service.getBooks().size(), equalTo(2));
                assertThat(brave.tracer().currentSpan(), not(nullValue()));

                assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
                assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("get books"));
                assertThat(TestSpanReporter.getAllSpans().get(0).parentId(), not(nullValue()));
                assertThat(TestSpanReporter.getAllSpans().get(1).name(), equalTo("post /bookstore"));
                assertThat(TestSpanReporter.getAllSpans().get(2).name(),
                    equalTo("post http://localhost:" + PORT + "/bookstore"));

                final Map<String, List<String>> response = getResponseHeaders(service);
                assertThatTraceHeadersArePresent(response, true);
            }
        } finally {
            if (span != null) {
                span.finish();
            }
        }

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(4));
        assertThat(TestSpanReporter.getAllSpans().get(3).name(), equalTo("test span"));
    }

    @Test
    public void testThatNewSpanIsCreatedInCaseOfFault() throws MalformedURLException {
        final BookStoreService service = createJaxWsService();

        try {
            service.removeBooks();
            fail("Expected SOAPFaultException to be raised");
        } catch (final SOAPFaultException ex) {
            /* expected exception */
        }

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("post /bookstore"));

        final Map<String, List<String>> headers = getResponseHeaders(service);
        assertFalse(headers.containsKey(TRACE_ID_NAME));
        assertFalse(headers.containsKey(SAMPLED_NAME));
        assertFalse(headers.containsKey(PARENT_SPAN_ID_NAME));
        assertFalse(headers.containsKey(SPAN_ID_NAME));
    }

    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvidedInCaseOfFault() throws MalformedURLException {
        final Tracing brave = Tracing.newBuilder()
            .localServiceName("book-store")
            .spanReporter(new TestSpanReporter())
            .build();

        final BookStoreService service = createJaxWsService(new Configurator() {
            @Override
            public void configure(final JaxWsProxyFactoryBean factory) {
                factory.getFeatures().add(new BraveClientFeature(brave));
                factory.getOutInterceptors().add(new LoggingOutInterceptor());
                factory.getInInterceptors().add(new LoggingInInterceptor());
            }
        });

        try {
            service.removeBooks();
            fail("Expected SOAPFaultException to be raised");
        } catch (final SOAPFaultException ex) {
            /* expected exception */
        }

        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), equalTo("post /bookstore"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(),
            equalTo("post http://localhost:" + PORT + "/bookstore"));

        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThatTraceHeadersArePresent(response, false);
    }

    private BookStoreService createJaxWsService() throws MalformedURLException {
        return createJaxWsService(new HashMap<String, List<String>>());
    }

    private BookStoreService createJaxWsService(final Map<String, List<String>> headers) throws MalformedURLException {
        return createJaxWsService(headers, null);
    }

    private BookStoreService createJaxWsService(final Configurator configurator) throws MalformedURLException {
        return createJaxWsService(new HashMap<String, List<String>>(), configurator);
    }

    private BookStoreService createJaxWsService(final Map<String, List<String>> headers,
            final Configurator configurator) throws MalformedURLException {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.setServiceClass(BookStoreService.class);
        factory.setAddress("http://localhost:" + PORT + "/BookStore");

        if (configurator != null) {
            configurator.configure(factory);
        }

        final BookStoreService service = (BookStoreService) factory.create();
        final Client proxy = ClientProxy.getClient(service);
        proxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);

        return service;
    }

    private Map<String, List<String>> getResponseHeaders(final BookStoreService service) {
        final Client proxy = ClientProxy.getClient(service);
        return CastUtils.cast((Map<?, ?>)proxy.getResponseContext().get(Message.PROTOCOL_HEADERS));
    }

    private void assertThatTraceIsPresent(Map<String, List<String>> headers, SpanId spanId) {
        assertThat(headers.get(SPAN_ID_NAME),
            hasItem(Long.toString(spanId.spanId())));
        assertThat(headers.get(TRACE_ID_NAME),
            hasItem(Long.toString(spanId.traceId())));
        assertThat(headers.get(SAMPLED_NAME),
            hasItem(Boolean.toString(spanId.sampled())));
        assertThat(headers.get(PARENT_SPAN_ID_NAME),
            hasItem(Long.toString(spanId.parentId())));
    }

    private void assertThatTraceHeadersArePresent(Map<String, List<String>> headers, boolean expectParent) {
        assertTrue(headers.containsKey(SPAN_ID_NAME));
        assertTrue(headers.containsKey(TRACE_ID_NAME));
        assertTrue(headers.containsKey(SAMPLED_NAME));

        if (expectParent) {
            assertTrue(headers.containsKey(PARENT_SPAN_ID_NAME));
        } else {
            assertFalse(headers.containsKey(PARENT_SPAN_ID_NAME));
        }
    }
}
