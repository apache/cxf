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
package org.apache.cxf.sts.token.provider;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.AbstractSecuredElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;

public final class TokenProviderUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(TokenProviderUtils.class);

    private TokenProviderUtils() {
        // complete
    }

    /**
     * Extract an address from a Participants EPR DOM element
     */
    public static String extractAddressFromParticipantsEPR(Object participants) {
        if (participants instanceof Element) {
            String localName = ((Element)participants).getLocalName();
            String namespace = ((Element)participants).getNamespaceURI();

            if (STSConstants.WSA_NS_05.equals(namespace) && "EndpointReference".equals(localName)) {
                LOG.fine("Found EndpointReference element");
                Element address =
                    DOMUtils.getFirstChildWithName((Element)participants,
                            STSConstants.WSA_NS_05, "Address");
                if (address != null) {
                    LOG.fine("Found address element");
                    return address.getTextContent();
                }
            } else if ((STSConstants.WSP_NS.equals(namespace) || STSConstants.WSP_NS_04.equals(namespace)
                || STSConstants.WSP_NS_06.equals(namespace))
                && "URI".equals(localName)) {
                return ((Element)participants).getTextContent();
            }
            LOG.fine("Participants element does not exist or could not be parsed");
            return null;
        } else if (participants instanceof JAXBElement<?>) {
            JAXBElement<?> jaxbElement = (JAXBElement<?>) participants;
            QName participantsName = jaxbElement.getName();
            if (STSConstants.WSA_NS_05.equals(participantsName.getNamespaceURI())
                && "EndpointReference".equals(participantsName.getLocalPart())) {
                LOG.fine("Found EndpointReference element");
                EndpointReferenceType endpointReference = (EndpointReferenceType)jaxbElement.getValue();
                if (endpointReference.getAddress() != null) {
                    LOG.fine("Found address element");
                    return endpointReference.getAddress().getValue();
                }
            }
            LOG.fine("Participants element does not exist or could not be parsed");
        }

        return null;
    }

    /**
     * Encrypt a Token element using the given arguments.
     */
    public static Element encryptToken(
        Element element,
        String id,
        STSPropertiesMBean stsProperties,
        EncryptionProperties encryptionProperties,
        KeyRequirements keyRequirements,
        Map<String, Object> messageContext
    ) throws WSSecurityException {
        String name = encryptionProperties.getEncryptionName();
        if (name == null) {
            name = stsProperties.getEncryptionUsername();
        }
        if (name == null) {
            LOG.fine("No encryption alias is configured");
            return element;
        }

        // Get the encryption algorithm to use
        String encryptionAlgorithm = keyRequirements.getEncryptionAlgorithm();
        if (encryptionAlgorithm == null) {
            // If none then default to what is configured
            encryptionAlgorithm = encryptionProperties.getEncryptionAlgorithm();
        } else {
            List<String> supportedAlgorithms =
                encryptionProperties.getAcceptedEncryptionAlgorithms();
            if (!supportedAlgorithms.contains(encryptionAlgorithm)) {
                encryptionAlgorithm = encryptionProperties.getEncryptionAlgorithm();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("EncryptionAlgorithm not supported, defaulting to: " + encryptionAlgorithm);
                }
            }
        }
        // Get the key-wrap algorithm to use
        String keyWrapAlgorithm = keyRequirements.getKeywrapAlgorithm();
        if (keyWrapAlgorithm == null) {
            // If none then default to what is configured
            keyWrapAlgorithm = encryptionProperties.getKeyWrapAlgorithm();
        } else {
            List<String> supportedAlgorithms =
                encryptionProperties.getAcceptedKeyWrapAlgorithms();
            if (!supportedAlgorithms.contains(keyWrapAlgorithm)) {
                keyWrapAlgorithm = encryptionProperties.getKeyWrapAlgorithm();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("KeyWrapAlgorithm not supported, defaulting to: " + keyWrapAlgorithm);
                }
            }
        }

        Document doc = element.getOwnerDocument();
        DocumentFragment frag = doc.createDocumentFragment();
        frag.appendChild(element);

        WSSecEncrypt builder = new WSSecEncrypt(doc);
        if (ConfigurationConstants.USE_REQ_SIG_CERT.equals(name)) {
            X509Certificate cert = getReqSigCert(messageContext);
            builder.setUseThisCert(cert);
        } else {
            builder.setUserInfo(name);
        }
        builder.setKeyIdentifierType(encryptionProperties.getKeyIdentifierType());
        builder.setSymmetricEncAlgorithm(encryptionAlgorithm);
        builder.setKeyEncAlgo(keyWrapAlgorithm);
        builder.setEmbedEncryptedKey(true);

        WSEncryptionPart encryptionPart = new WSEncryptionPart(id, "Element");
        encryptionPart.setElement(element);

        KeyGenerator keyGen = KeyUtils.getKeyGenerator(encryptionAlgorithm);
        SecretKey symmetricKey = keyGen.generateKey();

        builder.prepare(stsProperties.getEncryptionCrypto(), symmetricKey);
        builder.encryptForRef(null, Collections.singletonList(encryptionPart), symmetricKey);

        return (Element)frag.getFirstChild();
    }

    /**
     * Get the X509Certificate associated with the signature that was received. This cert is to be used
     * for encrypting the issued token.
     */
    public static X509Certificate getReqSigCert(Map<String, Object> messageContext) {
        @SuppressWarnings("unchecked")
        List<WSHandlerResult> results =
            (List<WSHandlerResult>) messageContext.get(WSHandlerConstants.RECV_RESULTS);
        // DOM
        X509Certificate cert = WSS4JUtils.getReqSigCert(results);
        if (cert != null) {
            return cert;
        }

        // Streaming
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingEventList =
            (List<SecurityEvent>) messageContext.get(SecurityEvent.class.getName() + ".in");
        if (incomingEventList != null) {
            for (SecurityEvent incomingEvent : incomingEventList) {
                if (WSSecurityEventConstants.SIGNED_PART == incomingEvent.getSecurityEventType()
                    || WSSecurityEventConstants.SignedElement
                        == incomingEvent.getSecurityEventType()) {
                    org.apache.xml.security.stax.securityToken.SecurityToken token =
                        ((AbstractSecuredElementSecurityEvent)incomingEvent).getSecurityToken();
                    try {
                        if (token != null && token.getX509Certificates() != null
                            && token.getX509Certificates().length > 0) {
                            return token.getX509Certificates()[0];
                        }
                    } catch (XMLSecurityException ex) {
                        LOG.log(Level.FINE, ex.getMessage(), ex);
                        return null;
                    }
                }
            }
        }

        return null;
    }
}
