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
package org.apache.cxf.tracing.opentracing;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.tracing.AbstractTracingProvider;
import org.apache.cxf.tracing.opentracing.internal.TextMapInjectAdapter;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;

public abstract class AbstractOpenTracingClientProvider extends AbstractTracingProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractOpenTracingClientProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.client.opentracing.span";
    
    private final Tracer tracer;

    public AbstractOpenTracingClientProvider(final Tracer tracer) {
        this.tracer = tracer;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders,
            URI uri, String method) {

        final Span parent = tracer.activeSpan();
        
        final Span activeSpan;
        final Scope scope;
        if (parent == null) {
            activeSpan = tracer.buildSpan(buildSpanDescription(uri.toString(), method)).start(); 
            scope = tracer.scopeManager().activate(activeSpan);
        } else {
            activeSpan = tracer.buildSpan(buildSpanDescription(uri.toString(), method)).asChildOf(parent).start();
            scope = tracer.scopeManager().activate(activeSpan);
        }

        // Set additional tags 
        activeSpan.setTag(Tags.HTTP_METHOD.getKey(), method);
        activeSpan.setTag(Tags.HTTP_URL.getKey(), uri.toString());

        tracer.inject(activeSpan.context(), Builtin.HTTP_HEADERS, new TextMapInjectAdapter(requestHeaders));
        
        // In case of asynchronous client invocation, the span should be detached as JAX-RS
        // client request / response filters are going to be executed in different threads.
        Span span = null;
        if (isAsyncInvocation()) {
            span = activeSpan;
            scope.close();
        }

        return new TraceScopeHolder<TraceScope>(new TraceScope(activeSpan, scope), 
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
            
            // If the client invocation was asynchronous , the trace span has been created
            // in another thread and should be re-attached to the current one.
            if (holder.isDetached()) {
                scope = tracer.scopeManager().activate(span);
            }

            span.setTag(Tags.HTTP_STATUS.getKey(), responseStatus);
            span.setTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
            span.finish();
            
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
            
            // If the client invocation was asynchronous , the trace span has been created
            // in another thread and should be re-attached to the current one.
            if (holder.isDetached()) {
                scope = tracer.scopeManager().activate(span);
            }

            span.setTag(Tags.ERROR.getKey(), Boolean.TRUE);
            if (ex != null) {
                final Map<String, Object> logEvent = new HashMap<>(2);
                logEvent.put("event", Tags.ERROR.getKey());
                logEvent.put("message", ex.getMessage());
                span.log(logEvent);
            }
            span.finish();
            
            scope.close();
        }
    }
}
