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

import org.apache.cxf.tracing.TracerContext;
import org.apache.htrace.Sampler;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceScope;

public class HTraceTracerContext implements TracerContext {
    private final Sampler< ? > sampler;
    
    public HTraceTracerContext(final Sampler< ? > sampler) {
        this.sampler = sampler;
    }
        
    @Override
    @SuppressWarnings("unchecked")
    public TraceScope startSpan(final String description) {
        return Trace.startSpan(description, sampler);
    }
}
