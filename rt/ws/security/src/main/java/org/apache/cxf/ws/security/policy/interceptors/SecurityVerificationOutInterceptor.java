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

package org.apache.cxf.ws.security.policy.interceptors;

import java.util.Collection;
import java.util.logging.Logger;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;

/**
 * Interceptor verifies critical policy security assertions for client side
 */
public class SecurityVerificationOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    public static final SecurityVerificationOutInterceptor INSTANCE = 
        new SecurityVerificationOutInterceptor();

    private static final Logger LOG = LogUtils.getL7dLogger(SecurityVerificationOutInterceptor.class);

    public SecurityVerificationOutInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    /**
     * Checks if some security assertions are specified without binding assertion and cannot be fulfilled.
     * Throw PolicyException in this case
     * 
     * @param message
     * @throws PolicyException if assertions are specified without binding
     */
    public void handleMessage(SoapMessage message) throws Fault {
        if (MessageUtils.isRequestor(message)) {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            if (aim != null && !isThereASecurityBinding(aim)) {
                AssertionInfo assertion = getSecuredPart(aim);
                if (assertion != null) {
                    String error = String
                        .format("%s assertion cannot be fulfilled without binding. "
                            + "At least one binding assertion (%s, %s, %s) must be specified in policy.",
                            assertion.getAssertion().getName(),
                            SP12Constants.TRANSPORT_BINDING.getLocalPart(),
                            SP12Constants.ASYMMETRIC_BINDING.getLocalPart(),
                            SP12Constants.SYMMETRIC_BINDING.getLocalPart());
                    assertion.setNotAsserted(error);
                    LOG.severe(error);
                    throw new PolicyException(assertion);
                }
            }
        }
    }
    
    private boolean isThereASecurityBinding(AssertionInfoMap aim) {
        return 
            PolicyUtils.isThereAnAssertionByLocalname(aim, SPConstants.TRANSPORT_BINDING)
            || PolicyUtils.isThereAnAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING)
            || PolicyUtils.isThereAnAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
    }
    
    private AssertionInfo getSecuredPart(AssertionInfoMap aim) {
        Collection<AssertionInfo> assertions = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_PARTS);
        if (!assertions.isEmpty()) {
            return assertions.iterator().next();
        }
        
        assertions = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_ELEMENTS);
        if (!assertions.isEmpty()) {
            return assertions.iterator().next();
        }
        
        assertions = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_PARTS);
        if (!assertions.isEmpty()) {
            return assertions.iterator().next();
        }
        
        assertions = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_ELEMENTS);
        if (!assertions.isEmpty()) {
            return assertions.iterator().next();
        }
        
        assertions = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.CONTENT_ENCRYPTED_ELEMENTS);
        if (!assertions.isEmpty()) {
            return assertions.iterator().next();
        }
        
        return null;
    }

}
