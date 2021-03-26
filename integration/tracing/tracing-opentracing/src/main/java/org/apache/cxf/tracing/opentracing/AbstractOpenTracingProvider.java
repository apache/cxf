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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.tracing.AbstractTracingProvider;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;

public abstract class AbstractOpenTracingProvider extends AbstractTracingProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractOpenTracingProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.opentracing.span";
    
    protected final Tracer tracer;
    
    protected AbstractOpenTracingProvider(final Tracer tracer) {
        this.tracer = tracer;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders,
            URI uri, String method) {

        SpanContext parent = tracer.extract(Builtin.HTTP_HEADERS, 
            new TextMapAdapter(
                requestHeaders
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, this::getFirstValueOrEmpty))
            ));
        
        final Span activeSpan;
        final Scope scope;
        if (parent == null) {
            activeSpan = tracer.buildSpan(buildSpanDescription(uri.getPath(), method)).start(); 
            scope = tracer.scopeManager().activate(activeSpan);
        } else {
            activeSpan = tracer.buildSpan(buildSpanDescription(uri.getPath(), method)).asChildOf(parent).start();
            scope = tracer.scopeManager().activate(activeSpan);
        }
        
        // Set additional tags
        activeSpan.setTag(Tags.HTTP_METHOD.getKey(), method);
        activeSpan.setTag(Tags.HTTP_URL.getKey(), uri.toString());
        
        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        Span span = null;
        if (isAsyncResponse()) {
           // Do not modify the current context span
            span = activeSpan;
            propagateContinuationSpan(span);
            scope.close();
        } 

        return new TraceScopeHolder<TraceScope>(new TraceScope(activeSpan, scope), span != null);
    }

    protected void stopTraceSpan(final Map<String, List<String>> requestHeaders,
                                 final Map<String, List<Object>> responseHeaders,
                                 final int responseStatus,
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
                scope = tracer.scopeManager().activate(span);
            }

            span.setTag(Tags.HTTP_STATUS.getKey(), responseStatus);
            span.setTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
            span.finish();
            
            scope.close();
        }
    }

    protected boolean isAsyncResponse() {
        return !PhaseInterceptorChain.getCurrentMessage().getExchange().isSynchronous();
    }

    private void propagateContinuationSpan(final Span continuation) {
        PhaseInterceptorChain.getCurrentMessage().put(Span.class, continuation);
    }

    private String getFirstValueOrEmpty(Map.Entry<String, List<String>> entry) {
        final List<String> values = entry.getValue();

        if (values == null || values.isEmpty()) {
            return "";
        }
        
        final String value = values.get(0);
        return (value != null) ? value : "";
    }
}
