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

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.model.Trust10;


public class Trust10Builder implements AssertionBuilder {
    public Trust10Builder() {
    }
    

    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {

        
        element = DOMUtils.getFirstElement(element);
        if (element == null || !element.getLocalName().equals("Policy")) {
            throw new IllegalArgumentException("Trust10 assertion doesn't contain any Policy");
        }
        
        Trust10 trust10 = new Trust10(SP11Constants.INSTANCE);

        if (DOMUtils.getFirstChildWithName(element, SP11Constants.MUST_SUPPORT_CLIENT_CHALLENGE) != null) {
            trust10.setMustSupportClientChallenge(true);
        }

        if (DOMUtils.getFirstChildWithName(element, SP11Constants.MUST_SUPPORT_SERVER_CHALLENGE) != null) {
            trust10.setMustSupportServerChallenge(true);
        }

        if (DOMUtils.getFirstChildWithName(element, SP11Constants.REQUIRE_CLIENT_ENTROPY) != null) {
            trust10.setRequireClientEntropy(true);
        }

        if (DOMUtils.getFirstChildWithName(element, SP11Constants.REQUIRE_SERVER_ENTROPY) != null) {
            trust10.setRequireServerEntropy(true);
        }

        if (DOMUtils.getFirstChildWithName(element, SP11Constants.MUST_SUPPORT_ISSUED_TOKENS) != null) {
            trust10.setMustSupportIssuedTokens(true);
        }

        return trust10;
    }

    public List<QName> getKnownElements() {
        return Collections.singletonList(SP11Constants.TRUST_10);
    }


    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }

}
