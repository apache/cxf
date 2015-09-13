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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.tracing.AbstractTracingProvider;
import org.apache.htrace.Sampler;
import org.apache.htrace.Span;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceScope;
import org.apache.htrace.impl.NeverSampler;

@Provider
public class HTraceClientProvider extends AbstractTracingProvider 
        implements ClientRequestFilter, ClientResponseFilter {
    private static final String TRACE_SPAN = "org.apache.cxf.tracing.client.htrace.span";
    
    private final Sampler< ? > sampler;
    
    public HTraceClientProvider() {
        this(NeverSampler.INSTANCE);
    }

    public HTraceClientProvider(final Sampler< ? > sampler) {
        this.sampler = sampler;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        Span span = Trace.currentSpan();
        
        if (span == null) {
            final TraceScope scope = Trace.startSpan(buildSpanDescription(requestContext.getUri().toString(), 
                requestContext.getMethod()), sampler);
            span = scope.getSpan();
            
            if (span != null) {
                requestContext.setProperty(TRACE_SPAN, scope);
            }
        }
        
        if (span != null) {
            final MultivaluedMap<String, Object> requestHeaders = requestContext.getHeaders();
            
            final String traceIdHeader = getTraceIdHeader();
            final String spanIdHeader = getSpanIdHeader();
            
            // Transfer tracing headers into the response headers
            requestHeaders.putSingle(traceIdHeader, Long.toString(span.getTraceId()));
            requestHeaders.putSingle(spanIdHeader, Long.toString(span.getSpanId()));
        }
    }
    
    @Override
    public void filter(final ClientRequestContext requestContext,
            final ClientResponseContext responseContext) throws IOException {
        
        final TraceScope scope = (TraceScope)requestContext.getProperty(TRACE_SPAN);
        if (scope != null) {
            scope.close();
        }
    }
}
