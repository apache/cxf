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

import java.util.concurrent.Callable;

import brave.Span;
import brave.Tracer;
import brave.Tracer.SpanInScope;
import brave.Tracing;
import brave.http.HttpTracing;
import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;

public class BraveTracerContext implements TracerContext {
    private final HttpTracing brave;
    private final Tracer tracer;
    private final Span continuationSpan;

    public BraveTracerContext(final HttpTracing brave) {
        this(brave, null);
    }

    public BraveTracerContext(final HttpTracing brave, final 
            Span continuationSpan) {
        this.brave = brave;
        this.tracer = brave.tracing().tracer();
        this.continuationSpan = continuationSpan;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TraceScope startSpan(final String description) {
        return new TraceScope(brave, tracer.nextSpan().name(description).start());
    }

    @Override
    public <T> T continueSpan(final Traceable<T> traceable) throws Exception {
        SpanInScope scope = null;
        
        if (tracer.currentSpan() == null && continuationSpan != null) {
            scope = tracer.withSpanInScope(continuationSpan);
        }

        try {
            return traceable.call(new BraveTracerContext(brave));
        } finally {
            if (continuationSpan != null && scope != null) {
                scope.close();
            }
        }
    }

    @Override
    public <T> Callable<T> wrap(final String description, final Traceable<T> traceable) {
        final Callable<T> callable = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return traceable.call(new BraveTracerContext(brave));
            }
        };

        // Carry over parent from the current thread
        final Span parent = tracer.currentSpan();
        return () -> {
            try (TraceScope span = newOrChildSpan(description, parent)) {
                return callable.call();
            } 
        };
    }

    @Override
    public void annotate(String key, String value) {
        final Span current = tracer.currentSpan();
        if (current != null) {
            current.tag(key, value);
        }
    }

    @Override
    public void timeline(String message) {
        final Span current = tracer.currentSpan();
        if (current != null) {
            current.annotate(message);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(final Class<T> clazz) {
        if (HttpTracing.class.equals(clazz)) {
            return (T)brave;
        } else if (Tracing.class.equals(clazz)) {
            return (T)brave.tracing();
        } else if (Tracer.class.equals(clazz)) {
            return (T)tracer;
        } else {
            throw new IllegalArgumentException("The class is '" + clazz
                  + "'not supported and cannot be unwrapped");
        }
    }
    
    private TraceScope newOrChildSpan(final String description, final Span parent) {
        if (parent == null) { 
            return new TraceScope(brave, tracer.newTrace().name(description).start());
        }
        return new TraceScope(brave, tracer.newChild(parent.context()).name(description).start());
    }
}
