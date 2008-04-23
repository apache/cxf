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
import org.apache.cxf.ws.policy.AlternativeSelector;
import org.apache.cxf.ws.policy.Assertor;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.TestAssertion;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 */
public class MinimalMaximalAlternativeSelectorTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl(); 
    } 
    
    @Test
    public void testChooseMinAlternative() {
        AlternativeSelector selector = new MinimalAlternativeSelector();
        
        PolicyEngine engine = control.createMock(PolicyEngine.class);
        Assertor assertor = control.createMock(Assertor.class);
               
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
        EasyMock.expect(engine.supportsAlternative(maxAlternative, assertor)).andReturn(true);
        EasyMock.expect(engine.supportsAlternative(minAlternative, assertor)).andReturn(true);
        
        control.replay();        
        Collection<PolicyAssertion> choice = 
            selector.selectAlternative(policy, engine, assertor); 
        assertEquals(0, choice.size());
        control.verify();
    }
    
    @Test
    public void testChooseMaxAlternative() {
        AlternativeSelector selector = new MaximalAlternativeSelector();
        
        PolicyEngine engine = control.createMock(PolicyEngine.class);
        Assertor assertor = control.createMock(Assertor.class);
               
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
        EasyMock.expect(engine.supportsAlternative(maxAlternative, assertor)).andReturn(true);
        EasyMock.expect(engine.supportsAlternative(minAlternative, assertor)).andReturn(true);
        
        control.replay();        
        Collection<PolicyAssertion> choice = selector.selectAlternative(policy, engine, assertor); 
        assertEquals(1, choice.size());
        assertSame(a1, choice.iterator().next());
        control.verify();
    }
}
