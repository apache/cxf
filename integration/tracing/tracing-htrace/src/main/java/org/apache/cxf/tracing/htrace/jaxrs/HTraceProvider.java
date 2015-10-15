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
import java.lang.annotation.Annotation;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.tracing.htrace.AbstractHTraceProvider;
import org.apache.htrace.core.TraceScope;
import org.apache.htrace.core.Tracer;

@Provider
public class HTraceProvider extends AbstractHTraceProvider 
    implements ContainerRequestFilter, ContainerResponseFilter {
    @Context 
    private ResourceInfo resourceInfo;
    
    public HTraceProvider(final Tracer tracer) {
        super(tracer);
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final TraceScopeHolder<TraceScope> holder = super.startTraceSpan(requestContext.getHeaders(), 
                                                requestContext.getUriInfo().getPath(),
                                                requestContext.getMethod());
        
        if (holder != null) {
            requestContext.setProperty(TRACE_SPAN, holder);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void filter(final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException {
        super.stopTraceSpan(requestContext.getHeaders(), 
                            responseContext.getHeaders(), 
                            (TraceScopeHolder<TraceScope>)requestContext.getProperty(TRACE_SPAN));
    }
    
    @Override
    protected boolean isAsyncResponse() {
        for (final Annotation[] annotations: resourceInfo.getResourceMethod().getParameterAnnotations()) {
            for (final Annotation annotation: annotations) {
                if (annotation.annotationType().equals(Suspended.class)) {
                    return true;
                }
            }
        }
        return false;
    }
}
