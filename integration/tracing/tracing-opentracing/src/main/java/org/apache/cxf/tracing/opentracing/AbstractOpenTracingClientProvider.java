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

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
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

        final ActiveSpan parent = tracer.activeSpan();
        ActiveSpan span = null; 
        if (parent == null) {
            span = tracer.buildSpan(buildSpanDescription(uri.toString(), method)).startActive();
        } else {
            span = tracer.buildSpan(buildSpanDescription(uri.toString(), method)).asChildOf(parent).startActive();
        }

        // Set additional tags 
        span.setTag(Tags.HTTP_METHOD.getKey(), method);
        span.setTag(Tags.HTTP_URL.getKey(), uri.toString());

        tracer.inject(span.context(), Builtin.HTTP_HEADERS, new TextMapInjectAdapter(requestHeaders));
        
        // In case of asynchronous client invocation, the span should be detached as JAX-RS
        // client request / response filters are going to be executed in different threads.
        Continuation continuation = null;
        if (isAsyncInvocation()) {
            continuation = span.capture();
            span.deactivate();
        }

        return new TraceScopeHolder<TraceScope>(new TraceScope(span, continuation), 
            continuation != null /* detached */);
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
            ActiveSpan span = scope.getSpan();
            
            // If the client invocation was asynchronous , the trace span has been created
            // in another thread and should be re-attached to the current one.
            if (holder.isDetached()) {
                span = scope.getContinuation().activate();
            }

            span.setTag(Tags.HTTP_STATUS.getKey(), responseStatus);
            span.close();
        }
    }
    
    protected void stopTraceSpan(final TraceScopeHolder<TraceScope> holder, final Throwable ex) {
        if (holder == null) {
            return;
        }

        final TraceScope scope = holder.getScope();
        if (scope != null) {
            ActiveSpan span = scope.getSpan();
            
            // If the client invocation was asynchronous , the trace span has been created
            // in another thread and should be re-attached to the current one.
            if (holder.isDetached()) {
                span = scope.getContinuation().activate();
            }


            if (ex != null) {
                final Map<String, Object> logEvent = new HashMap<>(2);
                logEvent.put("event", Tags.ERROR.getKey());
                logEvent.put("message", ex.getMessage());
                span.log(logEvent);
            }
            
            span.setTag(Tags.ERROR.getKey(), Boolean.TRUE);
            span.close();
        }
    }
}
