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

package org.apache.cxf.ws.policy.selector;

import java.util.Collection;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.policy.AlternativeSelector;
import org.apache.cxf.ws.policy.Assertor;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.TestAssertion;
import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class MinimalMaximalAlternativeSelectorTest {
    @Test
    public void testChooseMinAlternative() {
        Message m = new MessageImpl();
        AlternativeSelector selector = new MinimalAlternativeSelector();

        PolicyEngine engine = mock(PolicyEngine.class);
        Assertor assertor = mock(Assertor.class);

        Policy policy = new Policy();
        ExactlyOne ea = new ExactlyOne();
        All all = new All();
        PolicyAssertion a1 = new TestAssertion();
        all.addAssertion(a1);
        ea.addPolicyComponent(all);
        Collection<PolicyAssertion> maxAlternative =
            CastUtils.cast(all.getPolicyComponents(), PolicyAssertion.class);
        all = new All();
        ea.addPolicyComponent(all);
        Collection<PolicyAssertion> minAlternative =
            CastUtils.cast(all.getPolicyComponents(), PolicyAssertion.class);
        policy.addPolicyComponent(ea);
        when(engine.supportsAlternative(maxAlternative, assertor, m)).thenReturn(true);
        when(engine.supportsAlternative(minAlternative, assertor, m)).thenReturn(true);

        Collection<Assertion> choice =
            selector.selectAlternative(policy, engine, assertor, null, m);
        assertEquals(0, choice.size());
    }

    @Test
    public void testChooseMaxAlternative() {
        Message m = new MessageImpl();
        AlternativeSelector selector = new MaximalAlternativeSelector();

        PolicyEngine engine = mock(PolicyEngine.class);
        Assertor assertor = mock(Assertor.class);

        Policy policy = new Policy();
        ExactlyOne ea = new ExactlyOne();
        All all = new All();
        PolicyAssertion a1 = new TestAssertion();
        all.addAssertion(a1);
        ea.addPolicyComponent(all);
        Collection<PolicyAssertion> maxAlternative =
            CastUtils.cast(all.getPolicyComponents(), PolicyAssertion.class);
        all = new All();
        ea.addPolicyComponent(all);
        Collection<PolicyAssertion> minAlternative =
            CastUtils.cast(all.getPolicyComponents(), PolicyAssertion.class);
        policy.addPolicyComponent(ea);
        when(engine.supportsAlternative(maxAlternative, assertor, m)).thenReturn(true);
        when(engine.supportsAlternative(minAlternative, assertor, m)).thenReturn(true);

        Collection<Assertion> choice = selector.selectAlternative(policy, engine, assertor, null, m);
        assertEquals(1, choice.size());
        assertSame(a1, choice.iterator().next());
    }
}