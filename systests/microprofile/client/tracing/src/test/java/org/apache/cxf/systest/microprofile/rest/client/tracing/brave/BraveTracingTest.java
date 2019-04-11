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
package org.apache.cxf.systest.microprofile.rest.client.tracing.brave;

import java.net.URI;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import brave.ScopedSpan;
import brave.Tracing;
import brave.sampler.Sampler;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.microprofile.rest.client.tracing.BookRestClient;
import org.apache.cxf.systest.microprofile.rest.client.tracing.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.tracing.brave.TraceScope;
import org.apache.cxf.tracing.brave.jaxrs.BraveClientProvider;
import org.apache.cxf.tracing.brave.jaxrs.BraveFeature;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;

public class BraveTracingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(BraveTracingTest.class);

    private Tracing brave;
    private BraveClientProvider braveClientProvider;

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final Tracing brave = Tracing
                    .newBuilder()
                    .spanReporter(new TestSpanReporter())
                    .sampler(Sampler.ALWAYS_SAMPLE)
                    .build();

            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore<TraceScope>()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setProvider(new BraveFeature(brave));
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
        TestSpanReporter.clear();

        brave = Tracing
                .newBuilder()
                .spanReporter(new TestSpanReporter())
                .sampler(Sampler.ALWAYS_SAMPLE)
                .build();

        braveClientProvider = new BraveClientProvider(brave);
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvided() {
        final BookRestClient client = createRestClient();
        assertThat(client.getBooks(), hasSize(2));

        assertThat(TestSpanReporter.getAllSpans().size(), 
            equalTo(3));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), 
            equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), 
            equalTo("get /bookstore/books"));
        assertThat(TestSpanReporter.getAllSpans().get(2).name(), 
            equalTo("get http://localhost:" + PORT + "/bookstore/books"));
    }

    @Test
    public void testThatNewSpanIsCreatedWhenNotProvidedWithAsyncConduit() {
        final BookRestClient client = createAsyncRestClient();
        assertThat(client.getBooks(), hasSize(2));

        assertThat(TestSpanReporter.getAllSpans().size(), 
            equalTo(3));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), 
            equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), 
            equalTo("get /bookstore/books"));
        assertThat(TestSpanReporter.getAllSpans().get(2).name(), 
            equalTo("get http://localhost:" + PORT + "/bookstore/books"));
    }

    @Test
    public void testThatNewInnerSpanIsCreated() {
        final ScopedSpan span = brave.tracer().startScopedSpan("calling book service");

        try {
            final BookRestClient client = createRestClient();
            assertThat(client.getBooks(), hasSize(2));
        } finally {
            span.finish();
        }

        assertThat(TestSpanReporter.getAllSpans().size(), 
            equalTo(4));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), 
            equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), 
            equalTo("get /bookstore/books"));
        assertThat(TestSpanReporter.getAllSpans().get(2).name(), 
            equalTo("get http://localhost:" + PORT + "/bookstore/books"));
        assertThat(TestSpanReporter.getAllSpans().get(3).name(), 
            equalTo("calling book service"));
    }
    
    @Test
    public void testThatNewInnerSpanIsCreatedWithAsyncConduit() {
        final ScopedSpan span = brave.tracer().startScopedSpan("calling book service");

        try {
            final BookRestClient client = createAsyncRestClient();
            assertThat(client.getBooks(), hasSize(2));
        } finally {
            span.finish();
        }

        assertThat(TestSpanReporter.getAllSpans().size(), 
            equalTo(4));
        assertThat(TestSpanReporter.getAllSpans().get(0).name(), 
            equalTo("get books"));
        assertThat(TestSpanReporter.getAllSpans().get(1).name(), 
            equalTo("get /bookstore/books"));
        assertThat(TestSpanReporter.getAllSpans().get(2).name(), 
            equalTo("get http://localhost:" + PORT + "/bookstore/books"));
        assertThat(TestSpanReporter.getAllSpans().get(3).name(), 
            equalTo("calling book service"));
    }
    
    private BookRestClient createAsyncRestClient() {
        return createRestClientBuilder()
            .property(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE)
            .build(BookRestClient.class);
    }
    
    private BookRestClient createRestClient() {
        return createRestClientBuilder()
            .build(BookRestClient.class);
    }
    
    private RestClientBuilder createRestClientBuilder() {
        return RestClientBuilder.newBuilder()
            .baseUri(URI.create("http://localhost:" + PORT))
            .register(JacksonJsonProvider.class)
            .register(braveClientProvider);
    }
}
