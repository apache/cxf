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
package org.apache.cxf.tracing.brave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import brave.Tracing;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class BraveTraceTest {

    private static final String ADDRESS = "http://localhost:8182";
    private Server server;
    private BraveFeature logging;
    private BraveClientFeature clientLogging;
    private Localreporter localReporter;

    @Before
    public void startServer() {
        localReporter = new Localreporter();
        logging = createLoggingFeature(localReporter);
        clientLogging = createClientLoggingFeature(localReporter);
        server = createServer(logging);
    }

    @Test
    public void testMyService() {
        MyService myService = createProxy(clientLogging);
        myService.echo("test");
        for (MutableSpan span : localReporter.spans) {
            System.out.println(span);
        }
        Assert.assertEquals(2, localReporter.spans.size());

    }

    @After
    public void stopServer() {
        server.destroy();
    }

    private static Server createServer(Feature logging) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setAddress(ADDRESS);
        factory.setServiceBean(new MyServiceImpl());
        factory.setFeatures(Arrays.asList(logging));
        return factory.create();
    }

    private static MyService createProxy(Feature trace) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(MyService.class);
        factory.setAddress(ADDRESS);
        factory.setFeatures(Arrays.asList(trace));
        return (MyService)factory.create();
    }

    private static BraveFeature createLoggingFeature(SpanHandler handler) {
        Tracing brave =
            Tracing.newBuilder().localServiceName("myservice").addSpanHandler(handler).build();
        return new BraveFeature(brave);
    }

    private static BraveClientFeature createClientLoggingFeature(SpanHandler handler) {
        Tracing brave =
            Tracing.newBuilder().localServiceName("myservice").addSpanHandler(handler).build();
        return new BraveClientFeature(brave);
    }

    static final class Localreporter extends SpanHandler {
        List<MutableSpan> spans = new ArrayList<>();

        @Override
        public boolean end(TraceContext context, MutableSpan span, Cause cause) {
            return spans.add(span);
        }
    }

}
