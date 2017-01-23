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
package org.apache.cxf.tracing.brave;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.http.BraveHttpHeaders;
import com.github.kristofa.brave.http.HttpResponse;
import com.github.kristofa.brave.http.HttpServerRequest;
import com.github.kristofa.brave.http.HttpServerRequestAdapter;
import com.github.kristofa.brave.http.HttpServerResponseAdapter;
import com.github.kristofa.brave.http.SpanNameProvider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.tracing.AbstractTracingProvider;

public abstract class AbstractBraveProvider extends AbstractTracingProvider { 
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractBraveProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.brave.span";
        
    protected final Brave brave;
    protected final SpanNameProvider spanNameProvider;
            
    protected AbstractBraveProvider(final Brave brave) {
        this(brave, new ServerSpanNameProvider());
    }
    
    protected AbstractBraveProvider(final Brave brave, final SpanNameProvider spanNameProvider) {
        this.brave = brave;
        this.spanNameProvider = spanNameProvider;
    }

    protected TraceScopeHolder<ServerSpan> startTraceSpan(final Map<String, List<String>> requestHeaders,
            URI uri, String method) {

        final HttpServerRequest request = new HttpServerRequest() {
            @Override
            public URI getUri() {
                return uri;
            }
            
            @Override
            public String getHttpMethod() {
                return method;
            }
            
            @Override
            public String getHttpHeaderValue(String headerName) {
                List<String> value = requestHeaders.get(headerName);
                
                if (value != null && !value.isEmpty()) {
                    return value.get(0);
                }
                
                return null;
            }
        };
        
        brave.serverRequestInterceptor().handle(new HttpServerRequestAdapter(request, spanNameProvider));
        final ServerSpan serverSpan = brave.serverSpanThreadBinder().getCurrentServerSpan();
        
        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        boolean detached = false;
        if (isAsyncResponse()) {
            brave.serverSpanThreadBinder().setCurrentSpan(null);
            propagateContinuationSpan(serverSpan);
            detached = true;
        }
        
        return new TraceScopeHolder<ServerSpan>(serverSpan, detached);
    }
    
    private void transferRequestHeader(final Map<String, List<String>> requestHeaders,
            final Map<String, List<Object>> responseHeaders, final BraveHttpHeaders header) {
        if (requestHeaders.containsKey(header.getName())) {
            responseHeaders.put(header.getName(), CastUtils.cast(requestHeaders.get(header.getName())));
        }
    }

    protected void stopTraceSpan(final Map<String, List<String>> requestHeaders,
                                 final Map<String, List<Object>> responseHeaders,
                                 final int responseStatus,
                                 final TraceScopeHolder<ServerSpan> holder) {

        // Transfer tracing headers into the response headers
        transferRequestHeader(requestHeaders, responseHeaders, BraveHttpHeaders.SpanId);
        transferRequestHeader(requestHeaders, responseHeaders, BraveHttpHeaders.Sampled);
        transferRequestHeader(requestHeaders, responseHeaders, BraveHttpHeaders.ParentSpanId);
        transferRequestHeader(requestHeaders, responseHeaders, BraveHttpHeaders.TraceId);
        
        if (holder == null) {
            return;
        }
        
        final ServerSpan span = holder.getScope();
        if (span != null) {
            // If the service resource is using asynchronous processing mode, the trace
            // scope has been created in another thread and should be re-attached to the current 
            // one.
            if (holder.isDetached()) {
                brave.serverSpanThreadBinder().setCurrentSpan(span);
            } 
            
            final HttpResponse response = () -> responseStatus;
            brave.serverResponseInterceptor().handle(new HttpServerResponseAdapter(response));
        }
    }
    
    private void propagateContinuationSpan(final ServerSpan continuationScope) {
        JAXRSUtils.getCurrentMessage().put(ServerSpan.class, continuationScope);
    }
    
    protected boolean isAsyncResponse() {
        return !JAXRSUtils.getCurrentMessage().getExchange().isSynchronous();
    }
}
