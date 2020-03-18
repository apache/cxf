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

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMapExtractAdapter;
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
            new TextMapExtractAdapter(
                requestHeaders
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, this::getFirstValueOrEmpty))
            ));
        
        ActiveSpan scope = null;
        if (parent == null) {
            scope = tracer.buildSpan(buildSpanDescription(uri.getPath(), method)).startActive();
        } else {
            scope = tracer.buildSpan(buildSpanDescription(uri.getPath(), method)).asChildOf(parent).startActive();
        }
        
        // Set additional tags
        scope.setTag(Tags.HTTP_METHOD.getKey(), method);
        scope.setTag(Tags.HTTP_URL.getKey(), uri.toString());
        
        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        Continuation continuation = null;
        if (isAsyncResponse()) {
           // Do not modify the current context span
            continuation = scope.capture();
            propagateContinuationSpan(continuation);
            scope.deactivate();
        } 

        return new TraceScopeHolder<TraceScope>(new TraceScope(scope, continuation), 
            continuation != null);
    }

    protected void stopTraceSpan(final Map<String, List<String>> requestHeaders,
                                 final Map<String, List<Object>> responseHeaders,
                                 final int responseStatus,
                                 final TraceScopeHolder<TraceScope> holder) {

        if (holder == null) {
            return;
        }

        final TraceScope scope = holder.getScope();
        if (scope != null) {
            ActiveSpan span = scope.getSpan();

            // If the service resource is using asynchronous processing mode, the trace
            // scope has been created in another thread and should be re-attached to the current
            // one.
            if (holder.isDetached()) {
                span = scope.getContinuation().activate();
            }

            span.setTag(Tags.HTTP_STATUS.getKey(), responseStatus);
            span.setTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
            span.close();
        }
    }

    protected boolean isAsyncResponse() {
        return !PhaseInterceptorChain.getCurrentMessage().getExchange().isSynchronous();
    }

    private void propagateContinuationSpan(final Continuation continuationScope) {
        PhaseInterceptorChain.getCurrentMessage().put(Continuation.class, continuationScope);
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
