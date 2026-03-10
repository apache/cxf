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

import brave.http.HttpTracing;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.tracing.brave.AbstractBraveInterceptor.ParsedMessage;

public class BraveClientStartInterceptor extends AbstractBraveClientInterceptor {
    public BraveClientStartInterceptor(final HttpTracing brave) {
        this(Phase.PRE_STREAM, brave);
    }

    public BraveClientStartInterceptor(final String phase, final HttpTracing brave) {
        super(phase, brave);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        final ParsedMessage parsed = new ParsedMessage(message);

        final TraceScopeHolder<TraceScope> holder = super.startTraceSpan(parsed.getHeaders(),
            parsed.getUri(), parsed.getHttpMethod());

        if (holder != null) {
            message.getExchange().put(TRACE_SPAN, holder);
        }
    }
    
    @Override
    public void handleFault(Message message) {
        @SuppressWarnings("unchecked")
        final TraceScopeHolder<TraceScope> holder =
            (TraceScopeHolder<TraceScope>)message.getExchange().get(TRACE_SPAN);
        
        final Exception ex = message.getContent(Exception.class);
        super.stopTraceSpan(holder, (String) message.get(Message.HTTP_REQUEST_METHOD), getUri(message), ex);
    }
}
