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
package org.apache.cxf.ws.security.policy.builders;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Wss11;


public class WSS11Builder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.WSS11, SP12Constants.WSS11);

    public WSS11Builder() {
    }

    
    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;
        Wss11 wss11 = new Wss11(consts);
        processAlternative(element, wss11, consts);
        return wss11;
    }

    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }
    private void processAlternative(Element element, Wss11 parent, SPConstants consts) {
        Element polEl = PolicyConstants.findPolicyElement(element);
        if (polEl != null) {
            Element child = DOMUtils.getFirstElement(polEl);
            while (child != null) {
                String name = child.getLocalName();

                if (SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER.equals(name)) {
                    parent.setMustSupportRefKeyIdentifier(true);
                } else if (SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL.equals(name)) {
                    parent.setMustSupportRefIssuerSerial(true);
                } else if (SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI.equals(name)) {
                    parent.setMustSupportRefExternalURI(true);
                } else if (SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN.equals(name)) {
                    parent.setMustSupportRefEmbeddedToken(true);
    
                } else if (SPConstants.MUST_SUPPORT_REF_THUMBPRINT.equals(name)) {
                    parent.setMustSupportRefThumbprint(true);
    
                } else if (SPConstants.MUST_SUPPORT_REF_ENCRYPTED_KEY.equals(name)) {
                    parent.setMustSupportRefEncryptedKey(true);
    
                } else if (SPConstants.REQUIRE_SIGNATURE_CONFIRMATION.equals(name)) {
                    parent.setRequireSignatureConfirmation(true);
                }
                child = DOMUtils.getNextElement(child);
            }
        }
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
