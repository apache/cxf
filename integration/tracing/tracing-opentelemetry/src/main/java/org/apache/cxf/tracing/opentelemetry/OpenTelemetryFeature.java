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
package org.apache.cxf.tracing.opentelemetry;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@NoJSR250Annotations
@Provider(value = Type.Feature, scope = Scope.Server)
public class OpenTelemetryFeature extends DelegatingFeature<OpenTelemetryFeature.Portable> {
    public static final String DEFAULT_INSTRUMENTATION_NAME = "org.apache.cxf";

    public OpenTelemetryFeature() {
        super(new Portable());
    }

    public OpenTelemetryFeature(final String instrumentationName) { 
        super(new Portable(instrumentationName)); 
    }

    public OpenTelemetryFeature(final OpenTelemetry openTelemetry) {
        super(new Portable(openTelemetry));
    }

    public OpenTelemetryFeature(final OpenTelemetry openTelemetry, final String instrumentationName) {
        super(new Portable(openTelemetry, instrumentationName));
    }

    public OpenTelemetryFeature(final OpenTelemetry openTelemetry, final Tracer tracer) {
        super(new Portable(openTelemetry, tracer));
    }

    public static class Portable implements AbstractPortableFeature {
        private final OpenTelemetryStartInterceptor in;
        private final OpenTelemetryStopInterceptor out;

        public Portable() {
            this(GlobalOpenTelemetry.get());
        }

        public Portable(final String instrumentationName) {
            this(GlobalOpenTelemetry.get(), instrumentationName);
        }

        public Portable(final OpenTelemetry openTelemetry) {
            this(openTelemetry, DEFAULT_INSTRUMENTATION_NAME);
        }

        public Portable(final OpenTelemetry openTelemetry, final String instrumentationName) {
            in = new OpenTelemetryStartInterceptor(openTelemetry, instrumentationName);
            out = new OpenTelemetryStopInterceptor(openTelemetry, instrumentationName);
        }

        public Portable(final OpenTelemetry openTelemetry, final Tracer tracer) {
            in = new OpenTelemetryStartInterceptor(openTelemetry, tracer);
            out = new OpenTelemetryStopInterceptor(openTelemetry, tracer);
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(in);
            provider.getInFaultInterceptors().add(in);

            provider.getOutInterceptors().add(out);
            provider.getOutFaultInterceptors().add(out);
        }
    }
}
