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

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;

/**
 * 
 */
public class ClientPolicyInFaultInterceptor extends AbstractPolicyInterceptor {

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
        
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        if (null == pe) {
            return;
        }
        
        Conduit conduit = exchange.getConduit(msg);
        LOG.fine("conduit: " + conduit);
        
        // We do not know the underlying message type yet - so we pre-emptively add interceptors 
        // that can deal with all faults returned to this client endpoint.
        
        EndpointPolicy ep = pe.getClientEndpointPolicy(ei, conduit);        
        LOG.fine("ep: " + ep);
        
        List<Interceptor> faultInterceptors = ep.getFaultInterceptors();
        LOG.fine("faultInterceptors: " + faultInterceptors);
        for (Interceptor i : faultInterceptors) {
            msg.getInterceptorChain().add(i);
            LOG.log(Level.INFO, "Added interceptor of type {0}", i.getClass().getSimpleName());
        }
        
        // insert assertions of endpoint's fault vocabulary into message
        
        Collection<PolicyAssertion> assertions = ep.getFaultVocabulary();
        if (null != assertions) {
            msg.put(AssertionInfoMap.class, new AssertionInfoMap(assertions));
        }
    }
}
