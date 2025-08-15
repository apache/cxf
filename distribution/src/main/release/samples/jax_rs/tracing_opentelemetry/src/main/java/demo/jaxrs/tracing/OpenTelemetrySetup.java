/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package demo.jaxrs.tracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentracing.util.GlobalTracer;

public class OpenTelemetrySetup {

    public static OpenTelemetrySdk setup(String serviceName) {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName)));

        final SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(LoggingSpanExporter.create()).build())
            .setResource(resource).build();

        final OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();

        // injecting OTel OpenTracing SHIM for feign opentracing to work
        GlobalTracer.registerIfAbsent(OpenTracingShim.createTracerShim(openTelemetrySdk));

        return openTelemetrySdk;
    }
}
