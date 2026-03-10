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
package org.apache.cxf.tracing.opentelemetry;

import java.util.concurrent.Callable;

import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

public class OpenTelemetryContext implements TracerContext {
    private final Tracer tracer;
    private final Span continuation;

    public OpenTelemetryContext(final Tracer tracer) {
        this(tracer, null);
    }

    public OpenTelemetryContext(final Tracer tracer, final Span continuation) {
        this.tracer = tracer;
        this.continuation = continuation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Scope startSpan(final String description) {
        return newOrChildSpan(description, null);
    }

    @Override
    public <T> T continueSpan(final Traceable<T> traceable) throws Exception {
        Scope scope = null;

        if (Span.current() != null && continuation != null && !Span.current().getSpanContext().getSpanId()
            .equals(continuation.getSpanContext().getSpanId())) {
            scope = continuation.makeCurrent();
        }

        try {
            return traceable.call(new OpenTelemetryContext(tracer));
        } finally {
            if (continuation != null && scope != null) {
                scope.close();
            }
        }
    }

    @Override
    public <T> Callable<T> wrap(final String description, final Traceable<T> traceable) {
        final Callable<T> callable = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return traceable.call(new OpenTelemetryContext(tracer));
            }
        };

        // Carry over parent from the current thread
        final Context parent = Context.current();
        return () -> {
            try (Scope scope = newOrChildSpan(description, parent)) {
                return callable.call();
            }
        };
    }

    @Override
    public void annotate(String key, String value) {
        final Span current = Span.current();
        if (current != null) {
            current.setAttribute(key, value);
        }
    }

    @Override
    public void timeline(String message) {
        final Span current = Span.current();
        if (current != null) {
            current.addEvent(message);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(final Class<T> clazz) {
        if (Tracer.class.equals(clazz)) {
            return (T)tracer;
        } else {
            throw new IllegalArgumentException("The class is '" + clazz
                                               + "'not supported and cannot be unwrapped");
        }
    }

    private Scope newOrChildSpan(final String description, Context parent) {
        SpanBuilder spanBuilder = tracer.spanBuilder(description);
        if (parent != null) {
            spanBuilder.setParent(parent);
        }
        Span span = spanBuilder.startSpan();
        return new ScopedSpan(span, span.makeCurrent());
    }
}
