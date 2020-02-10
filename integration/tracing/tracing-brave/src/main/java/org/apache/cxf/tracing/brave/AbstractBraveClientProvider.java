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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import brave.Span;
import brave.Tracer.SpanInScope;
import brave.http.HttpClientAdapter;
import brave.http.HttpClientHandler;
import brave.http.HttpClientRequest;
import brave.http.HttpClientResponse;
import brave.http.HttpTracing;
import brave.propagation.Propagation.Setter;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.tracing.AbstractTracingProvider;
import org.apache.cxf.tracing.brave.internal.HttpAdapterFactory;
import org.apache.cxf.tracing.brave.internal.HttpAdapterFactory.Request;
import org.apache.cxf.tracing.brave.internal.HttpAdapterFactory.Response;
import org.apache.cxf.tracing.brave.internal.HttpClientAdapterFactory;

public abstract class AbstractBraveClientProvider extends AbstractTracingProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractBraveClientProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.client.brave.span";

    private final HttpTracing brave;

    public AbstractBraveClientProvider(final HttpTracing brave) {
        this.brave = brave;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders,
            URI uri, String method) {

        final Request request = HttpAdapterFactory.request(requestHeaders, uri, method);
        final HttpClientAdapter<Request, ?> adapter = HttpClientAdapterFactory.create(request);
        
        final HttpClientHandler<Request, ?> handler = HttpClientHandler.create(brave, adapter);
        final Span span = handler.handleSend(
            brave
                .tracing()
                .propagation()
                .injector(inject(requestHeaders)), 
            request);

        // In case of asynchronous client invocation, the span should be detached as JAX-RS
        // client request / response filters are going to be executed in different threads.
        SpanInScope scope = null;
        if (!isAsyncInvocation() && span != null) {
            scope = brave.tracing().tracer().withSpanInScope(span);
        }

        return new TraceScopeHolder<TraceScope>(new TraceScope(span, scope), scope == null /* detached */);
    }
    
    private <C> Setter<C, String> inject(final Map<String, List<String>> requestHeaders) {
        return (carrier, key, value) -> {
            if (!requestHeaders.containsKey(key)) {
                requestHeaders.put(key, Collections.singletonList(value));
            }
        };
    }

    private boolean isAsyncInvocation() {
        return !PhaseInterceptorChain.getCurrentMessage().getExchange().isSynchronous();
    }

    protected void stopTraceSpan(final TraceScopeHolder<TraceScope> holder, final int responseStatus) {
        if (holder == null) {
            return;
        }

        final TraceScope scope = holder.getScope();
        if (scope != null) {
            try {
                // If the client invocation was asynchronous , the trace span has been created
                // in another thread and should be re-attached to the current one.
                if (holder.isDetached()) {
                    brave.tracing().tracer().joinSpan(scope.getSpan().context());
                }
    
                final Response response = HttpAdapterFactory.response(responseStatus);
                final HttpClientAdapter<?, Response> adapter = HttpClientAdapterFactory.create(response);
                
                final HttpClientHandler<?, Response> handler = HttpClientHandler.create(brave, adapter);
                handler.handleReceive(response, null, scope.getSpan());
            } finally {
                scope.close();
            }
        }
    }
    
    protected void stopTraceSpan(final TraceScopeHolder<TraceScope> holder, final Throwable ex) {
        if (holder == null) {
            return;
        }

        final TraceScope scope = holder.getScope();
        if (scope != null) {
            try {
                // If the client invocation was asynchronous , the trace span has been created
                // in another thread and should be re-attached to the current one.
                if (holder.isDetached()) {
                    brave.tracing().tracer().joinSpan(scope.getSpan().context());
                }
    
                final HttpClientHandler<HttpClientRequest, HttpClientResponse> handler = 
                        HttpClientHandler.create(brave);
                handler.handleReceive(null, ex, scope.getSpan());
            } finally {
                scope.close();
            }
        }
    }
}
