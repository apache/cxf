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

import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;

/**
 * 
 */
public class PolicyVerificationInFaultInterceptor extends AbstractPolicyInterceptor {
    public static final PolicyVerificationInFaultInterceptor INSTANCE 
        = new PolicyVerificationInFaultInterceptor();
    private static final Logger LOG 
        = LogUtils.getL7dLogger(PolicyVerificationInFaultInterceptor.class);

    public PolicyVerificationInFaultInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    /** 
     * Determines the effective policy, and checks if one of its alternatives  
     * is supported.
     *  
     * @param message
     * @throws PolicyException if none of the alternatives is supported
     */
    protected void handle(Message message) {
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (null == aim) {
            return;
        }        
        
        if (!MessageUtils.isRequestor(message)) {
            LOG.fine("Not a requestor.");
            return; 
        }
        
        Exchange exchange = message.getExchange();
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
        
        Exception ex = message.getContent(Exception.class);
        if (null == ex) {
            ex = exchange.get(Exception.class);
        }
        assert null != ex;
        
        BindingFaultInfo bfi = getBindingFaultInfo(message, ex, boi);
        if (null == bfi) {
            LOG.fine("No binding fault info.");
            return;
        }
        
        getTransportAssertions(message);
        
        EffectivePolicy effectivePolicy = pe.getEffectiveClientFaultPolicy(ei, bfi);
        aim.checkEffectivePolicy(effectivePolicy.getPolicy());
        LOG.fine("Verified policies for inbound message.");
    }

}
