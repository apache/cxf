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

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.tracing.opentelemetry.AbstractOpenTelemetryClientProvider;
import org.apache.cxf.tracing.opentelemetry.TraceScope;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@Provider
public class OpenTelemetryClientProvider extends AbstractOpenTelemetryClientProvider
    implements ClientRequestFilter, ClientResponseFilter {

    public OpenTelemetryClientProvider(final OpenTelemetry openTelemetry, final String instrumentationName) {
        super(openTelemetry, instrumentationName);
    }

    public OpenTelemetryClientProvider(final OpenTelemetry openTelemetry, final Tracer tracer) {
        super(openTelemetry, tracer);
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        final TraceScopeHolder<TraceScope> holder = super.startTraceSpan(requestContext.getStringHeaders(),
                                                                         requestContext.getUri(),
                                                                         requestContext.getMethod());

        if (holder != null) {
            requestContext.setProperty(TRACE_SPAN, holder);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext)
        throws IOException {
        final TraceScopeHolder<TraceScope> holder = (TraceScopeHolder<TraceScope>)requestContext
            .getProperty(TRACE_SPAN);
        super.stopTraceSpan(holder, responseContext.getStatus());
    }
}
