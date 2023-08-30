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
package org.apache.cxf.tracing.opentelemetry.jaxrs;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@Provider
public class OpenTelemetryFeature implements Feature {
    public static final String DEFAULT_INSTRUMENTATION_NAME = "org.apache.cxf";

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public OpenTelemetryFeature() {
        this(GlobalOpenTelemetry.get());
    }

    public OpenTelemetryFeature(String instrumentationName) {
        this(GlobalOpenTelemetry.get(), instrumentationName);
    }

    public OpenTelemetryFeature(final OpenTelemetry openTelemetry) {
        this(openTelemetry, DEFAULT_INSTRUMENTATION_NAME);
    }

    public OpenTelemetryFeature(final OpenTelemetry openTelemetry, final String instrumentationName) {
        this(openTelemetry, openTelemetry.getTracer(instrumentationName));
    }

    public OpenTelemetryFeature(final OpenTelemetry openTelemetry, final Tracer tracer) {
        this.openTelemetry = openTelemetry;
        this.tracer = tracer;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new OpenTelemetryProvider(openTelemetry, tracer));
        context.register(new OpenTelemetryContextProvider(tracer));
        return true;
    }
}
