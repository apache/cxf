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
package org.apache.cxf.ws.addressing;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.ws.addressing.soap.MAPCodec;

public class WSAddressingFeature extends AbstractFeature {

    private MAPAggregator mapAggregator = new MAPAggregator();
    private MAPCodec mapCodec = new MAPCodec();
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        provider.getInInterceptors().add(mapAggregator);
        provider.getInInterceptors().add(mapCodec);
        
        provider.getOutInterceptors().add(mapAggregator);
        provider.getOutInterceptors().add(mapCodec);
        
        provider.getInFaultInterceptors().add(mapAggregator);
        provider.getInFaultInterceptors().add(mapCodec);
        
        provider.getOutFaultInterceptors().add(mapAggregator);
        provider.getOutFaultInterceptors().add(mapCodec);
    }

    public void setAllowDuplicates(boolean allow) {
        mapAggregator.setAllowDuplicates(allow);
    }
    
    public boolean isAllowDuplicates() {
        return mapAggregator.allowDuplicates();
    }

    public void setUsingAddressingAdvisory(boolean advisory) {
        mapAggregator.setUsingAddressingAdvisory(advisory);
    }
    
    public boolean isUsingAddressingAdvisory() {
        return mapAggregator.isUsingAddressingAdvisory();
    }
    

    public boolean isAddressingRequired() {
        return mapAggregator.isAddressingRequired();
    }
    public void setAddressingRequired(boolean required) {
        mapAggregator.setAddressingRequired(required);
    }
    
    
}
