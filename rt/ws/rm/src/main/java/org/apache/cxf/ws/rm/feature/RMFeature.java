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

package org.apache.cxf.ws.rm.feature;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.ws.rm.RMInInterceptor;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMOutInterceptor;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.policy.RMAssertion;
import org.apache.cxf.ws.rm.soap.RMSoapInterceptor;

/**
 * 
 */
public class RMFeature extends AbstractFeature {

    private RMAssertion rmAssertion;
    private DeliveryAssuranceType deliveryAssurance;
    private SourcePolicyType sourcePolicy;
    private DestinationPolicyType destinationPolicy;
    private RMStore store;

    private RMInInterceptor rmLogicalIn = new RMInInterceptor();
    private RMOutInterceptor rmLogicalOut = new RMOutInterceptor();
    private RMSoapInterceptor rmCodec = new RMSoapInterceptor();

    public void setDeliveryAssurance(DeliveryAssuranceType da) {
        deliveryAssurance = da;
    }

    public void setDestinationPolicy(DestinationPolicyType dp) {
        destinationPolicy = dp;
    }

    public void setRMAssertion(RMAssertion rma) {
        rmAssertion = rma;
    }

    public void setSourcePolicy(SourcePolicyType sp) {
        sourcePolicy = sp;
    }

    public void setStore(RMStore store) {
        this.store = store;
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {

        RMManager manager = bus.getExtension(RMManager.class);
        if (null != rmAssertion) {
            manager.setRMAssertion(rmAssertion);
        }
        if (null != deliveryAssurance) {
            manager.setDeliveryAssurance(deliveryAssurance);
        }
        if (null != sourcePolicy) {
            manager.setSourcePolicy(sourcePolicy);
        }
        if (null != destinationPolicy) {
            manager.setDestinationPolicy(destinationPolicy);
        }
        if (null != store) {
            manager.setStore(store);
        }

        rmLogicalIn.setBus(bus);
        rmLogicalOut.setBus(bus);

        provider.getInInterceptors().add(rmLogicalIn);
        provider.getInInterceptors().add(rmCodec);

        provider.getOutInterceptors().add(rmLogicalOut);
        provider.getOutInterceptors().add(rmCodec);

        provider.getInFaultInterceptors().add(rmLogicalIn);
        provider.getInFaultInterceptors().add(rmCodec);

        provider.getOutFaultInterceptors().add(rmLogicalOut);
        provider.getOutFaultInterceptors().add(rmCodec);

    }
}
