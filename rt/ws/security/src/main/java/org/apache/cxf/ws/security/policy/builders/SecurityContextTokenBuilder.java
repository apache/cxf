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
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.SecurityContextToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;


public class SecurityContextTokenBuilder implements AssertionBuilder<Element> {
    
    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.SECURITY_CONTEXT_TOKEN, SP12Constants.SECURITY_CONTEXT_TOKEN};
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory)
        throws IllegalArgumentException {

        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        SecurityContextToken contextToken = new SecurityContextToken(consts);

        String includeAttr = DOMUtils.getAttribute(element, consts.getIncludeToken());

        if (includeAttr != null) {
            contextToken.setInclusion(consts.getInclusionFromAttributeValue(includeAttr));
        }

        element = PolicyConstants.findPolicyElement(element);
        if (element == null && consts != SP11Constants.INSTANCE) {
            throw new IllegalArgumentException(
                "sp:SecurityContextToken/wsp:Policy must have a value"
            );
        }

        if (element != null) {
            if (DOMUtils.getFirstChildWithName(element, 
                    consts.getNamespace(),
                    SPConstants.REQUIRE_DERIVED_KEYS) != null) {
                contextToken.setDerivedKeys(true);
            }
    
            if (DOMUtils.getFirstChildWithName(element, 
                    consts.getNamespace(),
                    SPConstants.REQUIRE_EXTERNAL_URI_REFERENCE) != null) {
                contextToken.setRequireExternalUriRef(true);
            }
    
            if (DOMUtils.getFirstChildWithName(element,
                    consts.getNamespace(),
                    SPConstants.SC10_SECURITY_CONTEXT_TOKEN) != null) {
                contextToken.setSc10SecurityContextToken(true);
            }
    
            if (DOMUtils.getFirstChildWithName(element,
                    consts.getNamespace(),
                    SPConstants.SC13_SECURITY_CONTEXT_TOKEN) != null) {
                contextToken.setSc13SecurityContextToken(true);
            }
        }

        return contextToken;
    }

}
