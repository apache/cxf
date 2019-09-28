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

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.neethi.Policy;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.AsymmetricBinding;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CustomPolicyAlgorithmsTest extends AbstractPolicySecurityTest {

    @Test
    public void testSHA256AsymSigAlgorithm() throws Exception {

        final String rsaSha2SigMethod = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
        String policyName = "signed_elements_policy.xml";
        Policy policy = policyBuilder.getPolicy(this.getResourceAsStream(policyName));
        AssertionInfoMap aim = new AssertionInfoMap(policy);

        AssertionInfo assertInfo = aim.get(SP12Constants.ASYMMETRIC_BINDING).iterator().next();

        AsymmetricBinding binding = (AsymmetricBinding) assertInfo.getAssertion();

        // set Signature Algorithm to RSA SHA-256
        binding.getAlgorithmSuite().getAlgorithmSuiteType().setAsymmetricSignature(rsaSha2SigMethod);

        String sigMethod = binding.getAlgorithmSuite().getAlgorithmSuiteType().getAsymmetricSignature();

        assertNotNull(sigMethod);
        assertEquals(rsaSha2SigMethod, sigMethod);
    }

}
