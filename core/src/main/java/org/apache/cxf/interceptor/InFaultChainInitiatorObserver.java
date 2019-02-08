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

import java.util.Collection;
import java.util.SortedSet;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;

public class InFaultChainInitiatorObserver extends AbstractFaultChainInitiatorObserver {

    public InFaultChainInitiatorObserver(Bus bus) {
        super(bus);
    }

    protected void initializeInterceptors(Exchange ex, PhaseInterceptorChain chain) {
        Endpoint e = ex.getEndpoint();
        Client c = ex.get(Client.class);
        InterceptorProvider ip = ex.get(InterceptorProvider.class);

        chain.add(getBus().getInFaultInterceptors());
        if (c != null) {
            chain.add(c.getInFaultInterceptors());
        } else if (ip != null) {
            chain.add(ip.getInFaultInterceptors());
        }
        chain.add(e.getService().getInFaultInterceptors());
        chain.add(e.getInFaultInterceptors());
        chain.add(e.getBinding().getInFaultInterceptors());
        if (e.getService().getDataBinding() instanceof InterceptorProvider) {
            chain.add(((InterceptorProvider)e.getService().getDataBinding()).getInFaultInterceptors());
        }

        addToChain(chain, ex.getInFaultMessage());
        addToChain(chain, ex.getOutMessage());
    }
    private void addToChain(PhaseInterceptorChain chain, Message m) {
        Collection<InterceptorProvider> providers
            = CastUtils.cast((Collection<?>)m.get(Message.INTERCEPTOR_PROVIDERS));
        if (providers != null) {
            for (InterceptorProvider p : providers) {
                chain.add(p.getInFaultInterceptors());
            }
        }
        Collection<Interceptor<? extends Message>> is
            = CastUtils.cast((Collection<?>)m.get(Message.FAULT_IN_INTERCEPTORS));
        if (is != null) {
            chain.add(is);
        }
    }

    protected SortedSet<Phase> getPhases() {
        return getBus().getExtension(PhaseManager.class).getInPhases();
    }

    protected boolean isOutboundObserver() {
        return false;
    }
}
