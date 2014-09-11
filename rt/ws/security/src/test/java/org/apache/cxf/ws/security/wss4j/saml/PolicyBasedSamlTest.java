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
package org.apache.cxf.ws.security.wss4j.saml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

<<<<<<< HEAD
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.wss4j.AbstractPolicySecurityTest;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
=======
import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.AbstractPolicySecurityTest;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.wss4j.dom.validate.SamlAssertionValidator;
import org.apache.wss4j.policy.SP12Constants;
>>>>>>> a797797... Fixing tests following WSS4J upgrades + adding some SAML Subject Confirmation Method tests
import org.junit.Test;

/**
 * A test for using SAML Assertions via WS-SecurityPolicy expressions.
 */
public class PolicyBasedSamlTest extends AbstractPolicySecurityTest {

    @Test
    public void testSaml1Assertion() throws Exception {
        //
        // This should fail as the policy wants a SAML1 assertion and none is in the request
        //
        this.runInInterceptorAndValidate(
                "wsse-request-clean.xml",
                "saml_assertion_policy.xml",
                null,
                Arrays.asList(SP12Constants.SAML_TOKEN, SP12Constants.SUPPORTING_TOKENS),
                new ArrayList<CoverageType>());
        //
        // This should pass as the policy wants a SAML1 assertion and it is in the request
        //
        this.runInInterceptorAndValidate(
                "saml_request.xml",
                "saml_assertion_policy.xml",
                Arrays.asList(SP12Constants.SAML_TOKEN),
                null,
                new ArrayList<CoverageType>());
        //
        // This should fail as the policy wants a SAML1 assertion and a SAML2 Assertion
        // is in the request
        //
        this.runInInterceptorAndValidate(
                "saml2_request.xml",
                "saml_assertion_policy.xml",
                null,
                Arrays.asList(SP12Constants.SAML_TOKEN),
                new ArrayList<CoverageType>());
    }
     
    @Test
    public void testSaml2Assertion() throws Exception {
        //
        // This should fail as the policy wants a SAML2 assertion and none is in the request
        //
        this.runInInterceptorAndValidate(
                "wsse-request-clean.xml",
                "saml2_assertion_policy.xml",
                null,
                Arrays.asList(SP12Constants.SAML_TOKEN, SP12Constants.SUPPORTING_TOKENS),
                new ArrayList<CoverageType>());
        //
        // This should pass as the policy wants a SAML2 assertion and it is in the request
        //
        this.runInInterceptorAndValidate(
                "saml2_request.xml",
                "saml2_assertion_policy.xml",
                Arrays.asList(SP12Constants.SAML_TOKEN),
                null,
                new ArrayList<CoverageType>());
        //
        // This should fail as the policy wants a SAML2 assertion and a SAML1 Assertion
        // is in the request
        //
        this.runInInterceptorAndValidate(
                "saml_request.xml",
                "saml2_assertion_policy.xml",
                null,
                Arrays.asList(SP12Constants.SAML_TOKEN),
                new ArrayList<CoverageType>());
    }
    
    @Override
    protected void runInInterceptorAndValidateWss(Document document, AssertionInfoMap aim,
                                                  List<CoverageType> types) throws Exception {
                                              
        PolicyBasedWSS4JInInterceptor inHandler = 
            this.getInInterceptor(types);

        SoapMessage inmsg = this.getSoapMessageForDom(document, aim);
        
        // Necessary because the Bearer Assertion does not have an internal signature
        SamlAssertionValidator assertionValidator = new SamlAssertionValidator();
        assertionValidator.setRequireBearerSignature(false);
        inmsg.setContextualProperty(SecurityConstants.SAML2_TOKEN_VALIDATOR, assertionValidator);
        inmsg.setContextualProperty(SecurityConstants.SAML1_TOKEN_VALIDATOR, assertionValidator);
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

}
