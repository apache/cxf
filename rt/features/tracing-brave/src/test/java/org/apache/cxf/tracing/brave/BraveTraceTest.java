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

import com.github.kristofa.brave.Brave;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import zipkin.Span;
import zipkin.reporter.Reporter;

public class BraveTraceTest {
    
    private static final String ADDRESS = "http://localhost:8182";
    private Server server;
    private TraceFeature logging;
    private Localreporter localReporter;

    @Before
    public void startServer() {
        localReporter = new Localreporter();
        logging = createLoggingFeature(localReporter);
        server = createServer(logging);
    }

    @Test
    public void testMyService() {
        MyService myService = createProxy(logging);
        myService.echo("test");
        for (Span span : localReporter.spans) {
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
        factory.setFeatures(Arrays.asList(trace, new LoggingFeature()));
        return (MyService)factory.create();
    }

    private static TraceFeature createLoggingFeature(Reporter<Span> reporter) {
        Brave brave = new Brave.Builder("myservice").reporter(reporter).build();
        return new TraceFeature(brave);
    }
    
    static final class Localreporter implements Reporter<Span> {
        List<Span> spans = new ArrayList<Span>();

        @Override
        public void report(Span span) {
            spans.add(span);
        }
        
    }

}
