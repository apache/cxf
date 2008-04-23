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

package org.apache.cxf.ws.policy.builder.primitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 */
public class NestedPrimitiveAssertionTest extends Assert {

    private static final String TEST_NAMESPACE = "http://www.w3.org/2007/01/addressing/metadata";
    private static final QName TEST_NAME1 = new QName(TEST_NAMESPACE, "Addressing");
    private static final QName TEST_NAME2 = new QName(TEST_NAMESPACE, "AnonymousResponses");
    private static final QName TEST_NAME3 = new QName(TEST_NAMESPACE, "NonAnonymousResponses");
    
    public class CustomPrimitiveAssertion extends PrimitiveAssertion {
        private int x;
        public CustomPrimitiveAssertion(QName type, int x) {
            super(type, false);
            this.x = x;
        }
        
        @Override
        public boolean equal(PolicyComponent pc) {
            if (!(pc instanceof CustomPrimitiveAssertion)
                || !super.equal(pc)) {
                return false;
            }
            return x == ((CustomPrimitiveAssertion)pc).x;
        }
    }
    
    private Policy[] policies;
    
    
    @Before
    public void setUp() {
        policies = buildTestPolicies();
    }
    
    @Test
    public void testNoNeedToAssertWithEmptyPolicy() {
        PolicyAssertion a = new NestedPrimitiveAssertion(
                                new QName("abc"), false, null, false);
        AssertionInfoMap aim = new AssertionInfoMap(
                                Collections.singletonList(a));
        assertTrue("No need to assert", a.isAsserted(aim));
        a = new NestedPrimitiveAssertion(new QName("abc"), 
                                         false, 
                                         null, 
                                         false);
        assertTrue("No need to assert", a.isAsserted(aim));
    }
    
    
    @Test
    public void testNoNeedToAssertWithNonEmptyPolicy() {
        PolicyAssertion a = new NestedPrimitiveAssertion(
                                new QName("abc"), false, null, false);
        AssertionInfoMap aim = new AssertionInfoMap(
                                Collections.singletonList(a));
        assertTrue("No need to assert", a.isAsserted(aim));
        Policy p = new Policy();
        p.addAssertion(new PrimitiveAssertion(new QName("abc"), false));
        a = new NestedPrimitiveAssertion(new QName("abc"), 
                                         false, 
                                         p, 
                                         false);
        assertFalse("Primitive Assertions need to be asserted", 
                    a.isAsserted(aim));
        
        p = new Policy();
        p.addAssertion(new NestedPrimitiveAssertion(new QName("abc"), 
                                         false, 
                                         null, 
                                         false));
        a = new NestedPrimitiveAssertion(new QName("abc"), 
                                         false, 
                                         p, 
                                         false);
        assertTrue("No need to assert", a.isAsserted(aim));
    }
    
    
    @Test
    public void testAsserted() {
        PolicyAssertion a1 = 
            new CustomPrimitiveAssertion(new QName("abc"), 1);
        PolicyAssertion a2 = 
            new CustomPrimitiveAssertion(new QName("abc"), 2);
        Policy nested = new Policy();
        All all = new All();
        all.addAssertion(a2);
        nested.addPolicyComponent(all);
        
        NestedPrimitiveAssertion na = new NestedPrimitiveAssertion(new QName("nested"), 
                                         false, 
                                         nested,
                                         true);
        List<PolicyAssertion> ais = 
            new ArrayList<PolicyAssertion>();
        
        ais.add(a1);
        ais.add(a2);
        ais.add(na);
                
        AssertionInfoMap aim = new AssertionInfoMap(ais);
        
        assertFalse("Assertion has been asserted even though nether na nor a2 have been", 
                    na.isAsserted(aim));
        
        assertAssertion(aim, new QName("nested"), true, true);
        assertFalse("Assertion has been asserted even though a2 has not been", 
                    na.isAsserted(aim));
        
        // assert a1 only
        assertAssertion(aim, new QName("abc"), true, false);
        assertFalse("Assertion has been asserted even though a2 has not been", 
                    na.isAsserted(aim));
        // assert a2 tpp
        assertAssertion(aim, new QName("abc"), true, true);
        assertTrue("Assertion has not been asserted even though both na nad a2 have been", 
                    na.isAsserted(aim));
        
        PolicyAssertion a3 = new CustomPrimitiveAssertion(new QName("abc"), 3);
        all.addAssertion(a3);
        aim.getAssertionInfo(new QName("abc")).add(new AssertionInfo(a3));
        
        assertFalse("Assertion has been asserted even though a3 has not been", 
                    na.isAsserted(aim));
        
        assertAssertion(aim, new QName("abc"), true, true);
        assertTrue("Assertion has not been asserted even though na,a2,a3 have been", 
                   na.isAsserted(aim));
        
    }
    
