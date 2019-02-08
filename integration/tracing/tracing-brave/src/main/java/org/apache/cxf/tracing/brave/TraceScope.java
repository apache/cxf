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

import java.io.Closeable;

import brave.Span;
import brave.Tracer.SpanInScope;
import brave.http.HttpTracing;

public class TraceScope implements Closeable {
    private final Span span;
    private final SpanInScope scope;

    TraceScope(final HttpTracing brave, final Span span) {
        this(span, brave.tracing().tracer().withSpanInScope(span));
    }

    TraceScope(final Span span, final SpanInScope scope) {
        this.span = span;
        this.scope = scope;
    }
    
    public Span getSpan() {
        return span;
    }

    @Override
    public void close() {
        span.finish();
        if (scope != null) {
            scope.close();
        }
    }
}
