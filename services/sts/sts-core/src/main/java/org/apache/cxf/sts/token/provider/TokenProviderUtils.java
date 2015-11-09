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

import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

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
            } else if ((STSConstants.WSP_NS.equals(namespace) || STSConstants.WSP_NS_04.equals(namespace))
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

}
