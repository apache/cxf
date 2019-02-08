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
package org.apache.cxf.tracing.brave.jaxrs;

import brave.Span;
import brave.http.HttpTracing;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.tracing.TracerContext;
import org.apache.cxf.tracing.brave.BraveTracerContext;

public class BraveContextProvider implements ContextProvider< TracerContext > {
    private final HttpTracing brave;

    public BraveContextProvider(final HttpTracing brave) {
        this.brave = brave;
    }

    @Override
    public TracerContext createContext(final Message message) {
        // Check if there is a server span passed along with the message
        final Span continuationSpan = message.get(Span.class);

        // If server span is already present, let us check if it is detached
        // (asynchronous invocation)
        if (continuationSpan != null) {
            return new BraveTracerContext(brave, continuationSpan);
        }

        return new BraveTracerContext(brave);
    }

}
