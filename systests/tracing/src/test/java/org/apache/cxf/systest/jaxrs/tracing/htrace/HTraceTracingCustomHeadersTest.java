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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.TracerHeaders;
import org.apache.cxf.tracing.htrace.jaxrs.HTraceClientProvider;
import org.apache.cxf.tracing.htrace.jaxrs.HTraceFeature;
import org.apache.htrace.core.AlwaysSampler;
import org.apache.htrace.core.HTraceConfiguration;
import org.apache.htrace.core.SpanId;
import org.apache.htrace.core.StandardOutSpanReceiver;
import org.apache.htrace.core.Tracer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class HTraceTracingCustomHeadersTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(HTraceTracingCustomHeadersTest.class);
    
    private static final String CUSTOM_HEADER_SPAN_ID = "My-Span-Id";
    
    private Tracer tracer;
    private HTraceClientProvider htraceClientProvider;
    
    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Map<String, String> properties = new HashMap<String, String>();
            properties.put(Tracer.SPAN_RECEIVER_CLASSES_KEY, StandardOutSpanReceiver.class.getName());
            properties.put(Tracer.SAMPLER_CLASSES_KEY, AlwaysSampler.class.getName());
            
            final Map<String, Object> headers = new HashMap<String, Object>();
            headers.put(TracerHeaders.HEADER_SPAN_ID, CUSTOM_HEADER_SPAN_ID);
            
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setFeatures(Arrays.asList(new HTraceFeature(HTraceConfiguration.fromMap(properties), "test-tracer")));
            sf.setProperties(headers);
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
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put(Tracer.SPAN_RECEIVER_CLASSES_KEY, StandardOutSpanReceiver.class.getName());
        properties.put(Tracer.SAMPLER_CLASSES_KEY, AlwaysSampler.class.getName());
        
        tracer = new Tracer.Builder("tracer")
            .conf(HTraceConfiguration.fromMap(properties))
            .build();

        htraceClientProvider = new HTraceClientProvider(tracer);
    }
    
    @Test
    public void testThatNewSpanIsCreated() {
        final SpanId spanId = SpanId.fromRandom();
        
        final Response r = createWebClient("/bookstore/books")
            .header(CUSTOM_HEADER_SPAN_ID, spanId.toString())
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat((String)r.getHeaders().getFirst(CUSTOM_HEADER_SPAN_ID), equalTo(spanId.toString()));
    }
    
    @Test
    public void testThatNewChildSpanIsCreated() {
        final Response r = createWebClient("/bookstore/books", htraceClientProvider).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat((String)r.getHeaders().getFirst(CUSTOM_HEADER_SPAN_ID), notNullValue());
    }

    protected WebClient createWebClient(final String url, final Object ... providers) {
        final WebClient client = WebClient
            .create("http://localhost:" + PORT + url, Arrays.asList(providers))
            .accept(MediaType.APPLICATION_JSON);

        if (providers.length > 0) {
            final ClientConfiguration config = WebClient.getConfig(client);
            config.getRequestContext().put(TracerHeaders.HEADER_SPAN_ID, CUSTOM_HEADER_SPAN_ID);
        }

        return client;
    }
}
