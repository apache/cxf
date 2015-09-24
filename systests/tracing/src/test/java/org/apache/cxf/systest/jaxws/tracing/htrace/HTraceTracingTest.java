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
package org.apache.cxf.systest.jaxws.tracing.htrace;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.jaxrs.tracing.htrace.TestSpanReceiver;
import org.apache.cxf.systest.jaxws.tracing.BookStore;
import org.apache.cxf.systest.jaxws.tracing.BookStoreService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.TracerHeaders;
import org.apache.cxf.tracing.htrace.HTraceStartInterceptor;
import org.apache.cxf.tracing.htrace.HTraceStopInterceptor;
import org.apache.htrace.HTraceConfiguration;
import org.apache.htrace.Trace;
import org.apache.htrace.impl.AlwaysSampler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;

public class HTraceTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(HTraceTracingTest.class);
    
    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Map<String, String> properties = new HashMap<String, String>();
            final HTraceConfiguration conf = HTraceConfiguration.fromMap(properties);
            Trace.addReceiver(new TestSpanReceiver(conf));
            
            final JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
            sf.setServiceClass(BookStore.class);
            sf.setAddress("http://localhost:" + PORT);
            sf.getInInterceptors().add(new HTraceStartInterceptor(Phase.PRE_INVOKE, new AlwaysSampler(conf)));
            sf.getOutInterceptors().add(new HTraceStopInterceptor(Phase.PRE_MARSHAL));
            sf.create();
        }
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }
    
    @Before
    public void setUp() {
        TestSpanReceiver.clear();
    }
    
    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() throws MalformedURLException {
        final BookStoreService service = createJaxWsService();
        assertThat(service.getBooks().size(), equalTo(2));
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("POST /BookStore"));
        
        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThat(response.get(TracerHeaders.DEFAULT_HEADER_TRACE_ID), nullValue());
        assertThat(response.get(TracerHeaders.DEFAULT_HEADER_SPAN_ID), nullValue());
    }
    
    @Test
    public void testThatNewInnerSpanIsCreated() throws MalformedURLException {
        final BookStoreService service = createJaxWsService();
        final Client proxy = ClientProxy.getClient(service);
        
        final Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(TracerHeaders.DEFAULT_HEADER_TRACE_ID, Arrays.asList("10"));
        headers.put(TracerHeaders.DEFAULT_HEADER_SPAN_ID, Arrays.asList("20"));
        proxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);
        assertThat(service.getBooks().size(), equalTo(2));
        
        assertThat(TestSpanReceiver.getAllSpans().size(), equalTo(2));
        assertThat(TestSpanReceiver.getAllSpans().get(0).getDescription(), equalTo("Get Books"));
        assertThat(TestSpanReceiver.getAllSpans().get(1).getDescription(), equalTo("POST /BookStore"));
        
        final Map<String, List<String>> response = getResponseHeaders(service);
        assertThat(response.get(TracerHeaders.DEFAULT_HEADER_TRACE_ID), hasItems("10"));
        assertThat(response.get(TracerHeaders.DEFAULT_HEADER_SPAN_ID), hasItems("20"));
    }
    
    private BookStoreService createJaxWsService() throws MalformedURLException {
        return createJaxWsService(new HashMap<String, List<String>>());
    }
    
    private BookStoreService createJaxWsService(final Map<String, List<String>> headers) throws MalformedURLException {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.setServiceClass(BookStoreService.class);
        factory.setAddress("http://localhost:" + PORT + "/BookStore");
        
        final BookStoreService service = (BookStoreService) factory.create();
        final Client proxy = ClientProxy.getClient(service);
        proxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);
        
        return (BookStoreService) factory.create();
    }
    
    private Map<String, List<String>> getResponseHeaders(final BookStoreService service) {
        final Client proxy = ClientProxy.getClient(service);
        return CastUtils.cast((Map<?, ?>)proxy.getResponseContext().get(Message.PROTOCOL_HEADERS));
    }    
}
