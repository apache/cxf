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
package org.apache.cxf.systest.ws.wssec11;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.security.policy.custom.AlgorithmSuiteLoader;
import org.apache.neethi.Policy;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.AlgorithmSuite;

/**
 * This class retrieves a custom AlgorithmSuite for use with restricted security policies
 */
public class RestrictedAlgorithmSuiteLoader implements AlgorithmSuiteLoader {

    public RestrictedAlgorithmSuiteLoader(Bus bus) {
        bus.setExtension(this, AlgorithmSuiteLoader.class);
    }

    public AlgorithmSuite getAlgorithmSuite(Bus bus, SPConstants.SPVersion version, Policy nestedPolicy) {
        return new CustomAlgorithmSuite(version, nestedPolicy);
    }

    private static class CustomAlgorithmSuite extends AlgorithmSuite {

        CustomAlgorithmSuite(SPConstants.SPVersion version, Policy nestedPolicy) {
            super(version, nestedPolicy);

            for (String key : ALGORITHM_SUITE_TYPES.keySet()) {
                AlgorithmSuiteType algSuite = ALGORITHM_SUITE_TYPES.get(key);
                AlgorithmSuiteType newAlgSuite =
                    new AlgorithmSuiteType(algSuite.getName(), algSuite.getDigest(),
                                           algSuite.getEncryption(), algSuite.getSymmetricKeyWrap(),
                                           algSuite.getAsymmetricKeyWrap(), algSuite.getEncryptionKeyDerivation(),
                                           algSuite.getSignatureKeyDerivation(),
                                           algSuite.getEncryptionDerivedKeyLength(),
                                           algSuite.getSignatureDerivedKeyLength(),
                                           algSuite.getMinimumSymmetricKeyLength(),
                                           algSuite.getMaximumSymmetricKeyLength(), 512,
                                           algSuite.getMaximumAsymmetricKeyLength());
                ALGORITHM_SUITE_TYPES.put(key, newAlgSuite);
            }
        }

        @Override
        protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
            return new CustomAlgorithmSuite(getVersion(), nestedPolicy);
        }

    }
}
