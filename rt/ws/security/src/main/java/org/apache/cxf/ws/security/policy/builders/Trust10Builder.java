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
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;


public class Trust10Builder implements AssertionBuilder<Element> {
    public Trust10Builder() {
    }
    

    public Assertion build(Element element, AssertionBuilderFactory factory)
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

    public QName[] getKnownElements() {
        return new QName[] {SP11Constants.TRUST_10};
    }

}
