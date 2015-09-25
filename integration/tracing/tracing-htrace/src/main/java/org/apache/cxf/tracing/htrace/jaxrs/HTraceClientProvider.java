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
import javax.ws.rs.ext.Provider;

import org.apache.cxf.tracing.htrace.AbstractHTraceClientProvider;
import org.apache.htrace.Sampler;
import org.apache.htrace.TraceScope;
import org.apache.htrace.impl.NeverSampler;

@Provider
public class HTraceClientProvider extends AbstractHTraceClientProvider 
        implements ClientRequestFilter, ClientResponseFilter {
    private static final String TRACE_SPAN = "org.apache.cxf.tracing.client.htrace.span";
    
    public HTraceClientProvider() {
        this(NeverSampler.INSTANCE);
    }

    public HTraceClientProvider(final Sampler< ? > sampler) {
        super(sampler);
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        final TraceScope scope = super.startTraceSpan(requestContext.getStringHeaders(), 
            requestContext.getUri().toString(), requestContext.getMethod());

        if (scope != null) {
            requestContext.setProperty(TRACE_SPAN, scope);
        }
    }
    
    @Override
    public void filter(final ClientRequestContext requestContext,
            final ClientResponseContext responseContext) throws IOException {
        final TraceScope scope = (TraceScope)requestContext.getProperty(TRACE_SPAN);
        super.stopTraceSpan(scope);
    }
}
