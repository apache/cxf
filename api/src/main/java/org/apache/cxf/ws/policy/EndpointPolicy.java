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

import org.apache.cxf.interceptor.Interceptor;
import org.apache.neethi.Policy;

/**
 * Describes the policy of an inbound message. As the underlying message
 * type is not known, only the effective endpoint policy is calculated. 
 * The total of all assertions that may apply to an inbound message for an endpoint
 * (and hence the required interceptors) are available as vocabulary.
 */
public interface EndpointPolicy {
    
    Policy getPolicy();
    EndpointPolicy updatePolicy(Policy p);
    
    Collection<PolicyAssertion> getChosenAlternative();
    
    Collection<PolicyAssertion> getVocabulary();
    
    Collection<PolicyAssertion> getFaultVocabulary();
    
    List<Interceptor> getInterceptors();
    
    List<Interceptor> getFaultInterceptors();
}