    private void assertAssertion(AssertionInfoMap aim, 
                                 QName type, 
                                 boolean value,
                                 boolean all) {
        Collection<AssertionInfo> aic = aim.getAssertionInfo(type);
        if (!all) {
            AssertionInfo ai = aic.iterator().next();
            ai.setAsserted(value);
        } else {
            for (AssertionInfo ai : aic) {
                ai.setAsserted(value);
            }
        }
    }
    
       
    @Test
    public void testEqual() {
        PolicyAssertion other = new PrimitiveAssertion(new QName("abc"));
        for (int i = 0; i < policies.length; i++) {
            PolicyAssertion a = 
                (PolicyAssertion)policies[i].getFirstPolicyComponent();
            assertTrue("Assertion " + i + " should equal itself.", a.equal(a)); 
            assertTrue("Assertion " + i + " should not equal other.", !a.equal(other)); 
            for (int j = i + 1; j < policies.length; j++) {
                Assertion b = (Assertion)policies[j].getFirstPolicyComponent();
                if (j == 1) {
                    assertTrue("Assertion " + i + " should equal " + j + ".", a.equal(b));
                } else {
                    assertTrue("Assertion " + i + " unexpectedly equals assertion " + j + ".", !a.equal(b));
                }
            }
        }
    }
    
    protected static Policy[] buildTestPolicies() {
        Policy[] p = new Policy[5];
        int i = 0;
        
        p[i] = new Policy();
        NestedPrimitiveAssertion a = new NestedPrimitiveAssertion(TEST_NAME1, true);
        Policy nested = new Policy();
        a.setPolicy(nested);
        p[i++].addPolicyComponent(a);
        
        p[i] = new Policy();
        a = new NestedPrimitiveAssertion(TEST_NAME1, false);
        nested = new Policy();
        a.setPolicy(nested);
        p[i++].addPolicyComponent(a);
        
        p[i] = new Policy();
        a = new NestedPrimitiveAssertion(TEST_NAME1, false);
        nested = new Policy();
        a.setPolicy(nested);
        nested.addPolicyComponent(new PrimitiveAssertion(TEST_NAME2, true));
        nested.addPolicyComponent(new PrimitiveAssertion(TEST_NAME3, true));
        p[i++].addPolicyComponent(a);
        
        p[i] = new Policy();
        a = new NestedPrimitiveAssertion(TEST_NAME1, false);
        nested = new Policy();
        a.setPolicy(nested);
        ExactlyOne eo = new ExactlyOne();
        nested.addPolicyComponent(eo);
        eo.addPolicyComponent(new PrimitiveAssertion(TEST_NAME2));
        eo.addPolicyComponent(new PrimitiveAssertion(TEST_NAME3));  
        p[i++].addPolicyComponent(a);
        
        p[i] = new Policy();
        a = new NestedPrimitiveAssertion(TEST_NAME1, false);
        nested = new Policy();
        a.setPolicy(nested);
        nested.addPolicyComponent(new PrimitiveAssertion(TEST_NAME3));  
        p[i++].addPolicyComponent(a); 
        
        return p;
    }
}
