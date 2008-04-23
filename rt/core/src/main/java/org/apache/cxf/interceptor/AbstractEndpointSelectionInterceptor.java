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
package org.apache.cxf.interceptor;

import java.util.Set;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.MultipleEndpointObserver;

public abstract class AbstractEndpointSelectionInterceptor extends AbstractPhaseInterceptor<Message> {
    
    /**
     * @deprecated
     */
    public AbstractEndpointSelectionInterceptor() {
        super(null);
    }
    
    
    public AbstractEndpointSelectionInterceptor(String phase) {
        super(phase);
    }
    public AbstractEndpointSelectionInterceptor(String id, String phase) {
        super(id, phase);
    }

    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        Set<Endpoint> endpoints = CastUtils.cast((Set)ex.get(MultipleEndpointObserver.ENDPOINTS));

        Endpoint ep = selectEndpoint(message, endpoints);

        if (ep == null) {
            return;
        }

        ex.put(Endpoint.class, ep);
        ex.put(Binding.class, ep.getBinding());
        ex.put(Service.class, ep.getService());

        InterceptorChain chain = message.getInterceptorChain();
        chain.add(ep.getInInterceptors());
        chain.add(ep.getBinding().getInInterceptors());
        chain.add(ep.getService().getInInterceptors());

        chain.setFaultObserver(ep.getOutFaultObserver());
    }

    /**
     * Select an Endpoint which will be used for the rest of the invocation.
     * 
     * @param message
     * @param eps
     * @return
     */
    protected abstract Endpoint selectEndpoint(Message message, Set<Endpoint> eps);
}
