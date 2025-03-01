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
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;

public abstract class AbstractOpenTelemetryProvider extends AbstractTracingProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractOpenTelemetryProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.opentelemetry.span";

    protected final OpenTelemetry openTelemetry;

    protected final Tracer tracer;

    protected AbstractOpenTelemetryProvider(final OpenTelemetry openTelemetry, final String instrumentationName) {
        this(openTelemetry, openTelemetry.getTracer(instrumentationName));
    }

    protected AbstractOpenTelemetryProvider(final OpenTelemetry openTelemetry, final Tracer tracer) {
        this.openTelemetry = openTelemetry;
        this.tracer = tracer;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders,
                                                          URI uri, String method) {

        Context parent = openTelemetry.getPropagators().getTextMapPropagator()
            .extract(Context.current(), requestHeaders, TextMapInjectAdapter.get());
        Scope parentScope = parent.makeCurrent();

        SpanBuilder spanBuilder = tracer.spanBuilder(buildSpanDescription(uri.getPath(), method))
            .setSpanKind(SpanKind.SERVER)
            .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, method)
            .setAttribute(UrlAttributes.URL_PATH, uri.getPath())
            .setAttribute(UrlAttributes.URL_SCHEME, uri.getScheme())
            .setAttribute(ServerAttributes.SERVER_ADDRESS, uri.getHost())
            .setAttribute(ServerAttributes.SERVER_PORT, Long.valueOf(uri.getPort()));

        if (uri.getQuery() != null) {
            spanBuilder.setAttribute(UrlAttributes.URL_QUERY, uri.getQuery());
        }

        Span activeSpan = spanBuilder.startSpan();
        Scope scope = activeSpan.makeCurrent();

        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        Span span = null;
        if (isAsyncResponse()) {
            // Do not modify the current context span
            span = activeSpan;
            propagateContinuationSpan(span);
            scope.close();
            parentScope.close();
        }

        return new TraceScopeHolder<TraceScope>(new TraceScope(activeSpan, scope, parentScope), span != null);
    }

    protected void stopTraceSpan(final Map<String, List<String>> requestHeaders,
                                 final Map<String, List<Object>> responseHeaders, final int responseStatus,
                                 final TraceScopeHolder<TraceScope> holder) {

        if (holder == null) {
            return;
        }

        final TraceScope traceScope = holder.getScope();
        if (traceScope != null) {
            Span span = traceScope.getSpan();
            Scope scope = traceScope.getScope();

            // If the service resource is using asynchronous processing mode, the trace
            // scope has been created in another thread and should be re-attached to the current
            // one.
            if (holder.isDetached()) {
                scope = span.makeCurrent();
            }

            // Check if the response status code starts with 3, 4, or 5 (indicating redirection,
            // client error, or server error)
            // If true, set the error type attribute on the span with the response status code
            if (responseStatus >= 300) {
                span.setAttribute(ErrorAttributes.ERROR_TYPE, String.valueOf(responseStatus));
            }

            span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, responseStatus);
            span.end();

            scope.close();
            traceScope.getParentScope().close();
        }
    }

    protected boolean isAsyncResponse() {
        return !PhaseInterceptorChain.getCurrentMessage().getExchange().isSynchronous();
    }

    private void propagateContinuationSpan(final Span continuation) {
        PhaseInterceptorChain.getCurrentMessage().put(Span.class, continuation);
    }

}
