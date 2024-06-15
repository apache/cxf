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
package org.apache.cxf.systest.jaxws.tracing.micrometer;

import brave.Tracing;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.brave.TestSpanHandler;
import org.apache.cxf.systest.brave.jaxws.AbstractBraveTracingTest;
import org.apache.cxf.systest.jaxws.tracing.brave.BookStore;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.tracing.micrometer.DefaultMessageInObservationConvention;
import org.apache.cxf.tracing.micrometer.DefaultMessageOutObservationConvention;
import org.apache.cxf.tracing.micrometer.MessageInContext;
import org.apache.cxf.tracing.micrometer.MessageOutContext;
import org.apache.cxf.tracing.micrometer.ObservationClientFeature;
import org.apache.cxf.tracing.micrometer.ObservationFeature;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

import org.junit.After;
import org.junit.BeforeClass;

import static org.apache.cxf.systest.micrometer.ObservationRegistrySupport.createObservationRegistry;
import static org.junit.Assert.assertTrue;

public class MicrometerTracingTest extends AbstractBraveTracingTest {
    public static final String PORT = allocatePort(MicrometerTracingTest.class);

    private static MeterRegistry meterRegistry;

    public static class Server extends AbstractTestServerBase {
        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final Tracing brave = Tracing.newBuilder()
                .localServiceName("book-store")
                .addSpanHandler(new TestSpanHandler())
                .build();

            final ObservationRegistry observationRegistry = createObservationRegistry(meterRegistry, brave);
            final JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
            sf.setServiceClass(BookStore.class);
            sf.setAddress("http://localhost:" + PORT);
            sf.getFeatures().add(new ObservationFeature(observationRegistry,
                new DefaultMessageInObservationConvention() {
                    @Override
                    public String getContextualName(MessageInContext context) {
                        return context.getMessage().get(Message.HTTP_REQUEST_METHOD) + " " 
                                + context.getUri().getPath();
                    }
                    
                    @Override
                    public KeyValues getLowCardinalityKeyValues(MessageInContext context) {
                        KeyValues keyValues = super.getLowCardinalityKeyValues(context);
                        if (context.getResponse() != null) {
                            return keyValues.and(KeyValue.of("http.status_code",
                                String.valueOf(context.getResponse().get(Message.RESPONSE_CODE))));
                        }
                        return keyValues;
                    }
                }));
            server = sf.create();
        }

        @Override
        public void tearDown() throws Exception {
            server.destroy();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        meterRegistry = new SimpleMeterRegistry();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @After
    public void tearDown() {
        TestSpanHandler.clear();
    }
    
    @Override
    protected int getPort() {
        return Integer.parseInt(PORT);
    }
    
    @Override
    protected Feature getClientFeature(Tracing tracing) {
        return  new ObservationClientFeature(createObservationRegistry(meterRegistry, tracing),
            new DefaultMessageOutObservationConvention() {
                // To align with Brave's defaults
                @Override
                public String getContextualName(MessageOutContext context) {
                    return context.getMessage().get(Message.HTTP_REQUEST_METHOD)  + " "
                            + context.getUri().toString();
                }
            }
        );
    }
}
