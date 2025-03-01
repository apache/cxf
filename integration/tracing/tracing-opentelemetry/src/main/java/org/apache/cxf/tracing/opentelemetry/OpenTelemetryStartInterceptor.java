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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;

@NoJSR250Annotations
public class OpenTelemetryStartInterceptor extends AbstractOpenTelemetryInterceptor {
    public OpenTelemetryStartInterceptor(OpenTelemetry openTelemetry, String instrumentationName) {
        super(Phase.PRE_INVOKE, openTelemetry, instrumentationName);
    }

    public OpenTelemetryStartInterceptor(OpenTelemetry openTelemetry, Tracer tracer) {
        super(Phase.PRE_INVOKE, openTelemetry, tracer);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        final Map<String, List<String>> headers = CastUtils
            .cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        final TraceScopeHolder<TraceScope> holder = super.startTraceSpan(headers, getUri(message),
             (String)message.get(Message.HTTP_REQUEST_METHOD));

        if (holder != null) {
            message.getExchange().put(TRACE_SPAN, holder);
        }

        HttpServletRequest request = (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (request != null) {
            Span span = holder.getScope().getSpan();
            span.setAttribute(ClientAttributes.CLIENT_ADDRESS, request.getRemoteAddr());
            span.setAttribute(ClientAttributes.CLIENT_PORT, request.getRemotePort());
            span.setAttribute(NetworkAttributes.NETWORK_PEER_ADDRESS, request.getRemoteAddr());
            span.setAttribute(NetworkAttributes.NETWORK_PEER_PORT, request.getRemotePort());
            String protocol = request.getProtocol();
            if (protocol != null && protocol.contains("/")) {
                String protocolVersion = protocol.split("/")[1];
                span.setAttribute(NetworkAttributes.NETWORK_PROTOCOL_VERSION, protocolVersion);
            }

            final String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                span.setAttribute(UserAgentAttributes.USER_AGENT_ORIGINAL, userAgent);
            }
        }
    }
}
