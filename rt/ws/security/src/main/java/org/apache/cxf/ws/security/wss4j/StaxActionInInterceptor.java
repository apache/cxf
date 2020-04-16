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

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.securityEvent.OperationSecurityEvent;
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

        if (inActions == null || inActions.isEmpty()) {
            return;
        }

        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingSecurityEventList =
            (List<SecurityEvent>)soapMessage.get(SecurityEvent.class.getName() + ".in");

        if (incomingSecurityEventList == null) {
            LOG.warning("Security processing failed (actions mismatch)");
            WSSecurityException ex =
                new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_ERROR);
            throw WSS4JUtils.createSoapFault(soapMessage, soapMessage.getVersion(), ex);
        }

        // First check for a SOAP Fault with no security header if we are the client
        if (MessageUtils.isRequestor(soapMessage)
            && isEventInResults(WSSecurityEventConstants.NO_SECURITY, incomingSecurityEventList)) {
            OperationSecurityEvent securityEvent =
                (OperationSecurityEvent)findEvent(
                    WSSecurityEventConstants.OPERATION, incomingSecurityEventList
                );
            if (securityEvent != null
                && soapMessage.getVersion().getFault().equals(securityEvent.getOperation())) {
                LOG.warning("Request does not contain Security header, but it's a fault.");
                return;
            }
        }

        for (XMLSecurityConstants.Action action : inActions) {
            Event requiredEvent = null;
            if (WSSConstants.TIMESTAMP.equals(action)) {
                requiredEvent = WSSecurityEventConstants.TIMESTAMP;
            } else if (WSSConstants.USERNAMETOKEN.equals(action)) {
                requiredEvent = WSSecurityEventConstants.USERNAME_TOKEN;
            } else if (XMLSecurityConstants.SIGNATURE.equals(action)) {
                requiredEvent = WSSecurityEventConstants.SignatureValue;
            } else if (WSSConstants.SAML_TOKEN_SIGNED.equals(action)
                || WSSConstants.SAML_TOKEN_UNSIGNED.equals(action)) {
                requiredEvent = WSSecurityEventConstants.SAML_TOKEN;
            }

            if (requiredEvent != null
                && !isEventInResults(requiredEvent, incomingSecurityEventList)) {
                LOG.warning("Security processing failed (actions mismatch)");
                WSSecurityException ex =
                    new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_ERROR);
                throw WSS4JUtils.createSoapFault(soapMessage, soapMessage.getVersion(), ex);
            }

            if (XMLSecurityConstants.ENCRYPTION.equals(action)) {
                boolean foundEncryptionPart =
                    isEventInResults(WSSecurityEventConstants.ENCRYPTED_PART, incomingSecurityEventList);
                if (!foundEncryptionPart) {
                    foundEncryptionPart =
                        isEventInResults(WSSecurityEventConstants.EncryptedElement, incomingSecurityEventList);
                }
                if (!foundEncryptionPart) {
                    LOG.warning("Security processing failed (actions mismatch)");
                    WSSecurityException ex =
                        new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_ERROR);
                    throw WSS4JUtils.createSoapFault(soapMessage, soapMessage.getVersion(), ex);
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

    private SecurityEvent findEvent(Event event, List<SecurityEvent> incomingSecurityEventList) {
        for (SecurityEvent incomingEvent : incomingSecurityEventList) {
            if (event == incomingEvent.getSecurityEventType()) {
                return incomingEvent;
            }
        }
        return null;
    }

}
