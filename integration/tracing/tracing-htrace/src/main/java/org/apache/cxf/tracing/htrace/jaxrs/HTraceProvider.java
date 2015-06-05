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
package org.apache.cxf.tracing.htrace.jaxrs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.tracing.TracerHeaders;
import org.apache.htrace.Sampler;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceInfo;
import org.apache.htrace.TraceScope;
import org.apache.htrace.Tracer;
import org.apache.htrace.impl.NeverSampler;

import static org.apache.cxf.tracing.TracerHeaders.DEFAULT_HEADER_SPAN_ID;
import static org.apache.cxf.tracing.TracerHeaders.DEFAULT_HEADER_TRACE_ID;

@Provider
public class HTraceProvider implements ContainerRequestFilter, ContainerResponseFilter { 
    private static final Logger LOG = LogUtils.getL7dLogger(HTraceProvider.class);
    private static final String TRACE_SPAN = "org.apache.cxf.jaxrs.tracing.htrace.span";
        
    private final Sampler< ? > sampler;
    
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
        final long traceId = getFirstValueOrDefault(headers, getTraceIdHeader(), 
            Tracer.DONT_TRACE.traceId);
        
        // Try to extract the Span Id value from the request header
        final long spanId = getFirstValueOrDefault(headers, getSpanIdHeader(), 
            Tracer.DONT_TRACE.spanId); 
        
        if (traceId != Tracer.DONT_TRACE.traceId && spanId != Tracer.DONT_TRACE.spanId) {
            requestContext.setProperty(TRACE_SPAN, Trace.startSpan(requestContext.getUriInfo().getPath(), 
                (Sampler< TraceInfo >)sampler, new TraceInfo(traceId, spanId)));
        }
    }
    
    @Override
    public void filter(final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        
        final String traceIdHeader = getTraceIdHeader();
        final String spanIdHeader = getSpanIdHeader();

        // Transfer tracing headers into the response headers
        if (headers.containsKey(traceIdHeader) && headers.containsKey(spanIdHeader)) {
            responseContext.getHeaders().add(traceIdHeader, headers.getFirst(traceIdHeader));
            responseContext.getHeaders().add(spanIdHeader, headers.getFirst(spanIdHeader));
        }
        
        final Object value = requestContext.getProperty(TRACE_SPAN);
        if (value instanceof TraceScope) {
            final TraceScope span = (TraceScope)value;
            span.close();
        }
    }
    
    private static Long getFirstValueOrDefault(final MultivaluedMap<String, String> headers, 
            final String header, final long defaultValue) {
        String value = headers.getFirst(header);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                LOG.log(Level.FINE, String.format("Unable to parse '%s' header value to long number", header), ex);
            }
        }
        return defaultValue;
    }
    
    private static String getSpanIdHeader() {
        return getHeaderOrDefault(TracerHeaders.HEADER_SPAN_ID, DEFAULT_HEADER_SPAN_ID);
    }
    
    private static String getTraceIdHeader() {
        return getHeaderOrDefault(TracerHeaders.HEADER_TRACE_ID, DEFAULT_HEADER_TRACE_ID);
    }

    private static String getHeaderOrDefault(final String property, final String fallback) {
        final Message message = JAXRSUtils.getCurrentMessage();
        
        if (message != null) {
            final Object header = message.getContextualProperty(property);
            
            if (header instanceof String) {
                final String name = (String)header;
                if (!StringUtils.isEmpty(name)) {
                    return name;
                }
            }
        }
        
        return fallback;
    }
}
