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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.Names;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.AbstractSecuredElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants.Event;

/**
 * This interceptor handles parsing the StaX WS-Security results (events) + checks that the
 * specified crypto coverage events actually occurred. The default functionality is to enforce
 * that the SOAP Body, Timestamp, and WS-Addressing ReplyTo and FaultTo headers must be signed,
 * and the UsernameToken must be encrypted (if they exist in the message payload).
 *
 * Note that this interceptor must be explicitly added to the InInterceptor chain.
 */
public class StaxCryptoCoverageChecker extends AbstractPhaseInterceptor<SoapMessage> {
    public static final String SOAP_NS = WSConstants.URI_SOAP11_ENV;
    public static final String SOAP12_NS = WSConstants.URI_SOAP12_ENV;
    public static final String WSU_NS = WSConstants.WSU_NS;
    public static final String WSSE_NS = WSConstants.WSSE_NS;
    public static final String WSA_NS = Names.WSA_NAMESPACE_NAME;

    private boolean signBody;
    private boolean signTimestamp;
    private boolean encryptBody;
    private boolean signAddressingHeaders;
    private boolean signUsernameToken;
    private boolean encryptUsernameToken;

    public StaxCryptoCoverageChecker() {
        super(Phase.PRE_PROTOCOL);

        // Sign SOAP Body
        setSignBody(true);

        // Sign Timestamp
        setSignTimestamp(true);

        // Sign Addressing Headers
        setSignAddressingHeaders(true);

        // Encrypt UsernameToken
        setEncryptUsernameToken(true);
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {

        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingSecurityEventList =
            (List<SecurityEvent>)soapMessage.get(SecurityEvent.class.getName() + ".in");

        List<SecurityEvent> results = new ArrayList<>();
        if (incomingSecurityEventList != null) {
            // Get all Signed/Encrypted Results
            results.addAll(
                getEventFromResults(WSSecurityEventConstants.SIGNED_PART, incomingSecurityEventList));
            results.addAll(
                getEventFromResults(WSSecurityEventConstants.SignedElement, incomingSecurityEventList));

            if (encryptBody || encryptUsernameToken) {
                results.addAll(
                    getEventFromResults(WSSecurityEventConstants.ENCRYPTED_PART, incomingSecurityEventList));
                results.addAll(
                    getEventFromResults(WSSecurityEventConstants.EncryptedElement, incomingSecurityEventList));
            }
        }

        try {
            checkSignedBody(results);
            checkEncryptedBody(results);

            if (signTimestamp) {
                // We only insist on the Timestamp being signed if it is actually present in the message
                List<SecurityEvent> timestampResults =
                    getEventFromResults(WSSecurityEventConstants.TIMESTAMP, incomingSecurityEventList);
                if (!timestampResults.isEmpty()) {
                    checkSignedTimestamp(results);
                }
            }

            if (signAddressingHeaders) {
                AddressingProperties addressingProperties =
                    (AddressingProperties)soapMessage.get("jakarta.xml.ws.addressing.context.inbound");
                checkSignedAddressing(results, addressingProperties);
            }

            if (signUsernameToken || encryptUsernameToken) {
                // We only insist on the UsernameToken being signed/encrypted if it is actually
                // present in the message
                List<SecurityEvent> usernameTokenResults =
                    getEventFromResults(WSSecurityEventConstants.USERNAME_TOKEN, incomingSecurityEventList);
                if (!usernameTokenResults.isEmpty()) {
                    if (signUsernameToken) {
                        checkSignedUsernameToken(results);
                    }

                    if (encryptUsernameToken) {
                        checkEncryptedUsernameToken(results);
                    }
                }
            }
        } catch (WSSecurityException e) {
            throw createSoapFault(soapMessage.getVersion(), e);
        }
    }

    private List<SecurityEvent> getEventFromResults(Event event, List<SecurityEvent> incomingSecurityEventList) {
        List<SecurityEvent> results = new ArrayList<>();
        for (SecurityEvent incomingEvent : incomingSecurityEventList) {
            if (event == incomingEvent.getSecurityEventType()) {
                results.add(incomingEvent);
            }
        }
        return results;
    }

    private void checkSignedBody(List<SecurityEvent> results) throws WSSecurityException {
        if (!signBody) {
            return;
        }

        boolean isBodySigned = false;
        for (SecurityEvent signedEvent : results) {
            AbstractSecuredElementSecurityEvent securedEvent =
                (AbstractSecuredElementSecurityEvent)signedEvent;
            if (!securedEvent.isSigned()) {
                continue;
            }

            List<QName> signedPath = securedEvent.getElementPath();
            if (isBody(signedPath)) {
                isBodySigned = true;
                break;
            }
        }

        if (!isBodySigned) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          new Exception("The SOAP Body is not signed"));
        }
    }

    private void checkEncryptedBody(List<SecurityEvent> results) throws WSSecurityException {
        if (!encryptBody) {
            return;
        }

        boolean isBodyEncrypted = false;
        for (SecurityEvent signedEvent : results) {
            AbstractSecuredElementSecurityEvent securedEvent =
                (AbstractSecuredElementSecurityEvent)signedEvent;
            if (!securedEvent.isEncrypted()) {
                continue;
            }

            List<QName> encryptedPath = securedEvent.getElementPath();
            if (isBody(encryptedPath)) {
                isBodyEncrypted = true;
                break;
            }
        }

        if (!isBodyEncrypted) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          new Exception("The SOAP Body is not encrypted"));
        }
    }

    private void checkSignedTimestamp(List<SecurityEvent> results) throws WSSecurityException {
        if (!signTimestamp) {
            return;
        }

        boolean isTimestampSigned = false;
        for (SecurityEvent signedEvent : results) {
            AbstractSecuredElementSecurityEvent securedEvent =
                (AbstractSecuredElementSecurityEvent)signedEvent;
            if (!securedEvent.isSigned()) {
                continue;
            }

            List<QName> signedPath = securedEvent.getElementPath();
            if (isTimestamp(signedPath)) {
                isTimestampSigned = true;
                break;
            }
        }

        if (!isTimestampSigned) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          new Exception("The Timestamp is not signed"));
        }
    }

    private void checkSignedAddressing(
        List<SecurityEvent> results,
        AddressingProperties addressingProperties
    ) throws WSSecurityException {
        if (!signAddressingHeaders || addressingProperties == null
            || (addressingProperties.getReplyTo() == null && addressingProperties.getFaultTo() == null)) {
            return;
        }

        boolean isReplyToSigned = false;
        boolean isFaultToSigned = false;
        for (SecurityEvent signedEvent : results) {
            AbstractSecuredElementSecurityEvent securedEvent =
                (AbstractSecuredElementSecurityEvent)signedEvent;
            if (!securedEvent.isSigned()) {
                continue;
            }

            List<QName> signedPath = securedEvent.getElementPath();
            if (isReplyTo(signedPath)) {
                isReplyToSigned = true;
            }
            if (isFaultTo(signedPath)) {
                isFaultToSigned = true;
            }

            if (isReplyToSigned && isFaultToSigned) {
                break;
            }
        }

        if (!isReplyToSigned && (addressingProperties.getReplyTo() != null)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          new Exception("The Addressing headers are not signed"));
        }

        if (!isFaultToSigned && (addressingProperties.getFaultTo() != null)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          new Exception("The Addressing headers are not signed"));
        }
    }

    private void checkSignedUsernameToken(List<SecurityEvent> results) throws WSSecurityException {
        if (!signUsernameToken) {
            return;
        }

        boolean isUsernameTokenSigned = false;
        for (SecurityEvent signedEvent : results) {
            AbstractSecuredElementSecurityEvent securedEvent =
                (AbstractSecuredElementSecurityEvent)signedEvent;
            if (!securedEvent.isSigned()) {
                continue;
            }

            List<QName> signedPath = securedEvent.getElementPath();
            if (isUsernameToken(signedPath)) {
                isUsernameTokenSigned = true;
                break;
            }
        }

        if (!isUsernameTokenSigned) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          new Exception("The UsernameToken is not signed"));
        }
    }

    private void checkEncryptedUsernameToken(List<SecurityEvent> results) throws WSSecurityException {
        if (!encryptUsernameToken) {
            return;
        }

        boolean isUsernameTokenEncrypted = false;
        for (SecurityEvent encryptedEvent : results) {
            AbstractSecuredElementSecurityEvent securedEvent =
                (AbstractSecuredElementSecurityEvent)encryptedEvent;
            if (!securedEvent.isEncrypted()) {
                continue;
            }

            List<QName> encryptedPath = securedEvent.getElementPath();
            if (isUsernameToken(encryptedPath)) {
                isUsernameTokenEncrypted = true;
                break;
            }
        }

        if (!isUsernameTokenEncrypted) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          new Exception("The UsernameToken is not encrypted"));
        }
    }

    private boolean isEnvelope(QName qname) {
        return "Envelope".equals(qname.getLocalPart())
            && (SOAP_NS.equals(qname.getNamespaceURI())
                || SOAP12_NS.equals(qname.getNamespaceURI()));
    }

    private boolean isSoapHeader(QName qname) {
        return "Header".equals(qname.getLocalPart())
            && (SOAP_NS.equals(qname.getNamespaceURI())
                || SOAP12_NS.equals(qname.getNamespaceURI()));
    }

    private boolean isSecurityHeader(QName qname) {
        return "Security".equals(qname.getLocalPart()) && WSSE_NS.equals(qname.getNamespaceURI());
    }

    private boolean isTimestamp(List<QName> qnames) {
        return qnames != null
            && qnames.size() == 4
            && isEnvelope(qnames.get(0))
            && isSoapHeader(qnames.get(1))
            && isSecurityHeader(qnames.get(2))
            && "Timestamp".equals(qnames.get(3).getLocalPart())
            && WSU_NS.equals(qnames.get(3).getNamespaceURI());
    }

    private boolean isReplyTo(List<QName> qnames) {
        return qnames != null && qnames.size() == 3
            && isEnvelope(qnames.get(0))
            && isSoapHeader(qnames.get(1))
            && "ReplyTo".equals(qnames.get(2).getLocalPart())
            && WSA_NS.equals(qnames.get(2).getNamespaceURI());
    }

    private boolean isFaultTo(List<QName> qnames) {
        return qnames != null && qnames.size() == 3
            && isEnvelope(qnames.get(0))
            && isSoapHeader(qnames.get(1))
            && "FaultTo".equals(qnames.get(2).getLocalPart())
            && WSA_NS.equals(qnames.get(2).getNamespaceURI());
    }

    private boolean isBody(List<QName> qnames) {
        return qnames != null && qnames.size() == 2
            && isEnvelope(qnames.get(0))
            && "Body".equals(qnames.get(1).getLocalPart())
            && (SOAP_NS.equals(qnames.get(1).getNamespaceURI())
                || SOAP12_NS.equals(qnames.get(1).getNamespaceURI()));
    }

    private boolean isUsernameToken(List<QName> qnames) {
        return qnames != null && qnames.size() == 4
            && isEnvelope(qnames.get(0))
            && isSoapHeader(qnames.get(1))
            && isSecurityHeader(qnames.get(2))
            && "UsernameToken".equals(qnames.get(3).getLocalPart())
            && WSSE_NS.equals(qnames.get(3).getNamespaceURI());
    }

    public boolean isSignBody() {
        return signBody;
    }

    public final void setSignBody(boolean signBody) {
        this.signBody = signBody;
    }

    public boolean isSignTimestamp() {
        return signTimestamp;
    }

    public final void setSignTimestamp(boolean signTimestamp) {
        this.signTimestamp = signTimestamp;
    }

    public boolean isEncryptBody() {
        return encryptBody;
    }

    public final void setEncryptBody(boolean encryptBody) {
        this.encryptBody = encryptBody;
    }

    public boolean isSignAddressingHeaders() {
        return signAddressingHeaders;
    }

    public final void setSignAddressingHeaders(boolean signAddressingHeaders) {
        this.signAddressingHeaders = signAddressingHeaders;
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
    private SoapFault createSoapFault(SoapVersion version, WSSecurityException e) {
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

    public boolean isSignUsernameToken() {
        return signUsernameToken;
    }

    public void setSignUsernameToken(boolean signUsernameToken) {
        this.signUsernameToken = signUsernameToken;
    }

    public boolean isEncryptUsernameToken() {
        return encryptUsernameToken;
    }

    public final void setEncryptUsernameToken(boolean encryptUsernameToken) {
        this.encryptUsernameToken = encryptUsernameToken;
    }
}
