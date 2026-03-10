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
package org.apache.cxf.tracing.brave;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import brave.Span;
import brave.Tracer.SpanInScope;
import brave.http.HttpServerHandler;
import brave.http.HttpServerRequest;
import brave.http.HttpServerResponse;
import brave.http.HttpTracing;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.tracing.AbstractTracingProvider;
import org.apache.cxf.tracing.brave.internal.HttpAdapterFactory;
import org.apache.cxf.tracing.brave.internal.HttpAdapterFactory.Request;
import org.apache.cxf.tracing.brave.internal.HttpAdapterFactory.Response;
import org.apache.cxf.tracing.brave.internal.HttpServerAdapterFactory;

public abstract class AbstractBraveProvider extends AbstractTracingProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractBraveProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.brave.span";

    protected final HttpTracing brave;
    
    protected AbstractBraveProvider(final HttpTracing brave) {
        this.brave = brave;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders,
            URI uri, String method) {

        final Request request = HttpAdapterFactory.request(requestHeaders, uri, method);
        final HttpServerRequest wrapper = HttpServerAdapterFactory.create(request);
        
        final HttpServerHandler<HttpServerRequest, ?> handler = HttpServerHandler.create(brave);
        Span span = handler.handleReceive(wrapper);
        
        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        SpanInScope scope = null;
        if (isAsyncResponse() && span != null) {
           // Do not modify the current context span
            propagateContinuationSpan(span);
        } else if (span != null) {
            scope = brave.tracing().tracer().withSpanInScope(span);
        }

        return new TraceScopeHolder<TraceScope>(new TraceScope(span, scope), scope == null /* detached */);
    }

    protected void stopTraceSpan(final Map<String, List<String>> requestHeaders,
                                 final Map<String, List<Object>> responseHeaders,
                                 final String method,
                                 final URI uri,
                                 final int responseStatus,
                                 final TraceScopeHolder<TraceScope> holder) {
        if (holder == null) {
            return;
        }

        final TraceScope scope = holder.getScope();
        Span span = null;
        if (scope != null) {
            try {
                // If the service resource is using asynchronous processing mode, the trace
                // scope has been created in another thread and should be re-attached to the current
                // one.
                if (holder.isDetached()) {
                    span = brave.tracing().tracer().joinSpan(scope.getSpan().context());
                }
    
                final Response response = HttpAdapterFactory.response(method, uri.getPath(), responseStatus);
                final HttpServerResponse wrapper = HttpServerAdapterFactory.create(response);
                
                final HttpServerHandler<?, HttpServerResponse> handler = HttpServerHandler.create(brave);
                handler.handleSend(wrapper, scope.getSpan());
            } finally {
                scope.close();
                if (span != null) {
                    // We do not care about the span created by joinSpan, since it 
                    // should be managed by the scope.getSpan() itself. 
                    span.abandon();
                }
            }
        }
    }

    protected boolean isAsyncResponse() {
        return !PhaseInterceptorChain.getCurrentMessage().getExchange().isSynchronous();
    }

    private void propagateContinuationSpan(final Span continuationScope) {
        PhaseInterceptorChain.getCurrentMessage().put(Span.class, continuationScope);
    }
}
