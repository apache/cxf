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
package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.policy.PolicyCalculator;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyDataEngineImplTest {
    private static final QName TEST_POLICY_NAME = new QName("http://test", "TestPolicy");

    class TestPolicy {
    }

    class TestPolicyCalculator implements PolicyCalculator<TestPolicy> {

        public Class<TestPolicy> getDataClass() {
            return TestPolicy.class;
        }

        public QName getDataClassName() {
            return TEST_POLICY_NAME;
        }

        public TestPolicy intersect(TestPolicy policy1, TestPolicy policy2) {
            return policy1;
        }

        public boolean isAsserted(Message message, TestPolicy policy, TestPolicy refPolicy) {
            return true;
        }

    }

    public AssertionInfo getTestPolicyAssertionInfo(TestPolicy policy) {
        JaxbAssertion<TestPolicy> assertion =
            new JaxbAssertion<>(TEST_POLICY_NAME, false);
        assertion.setData(policy);
        return new AssertionInfo(assertion);
    }

    @Test
    public void testAssertMessageNullAim() {
        checkAssertWithMap(null);
    }

    @Test
    public void testAssertMessageEmptyAim() {
        checkAssertWithMap(new AssertionInfoMap(CastUtils.cast(Collections.EMPTY_LIST,
                                                               PolicyAssertion.class)));
    }

    @Test
    public void testAssertMessage() {
        TestPolicy policy = new TestPolicy();
        AssertionInfo ai = getTestPolicyAssertionInfo(policy);
        AssertionInfoMap aim = new AssertionInfoMap(CastUtils.cast(Collections.EMPTY_LIST,
                                                                   PolicyAssertion.class));
        Collection<AssertionInfo> ais = new ArrayList<>();
        ais.add(ai);
        aim.put(TEST_POLICY_NAME, ais);
        checkAssertWithMap(aim);
        assertTrue(ai.isAsserted());
    }

    /**
     * Simply check that it runs without any exceptions
     *
     * @param assertionInfoMap
     */
    private void checkAssertWithMap(AssertionInfoMap assertionInfoMap) {
        PolicyDataEngineImpl pde = new PolicyDataEngineImpl(null);
        pde.setPolicyEngine(new PolicyEngineImpl());
        TestPolicy confPol = new TestPolicy();
        PolicyCalculator<TestPolicy> policyCalculator = new TestPolicyCalculator();
        Message message = mock(Message.class);
        when(message.get(TestPolicy.class)).thenReturn(confPol);
        when(message.get(AssertionInfoMap.class)).thenReturn(assertionInfoMap);
        pde.assertMessage(message, confPol, policyCalculator);
    }
}