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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.policy.builder.primitive.NestedPrimitiveAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
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
public class AssertionInfoMapTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();        
    } 
    
    @Test
    public void testAlternativeSupported() {
        PolicyAssertion a1 = control.createMock(PolicyAssertion.class);
        QName aqn = new QName("http://x.y.z", "a");
        EasyMock.expect(a1.getName()).andReturn(aqn).anyTimes();
        PolicyAssertion a2 = control.createMock(PolicyAssertion.class);
        EasyMock.expect(a2.getName()).andReturn(aqn).anyTimes();
        PolicyAssertion b = control.createMock(PolicyAssertion.class);
        QName bqn = new QName("http://x.y.z", "b");
        EasyMock.expect(b.getName()).andReturn(bqn).anyTimes();
        PolicyAssertion c = control.createMock(PolicyAssertion.class);
        QName cqn = new QName("http://x.y.z", "c");
        EasyMock.expect(c.getName()).andReturn(cqn).anyTimes();
        AssertionInfoMap aim = new AssertionInfoMap(CastUtils.cast(Collections.EMPTY_LIST, 
                                                                   PolicyAssertion.class));
        AssertionInfo ai1 = new AssertionInfo(a1);
        AssertionInfo ai2 = new AssertionInfo(a2);
        Collection<AssertionInfo> ais = new ArrayList<AssertionInfo>();
        AssertionInfo bi = new AssertionInfo(b);
        AssertionInfo ci = new AssertionInfo(c);
        ais.add(ai1);
        ais.add(ai2);
        aim.put(aqn, ais);
        aim.put(bqn, Collections.singleton(bi));
        aim.put(cqn, Collections.singleton(ci));
        ai2.setAsserted(true);
        bi.setAsserted(true);
        ci.setAsserted(true);
        EasyMock.expect(a1.equal(a1)).andReturn(true).anyTimes();
        EasyMock.expect(a2.equal(a2)).andReturn(true).anyTimes();
        EasyMock.expect(b.equal(b)).andReturn(true).anyTimes();
        EasyMock.expect(c.equal(c)).andReturn(true).anyTimes();
        
        EasyMock.expect(a2.isAsserted(aim)).andReturn(true).anyTimes();
        EasyMock.expect(b.isAsserted(aim)).andReturn(true).anyTimes();
        EasyMock.expect(c.isAsserted(aim)).andReturn(true).anyTimes();
        
        
        List<PolicyAssertion> alt1 = new ArrayList<PolicyAssertion>();
        alt1.add(a1);
        alt1.add(b);
        
        List<PolicyAssertion> alt2 = new ArrayList<PolicyAssertion>();
        alt2.add(a2);
        alt2.add(c);
                
        control.replay();
        assertTrue(!aim.supportsAlternative(alt1, new ArrayList<QName>()));
        assertTrue(aim.supportsAlternative(alt2, new ArrayList<QName>()));
        control.verify();     
    }  
    
    @Test
    public void testCheckEffectivePolicy() { 
        Policy p = new Policy();
        QName aqn = new QName("http://x.y.z", "a");
        PolicyAssertion a = new PrimitiveAssertion(aqn);
        QName bqn = new QName("http://x.y.z", "b");
        PolicyAssertion b = new PrimitiveAssertion(bqn);
        QName cqn = new QName("http://x.y.z", "c");
        PolicyAssertion c = new PrimitiveAssertion(cqn);
        All alt1 = new All();
        alt1.addAssertion(a);
        alt1.addAssertion(b);
        All alt2 = new All();
        alt2.addAssertion(c);
        ExactlyOne ea = new ExactlyOne();
        ea.addPolicyComponent(alt1);
        ea.addPolicyComponent(alt2);
        p.addPolicyComponent(ea);   
        AssertionInfoMap aim = new AssertionInfoMap(CastUtils.cast(Collections.EMPTY_LIST, 
                                                                   PolicyAssertion.class));
        AssertionInfo ai = new AssertionInfo(a);
        AssertionInfo bi = new AssertionInfo(b);
        AssertionInfo ci = new AssertionInfo(c);
        aim.put(aqn, Collections.singleton(ai));
        aim.put(bqn, Collections.singleton(bi));
        aim.put(cqn, Collections.singleton(ci));
        
        try {
            aim.checkEffectivePolicy(p);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        
        ai.setAsserted(true);
        ci.setAsserted(true);
        
        aim.checkEffectivePolicy(p);
    } 
    
    @Test
    public void testCheck() throws PolicyException {
        QName aqn = new QName("http://x.y.z", "a");
        PolicyAssertion a = new PrimitiveAssertion(aqn);
        Collection<PolicyAssertion> assertions = new ArrayList<PolicyAssertion>();
        assertions.add(a);
        AssertionInfoMap aim = new AssertionInfoMap(assertions);
        try {
            aim.check();
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            assertEquals("NOT_ASSERTED_EXC", ex.getCode());
        }
        aim.get(aqn).iterator().next().setAsserted(true);
        aim.check();
    }
    
    @Test
    public void testAllAssertionsIn() { 
        
        Policy nested = new Policy();
        PolicyAssertion nb = new PrimitiveAssertion(
            new QName("http://x.y.z", "b"));
        nested.addAssertion(nb);
        
        Policy p = new Policy();
        PolicyAssertion a1 = new PrimitiveAssertion(
                                new QName("http://x.y.z", "a"));
        PolicyAssertion a2 = new PrimitiveAssertion(
                                 new QName("http://x.y.z", "a"));
        PolicyAssertion b = new PrimitiveAssertion(
                                new QName("http://x.y.z", "b"));
        PolicyAssertion c = new NestedPrimitiveAssertion(
                               new QName("http://x.y.z", "c"), false, nested, true);
        
        All alt1 = new All();
        alt1.addAssertion(a1);
        alt1.addAssertion(b);
        All alt2 = new All();
        alt1.addAssertion(a2);
        alt2.addAssertion(c);
        ExactlyOne ea = new ExactlyOne();
        ea.addPolicyComponent(alt1);
        ea.addPolicyComponent(alt2);
        p.addPolicyComponent(ea);  
        
        AssertionInfoMap aim = new AssertionInfoMap(p);
        
        Collection<AssertionInfo> listA = 
            aim.getAssertionInfo(new QName("http://x.y.z", "a"));
        assertEquals("2 A assertions should've been added", 2, listA.size());
        AssertionInfo[] ais = listA.toArray(new AssertionInfo[] {});
        assertTrue("Two different A instances should be added", 
                   ais[0].getAssertion() == a1 && ais[1].getAssertion() == a2 
                   || ais[0].getAssertion() == a2 && ais[1].getAssertion() == a1);
        
        Collection<AssertionInfo> listB = 
            aim.getAssertionInfo(new QName("http://x.y.z", "b"));
        assertEquals("2 B assertions should've been added", 2, listB.size());
        ais = listB.toArray(new AssertionInfo[] {});
        assertTrue("Two different B instances should be added", 
                   ais[0].getAssertion() == nb && ais[1].getAssertion() == b 
                   || ais[0].getAssertion() == b && ais[1].getAssertion() == nb);
        
        Collection<AssertionInfo> listC = 
            aim.getAssertionInfo(new QName("http://x.y.z", "c"));
        assertEquals("1 C assertion should've been added", 1, listC.size());
        ais = listC.toArray(new AssertionInfo[] {});
        assertSame("One C instances should be added", 
                   ais[0].getAssertion(), c);
        
    }
}
