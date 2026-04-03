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
package org.apache.cxf.systest.jaxrs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProperties;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProvider;
import org.apache.cxf.metrics.micrometer.provider.DefaultExceptionClassProvider;
import org.apache.cxf.metrics.micrometer.provider.DefaultTimedAnnotationProvider;
import org.apache.cxf.metrics.micrometer.provider.StandardTags;
import org.apache.cxf.metrics.micrometer.provider.StandardTagsProvider;
import org.apache.cxf.metrics.micrometer.provider.jaxrs.JaxrsOperationTagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.jaxrs.JaxrsTags;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for CXF-9206: cxf.server.requests metric must not be
 * counted twice for JAX-RS server endpoints when MetricsFeature is used.
 *
 * Root cause: the CXF-9168 fix (de39a25) made createEndpointContext() eagerly
 * return a MicrometerServerMetricsContext for all server-side endpoints so that
 * requests failing before a BindingOperationInfo is available are still counted.
 * For JAX-RS, createResourceContext() already returns its own MetricsContext per
 * request, so ExchangeMetrics ended up incrementing cxf.server.requests twice.
 *
 * The fix skips creating a server context in createEndpointContext() when the
 * endpoint uses the JAX-RS binding (JAXRSBindingFactory.JAXRS_BINDING_ID).
 */
public class JaxRsMetricsClientServerTest extends AbstractBusClientServerTestBase {

    private static final MeterRegistry METER_REGISTRY = new SimpleMeterRegistry();
    private static final String PORT = allocatePort(JaxRsMetricsClientServerTest.class);


    @Path("/hello")
    public interface HelloService {
        @GET
        @Path("/{name}")
        @Produces(MediaType.TEXT_PLAIN)
        String sayHello(@PathParam("name") String name);
    }

    public static class HelloServiceImpl implements HelloService {
        @Override
        public String sayHello(String name) {
            return "Hello " + name;
        }
    }


    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            var jaxrsTags = new JaxrsTags();
            var operationsCustomizer = new JaxrsOperationTagsCustomizer(jaxrsTags);
            var tagsProvider = new StandardTagsProvider(
                new DefaultExceptionClassProvider(), new StandardTags());
            var properties = new MicrometerMetricsProperties();

            var provider = new MicrometerMetricsProvider(
                METER_REGISTRY,
                tagsProvider,
                List.of(operationsCustomizer),
                new DefaultTimedAnnotationProvider(),
                properties
            );

            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(HelloServiceImpl.class);
            sf.setResourceProvider(HelloServiceImpl.class,
                new SingletonResourceProvider(new HelloServiceImpl()));
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.setFeatures(Arrays.asList(new MetricsFeature(provider)));
            sf.create();
        }
    }


    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus();
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }


    private HelloService createClient() {
        JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();
        factory.setServiceClass(HelloService.class);
        factory.setAddress("http://localhost:" + PORT + "/");
        factory.setHeaders(Collections.singletonMap("Accept", "text/plain"));
        return (HelloService) factory.create();
    }


    /**
     * CXF-9206: a single successful JAX-RS request must produce exactly one
     * sample in cxf.server.requests (count == 1), not two.
     */
    @Test
    public void testSuccessfulRequestIsCountedOnce() {
        METER_REGISTRY.clear();

        String response = createClient().sayHello("world");
        assertEquals("Hello world", response);

        Timer timer = METER_REGISTRY.find("cxf.server.requests").timer();
        assertNotNull("Expected cxf.server.requests timer to be registered", timer);
        assertEquals(
            "cxf.server.requests count should be 1 for a single request",
            1,
            timer.count()
        );
    }

    /**
     * Two successive requests must accumulate to count == 2, not 4.
     */
    @Test
    public void testTwoRequestsAreCountedTwice() {
        METER_REGISTRY.clear();

        HelloService client = createClient();
        client.sayHello("alice");
        client.sayHello("bob");

        Timer timer = METER_REGISTRY.find("cxf.server.requests").timer();
        assertNotNull("Expected cxf.server.requests timer to be registered", timer);
        assertEquals(
            "cxf.server.requests count should be 2 for two requests",
            2,
            timer.count()
        );
    }
}
