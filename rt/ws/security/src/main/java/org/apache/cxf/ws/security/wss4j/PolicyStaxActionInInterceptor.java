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
package org.apache.cxf.ws.security.wss4j;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.stax.securityEvent.OperationSecurityEvent;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants.Event;

/**
 * This interceptor marks the CXF AssertionInfos as asserted. WSS4J 2.0 (StAX) takes care of all
 * policy validation, so we are just asserting the appropriate AssertionInfo objects in CXF to 
 * make sure that policy validation passes.
 */
public class PolicyStaxActionInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    
    private static final Logger LOG = 
        LogUtils.getL7dLogger(PolicyStaxActionInInterceptor.class);
    
    public PolicyStaxActionInInterceptor() {
        super(Phase.PRE_PROTOCOL);
        this.getBefore().add(StaxSecurityContextInInterceptor.class.getName());
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {
        
        AssertionInfoMap aim = soapMessage.get(AssertionInfoMap.class);
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingSecurityEventList = 
            (List<SecurityEvent>)soapMessage.get(SecurityEvent.class.getName() + ".in");
        if (aim == null || incomingSecurityEventList == null) {
            return;
        }
        
        // First check for a SOAP Fault with no security header if we are the client
        // In this case don't blanket assert security policies
        if (MessageUtils.isRequestor(soapMessage)
            && isEventInResults(WSSecurityEventConstants.NoSecurity, incomingSecurityEventList)) {
            OperationSecurityEvent securityEvent = 
                (OperationSecurityEvent)findEvent(
                    WSSecurityEventConstants.Operation, incomingSecurityEventList
                );
            if (securityEvent != null 
                && soapMessage.getVersion().getFault().equals(securityEvent.getOperation())) {
                LOG.warning("Request does not contain Security header, but it's a fault.");
                return;
            }
        }
        
        assertAllSecurityAssertions(aim);
        assertAllAlgorithmSuites(SP11Constants.SP_NS, aim);
        assertAllAlgorithmSuites(SP12Constants.SP_NS, aim);
    }
    
    private boolean isEventInResults(Event event, List<SecurityEvent> incomingSecurityEventList) {
        for (SecurityEvent incomingEvent : incomingSecurityEventList) {
            if (event == incomingEvent.getSecurityEventType()) {
                return true;
            }
        }
        return false;
    }
    
    private SecurityEvent findEvent(Event event, List<SecurityEvent> incomingSecurityEventList) {
        for (SecurityEvent incomingEvent : incomingSecurityEventList) {
            if (event == incomingEvent.getSecurityEventType()) {
                return incomingEvent;
            }
        }
        return null;
    }
    
    private void assertAllSecurityAssertions(AssertionInfoMap aim) {
        for (QName key : aim.keySet()) {
            if (SP11Constants.SP_NS.equals(key.getNamespaceURI())
                || SP12Constants.SP_NS.equals(key.getNamespaceURI())
                || SP13Constants.SP_NS.equals(key.getNamespaceURI())) {
                Collection<AssertionInfo> ais = aim.get(key);
                if (ais != null && !ais.isEmpty()) {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                }
            }
        }
        
    }
    
    private void assertAllAlgorithmSuites(String spNamespace, AssertionInfoMap aim) {
        Collection<AssertionInfo> sp11Ais = 
            aim.get(new QName(spNamespace, SPConstants.ALGORITHM_SUITE));
        if (sp11Ais != null) {
            for (AssertionInfo ai : sp11Ais) {
                ai.setAsserted(true);
                AlgorithmSuite algorithmSuite = (AlgorithmSuite)ai.getAssertion();
                AlgorithmSuiteType algorithmSuiteType = algorithmSuite.getAlgorithmSuiteType();
                String namespace = algorithmSuiteType.getNamespace();
                if (namespace == null) {
                    namespace = spNamespace;
                }
                Collection<AssertionInfo> algAis = 
                    aim.get(new QName(namespace, algorithmSuiteType.getName()));
                if (algAis != null) {
                    for (AssertionInfo algAi : algAis) {
                        algAi.setAsserted(true);
                    }
                }
            }
        }
    }


}
