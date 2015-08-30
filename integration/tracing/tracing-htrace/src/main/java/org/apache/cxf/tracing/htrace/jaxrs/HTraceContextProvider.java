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

import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.tracing.TracerContext;
import org.apache.cxf.tracing.htrace.HTraceTracerContext;
import org.apache.htrace.Sampler;
import org.apache.htrace.Span;
import org.apache.htrace.impl.NeverSampler;

@Provider
public class HTraceContextProvider implements ContextProvider< TracerContext > {
    private Sampler< ? > sampler = NeverSampler.INSTANCE;
    
    public HTraceContextProvider(final Sampler< ? > sampler) {
        this.sampler = sampler;
    }

    @Override
    public TracerContext createContext(final Message message) {
        // Check if there is a trace scope passed along with the message
        final Span continuationSpan = message.get(Span.class);
        
        // If trace scope is already present, let us check if it is detached 
        // (asynchronous invocation)
        if (continuationSpan != null) {
            return new HTraceTracerContext(sampler, continuationSpan);
        }
        
        return new HTraceTracerContext(sampler);
    }
}
