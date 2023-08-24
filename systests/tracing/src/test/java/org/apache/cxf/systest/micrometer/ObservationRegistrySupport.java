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
package org.apache.cxf.systest.micrometer;

import brave.Tracing;

import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
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

public final class ObservationRegistrySupport {
    private ObservationRegistrySupport() {
    }

    public static ObservationRegistry createObservationRegistry(final MeterRegistry meterRegistry,
            final Tracing brave) {

        final CurrentTraceContext bridgeContext = new BraveCurrentTraceContext(brave.currentTraceContext());
        final Tracer tracer = new BraveTracer(brave.tracer(), bridgeContext, new BraveBaggageManager());
        final BravePropagator propagator = new BravePropagator(brave);

        final ObservationRegistry observationRegistry = ObservationRegistry.create();

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

        return observationRegistry;
    }
}
