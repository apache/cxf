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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.tracing.AbstractTracingProvider;
import org.apache.cxf.tracing.opentelemetry.internal.TextMapInjectAdapter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;

public abstract class AbstractOpenTelemetryClientProvider extends AbstractTracingProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractOpenTelemetryClientProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.client.opentelemetry.span";

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;

    protected AbstractOpenTelemetryClientProvider(final OpenTelemetry openTelemetry, final String instrumentationName) {
        this(openTelemetry, openTelemetry.getTracer(instrumentationName));
    }

    protected AbstractOpenTelemetryClientProvider(final OpenTelemetry openTelemetry, Tracer tracer) {
        this.openTelemetry = openTelemetry;
        this.tracer = tracer;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders,
                                                          URI uri, String method) {
        Context parentContext = Context.current();
        Span activeSpan = tracer.spanBuilder(buildSpanDescription(uri.toString(), method))
                .setParent(parentContext).setSpanKind(SpanKind.CLIENT)
                .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, method)
                .setAttribute(ServerAttributes.SERVER_ADDRESS, uri.getHost())
                .setAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(uri.getPort()))
                .setAttribute(UrlAttributes.URL_FULL, uri.toString())
                .setAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, uri.getHost())
                .setAttribute(NetworkAttributes.NETWORK_PEER_PORT, Long.valueOf(uri.getPort()))
                .startSpan();
        Scope scope = activeSpan.makeCurrent();

        openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), requestHeaders,
                                                                           TextMapInjectAdapter.get());

        // In case of asynchronous client invocation, the span should be detached as JAX-RS
        // client request / response filters are going to be executed in different threads.
        Span span = null;
        if (isAsyncInvocation()) {
            span = activeSpan;
            scope.close();
        }

        return new TraceScopeHolder<TraceScope>(new TraceScope(activeSpan, scope, null),
                                                span != null /* detached */);
    }

    private boolean isAsyncInvocation() {
        return !PhaseInterceptorChain.getCurrentMessage().getExchange().isSynchronous();
    }

    protected void stopTraceSpan(final TraceScopeHolder<TraceScope> holder, final int responseStatus) {
        if (holder == null) {
            return;
        }

        final TraceScope traceScope = holder.getScope();
        if (traceScope != null) {
            Span span = traceScope.getSpan();
            Scope scope = traceScope.getScope();

            // If the client invocation was asynchronous, the trace span has been created
            // in another thread and should be re-attached to the current one.
            if (holder.isDetached()) {
                scope = span.makeCurrent();
            }

            // Check if the response status code starts with 3, 4, or 5 (indicating redirection,
            // client error, or server error)
            // If true, set the error type attribute on the span with the response status code
            if (responseStatus >= 300) {
                span.setAttribute(ErrorAttributes.ERROR_TYPE, String.valueOf(responseStatus));
            }

            span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE.getKey(), responseStatus);

            span.end();

            scope.close();
        }
    }

    protected void stopTraceSpan(final TraceScopeHolder<TraceScope> holder, final Throwable ex) {
        if (holder == null) {
            return;
        }

        final TraceScope traceScope = holder.getScope();
        if (traceScope != null) {
            Span span = traceScope.getSpan();
            Scope scope = traceScope.getScope();

            // If the client invocation was asynchronous, the trace span has been created
            // in another thread and should be re-attached to the current one.
            if (holder.isDetached()) {
                scope = span.makeCurrent();
            }

            span.setStatus(StatusCode.ERROR);
            if (ex != null) {
                span.recordException(ex);
            }
            span.end();

            scope.close();
        }
    }
}
