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


import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SP13Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;


public class UsernameTokenBuilder implements AssertionBuilder<Element> {

    PolicyBuilder builder;
    public UsernameTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory) {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        UsernameToken usernameToken = new UsernameToken(consts);
        usernameToken.setOptional(PolicyConstants.isOptional(element));
        usernameToken.setIgnorable(PolicyConstants.isIgnorable(element));

        String attribute = element.getAttributeNS(element.getNamespaceURI(), SPConstants.ATTR_INCLUDE_TOKEN);
        if (attribute != null) {
            usernameToken.setInclusion(consts.getInclusionFromAttributeValue(attribute));
        }

        Element polEl = PolicyConstants.findPolicyElement(element);
        if (polEl == null && consts != SP11Constants.INSTANCE) {
            throw new IllegalArgumentException(
                "sp:UsernameToken/wsp:Policy must have a value"
            );
        }
        if (polEl != null) {
            NodeList children = polEl.getChildNodes();
            if (children != null) {
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element) {
                        child = (Element)child;
                        QName qname = new QName(child.getNamespaceURI(), child.getLocalName());
                        if (SPConstants.USERNAME_TOKEN10.equals(qname.getLocalPart())) {
                            usernameToken.setUseUTProfile10(true);
                        } else if (SPConstants.USERNAME_TOKEN11.equals(qname.getLocalPart())) {
                            usernameToken.setUseUTProfile11(true);
                        } else if (SP12Constants.NO_PASSWORD.equals(qname)) {
                            usernameToken.setNoPassword(true);
                        } else if (SP12Constants.HASH_PASSWORD.equals(qname)) {
                            usernameToken.setHashPassword(true);
                        } else if (SP12Constants.REQUIRE_DERIVED_KEYS.equals(qname)) {
                            usernameToken.setDerivedKeys(true);
                        } else if (SP12Constants.REQUIRE_EXPLICIT_DERIVED_KEYS.equals(qname)) {
                            usernameToken.setExplicitDerivedKeys(true);
                        } else if (SP12Constants.REQUIRE_IMPLIED_DERIVED_KEYS.equals(qname)) {
                            usernameToken.setImpliedDerivedKeys(true);
                        } else if (SP13Constants.USERNAME_TOKEN_CREATED.equals(qname)) {
                            usernameToken.setRequireCreated(true);
                        } else if (SP13Constants.USERNAME_TOKEN_NONCE.equals(qname)) {
                            usernameToken.setRequireNonce(true);
                        }
                    }
                }
            }
        }
        return usernameToken;
    }

    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.USERNAME_TOKEN, SP12Constants.USERNAME_TOKEN};
    }
}
