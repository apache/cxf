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
package org.apache.cxf.ws.security.wss4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.cxf.ws.security.wss4j.policyvalidators.PolicyValidatorParameters;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecurityPolicyValidator;
import org.apache.neethi.Policy;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.policy.SP12Constants;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * A test for plugging in custom SecurityPolicy Validators
 */
public class PluggablePolicyValidatorTest extends AbstractPolicySecurityTest {

    @Test
    public void testEncryptedElementsPolicyValidator() throws Exception {
        // This should work (body content is encrypted)
        this.runInInterceptorAndValidate(
            "encrypted_body_content.xml",
            "content_encrypted_elements_policy.xml",
            Arrays.asList(SP12Constants.CONTENT_ENCRYPTED_ELEMENTS),
            null,
            Arrays.asList(CoverageType.ENCRYPTED),
            null);

        // This should fail (body content is encrypted, not the element)
        this.runInInterceptorAndValidate(
            "encrypted_body_content.xml",
            "encrypted_elements_policy2.xml",
            null,
            Arrays.asList(SP12Constants.ENCRYPTED_ELEMENTS),
            Arrays.asList(CoverageType.ENCRYPTED),
            null);

        // Now plug in a custom SecurityPolicyValidator to allow the EncryptedElements policy
        // to pass
        Map<QName, SecurityPolicyValidator> validators = new HashMap<>();
        validators.put(SP12Constants.ENCRYPTED_ELEMENTS, new NOOpPolicyValidator());
        this.runInInterceptorAndValidate(
            "encrypted_body_content.xml",
            "encrypted_elements_policy2.xml",
            Arrays.asList(SP12Constants.ENCRYPTED_ELEMENTS),
            null,
            Arrays.asList(CoverageType.ENCRYPTED),
            validators);
    }

    private void runInInterceptorAndValidate(
        String document, String policyDocument, List<QName> assertedInAssertions,
        List<QName> notAssertedInAssertions, List<CoverageType> types,
        Map<QName, SecurityPolicyValidator> validators
    ) throws Exception {

        final Policy policy =
            this.policyBuilder.getPolicy(this.readDocument(policyDocument).getDocumentElement());

        final Document doc = this.readDocument(document);

        final AssertionInfoMap aim = new AssertionInfoMap(policy);

        this.runInInterceptorAndValidateWss(doc, aim, types, validators);

        try {
            aim.checkEffectivePolicy(policy);
        } catch (PolicyException e) {
            // Expected but not relevant
        } finally {
            if (assertedInAssertions != null) {
                for (QName assertionType : assertedInAssertions) {
                    Collection<AssertionInfo> ais = aim.get(assertionType);
                    assertNotNull(ais);
                    for (AssertionInfo ai : ais) {
                        checkAssertion(aim, assertionType, ai, true);
                    }
                }
            }

            if (notAssertedInAssertions != null) {
                for (QName assertionType : notAssertedInAssertions) {
                    Collection<AssertionInfo> ais = aim.get(assertionType);
                    assertNotNull(ais);
                    for (AssertionInfo ai : ais) {
                        checkAssertion(aim, assertionType, ai, false);
                    }
                }
            }
        }
    }

    private void runInInterceptorAndValidateWss(
        Document document, AssertionInfoMap aim, List<CoverageType> types,
        Map<QName, SecurityPolicyValidator> validators
    ) throws Exception {

        PolicyBasedWSS4JInInterceptor inHandler = this.getInInterceptor(types);

        SoapMessage inmsg = this.getSoapMessageForDom(document, aim);

        Element securityHeaderElem = WSSecurityUtil.getSecurityHeader(document, "");
        if (securityHeaderElem != null) {
            SoapHeader securityHeader = new SoapHeader(new QName(securityHeaderElem.getNamespaceURI(),
                                                                 securityHeaderElem.getLocalName()),
                                                       securityHeaderElem);
            inmsg.getHeaders().add(securityHeader);
        }

        if (validators != null) {
            inmsg.put(SecurityConstants.POLICY_VALIDATOR_MAP, validators);
        }

        inHandler.handleMessage(inmsg);

        for (CoverageType type : types) {
            switch(type) {
            case SIGNED:
                this.verifyWss4jSigResults(inmsg);
                break;
            case ENCRYPTED:
                this.verifyWss4jEncResults(inmsg);
                break;
            default:
                fail("Unsupported coverage type.");
            }
        }
    }

    private static final class NOOpPolicyValidator implements SecurityPolicyValidator {

        @Override
        public boolean canValidatePolicy(AssertionInfo assertionInfo) {
            return true;
        }

        @Override
        public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
        }

    };

}
