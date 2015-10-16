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
package org.apache.cxf.tracing.htrace;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.tracing.AbstractTracingProvider;
import org.apache.htrace.core.SpanId;
import org.apache.htrace.core.TraceScope;
import org.apache.htrace.core.Tracer;

public abstract class AbstractHTraceProvider extends AbstractTracingProvider { 
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractHTraceProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.htrace.span";
        
    private final Tracer tracer;
        
    public AbstractHTraceProvider(final Tracer tracer) {
        this.tracer = tracer;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders, 
            String path, String method) {
        
        // Try to extract the Span Id value from the request header
        final SpanId spanId = getFirstValueOrDefault(requestHeaders, getSpanIdHeader(), SpanId.INVALID); 
        
        TraceScope traceScope = null;
        if (spanId == SpanId.INVALID) {
            traceScope = tracer.newScope(buildSpanDescription(path, method));
        } else {
            traceScope = tracer.newScope(buildSpanDescription(path, method), spanId);
        }
        
        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        boolean detached = false;
        if (isAsyncResponse()) {
            traceScope.detach();
            propagateContinuationSpan(traceScope);
            detached = true;
        }
        
        return new TraceScopeHolder<TraceScope>(traceScope, detached);
    }

    protected void stopTraceSpan(final Map<String, List<String>> requestHeaders,
                                 final Map<String, List<Object>> responseHeaders,
                                 final TraceScopeHolder<TraceScope> holder) {
        final String spanIdHeader = getSpanIdHeader();

        // Transfer tracing headers into the response headers
        if (requestHeaders.containsKey(spanIdHeader)) {
            responseHeaders.put(spanIdHeader, CastUtils.cast(requestHeaders.get(spanIdHeader)));
        }
        
        if (holder == null) {
            return;
        }
        
        final TraceScope span = holder.getScope();
        if (span != null) {
            // If the service resource is using asynchronous processing mode, the trace
            // scope has been created in another thread and should be re-attached to the current 
            // one.
            if (holder.isDetached()) {
                span.reattach(); 
                span.close();
            } else {            
                span.close();
            }
        }
    }
    
    private void propagateContinuationSpan(final TraceScope continuationScope) {
        JAXRSUtils.getCurrentMessage().put(TraceScope.class, continuationScope);
    }
    
    protected boolean isAsyncResponse() {
        return !JAXRSUtils.getCurrentMessage().getExchange().isSynchronous();
    }
    
    private static SpanId getFirstValueOrDefault(final Map<String, List<String>> headers, 
            final String header, final SpanId defaultValue) {
        List<String> value = headers.get(header);
        if (value != null && !value.isEmpty()) {
            try {
                return SpanId.fromString(value.get(0));
            } catch (NumberFormatException ex) {
                LOG.log(Level.FINE, String.format("Unable to parse '%s' header value to Span Id", header), ex);
            }
        }
        return defaultValue;
    }
}
