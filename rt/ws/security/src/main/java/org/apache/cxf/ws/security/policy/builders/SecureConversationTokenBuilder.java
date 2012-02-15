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

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;


public class SecureConversationTokenBuilder implements AssertionBuilder<Element> {
    
    PolicyBuilder builder;
    public SecureConversationTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.SECURE_CONVERSATION_TOKEN, SP12Constants.SECURE_CONVERSATION_TOKEN};
    }

    public Assertion build(Element element, AssertionBuilderFactory factory)
        throws IllegalArgumentException {

        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;
        
        
        SecureConversationToken conversationToken = new SecureConversationToken(consts);
        conversationToken.setOptional(PolicyConstants.isOptional(element));
        conversationToken.setIgnorable(PolicyConstants.isIgnorable(element));

        String attribute = DOMUtils.getAttribute(element, consts.getIncludeToken());
        if (attribute != null) {
            conversationToken.setInclusion(consts.getInclusionFromAttributeValue(attribute.trim()));
        }
        
        Element elem = DOMUtils.getFirstElement(element);
        boolean foundPolicy = false;
        while (elem != null) {
            QName qn = DOMUtils.getElementQName(elem);
            if (Constants.isPolicyElement(qn)) {
                foundPolicy = true;
                if (DOMUtils.getFirstChildWithName(elem, 
                                                   consts.getNamespace(),
                                                   SPConstants.REQUIRE_DERIVED_KEYS) != null) {
                    conversationToken.setDerivedKeys(true);
                } else if (DOMUtils.getFirstChildWithName(elem, 
                                                          SP12Constants
                                                              .REQUIRE_IMPLIED_DERIVED_KEYS) 
                                                          != null) {
                    conversationToken.setImpliedDerivedKeys(true);
                } else if (DOMUtils.getFirstChildWithName(elem, 
                                                          SP12Constants
                                                              .REQUIRE_EXPLICIT_DERIVED_KEYS)
                                                              != null) {
                    conversationToken.setExplicitDerivedKeys(true);
                }


                if (DOMUtils.getFirstChildWithName(elem,
                                                   consts.getNamespace(),
                                                   SPConstants.REQUIRE_EXTERNAL_URI_REFERENCE) != null) {
                    conversationToken.setRequireExternalUriRef(true);
                }

                if (DOMUtils.getFirstChildWithName(elem, 
                                                   consts.getNamespace(),
                                                   SPConstants.SC10_SECURITY_CONTEXT_TOKEN) != null) {
                    conversationToken.setSc10SecurityContextToken(true);
                }
                
                if (DOMUtils.getFirstChildWithName(elem, 
                        consts.getNamespace(),
                        SPConstants.SC13_SECURITY_CONTEXT_TOKEN) != null) {
                    conversationToken.setSc13SecurityContextToken(true);
                }

                Element bootstrapPolicyElement = DOMUtils.getFirstChildWithName(elem, 
                                                                                consts.getNamespace(),
                                                                                SPConstants.BOOTSTRAP_POLICY);
                if (bootstrapPolicyElement != null) {
                    Policy policy = builder.getPolicy(DOMUtils.getFirstElement(bootstrapPolicyElement));
                    conversationToken.setBootstrapPolicy(policy);
                }

            } else if (consts.getNamespace().equals(qn.getNamespaceURI())
                && SPConstants.ISSUER.equals(qn.getLocalPart())) {
                conversationToken.setIssuerEpr(DOMUtils.getFirstElement(elem));                
            }
            elem = DOMUtils.getNextElement(elem);
        }
        
        if (!foundPolicy && consts != SP11Constants.INSTANCE) {
            throw new IllegalArgumentException(
                "sp:SecureConversationToken/wsp:Policy must have a value"
            );
        }
        
        return conversationToken;
    }

}
