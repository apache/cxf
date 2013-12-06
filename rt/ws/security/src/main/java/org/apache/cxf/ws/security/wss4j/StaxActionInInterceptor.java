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

import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants.Event;

/**
 * This interceptor handles parsing the StaX WS-Security results (events) + checks to see
 * whether the required Actions were fulfilled. If no Actions were defined in the configuration,
 * then no checking is done on the received security events.
 */
public class StaxActionInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    
    private static final Logger LOG = 
        LogUtils.getL7dLogger(StaxActionInInterceptor.class);
                                                            
    private final List<XMLSecurityConstants.Action> inActions;
    
    public StaxActionInInterceptor(List<XMLSecurityConstants.Action> inActions) {
        super(Phase.PRE_PROTOCOL);
        this.inActions = inActions;
        this.getBefore().add(StaxSecurityContextInInterceptor.class.getName());
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {
        
        if (inActions == null || inActions.size() == 0) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingSecurityEventList = 
            (List<SecurityEvent>)soapMessage.get(SecurityEvent.class.getName() + ".in");

        if (incomingSecurityEventList == null) {
            LOG.warning("Security processing failed (actions mismatch)");
            WSSecurityException ex = 
                new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
            throw createSoapFault(soapMessage.getVersion(), ex);
        }
        
        for (XMLSecurityConstants.Action action : inActions) {
            Event requiredEvent = null;
            if (WSSConstants.TIMESTAMP.equals(action)) {
                requiredEvent = WSSecurityEventConstants.Timestamp;
            } else if (WSSConstants.USERNAMETOKEN.equals(action)) {
                requiredEvent = WSSecurityEventConstants.UsernameToken;
            } else if (WSSConstants.SIGNATURE.equals(action)) {
                requiredEvent = WSSecurityEventConstants.SignatureValue;
            } else if (WSSConstants.SAML_TOKEN_SIGNED.equals(action)
                || WSSConstants.SAML_TOKEN_UNSIGNED.equals(action)) {
                requiredEvent = WSSecurityEventConstants.SamlToken;
            }
            
            if (requiredEvent != null 
                && !isEventInResults(requiredEvent, incomingSecurityEventList)) {
                LOG.warning("Security processing failed (actions mismatch)");
                WSSecurityException ex = 
                    new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
                throw createSoapFault(soapMessage.getVersion(), ex);
            }
            
            if (WSSConstants.ENCRYPT.equals(action)) {
                boolean foundEncryptionPart = 
                    isEventInResults(WSSecurityEventConstants.EncryptedPart, incomingSecurityEventList);
                if (!foundEncryptionPart) {
                    foundEncryptionPart =
                        isEventInResults(WSSecurityEventConstants.EncryptedElement, incomingSecurityEventList);
                }
                if (!foundEncryptionPart) {
                    LOG.warning("Security processing failed (actions mismatch)");
                    WSSecurityException ex = 
                        new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
                    throw createSoapFault(soapMessage.getVersion(), ex);
                }
            } 
        }
    }
    
    private boolean isEventInResults(Event event, List<SecurityEvent> incomingSecurityEventList) {
        for (SecurityEvent incomingEvent : incomingSecurityEventList) {
            if (event == incomingEvent.getSecurityEventType()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Create a SoapFault from a WSSecurityException, following the SOAP Message Security
     * 1.1 specification, chapter 12 "Error Handling".
     * 
     * When the Soap version is 1.1 then set the Fault/Code/Value from the fault code
     * specified in the WSSecurityException (if it exists).
     * 
     * Otherwise set the Fault/Code/Value to env:Sender and the Fault/Code/Subcode/Value
     * as the fault code from the WSSecurityException.
     */
    private SoapFault 
    createSoapFault(SoapVersion version, WSSecurityException e) {
        SoapFault fault;
        javax.xml.namespace.QName faultCode = e.getFaultCode();
        if (version.getVersion() == 1.1 && faultCode != null) {
            fault = new SoapFault(e.getMessage(), e, faultCode);
        } else {
            fault = new SoapFault(e.getMessage(), e, version.getSender());
            if (version.getVersion() != 1.1 && faultCode != null) {
                fault.setSubCode(faultCode);
            }
        }
        return fault;
    }
}
