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
import org.apache.cxf.tracing.AbstractTracingProvider;
import org.apache.htrace.Sampler;
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
    protected TraceScope startTraceSpan(final Map<String, List<String>> requestHeaders, String path) {
        
        // Try to extract the Trace Id value from the request header
        final long traceId = getFirstValueOrDefault(requestHeaders, getTraceIdHeader(), 
            Tracer.DONT_TRACE.traceId);
        
        // Try to extract the Span Id value from the request header
        final long spanId = getFirstValueOrDefault(requestHeaders, getSpanIdHeader(), 
            Tracer.DONT_TRACE.spanId); 
        
        if (traceId == Tracer.DONT_TRACE.traceId || spanId == Tracer.DONT_TRACE.spanId) {
            return Trace.startSpan(path, (Sampler< TraceInfo >)sampler);
        } 
        
        return Trace.startSpan(path, new MilliSpan
            .Builder()
            .spanId(spanId)
            .traceId(traceId)
            .build());
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
            span.close();
        }
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
