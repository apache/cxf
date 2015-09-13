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
import org.apache.htrace.Sampler;
import org.apache.htrace.Span;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceInfo;
import org.apache.htrace.TraceScope;
import org.apache.htrace.Tracer;
import org.apache.htrace.impl.MilliSpan;

public abstract class AbstractHTraceProvider extends AbstractTracingProvider { 
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractHTraceProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.htrace.span";
        
    private final Sampler< ? > sampler;
        
    public AbstractHTraceProvider(final Sampler< ? > sampler) {
        this.sampler = sampler;
    }

    @SuppressWarnings("unchecked")
    protected TraceScope startTraceSpan(final Map<String, List<String>> requestHeaders, String path, String method) {
        
        // Try to extract the Trace Id value from the request header
        final long traceId = getFirstValueOrDefault(requestHeaders, getTraceIdHeader(), 
            Tracer.DONT_TRACE.traceId);
        
        // Try to extract the Span Id value from the request header
        final long spanId = getFirstValueOrDefault(requestHeaders, getSpanIdHeader(), 
            Tracer.DONT_TRACE.spanId); 
        
        TraceScope traceScope = null;
        if (traceId == Tracer.DONT_TRACE.traceId || spanId == Tracer.DONT_TRACE.spanId) {
            traceScope = Trace.startSpan(buildSpanDescription(path, method), (Sampler< TraceInfo >)sampler);
        } else {
            traceScope = Trace.startSpan(buildSpanDescription(path, method), new MilliSpan
                .Builder()
                .spanId(spanId)
                .traceId(traceId)
                .build());
        }
        
        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        if (isAsyncResponse()) {
            propagateContinuationSpan(traceScope.detach());
        }
        
        return traceScope;
    }

    protected void stopTraceSpan(final Map<String, List<String>> requestHeaders,
                                 final Map<String, List<Object>> responseHeaders,
                                 final TraceScope span) {
        final String traceIdHeader = getTraceIdHeader();
        final String spanIdHeader = getSpanIdHeader();

        // Transfer tracing headers into the response headers
        if (requestHeaders.containsKey(traceIdHeader) && requestHeaders.containsKey(spanIdHeader)) {
            responseHeaders.put(traceIdHeader, CastUtils.cast(requestHeaders.get(traceIdHeader)));
            responseHeaders.put(spanIdHeader, CastUtils.cast(requestHeaders.get(spanIdHeader)));
        }
        
        if (span != null) {
            // If the service resource is using asynchronous processing mode, the trace
            // scope has been created in another thread and should be re-attached to the current 
            // one.
            if (span.isDetached()) {
                final TraceScope continueSpan = Trace.continueSpan(span.getSpan()); 
                continueSpan.close();
            } else {            
                span.close();
            }
        }
    }
    
    private void propagateContinuationSpan(final Span continuationSpan) {
        JAXRSUtils.getCurrentMessage().put(Span.class, continuationSpan);
    }
    
    protected boolean isAsyncResponse() {
        return !JAXRSUtils.getCurrentMessage().getExchange().isSynchronous();
    }
    
    private static Long getFirstValueOrDefault(final Map<String, List<String>> headers, 
            final String header, final long defaultValue) {
        List<String> value = headers.get(header);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value.get(0));
            } catch (NumberFormatException ex) {
                LOG.log(Level.FINE, String.format("Unable to parse '%s' header value to long number", header), ex);
            }
        }
        return defaultValue;
    }
}
