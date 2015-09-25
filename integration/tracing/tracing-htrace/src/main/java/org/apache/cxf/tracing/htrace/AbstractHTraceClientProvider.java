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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.tracing.AbstractTracingProvider;
import org.apache.htrace.Sampler;
import org.apache.htrace.Span;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceScope;

public abstract class AbstractHTraceClientProvider extends AbstractTracingProvider { 
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractHTraceClientProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.htrace.span";
        
    private final Sampler< ? > sampler;
        
    public AbstractHTraceClientProvider(final Sampler< ? > sampler) {
        this.sampler = sampler;
    }

    protected TraceScope startTraceSpan(final Map<String, List<String>> requestHeaders, String path, String method) {
        Span span = Trace.currentSpan();
        TraceScope scope = null;
        
        if (span == null) {
            scope = Trace.startSpan(buildSpanDescription(path, method), sampler);
            span = scope.getSpan();
        }
        
        if (span != null) {
            final String traceIdHeader = getTraceIdHeader();
            final String spanIdHeader = getSpanIdHeader();
            
            // Transfer tracing headers into the response headers
            requestHeaders.put(traceIdHeader, Collections.singletonList(Long.toString(span.getTraceId())));
            requestHeaders.put(spanIdHeader, Collections.singletonList(Long.toString(span.getSpanId())));
        }
        
        // In case of asynchronous client invocation, the span should be detached as JAX-RS 
        // client request / response filters are going to be executed in different threads.
        if (isAsyncInvocation() && scope != null) {
            scope.detach();
        }
        
        return scope;
    }
    
    private boolean isAsyncInvocation() {
        return !JAXRSUtils.getCurrentMessage().getExchange().isSynchronous();
    }

    protected void stopTraceSpan(final TraceScope scope) {
        if (scope != null) {
            // If the client invocation was asynchronous , the trace scope has been created 
            // in another thread and should be re-attached to the current one.
            if (scope.isDetached()) {
                final TraceScope continueSpan = Trace.continueSpan(scope.getSpan()); 
                continueSpan.close();
            } else {
                scope.close();
            }
        }
    }
}
