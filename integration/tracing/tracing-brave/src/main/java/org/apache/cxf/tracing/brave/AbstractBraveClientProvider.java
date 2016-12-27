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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.http.HttpClientRequest;
import com.github.kristofa.brave.http.HttpClientRequestAdapter;
import com.github.kristofa.brave.http.HttpClientResponseAdapter;
import com.github.kristofa.brave.http.HttpResponse;
import com.github.kristofa.brave.http.SpanNameProvider;
import com.twitter.zipkin.gen.Span;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.tracing.AbstractTracingProvider;

public abstract class AbstractBraveClientProvider extends AbstractTracingProvider { 
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractBraveClientProvider.class);
    protected static final String TRACE_SPAN = "org.apache.cxf.tracing.client.brave.span";
        
    private final Brave brave;
    private final SpanNameProvider spanNameProvider;
        
    public AbstractBraveClientProvider(final Brave brave) {
        this.brave = brave;
        this.spanNameProvider = new ClientSpanNameProvider();
    }

    protected TraceScopeHolder<Span> startTraceSpan(final Map<String, List<String>> requestHeaders, 
            URI uri, String method) {
        final HttpClientRequest request = new HttpClientRequest() {
            @Override
            public URI getUri() {
                return uri;
            }
            
            @Override
            public String getHttpMethod() {
                return method;
            }
            
            @Override
            public void addHeader(String header, String value) {
                requestHeaders.put(header, Collections.singletonList(value));
            }
        };
        
        brave.clientRequestInterceptor().handle(new HttpClientRequestAdapter(request, spanNameProvider));
        final Span span = brave.clientSpanThreadBinder().getCurrentClientSpan();
        // In case of asynchronous client invocation, the span should be detached as JAX-RS 
        // client request / response filters are going to be executed in different threads.
        boolean detached = false;
        if (isAsyncInvocation() && span != null) {
            brave.clientSpanThreadBinder().setCurrentSpan(null);
            detached = true;
        }
        
        return new TraceScopeHolder<Span>(span, detached);
    }
    
    private boolean isAsyncInvocation() {
        return !JAXRSUtils.getCurrentMessage().getExchange().isSynchronous();
    }

    protected void stopTraceSpan(final TraceScopeHolder<Span> holder, final int responseStatus) {
        if (holder == null) {
            return;
        }
        
        final Span span = holder.getScope();
        if (span != null) {
            // If the client invocation was asynchronous , the trace span has been created 
            // in another thread and should be re-attached to the current one.
            if (holder.isDetached()) {
                brave.clientSpanThreadBinder().setCurrentSpan(span);
            }
            
            final HttpResponse response = () -> responseStatus;
            brave.clientResponseInterceptor().handle(new HttpClientResponseAdapter(response));
        }
    }
}
