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
package org.apache.cxf.systest.jaxrs.tracing.brave;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.function.Function;

import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.tracing.micrometer.DefaultMessageOutObservationConvention;
import org.apache.cxf.tracing.micrometer.MessageOutContext;
import org.apache.cxf.tracing.micrometer.ObservationClientFeature;
import org.apache.cxf.tracing.micrometer.jaxrs.ContainerRequestReceiverContext;
import org.apache.cxf.tracing.micrometer.jaxrs.ContainerRequestSenderObservationContext;
import org.apache.cxf.tracing.micrometer.jaxrs.DefaultContainerRequestReceiverObservationConvention;
import org.apache.cxf.tracing.micrometer.jaxrs.DefaultContainerRequestSenderObservationConvention;
import org.apache.cxf.tracing.micrometer.jaxrs.ObservationClientProvider;
import org.apache.cxf.tracing.micrometer.jaxrs.ObservationFeature;
import org.junit.BeforeClass;

import brave.Tracing;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;

public class BraveMicrometerTracingTest extends AbstractBraveTracingTest {

    MicrometerObservationInstrumentation instrumentation = new MicrometerObservationInstrumentation();

    static MicrometerObservationInstrumentation serverInstrumentation = new MicrometerObservationInstrumentation();

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly",
                   launchServer(BraveMicrometerServer.class, true));
    }

    public static class BraveMicrometerServer extends BraveServer {

        @Override
        Object getProvider(Tracing tracing) {
            return serverInstrumentation.apply(tracing).feature;
        }
    }

    @Override
    protected Object getClientProvider(Tracing brave) {
        return instrumentation.apply(brave).clientProvider;
    }

    @Override
    protected Feature getClientFeature(Tracing brave) {
        return instrumentation.apply(brave).clientFeature;
    }

    static class MicrometerObservationInstrumentation implements Function<Tracing, InputData> {

        boolean initialized;

        ObservationRegistry observationRegistry = ObservationRegistry.create();

        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        @Override
        public InputData apply(Tracing tracing) {
            if (!initialized) {
                CurrentTraceContext bridgeContext = new BraveCurrentTraceContext(tracing.currentTraceContext());
                Tracer tracer = new BraveTracer(tracing.tracer(), bridgeContext, new BraveBaggageManager());
                BravePropagator propagator = new BravePropagator(tracing);


                observationRegistry.observationConfig().observationHandler(
                        new DefaultMeterObservationHandler(meterRegistry));
                observationRegistry.observationConfig().observationHandler(
                        new FirstMatchingCompositeObservationHandler(
                                new PropagatingSenderTracingObservationHandler<>(tracer, propagator),
                                new PropagatingReceiverTracingObservationHandler<>(tracer, propagator),
                                new DefaultTracingObservationHandler(tracer) {
                                    // To align with Brave's defaults
                                    @Override
                                    public String getSpanName(Context context) {
                                        String name = context.getName();
                                        if (StringUtils.isNotBlank(context.getContextualName())) {
                                            name = context.getContextualName();
                                        }
                                        return name;
                                    }
                                }));
                initialized = true;
            }

            // To align with Brave's defaults
            ObservationFeature feature = new ObservationFeature(observationRegistry, new DefaultContainerRequestReceiverObservationConvention() {
                @Override
                public String getContextualName(ContainerRequestReceiverContext context) {
                    return context.getRequestContext().getMethod() + " /" + context.getRequestContext().getUriInfo().getPath();
                }

                @Override
                public KeyValues getLowCardinalityKeyValues(ContainerRequestReceiverContext context) {
                    KeyValues keyValues = super.getLowCardinalityKeyValues(context);
                    if (context.getResponse() != null) {
                        return keyValues.and(KeyValue.of("http.status_code", String.valueOf(context.getResponse().getStatus())));
                    }
                    return keyValues;
                }
            });
            ObservationClientFeature clientFeature = new ObservationClientFeature(observationRegistry, new DefaultMessageOutObservationConvention() {
                // To align with Brave's defaults
                @Override
                public String getContextualName(MessageOutContext context) {
                    return super.getContextualName(context) + " " + context.getUri().toString();
                }
            });
            ObservationClientProvider clientProvider = new ObservationClientProvider(observationRegistry, new DefaultContainerRequestSenderObservationConvention() {
                @Override
                public String getContextualName(ContainerRequestSenderObservationContext context) {
                    // To align with Brave's defaults
                    return context.getRequestContext().getMethod() + " " + context.getRequestContext().getUri().toString();
                }
            });
            return new InputData(feature, clientFeature, clientProvider);
        }
    }

    static class InputData {
        jakarta.ws.rs.core.Feature feature;
        Feature clientFeature;

        Object clientProvider;

        InputData(jakarta.ws.rs.core.Feature feature, Feature clientFeature, Object clientProvider) {
            this.feature = feature;
            this.clientFeature = clientFeature;
            this.clientProvider = clientProvider;
        }
    }
}
