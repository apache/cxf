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
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Trust13;


public class Trust13Builder implements AssertionBuilder {

    public PolicyAssertion build(Element element) {
        element = DOMUtils.getFirstChildWithName(element, SPConstants.POLICY);

        if (element == null) {
            throw new IllegalArgumentException(
                    "Trust13 assertion doesn't contain any Policy");
        }

        Trust13 trust13 = new Trust13(SP12Constants.INSTANCE);

        if (DOMUtils
                .getFirstChildWithName(element, SP12Constants.MUST_SUPPORT_CLIENT_CHALLENGE) != null) {
            trust13.setMustSupportClientChallenge(true);
        }

        if (DOMUtils
                .getFirstChildWithName(element, SP12Constants.MUST_SUPPORT_SERVER_CHALLENGE) != null) {
            trust13.setMustSupportServerChallenge(true);
        }

        if (DOMUtils.getFirstChildWithName(element, SP12Constants.REQUIRE_CLIENT_ENTROPY) != null) {
            trust13.setRequireClientEntropy(true);
        }

        if (DOMUtils.getFirstChildWithName(element, SP12Constants.REQUIRE_SERVER_ENTROPY) != null) {
            trust13.setRequireServerEntropy(true);
        }

        if (DOMUtils.getFirstChildWithName(element, SP12Constants.MUST_SUPPORT_ISSUED_TOKENS) != null) {
            trust13.setMustSupportIssuedTokens(true);
        }
        
        if (DOMUtils.getFirstChildWithName(element,
                                           SP12Constants.REQUIRE_REQUEST_SECURITY_TOKEN_COLLECTION) != null) {
            trust13.setRequireRequestSecurityTokenCollection(true);
        }
        
        if (DOMUtils.getFirstChildWithName(element, SP12Constants.REQUIRE_APPLIES_TO) != null) {
            trust13.setRequireAppliesTo(true);
        }

        return trust13;
    }
    public List<QName> getKnownElements() {
        return Collections.singletonList(SP12Constants.TRUST_13);
    }
    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }

}
