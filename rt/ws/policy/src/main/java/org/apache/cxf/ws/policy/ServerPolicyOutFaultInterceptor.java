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
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

/**
 * 
 */
public class ServerPolicyOutFaultInterceptor extends AbstractPolicyInterceptor {
    public static final ServerPolicyOutFaultInterceptor INSTANCE = new ServerPolicyOutFaultInterceptor();
    private static final Logger LOG = LogUtils.getL7dLogger(ServerPolicyOutFaultInterceptor.class);
    
    public ServerPolicyOutFaultInterceptor() {
        super(PolicyConstants.SERVER_POLICY_OUT_FAULT_INTERCEPTOR_ID, Phase.SETUP);
    }
       
    protected void handle(Message msg) {        
        if (MessageUtils.isRequestor(msg)) {
            LOG.fine("Is a requestor.");
            return;
        }
        
        Exchange exchange = msg.getExchange();
        assert null != exchange;
        
        BindingOperationInfo boi = exchange.get(BindingOperationInfo.class);
        if (null == boi) {
            LOG.fine("No binding operation info.");
            return;
        }
        
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
        
        Destination destination = exchange.getDestination();
        
        Exception ex = exchange.get(Exception.class);
        
        List<Interceptor<? extends Message>> faultInterceptors = 
            new ArrayList<Interceptor<? extends Message>>();
        Collection<Assertion> assertions = new ArrayList<Assertion>();

        // 1. Check overridden policy
        Policy p = (Policy)msg.getContextualProperty(PolicyConstants.POLICY_OVERRIDE);
        if (p != null) {
            EndpointPolicyImpl endpi = new EndpointPolicyImpl(p);
            EffectivePolicyImpl effectivePolicy = new EffectivePolicyImpl();
            effectivePolicy.initialise(endpi, (PolicyEngineImpl)pe, false, true);
            PolicyUtils.logPolicy(LOG, Level.FINEST, "Using effective policy: ", 
                                  effectivePolicy.getPolicy());
            
            faultInterceptors.addAll(effectivePolicy.getInterceptors());
            assertions.addAll(effectivePolicy.getChosenAlternative());
        } else {
            // 2. Process effective server policy
            BindingFaultInfo bfi = getBindingFaultInfo(msg, ex, boi);

            if (bfi == null 
                && msg.get(FaultMode.class) != FaultMode.UNCHECKED_APPLICATION_FAULT
                && msg.get(FaultMode.class) != FaultMode.CHECKED_APPLICATION_FAULT) {
                return;
            }
            
            EffectivePolicy effectivePolicy = pe.getEffectiveServerFaultPolicy(ei, boi, bfi, destination);
            if (effectivePolicy != null) {
                faultInterceptors.addAll(effectivePolicy.getInterceptors());
                assertions.addAll(effectivePolicy.getChosenAlternative());            
            }
        }
        
        // add interceptors into message chain
        for (Interceptor<? extends Message> oi : faultInterceptors) {
            msg.getInterceptorChain().add(oi);
            LOG.log(Level.FINE, "Added interceptor of type {0}", oi.getClass().getSimpleName());
        }
        
        // insert assertions of the chosen alternative into the message
        if (null != assertions && !assertions.isEmpty()) {
            msg.put(AssertionInfoMap.class, new AssertionInfoMap(assertions));
        }
    }
}
