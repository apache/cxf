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
package org.apache.cxf.tracing.opentracing.jaxrs;

import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.tracing.TracerContext;
import org.apache.cxf.tracing.opentracing.OpenTracingContext;

import io.opentracing.Span;
import io.opentracing.Tracer;

public class OpenTracingContextProvider implements ContextProvider< TracerContext > {
    private final Tracer tracer;

    public OpenTracingContextProvider(final Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public TracerContext createContext(final Message message) {
        // Check if there is a server span passed along with the message
        final Span continuation = message.get(Span.class);

        // If server span is already present, let us check if it is detached
        // (asynchronous invocation)
        if (continuation != null) {
            return new OpenTracingContext(tracer, continuation);
        }

        return new OpenTracingContext(tracer);
    }

}
