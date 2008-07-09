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
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;


public class UsernameTokenBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.USERNAME_TOKEN, SP12Constants.USERNAME_TOKEN);

    PolicyBuilder builder;
    public UsernameTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    public PolicyAssertion build(Element element) {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        UsernameToken usernameToken = new UsernameToken(consts);

        String attribute = DOMUtils.getAttribute(element, SP11Constants.INCLUDE_TOKEN);
        if (attribute != null) {
            usernameToken.setInclusion(consts.getInclusionFromAttributeValue(attribute));
        }

        Element policyElement = DOMUtils.getFirstElement(element);

        if (policyElement != null) {

            Policy policy = builder.getPolicy(policyElement);
            policy = (Policy)policy.normalize(false);

            for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
                processAlternative((List)iterator.next(), usernameToken, consts);

                /*
                 * since there should be only one alternative
                 */
                break;
            }
        }

        return usernameToken;
    }

    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    private void processAlternative(List assertions, UsernameToken parent, SPConstants consts) {

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion)iterator.next();
            QName qname = assertion.getName();

            if (!consts.getNamespace().equals(qname.getNamespaceURI())) {
                continue;
            }
            
            if (SPConstants.USERNAME_TOKEN10.equals(qname.getLocalPart())) {
                parent.setUseUTProfile10(true);
            } else if (SPConstants.USERNAME_TOKEN11.equals(qname.getLocalPart())) {
                parent.setUseUTProfile11(true);
            } else if (SP12Constants.NO_PASSWORD.equals(qname)) {
                parent.setNoPassword(true);
            } else if (SP12Constants.HASH_PASSWORD.equals(qname)) {
                parent.setHashPassword(true);
            } else if (SP12Constants.REQUIRE_DERIVED_KEYS.equals(qname)) {
                parent.setDerivedKeys(true);
            } else if (SP12Constants.REQUIRE_EXPLICIT_DERIVED_KEYS.equals(qname)) {
                parent.setExplicitDerivedKeys(true);
            } else if (SP12Constants.REQUIRE_IMPLIED_DERIVED_KEYS.equals(qname)) {
                parent.setImpliedDerivedKeys(true);
            }
        }
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
