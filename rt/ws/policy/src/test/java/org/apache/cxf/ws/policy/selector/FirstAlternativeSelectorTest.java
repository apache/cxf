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

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;


/**
 *
 */
public class FirstAlternativeSelectorTest {

    private IMocksControl control;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    @Test
    public void testChooseAlternative() {
        AlternativeSelector selector = new FirstAlternativeSelector();

        PolicyEngine engine = control.createMock(PolicyEngine.class);
        Assertor assertor = control.createMock(Assertor.class);

        Policy policy = new Policy();
        ExactlyOne ea = new ExactlyOne();
        All all = new All();
        PolicyAssertion a1 = new TestAssertion();
        all.addAssertion(a1);
        ea.addPolicyComponent(all);
        Collection<PolicyAssertion> firstAlternative =
            CastUtils.cast(all.getPolicyComponents(), PolicyAssertion.class);
        policy.addPolicyComponent(ea);
        Message m = new MessageImpl();

        EasyMock.expect(engine.supportsAlternative(firstAlternative, assertor, m)).andReturn(false);
        control.replay();

        assertNull(selector.selectAlternative(policy, engine, assertor, null, m));
        control.verify();

        control.reset();
        EasyMock.expect(engine.supportsAlternative(firstAlternative, assertor, m)).andReturn(true);
        control.replay();
        Collection<Assertion> chosen = selector.selectAlternative(policy, engine, assertor, null, m);
        assertSame(1, chosen.size());
        assertSame(chosen.size(), firstAlternative.size());
        assertSame(chosen.iterator().next(), firstAlternative.iterator().next());
        control.verify();

        control.reset();
        All other = new All();
        other.addAssertion(a1);
        ea.addPolicyComponent(other);
        Collection<PolicyAssertion> secondAlternative =
            CastUtils.cast(other.getPolicyComponents(), PolicyAssertion.class);
        EasyMock.expect(engine.supportsAlternative(firstAlternative, assertor, m)).andReturn(false);
        EasyMock.expect(engine.supportsAlternative(secondAlternative, assertor, m)).andReturn(true);
        control.replay();

        chosen = selector.selectAlternative(policy, engine, assertor, null, m);
        assertSame(1, chosen.size());
        assertSame(chosen.size(), secondAlternative.size());
        assertSame(chosen.iterator().next(), secondAlternative.iterator().next());
        control.verify();
    }
}