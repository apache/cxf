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

import com.github.kristofa.brave.Brave;
import com.twitter.zipkin.gen.Span;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

public class BraveClientStopInterceptor extends AbstractBraveClientInterceptor {
    public BraveClientStopInterceptor(final Brave brave) {
        this(Phase.RECEIVE, brave);
    }
    
    public BraveClientStopInterceptor(final String phase, final Brave brave) {
        super(phase, brave);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        @SuppressWarnings("unchecked")
        final TraceScopeHolder<Span> holder = 
            (TraceScopeHolder<Span>)message.getExchange().get(TRACE_SPAN);
        
        Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
        if (responseCode == null) {
            responseCode = 200;
        }
        
        super.stopTraceSpan(holder, responseCode);
    }
}
