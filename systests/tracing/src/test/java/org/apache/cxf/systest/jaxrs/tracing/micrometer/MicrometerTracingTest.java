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
package org.apache.cxf.systest.jaxrs.tracing.micrometer;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import brave.Tracing;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.brave.TestSpanHandler;
import org.apache.cxf.systest.brave.jaxrs.AbstractBraveTracingTest;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.systest.jaxrs.tracing.NullPointerExceptionMapper;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.tracing.brave.TraceScope;
import org.apache.cxf.tracing.micrometer.DefaultMessageOutObservationConvention;
import org.apache.cxf.tracing.micrometer.MessageOutContext;
import org.apache.cxf.tracing.micrometer.ObservationClientFeature;
import org.apache.cxf.tracing.micrometer.jaxrs.ContainerRequestReceiverContext;
import org.apache.cxf.tracing.micrometer.jaxrs.ContainerRequestSenderObservationContext;
import org.apache.cxf.tracing.micrometer.jaxrs.DefaultContainerRequestReceiverObservationConvention;
import org.apache.cxf.tracing.micrometer.jaxrs.DefaultContainerRequestSenderObservationConvention;
import org.apache.cxf.tracing.micrometer.jaxrs.ObservationClientProvider;
import org.apache.cxf.tracing.micrometer.jaxrs.ObservationFeature;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

import org.junit.BeforeClass;

import static org.apache.cxf.systest.micrometer.ObservationRegistrySupport.createObservationRegistry; 
import static org.junit.Assert.assertTrue;

public class MicrometerTracingTest extends AbstractBraveTracingTest {
    public static final String PORT = allocatePort(MicrometerTracingTest.class);

    private static MeterRegistry meterRegistry;

    public static class MicrometerServer extends AbstractTestServerBase {
        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final Tracing brave = Tracing
                .newBuilder()
                .addSpanHandler(new TestSpanHandler())
                .build();

            final ObservationRegistry observationRegistry = createObservationRegistry(meterRegistry, brave);
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore<TraceScope>()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setProvider(new ObservationFeature(observationRegistry,
                new DefaultContainerRequestReceiverObservationConvention() {
                    @Override
                    public String getContextualName(ContainerRequestReceiverContext context) {
                        return context.getRequestContext().getMethod() + " /"
                                + context.getRequestContext().getUriInfo().getPath();
                    }
    
                    @Override
                    public KeyValues getLowCardinalityKeyValues(ContainerRequestReceiverContext context) {
                        KeyValues keyValues = super.getLowCardinalityKeyValues(context);
                        if (context.getResponse() != null) {
                            return keyValues.and(KeyValue.of("http.status_code",
                                String.valueOf(context.getResponse().getStatus())));
                        }
                        return keyValues;
                    }
                }
            ));
            sf.setProvider(new NullPointerExceptionMapper());
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

        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly",
                   launchServer(MicrometerServer.class, true));
    }

    @Override
    protected Object getClientProvider(Tracing tracing) {
        return new ObservationClientProvider(createObservationRegistry(meterRegistry, tracing),
            new DefaultContainerRequestSenderObservationConvention() {
                @Override
                public String getContextualName(ContainerRequestSenderObservationContext context) {
                    // To align with Brave's defaults
                    return context.getRequestContext().getMethod() + " "
                        + context.getRequestContext().getUri().toString();
                }
            });
    }

    @Override
    protected Feature getClientFeature(Tracing tracing) {
        return  new ObservationClientFeature(createObservationRegistry(meterRegistry, tracing),
            new DefaultMessageOutObservationConvention() {
                // To align with Brave's defaults
                @Override
                public String getContextualName(MessageOutContext context) {
                    return super.getContextualName(context) + " " + context.getUri().toString();
                }
            }
        );
    }
    
    @Override
    protected int getPort() {
        return Integer.parseInt(PORT);
    }
}
