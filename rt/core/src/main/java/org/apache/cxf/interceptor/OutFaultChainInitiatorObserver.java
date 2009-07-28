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

import java.util.SortedSet;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;

public class OutFaultChainInitiatorObserver extends AbstractFaultChainInitiatorObserver {

    public OutFaultChainInitiatorObserver(Bus bus) {
        super(bus);
    }

    protected void initializeInterceptors(Exchange ex, PhaseInterceptorChain chain) {
        Endpoint e = ex.get(Endpoint.class);
        Client c = ex.get(Client.class);

        chain.add(getBus().getOutFaultInterceptors());
        if (c != null) {
            chain.add(c.getOutFaultInterceptors());
        }
        chain.add(e.getService().getOutFaultInterceptors());
        chain.add(e.getOutFaultInterceptors());
        chain.add(e.getBinding().getOutFaultInterceptors());
        if (e.getService().getDataBinding() instanceof InterceptorProvider) {
            chain.add(((InterceptorProvider)e.getService().getDataBinding()).getOutFaultInterceptors());
        }
    }
    
    protected SortedSet<Phase> getPhases() {
        return getBus().getExtension(PhaseManager.class).getOutPhases();
    }

    protected boolean isOutboundObserver() {
        return true;
    }
}