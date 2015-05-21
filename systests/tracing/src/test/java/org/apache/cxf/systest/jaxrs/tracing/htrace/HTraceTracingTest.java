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
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.tracing.htrace.HTraceFeature;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.TracerHeaders;
import org.apache.htrace.HTraceConfiguration;
import org.apache.htrace.impl.AlwaysSampler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class HTraceTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(HTraceTracingTest.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Map<String, String> properties = new HashMap<String, String>();
            properties.put("span.receiver", TestSpanReceiver.class.getName());
            properties.put("sampler", AlwaysSampler.class.getName());
            
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
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
    }
    
    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() {
        final Response r = createWebClient("/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(1));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        
        assertFalse(r.getHeaders().containsKey(TracerHeaders.HEADER_TRACE_ID));
        assertFalse(r.getHeaders().containsKey(TracerHeaders.HEADER_SPAN_ID));
    }
    
    @Test
    public void testThatNewInnerSpanIsCreated() {
        final Response r = createWebClient("/bookstore/books")
            .header(TracerHeaders.HEADER_TRACE_ID, 10L)
            .header(TracerHeaders.HEADER_SPAN_ID, 20L)
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("bookstore/books"));
        
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.HEADER_TRACE_ID), equalTo("10"));
        assertThat((String)r.getHeaders().getFirst(TracerHeaders.HEADER_SPAN_ID), equalTo("20"));
    }
    
    protected WebClient createWebClient(final String url) {
        return WebClient
            .create("http://localhost:" + PORT + url)
            .accept(MediaType.APPLICATION_JSON);
    }
}
