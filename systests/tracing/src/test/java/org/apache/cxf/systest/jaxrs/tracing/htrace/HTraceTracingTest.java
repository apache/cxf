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
package org.apache.cxf.systest.jaxrs.tracing.htrace;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.TestSpanReceiver;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.TracerHeaders;
import org.apache.cxf.tracing.htrace.jaxrs.HTraceClientProvider;
import org.apache.cxf.tracing.htrace.jaxrs.HTraceFeature;
import org.apache.htrace.HTraceConfiguration;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceScope;
import org.apache.htrace.impl.AlwaysSampler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class HTraceTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(HTraceTracingTest.class);
    
    private HTraceClientProvider htraceClientProvider;

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Map<String, String> properties = new HashMap<String, String>();
            properties.put("span.receiver", TestSpanReceiver.class.getName());
            properties.put("sampler", AlwaysSampler.class.getName());
            
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setFeatures(Arrays.asList(new HTraceFeature(HTraceConfiguration.fromMap(properties))));
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
        TestSpanReceiver.clear();
        
        htraceClientProvider = new HTraceClientProvider(
            new AlwaysSampler(HTraceConfiguration.EMPTY));
    }
    
    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("GET bookstore/books"));
        
        assertFalse(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_TRACE_ID));
        assertFalse(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_SPAN_ID));
    }
    
    @Test
    public void testThatNewInnerSpanIsCreated() {
        final Response r = createWebClient("/bookstore/books")
            .header(TracerHeaders.DEFAULT_HEADER_TRACE_ID, 10L)
            .header(TracerHeaders.DEFAULT_HEADER_SPAN_ID, 20L)
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("GET bookstore/books"));
        
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_TRACE_ID), equalTo("10"));
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_SPAN_ID), equalTo("20"));
    }
    
    @Test
    public void testThatCurrentSpanIsAnnotatedWithKeyValue() {
        final Response r = createWebClient("/bookstore/book/1")
            .header(TracerHeaders.DEFAULT_HEADER_TRACE_ID, 10L)
            .header(TracerHeaders.DEFAULT_HEADER_SPAN_ID, 20L)
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("GET bookstore/book/1"));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getKVAnnotations().size(), equalTo(1));
        
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_TRACE_ID), equalTo("10"));
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_SPAN_ID), equalTo("20"));
    }
    
    @Test
    public void testThatParallelSpanIsAnnotatedWithTimeline() {
        final Response r = createWebClient("/bookstore/process")
            .header(TracerHeaders.DEFAULT_HEADER_TRACE_ID, 10L)
            .header(TracerHeaders.DEFAULT_HEADER_SPAN_ID, 20L)
            .put("");
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("PUT bookstore/process"));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getTimelineAnnotations().size(), equalTo(0));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("Processing books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getTimelineAnnotations().size(), equalTo(1));
        
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_TRACE_ID), equalTo("10"));
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_SPAN_ID), equalTo("20"));
    }
    
    @Test
    public void testThatNewChildSpanIsCreatedWhenParentIsProvided() {
        final Response r = createWebClient("/bookstore/books", htraceClientProvider).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getParents().length, equalTo(1));
        
        assertTrue(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_TRACE_ID));
        assertTrue(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_SPAN_ID));
    }
    
    @Test
    public void testThatNewInnerSpanIsCreatedUsingAsyncInvocation() {
        final Response r = createWebClient("/bookstore/books/async")
            .header(TracerHeaders.DEFAULT_HEADER_TRACE_ID, 10L)
            .header(TracerHeaders.DEFAULT_HEADER_SPAN_ID, 20L)
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("GET bookstore/books/async"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("Processing books"));
        
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_TRACE_ID), equalTo("10"));
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_SPAN_ID), equalTo("20"));
    }
    
    @Test
    public void testThatOuterSpanIsCreatedUsingAsyncInvocation() {
        final Response r = createWebClient("/bookstore/books/async/notrace")
            .header(TracerHeaders.DEFAULT_HEADER_TRACE_ID, 10L)
            .header(TracerHeaders.DEFAULT_HEADER_SPAN_ID, 20L)
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), 
            equalTo("GET bookstore/books/async/notrace"));
        
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_TRACE_ID), equalTo("10"));
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.DEFAULT_HEADER_SPAN_ID), equalTo("20"));
    }
    
    @Test
    public void testThatNewSpanIsCreatedUsingAsyncInvocation() {
        final Response r = createWebClient("/bookstore/books/async").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("GET bookstore/books/async"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("Processing books"));
    }
    
    @Test
    public void testThatNewSpanIsCreatedWhenNotProvidedUsingAsyncClient() throws Exception {
        final WebClient client = createWebClient("/bookstore/books", htraceClientProvider);
        final Future<Response> f = client.async().get();
        
        final Response r = f.get(1, TimeUnit.SECONDS);
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("GET bookstore/books"));
        assertThat(TestSpanReceiver.getAllSpans().get(2).getDescription(), equalTo("GET " + client.getCurrentURI()));
        
        assertTrue(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_TRACE_ID));
        assertTrue(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_SPAN_ID));
    }
    
    @Test
    public void testThatProvidedSpanIsNotClosedWhenActive() throws MalformedURLException {
        final Map<String, String> properties = new HashMap<String, String>();
        final HTraceConfiguration conf = HTraceConfiguration.fromMap(properties);        
        final AlwaysSampler sampler = new AlwaysSampler(conf);
        
        try (final TraceScope scope = Trace.startSpan("test span", sampler)) {
            final Response r = createWebClient("/bookstore/books", htraceClientProvider).get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            
            assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
            assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
            assertThat(TestSpanReceiver.getAllSpans().get(0).getParents().length, equalTo(1));
            assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("GET bookstore/books"));
            
            assertTrue(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_TRACE_ID));
            assertTrue(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_SPAN_ID));
        }
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(3));
        assertThat(TestSpanReceiver.getAllSpans().get(2).getDescription(), equalTo("test span"));
    }
    
    @Test
    public void testThatProvidedSpanIsNotDetachedWhenActiveUsingAsyncClient() throws Exception {
        final Map<String, String> properties = new HashMap<String, String>();
        final HTraceConfiguration conf = HTraceConfiguration.fromMap(properties);        
        final AlwaysSampler sampler = new AlwaysSampler(conf);

        final WebClient client = createWebClient("/bookstore/books", htraceClientProvider);
        try (final TraceScope scope = Trace.startSpan("test span", sampler)) {
            final Future<Response> f = client.async().get();
        
            final Response r = f.get(1, TimeUnit.SECONDS);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertThat(scope.isDetached(), equalTo(false));
            
            assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
            assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
            assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("GET bookstore/books"));
            
            assertTrue(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_TRACE_ID));
            assertTrue(r.getHeaders().containsKey(TracerHeaders.DEFAULT_HEADER_SPAN_ID));
        }
        
        assertThat(TestSpanReceiver.getAllSpans().get(2).getDescription(), equalTo("test span"));
    }
    
    protected WebClient createWebClient(final String url, final Object ... providers) {
        return WebClient
            .create("http://localhost:" + PORT + url, Arrays.asList(providers))
            .accept(MediaType.APPLICATION_JSON);
    }
}
