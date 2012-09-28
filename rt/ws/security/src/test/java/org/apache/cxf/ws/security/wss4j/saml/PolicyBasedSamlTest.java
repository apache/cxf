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

import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.wss4j.AbstractPolicySecurityTest;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
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
    
}
