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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;
import org.apache.htrace.Sampler;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceScope;
import org.apache.htrace.wrappers.TraceCallable;

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
    
    @Override
    public <T> Callable<T> wrap(final String desription, final Traceable<T> traceable) {
        final Callable<T> callable = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return traceable.call(new HTraceTracerContext(sampler));
            }
        };
        
        // TODO: Replace with HTrace's wrap() method once the version with
        // callable and description becomes available.
        if (Trace.isTracing()) {
            return new TraceCallable<T>(Trace.currentSpan(), callable, desription);
        } else {
            return callable;
        }
    }

    @Override
    public void annotate(byte[] key, byte[] value) {
        Trace.addKVAnnotation(key, value);
    }
    
    @Override
    public void annotate(String key, String value) {
        annotate(key.getBytes(StandardCharsets.UTF_8), 
            value.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public void timeline(String message) {
        Trace.addTimelineAnnotation(message);
    }
}
