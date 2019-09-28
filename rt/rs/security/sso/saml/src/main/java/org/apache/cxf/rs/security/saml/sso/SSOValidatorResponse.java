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
package org.apache.cxf.rs.security.saml.sso;

import java.time.Instant;

import org.w3c.dom.Element;

import org.opensaml.saml.saml2.core.Assertion;

/**
 * Some information that encapsulates a successful validation by the SAMLSSOResponseValidator
 */
public class SSOValidatorResponse {
    private Instant sessionNotOnOrAfter;
    private Instant created;
    private String responseId;
    private String assertion;
    private Element assertionElement;
    private Assertion opensamlAssertion;

    public String getAssertion() {
        return assertion;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public Instant getSessionNotOnOrAfter() {
        return sessionNotOnOrAfter;
    }

    public void setSessionNotOnOrAfter(Instant sessionNotOnOrAfter) {
        this.sessionNotOnOrAfter = sessionNotOnOrAfter;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public Element getAssertionElement() {
        return assertionElement;
    }

    public void setAssertionElement(Element assertionElement) {
        this.assertionElement = assertionElement;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Assertion getOpensamlAssertion() {
        return opensamlAssertion;
    }

    public void setOpensamlAssertion(Assertion opensamlAssertion) {
        this.opensamlAssertion = opensamlAssertion;
    }
}
