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

import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.neethi.PolicyRegistry;

/**
 * 
 */
public interface PolicyEngine {
    
    // configuration

    boolean isEnabled();
    
    void setEnabled(boolean e);
    
    AlternativeSelector getAlternativeSelector();
    
    void setAlternativeSelector(AlternativeSelector selector);
   
    boolean isIgnoreUnknownAssertions();
    
    void setIgnoreUnknownAssertions(boolean ignoreUnknownAssertions);
    
    //
    
    boolean supportsAlternative(Collection<PolicyAssertion> alterative, Assertor assertor);
    
    // available throughout the outbound path
    
    EffectivePolicy getEffectiveClientRequestPolicy(EndpointInfo ei, BindingOperationInfo boi, Conduit c);
    
    void setEffectiveClientRequestPolicy(EndpointInfo ei, BindingOperationInfo boi, EffectivePolicy ep);
    
    EffectivePolicy getEffectiveServerResponsePolicy(EndpointInfo ei, BindingOperationInfo boi, 
                                                     Destination d);
    
    void setEffectiveServerResponsePolicy(EndpointInfo ei, BindingOperationInfo boi, EffectivePolicy ep);
    
    EffectivePolicy getEffectiveServerFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi, Destination d);
    
    void setEffectiveServerFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi, EffectivePolicy ep); 
    
    // available throughout the inbound path
        
    EndpointPolicy getClientEndpointPolicy(EndpointInfo ei, Conduit conduit);
    EndpointPolicy getServerEndpointPolicy(EndpointInfo ei, Destination destination);
    
    void setServerEndpointPolicy(EndpointInfo ei, EndpointPolicy ep);
    void setClientEndpointPolicy(EndpointInfo ei, EndpointPolicy ep);
    
    // only available after message type has been determined
    
    EffectivePolicy getEffectiveServerRequestPolicy(EndpointInfo ei, BindingOperationInfo boi);
    
    void setEffectiveServerRequestPolicy(EndpointInfo ei, BindingOperationInfo boi, EffectivePolicy ep);
    
    EffectivePolicy getEffectiveClientResponsePolicy(EndpointInfo ei, BindingOperationInfo boi);
    
    void setEffectiveClientResponsePolicy(EndpointInfo ei, BindingOperationInfo boi, EffectivePolicy ep);
    
    EffectivePolicy getEffectiveClientFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi);
    
    void setEffectiveClientFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi, EffectivePolicy ep);
    
    
    
    PolicyRegistry getRegistry();
    
}
