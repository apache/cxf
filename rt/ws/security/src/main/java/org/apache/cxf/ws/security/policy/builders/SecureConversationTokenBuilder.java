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
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.neethi.Policy;


public class SecureConversationTokenBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.SECURE_CONVERSATION_TOKEN, SP12Constants.SECURE_CONVERSATION_TOKEN);
    
    PolicyBuilder builder;
    public SecureConversationTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {

        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;
        
        
        SecureConversationToken conversationToken = new SecureConversationToken(consts);

        String attribute = DOMUtils.getAttribute(element, consts.getIncludeToken());
        if (attribute == null) {
            throw new IllegalArgumentException("SecurityContextToken doesn't contain "
                                               + "any sp:IncludeToken attribute");
        }

        String inclusionValue = attribute.trim();

        conversationToken.setInclusion(consts.getInclusionFromAttributeValue(inclusionValue));

        Element issuer = DOMUtils.getFirstChildWithName(element, consts.getNamespace(), SPConstants.ISSUER);
        if (issuer != null) {
            conversationToken.setIssuerEpr(DOMUtils.getFirstElement(issuer));
        }

        element = DOMUtils.getFirstChildWithName(element, SPConstants.POLICY);
        if (element != null) {
            if (DOMUtils.getFirstChildWithName(element, 
                                               consts.getNamespace(),
                                               SPConstants.REQUIRE_DERIVED_KEYS) != null) {
                conversationToken.setDerivedKeys(true);
            } else if (DOMUtils.getFirstChildWithName(element, 
                                                      SP12Constants.REQUIRE_IMPLIED_DERIVED_KEYS) != null) {
                conversationToken.setImpliedDerivedKeys(true);
            } else if (DOMUtils.getFirstChildWithName(element, 
                                                      SP12Constants.REQUIRE_EXPLICIT_DERIVED_KEYS) != null) {
                conversationToken.setExplicitDerivedKeys(true);
            }


            if (DOMUtils.getFirstChildWithName(element,
                                               consts.getNamespace(),
                                               SPConstants.REQUIRE_EXTERNAL_URI_REFERENCE) != null) {
                conversationToken.setRequireExternalUriRef(true);
            }

            if (DOMUtils.getFirstChildWithName(element, 
                                               consts.getNamespace(),
                                               SPConstants.SC10_SECURITY_CONTEXT_TOKEN) != null) {
                conversationToken.setSc10SecurityContextToken(true);
            }

            Element bootstrapPolicyElement = DOMUtils.getFirstChildWithName(element, 
                                                                            consts.getNamespace(),
                                                                            SPConstants.BOOTSTRAP_POLICY);
            if (bootstrapPolicyElement != null) {
                Policy policy = builder.getPolicy(DOMUtils.getFirstElement(bootstrapPolicyElement));
                conversationToken.setBootstrapPolicy(policy);
            }
        }

        return conversationToken;
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }

}
