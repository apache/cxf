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
package org.apache.cxf.tracing.opentracing;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import io.opentracing.Tracer;

public class OpenTracingClientStopInterceptor extends AbstractOpenTracingClientInterceptor {
    public OpenTracingClientStopInterceptor(final Tracer tracer) {
        this(Phase.RECEIVE, tracer);
    }

    public OpenTracingClientStopInterceptor(final String phase, final Tracer tracer) {
        super(phase, tracer);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        @SuppressWarnings("unchecked")
        final TraceScopeHolder<TraceScope> holder =
            (TraceScopeHolder<TraceScope>)message.getExchange().get(TRACE_SPAN);

        Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
        if (responseCode == null) {
            responseCode = 200;
        }

        super.stopTraceSpan(holder, responseCode);
    }
}
