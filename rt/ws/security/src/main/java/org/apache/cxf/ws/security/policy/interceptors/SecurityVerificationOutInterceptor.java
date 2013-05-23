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
            if (aim != null) {
                Collection<AssertionInfo> aisTransport = 
                    NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING);
                Collection<AssertionInfo> aisAssymetric = 
                    NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
                Collection<AssertionInfo> aisSymetric = 
                    NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
                
                if (aisTransport.isEmpty() && aisAssymetric.isEmpty() && aisSymetric.isEmpty()) {
                    
                    Collection<AssertionInfo> aisSignedParts = 
                        NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_PARTS);
                    checkAssertion(aisSignedParts, SPConstants.SIGNED_PARTS);
                    
                    Collection<AssertionInfo> aisSignedElements = aim.get(SP12Constants.SIGNED_ELEMENTS);
                    checkAssertion(aisSignedElements, SPConstants.SIGNED_ELEMENTS);
                    
                    Collection<AssertionInfo> aisEncryptedParts = aim.get(SP12Constants.ENCRYPTED_PARTS);
                    checkAssertion(aisEncryptedParts, SPConstants.ENCRYPTED_PARTS);
                    
                    Collection<AssertionInfo> aisEncryptedElements = 
                        aim.get(SP12Constants.ENCRYPTED_ELEMENTS);
                    checkAssertion(aisEncryptedElements, SPConstants.ENCRYPTED_ELEMENTS);
                    
                    Collection<AssertionInfo> aisContentEncryptedElements = 
                        aim.get(SP12Constants.CONTENT_ENCRYPTED_ELEMENTS);
                    checkAssertion(aisContentEncryptedElements, SPConstants.CONTENT_ENCRYPTED_ELEMENTS);
                }
            }
        }
    }

    private void checkAssertion(Collection<AssertionInfo> ais, String assertionName) {
        if ((ais != null) && (!ais.isEmpty())) {
            String error = String
                .format("%s assertion cannot be fulfilled without binding. "
                        + "At least one binding assertion (%s, %s, %s) must be specified in policy.",
                        assertionName, SP12Constants.TRANSPORT_BINDING.getLocalPart(),
                        SP12Constants.ASYMMETRIC_BINDING.getLocalPart(),
                        SP12Constants.SYMMETRIC_BINDING.getLocalPart());
            AssertionInfo info = ais.iterator().next();
            info.setNotAsserted(error);
            LOG.severe(error);
            throw new PolicyException(info);
        }
    }
}
