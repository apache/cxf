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

import java.util.concurrent.Callable;

import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;
import org.apache.htrace.core.Span;
import org.apache.htrace.core.TraceScope;
import org.apache.htrace.core.Tracer;

public class HTraceTracerContext implements TracerContext {
    private final Tracer tracer;
    private final TraceScope continuationScope;

    public HTraceTracerContext(final Tracer tracer) {
        this(tracer, null);
    }

    public HTraceTracerContext(final Tracer tracer, final TraceScope continuationScope) {
        this.tracer = tracer;
        this.continuationScope = continuationScope;
    }
        
    @Override
    @SuppressWarnings("unchecked")
    public TraceScope startSpan(final String description) {
        return tracer.newScope(description);
    }
    
    @Override
    public <T> T continueSpan(final Traceable<T> traceable) throws Exception {
        boolean attached = false;
        if (!isTracing() && continuationScope != null) {
            continuationScope.reattach();
            attached = true;
        }
        
        try {
            return traceable.call(new HTraceTracerContext(tracer));
        } finally {
            if (continuationScope != null && attached) {
                continuationScope.detach();
            }
        }
    }
    
    @Override
    public <T> Callable<T> wrap(final String description, final Traceable<T> traceable) {
        final Callable<T> callable = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return traceable.call(new HTraceTracerContext(tracer));
            }
        };
        
        return tracer.wrap(callable, description);
    }
    
    @Override
    public void annotate(String key, String value) {
        final Span currentSpan = Tracer.getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addKVAnnotation(key, value);
        }
    }
    
    @Override
    public void timeline(String message) {
        final Span currentSpan = Tracer.getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addTimelineAnnotation(message);
        }
    }
    
    private boolean isTracing() {
        return Tracer.getCurrentSpan() != null;
    }
}
