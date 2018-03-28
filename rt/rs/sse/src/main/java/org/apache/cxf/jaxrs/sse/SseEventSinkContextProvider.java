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


package org.apache.cxf.jaxrs.sse;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public class SseEventSinkContextProvider implements ContextProvider<SseEventSink> {

    @Override
    public SseEventSink createContext(Message message) {
        final HttpServletRequest request = (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (request == null) {
            throw new IllegalStateException("Unable to retrieve HTTP request from the context");
        }

        final MessageBodyWriter<OutboundSseEvent> writer = new OutboundSseEventBodyWriter(
            ServerProviderFactory.getInstance(message), message.getExchange());

        AsyncContext ctx = request.startAsync();
        ctx.setTimeout(0);

        message.getInterceptorChain().add(new SuspendPhaseInterceptor());

        return new SseEventSinkImpl(writer, ctx);
    }

    private static class SuspendPhaseInterceptor
        implements PhaseInterceptor<Message> {

        @Override
        public Set<String> getAfter() {
            return Collections.emptySet();
        }

        @Override
        public Set<String> getBefore() {
            return Collections.singleton(
                "org.apache.cxf.interceptor.OutgoingChainInterceptor");
        }

        @Override
        public String getId() {
            return "SSE SUSPEND";
        }

        @Override
        public String getPhase() {
            return Phase.POST_INVOKE;
        }

        @Override
        public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
            return Collections.emptySet();
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            message.getInterceptorChain().suspend();
        }

        @Override
        public void handleFault(Message message) {
        }

    }
}