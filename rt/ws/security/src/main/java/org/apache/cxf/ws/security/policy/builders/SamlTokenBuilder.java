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

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;


public class SamlTokenBuilder implements AssertionBuilder<Element> {

    PolicyBuilder builder;
    public SamlTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory) {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        SamlToken samlToken = new SamlToken(consts);
        samlToken.setOptional(PolicyConstants.isOptional(element));
        samlToken.setIgnorable(PolicyConstants.isIgnorable(element));

        String attribute = element.getAttributeNS(element.getNamespaceURI(), SPConstants.ATTR_INCLUDE_TOKEN);
        if (attribute != null) {
            samlToken.setInclusion(consts.getInclusionFromAttributeValue(attribute));
        }
        
        Element child = DOMUtils.getFirstElement(element);
        while (child != null) {
            String ln = child.getLocalName();
            if (org.apache.neethi.Constants.ELEM_POLICY.equals(ln)) {
                NodeList policyChildren = child.getChildNodes();
                if (policyChildren != null) {
                    for (int i = 0; i < policyChildren.getLength(); i++) {
                        Node policyChild = policyChildren.item(i);
                        if (policyChild instanceof Element) {
                            QName qname = 
                                new QName(policyChild.getNamespaceURI(), policyChild.getLocalName());
                            String localname = qname.getLocalPart();
                            if (SPConstants.SAML_11_TOKEN_10.equals(localname)) {
                                samlToken.setUseSamlVersion11Profile10(true);
                            } else if (SPConstants.SAML_11_TOKEN_11.equals(localname)) {
                                samlToken.setUseSamlVersion11Profile11(true);
                            } else if (SPConstants.SAML_20_TOKEN_11.equals(localname)) {
                                samlToken.setUseSamlVersion20Profile11(true);
                            } else if (SPConstants.REQUIRE_DERIVED_KEYS.equals(localname)) {
                                samlToken.setDerivedKeys(true);
                            } else if (SPConstants.REQUIRE_EXPLICIT_DERIVED_KEYS.equals(localname)) {
                                samlToken.setExplicitDerivedKeys(true);
                            } else if (SPConstants.REQUIRE_IMPLIED_DERIVED_KEYS.equals(localname)) {
                                samlToken.setImpliedDerivedKeys(true);
                            } else if (SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE.equals(localname)) {
                                samlToken.setRequireKeyIdentifierReference(true);
                            }
                        }
                    }
                }
            }
            child = DOMUtils.getNextElement(child);
        }
        return samlToken;
    }

    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.SAML_TOKEN, SP12Constants.SAML_TOKEN};
    }
}
