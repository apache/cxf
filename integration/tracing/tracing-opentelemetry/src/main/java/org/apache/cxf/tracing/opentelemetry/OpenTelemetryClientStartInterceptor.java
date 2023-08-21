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

import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public class OpenTelemetryClientStartInterceptor extends AbstractOpenTelemetryClientInterceptor {
    public OpenTelemetryClientStartInterceptor(final OpenTelemetry openTelemetry, final String instrumentationName) {
        this(Phase.PRE_STREAM, openTelemetry, instrumentationName);
    }

    public OpenTelemetryClientStartInterceptor(final OpenTelemetry openTelemetry, final Tracer tracer) {
        this(Phase.PRE_STREAM, openTelemetry, tracer);
    }

    public OpenTelemetryClientStartInterceptor(final String phase, final OpenTelemetry openTelemetry, 
            final String instrumentationName) {
        super(phase, openTelemetry, instrumentationName);
    }

    public OpenTelemetryClientStartInterceptor(final String phase, final OpenTelemetry openTelemetry, 
            final Tracer tracer) {
        super(phase, openTelemetry, tracer);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        final Map<String, List<String>> headers = CastUtils
            .cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        final TraceScopeHolder<TraceScope> holder = super.startTraceSpan(headers, getUri(message),
                                                                         (String)message
                                                                             .get(Message.HTTP_REQUEST_METHOD));

        if (holder != null) {
            message.getExchange().put(TRACE_SPAN, holder);
        }
    }

    @Override
    public void handleFault(Message message) {
        @SuppressWarnings("unchecked")
        final TraceScopeHolder<TraceScope> holder = (TraceScopeHolder<TraceScope>)message.getExchange()
            .get(TRACE_SPAN);

        final Exception ex = message.getContent(Exception.class);
        super.stopTraceSpan(holder, ex);
    }
}
