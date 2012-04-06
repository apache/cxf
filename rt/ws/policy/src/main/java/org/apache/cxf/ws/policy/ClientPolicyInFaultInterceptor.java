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

package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

/**
 * 
 */
public class ClientPolicyInFaultInterceptor extends AbstractPolicyInterceptor {
    public static final ClientPolicyInFaultInterceptor INSTANCE = new ClientPolicyInFaultInterceptor();
    
    private static final Logger LOG = LogUtils.getL7dLogger(ClientPolicyInFaultInterceptor.class);
    
    public ClientPolicyInFaultInterceptor() {
        super(PolicyConstants.CLIENT_POLICY_IN_FAULT_INTERCEPTOR_ID, Phase.RECEIVE);
    }
    
    protected void handle(Message msg) {        
        if (!MessageUtils.isRequestor(msg)) {
            LOG.fine("Not a requestor.");
            return;
        }
        
        Exchange exchange = msg.getExchange();
        assert null != exchange;
        
        Endpoint e = exchange.get(Endpoint.class);
        if (null == e) {
            LOG.fine("No endpoint.");
            return;
        }
        EndpointInfo ei = e.getEndpointInfo();
        
        Bus bus = exchange.get(Bus.class);
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        if (null == pe) {
            return;
        }
        
        Conduit conduit = exchange.getConduit(msg);
        LOG.fine("conduit: " + conduit);

        List<Interceptor<? extends Message>> faultInterceptors = 
            new ArrayList<Interceptor<? extends Message>>();
        Collection<Assertion> assertions = new ArrayList<Assertion>();
        
        // 1. Check overridden policy
        Policy p = (Policy)msg.getContextualProperty(PolicyConstants.POLICY_OVERRIDE);
        if (p != null) {
            EndpointPolicyImpl endpi = new EndpointPolicyImpl(p);
            EffectivePolicyImpl effectivePolicy = new EffectivePolicyImpl();
            effectivePolicy.initialise(endpi, (PolicyEngineImpl)pe, true, true);
            PolicyUtils.logPolicy(LOG, Level.FINEST, "Using effective policy: ", 
                                  effectivePolicy.getPolicy());
            
            faultInterceptors.addAll(effectivePolicy.getInterceptors());
            assertions.addAll(effectivePolicy.getChosenAlternative());
        } else {
            // 2. Process endpoint policy
            // We do not know the underlying message type yet - so we pre-emptively add interceptors 
            // that can deal with all faults returned to this client endpoint.
            
            EndpointPolicy ep = pe.getClientEndpointPolicy(ei, conduit);        
            LOG.fine("ep: " + ep);
            if (ep != null) {
                faultInterceptors.addAll(ep.getFaultInterceptors());
                assertions.addAll(ep.getFaultVocabulary());
            }
        }
        
        // add interceptors into message chain
        LOG.fine("faultInterceptors: " + faultInterceptors);
        for (Interceptor<? extends Message> i : faultInterceptors) {
            msg.getInterceptorChain().add(i);
            LOG.log(Level.FINE, "Added interceptor of type {0}", i.getClass().getSimpleName());
        }
        
        // insert assertions of endpoint's fault vocabulary into message        
        if (!assertions.isEmpty()) {
            msg.put(AssertionInfoMap.class, new AssertionInfoMap(assertions));
        }
    }
}
