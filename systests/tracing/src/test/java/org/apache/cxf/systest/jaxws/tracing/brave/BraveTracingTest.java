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

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.SpanId;
import com.github.kristofa.brave.http.BraveHttpHeaders;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.TestSpanReporter;
import org.apache.cxf.systest.jaxws.tracing.BookStoreService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.brave.BraveClientFeature;
import org.apache.cxf.tracing.brave.BraveFeature;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import zipkin.Constants;

public class BraveTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(BraveTracingTest.class);
    
    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Brave brave = new Brave.Builder("book-store")
                .reporter(new TestSpanReporter())
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
        assertThat(TestSpanReporter.getAllSpans().get(0).name, equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name, equalTo("post /bookstore"));
        
        final Map<String, List<String>> headers = getResponseHeaders(service);
        assertFalse(headers.containsKey(BraveHttpHeaders.TraceId.getName()));
        assertFalse(headers.containsKey(BraveHttpHeaders.Sampled.getName()));
        assertFalse(headers.containsKey(BraveHttpHeaders.ParentSpanId.getName()));
        assertFalse(headers.containsKey(BraveHttpHeaders.ParentSpanId.getName()));
    }
    
    @Test
    public void testThatNewInnerSpanIsCreated() throws MalformedURLException {
        final Random random = new Random();
        
        final SpanId spanId = SpanId
            .builder()
            .traceId(random.nextLong())
            .parentId(random.nextLong())
            .spanId(random.nextLong())
            .sampled(true)
            .build();

        final Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(BraveHttpHeaders.SpanId.getName(), Arrays.asList(Long.toString(spanId.spanId)));
        headers.put(BraveHttpHeaders.TraceId.getName(), Arrays.asList(Long.toString(spanId.traceId)));
        headers.put(BraveHttpHeaders.Sampled.getName(), Arrays.asList(Boolean.toString(spanId.sampled())));
        headers.put(BraveHttpHeaders.ParentSpanId.getName(), Arrays.asList(Long.toString(spanId.parentId)));

        final BookStoreService service = createJaxWsService(headers);
        assertThat(service.getBooks().size(), equalTo(2));
        
        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(0).name, equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name, equalTo("post /bookstore"));
        
        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThatTraceIsPresent(response, spanId);
    }
    
    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() throws MalformedURLException {
        final Brave brave = new Brave.Builder("book-store")
            .reporter(new TestSpanReporter())
            .build();
    
        final BookStoreService service = createJaxWsService(new Configurator() {
            @Override
            public void configure(final JaxWsProxyFactoryBean factory) {
                factory.getFeatures().add(new BraveClientFeature(brave));
            }
        });
        assertThat(service.getBooks().size(), equalTo(2));
        
        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReporter.getAllSpans().get(0).name, equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(0).parentId, not(nullValue()));
        assertThat(TestSpanReporter.getAllSpans().get(1).name, equalTo("post /bookstore"));
        assertThat(TestSpanReporter.getAllSpans().get(2).name, 
            equalTo("post http://localhost:" + PORT + "/bookstore"));
        
        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThatTraceHeadersArePresent(response, false);
    }
    
    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws MalformedURLException {
        final Brave brave = new Brave.Builder("book-store")
            .reporter(new TestSpanReporter())
            .build();

        final BookStoreService service = createJaxWsService(new Configurator() {
            @Override
            public void configure(final JaxWsProxyFactoryBean factory) {
                factory.getFeatures().add(new BraveClientFeature(brave));
            }
        });

        try {
            brave.localTracer().startNewSpan(Constants.LOCAL_COMPONENT, "test span");
            
            assertThat(service.getBooks().size(), equalTo(2));
            assertThat(brave.localSpanThreadBinder().getCurrentLocalSpan(), not(nullValue()));
            
            assertThat(TestSpanReporter.getAllSpans().size(), equalTo(3));
            assertThat(TestSpanReporter.getAllSpans().get(0).name, equalTo("get books"));
            assertThat(TestSpanReporter.getAllSpans().get(0).parentId, not(nullValue()));
            assertThat(TestSpanReporter.getAllSpans().get(1).name, equalTo("post /bookstore"));
            assertThat(TestSpanReporter.getAllSpans().get(2).name, 
                equalTo("post http://localhost:" + PORT + "/bookstore"));
            
            final Map<String, List<String>> response = getResponseHeaders(service);
            assertThatTraceHeadersArePresent(response, true);
        } finally {
            brave.localTracer().finishSpan();
        }
        
        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(4));
        assertThat(TestSpanReporter.getAllSpans().get(3).name, equalTo("test span"));
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
        assertThat(TestSpanReporter.getAllSpans().get(0).name, equalTo("post /bookstore"));
        
        final Map<String, List<String>> headers = getResponseHeaders(service);
        assertFalse(headers.containsKey(BraveHttpHeaders.TraceId.getName()));
        assertFalse(headers.containsKey(BraveHttpHeaders.Sampled.getName()));
        assertFalse(headers.containsKey(BraveHttpHeaders.ParentSpanId.getName()));
        assertFalse(headers.containsKey(BraveHttpHeaders.ParentSpanId.getName()));
    }
    
    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvidedInCaseOfFault() throws MalformedURLException {
        final Brave brave = new Brave.Builder("book-store")
            .reporter(new TestSpanReporter())
            .build();
    
        final BookStoreService service = createJaxWsService(new Configurator() {
            @Override
            public void configure(final JaxWsProxyFactoryBean factory) {
                factory.getFeatures().add(new BraveClientFeature(brave));
            }
        });
        
        try {
            service.removeBooks();
            fail("Expected SOAPFaultException to be raised");
        } catch (final SOAPFaultException ex) {
            /* expected exception */
        }
        
        assertThat(TestSpanReporter.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReporter.getAllSpans().get(0).name, equalTo("post /bookstore"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name, 
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
        assertThat(headers.get(BraveHttpHeaders.SpanId.getName()), 
            hasItem(Long.toString(spanId.spanId)));
        assertThat(headers.get(BraveHttpHeaders.TraceId.getName()), 
            hasItem(Long.toString(spanId.traceId)));
        assertThat(headers.get(BraveHttpHeaders.Sampled.getName()), 
            hasItem(Boolean.toString(spanId.sampled())));
        assertThat(headers.get(BraveHttpHeaders.ParentSpanId.getName()), 
            hasItem(Long.toString(spanId.parentId)));
    }

    private void assertThatTraceHeadersArePresent(Map<String, List<String>> headers, boolean expectParent) {
        assertTrue(headers.containsKey(BraveHttpHeaders.SpanId.getName()));
        assertTrue(headers.containsKey(BraveHttpHeaders.TraceId.getName()));
        assertTrue(headers.containsKey(BraveHttpHeaders.Sampled.getName()));
        
        if (expectParent) {
            assertTrue(headers.containsKey(BraveHttpHeaders.ParentSpanId.getName()));
        } else {
            assertFalse(headers.containsKey(BraveHttpHeaders.ParentSpanId.getName()));
        }
    }
}
