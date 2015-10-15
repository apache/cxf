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
import org.apache.htrace.core.Span;
import org.apache.htrace.core.TraceScope;
import org.apache.htrace.core.Tracer;

public abstract class AbstractHTraceClientProvider extends AbstractTracingProvider { 
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractHTraceClientProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.htrace.span";
        
    private final Tracer tracer;
        
    public AbstractHTraceClientProvider(final Tracer tracer) {
        this.tracer = tracer;
    }

    protected TraceScopeHolder<TraceScope> startTraceSpan(final Map<String, List<String>> requestHeaders, 
            String path, String method) {

        Span span = Tracer.getCurrentSpan();
        TraceScope traceScope = null;
        
        if (span == null) {
            traceScope = tracer.newScope(buildSpanDescription(path, method));
            span = traceScope.getSpan();
        }
        
        if (span != null) {
            final String spanIdHeader = getSpanIdHeader();
            // Transfer tracing headers into the response headers
            requestHeaders.put(spanIdHeader, Collections.singletonList(span.getSpanId().toString()));
        }
        
        // In case of asynchronous client invocation, the span should be detached as JAX-RS 
        // client request / response filters are going to be executed in different threads.
        boolean detached = false;
        if (isAsyncInvocation() && traceScope != null) {
            traceScope.detach();
            detached = true;
        }
        
        return new TraceScopeHolder<TraceScope>(traceScope, detached);
    }
    
    private boolean isAsyncInvocation() {
        return !JAXRSUtils.getCurrentMessage().getExchange().isSynchronous();
    }

    protected void stopTraceSpan(final TraceScopeHolder<TraceScope> holder) {
        if (holder == null) {
            return;
        }
        
        final TraceScope scope = holder.getScope();
        if (scope != null) {
            // If the client invocation was asynchronous , the trace scope has been created 
            // in another thread and should be re-attached to the current one.
            if (holder.isDetached()) {
                scope.reattach(); 
                scope.close();
            } else {
                scope.close();
            }
        }
    }
}
