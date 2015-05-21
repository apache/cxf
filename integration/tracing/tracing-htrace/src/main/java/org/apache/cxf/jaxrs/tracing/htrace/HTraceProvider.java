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
package org.apache.cxf.jaxrs.tracing.htrace;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.htrace.Sampler;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceInfo;
import org.apache.htrace.TraceScope;
import org.apache.htrace.Tracer;
import org.apache.htrace.impl.NeverSampler;

import static org.apache.cxf.tracing.TracerHeaders.HEADER_SPAN_ID;
import static org.apache.cxf.tracing.TracerHeaders.HEADER_TRACE_ID;

@Provider
public class HTraceProvider implements ContainerRequestFilter, ContainerResponseFilter { 
    private static final Logger LOG = LogUtils.getL7dLogger(HTraceProvider.class);
        
    private final Sampler< ? > sampler;
    
    // Keep the parent spans in the thread-local storage. The span created during request
    // phase should be closed during the response phase in order to mark the start/end 
    // bounds 
    private final ThreadLocal<TraceScope> parent = new ThreadLocal<TraceScope>() {
        @Override
        protected TraceScope initialValue() {
            return null;
        }
    };
    
    public HTraceProvider() {
        this(NeverSampler.INSTANCE);
    }

    public HTraceProvider(final Sampler< ? > sampler) {
        this.sampler = sampler;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        
        // Try to extract the Trace Id value from the request header
        final long traceId = getFirstValueOrDefault(headers, HEADER_TRACE_ID, 
            Tracer.DONT_TRACE.traceId);
        
        // Try to extract the Span Id value from the request header
        final long spanId = getFirstValueOrDefault(headers, HEADER_SPAN_ID, 
            Tracer.DONT_TRACE.spanId); 
        
        if (traceId != Tracer.DONT_TRACE.traceId && spanId != Tracer.DONT_TRACE.spanId) {
            parent.set(Trace.startSpan(requestContext.getUriInfo().getPath(), (Sampler< TraceInfo >)sampler,
                new TraceInfo(traceId, spanId)));
        }
    }
    
    @Override
    public void filter(final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        
        // Transfer tracing headers into the response headers
        if (headers.containsKey(HEADER_TRACE_ID) && headers.containsKey(HEADER_SPAN_ID)) {
            responseContext.getHeaders().add(HEADER_TRACE_ID, headers.getFirst(HEADER_TRACE_ID));
            responseContext.getHeaders().add(HEADER_SPAN_ID, headers.getFirst(HEADER_SPAN_ID));
        }
        
        try (final TraceScope span = parent.get()) {
            if (span != null) {
                parent.remove();
            }
        }
    }
    
    private static Long getFirstValueOrDefault(final MultivaluedMap<String, String> headers, 
            final String header, final long defaultValue) {
        
        if (!headers.containsKey(header)) {
            return defaultValue;
        }
        
        try {
            final List< String > values = headers.get(header);
            if (values != null && !values.isEmpty()) {
                return Long.parseLong(values.get(0));
            }
        } catch (NumberFormatException ex) {
            LOG.log(Level.FINE, String.format("Unable to parse '%s' header value to long number", header), ex);
        }
        
        return defaultValue;
    }
}
