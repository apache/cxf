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
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Wss11;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;


public class WSS11Builder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.WSS11, SP12Constants.WSS11);

    PolicyBuilder builder;
    public WSS11Builder(PolicyBuilder b) {
        builder = b;
    }

    
    
    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;
        Wss11 wss11 = new Wss11(consts);

        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy)policy.normalize(false);

        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
            processAlternative((List)iterator.next(), wss11, consts);
            /*
             * since there should be only one alternative
             */
            break;
        }

        return wss11;
    }

    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    private void processAlternative(List assertions, Wss11 parent, SPConstants consts) {

        Assertion assertion;
        QName name;

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            assertion = (Assertion)iterator.next();
            name = assertion.getName();

            if (!consts.getNamespace().equals(name.getNamespaceURI())) {
                continue;
            }

            if (SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER.equals(name.getLocalPart())) {
                parent.setMustSupportRefKeyIdentifier(true);

            } else if (SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL.equals(name.getLocalPart())) {
                parent.setMustSupportRefIssuerSerial(true);

            } else if (SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI.equals(name.getLocalPart())) {
                parent.setMustSupportRefExternalURI(true);

            } else if (SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN.equals(name.getLocalPart())) {
                parent.setMustSupportRefEmbeddedToken(true);

            } else if (SPConstants.MUST_SUPPORT_REF_THUMBPRINT.equals(name.getLocalPart())) {
                parent.setMustSupportRefThumbprint(true);

            } else if (SPConstants.MUST_SUPPORT_REF_ENCRYPTED_KEY.equals(name.getLocalPart())) {
                parent.setMustSupportRefEncryptedKey(true);

            } else if (SPConstants.REQUIRE_SIGNATURE_CONFIRMATION.equals(name.getLocalPart())) {
                parent.setRequireSignatureConfirmation(true);
            }
        }
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
