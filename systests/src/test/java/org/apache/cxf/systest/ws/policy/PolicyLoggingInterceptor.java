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

package org.apache.cxf.systest.ws.policy;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.policy.PolicyUtils;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;

public class PolicyLoggingInterceptor extends AbstractPhaseInterceptor {

    private static final Logger LOG = LogUtils.getLogger(PolicyLoggingInterceptor.class);
    
    private Bus bus;
    
    PolicyLoggingInterceptor(boolean o) {
        super(o ? Phase.POST_STREAM : Phase.POST_INVOKE);
    }
    
    public void setBus(Bus b) {
        bus = b;
    }
    
    public void handleMessage(Message message) throws Fault {
        EndpointInfo ei = message.getExchange().get(Endpoint.class).getEndpointInfo();
        BindingOperationInfo boi = message.getExchange().get(BindingOperationInfo.class);
        LOG.fine("Getting effective server request policy for endpoint " + ei
                 + " and binding operation " + boi);
        EffectivePolicy ep = 
            bus.getExtension(PolicyEngine.class).getEffectiveServerRequestPolicy(ei, boi);                
        for (Iterator it = ep.getPolicy().getAlternatives(); it.hasNext();) {
            Collection<PolicyAssertion> as = 
                CastUtils.cast((Collection)it.next(), PolicyAssertion.class);
            LOG.fine("Checking alternative with " + as.size() + " assertions.");
            for (PolicyAssertion a : as) {
                LOG.fine("Assertion: " + a.getClass().getName());
                HTTPServerPolicy p = (JaxbAssertion.cast(a, HTTPServerPolicy.class)).getData(); 
                LOG.fine("server policy: " + PolicyUtils.toString(p));
            }
        }

    }

}
