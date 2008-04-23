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

import java.util.Collections;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertionBuilder;
import org.apache.neethi.Policy;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class IntersectorTest extends Assert {

    private static final QName NAME1 = new QName("http://x.y.z", "a");
    private static final QName NAME2 = new QName("http://x.y.z", "a");
    
    private IMocksControl control = EasyMock.createNiceControl();
    private Intersector intersector;
    private AssertionBuilderRegistry reg;    
    private PrimitiveAssertionBuilder pab1;
    private PrimitiveAssertionBuilder pab2;
    
    @Before
    public void setUp() {
        reg = control.createMock(AssertionBuilderRegistry.class);
        intersector = new Intersector(reg);
        pab1 = new PrimitiveAssertionBuilder();
        pab1.setKnownElements(Collections.singleton(NAME1));
        pab2 = new PrimitiveAssertionBuilder();
        pab2.setKnownElements(Collections.singleton(NAME2));
    }
   
    @Test
    public void testCompatiblePoliciesBothEmpty() {
        Policy p1 = new Policy();
        Policy p2 = new Policy();
        assertTrue(intersector.compatiblePolicies(p1, p2));
    }
    
    @Test
    public void testCompatiblePoliciesOneEmpty() {
        Policy p1 = new Policy();
        Policy p2 = new Policy();
        p2.addPolicyComponent(new PrimitiveAssertion(NAME1));
        assertTrue(intersector.compatiblePolicies(p1, p2));
    }
    
    @Test
    public void testIntersectPoliciesBothEmpty() {
        Policy p1 = new Policy();
        Policy p2 = new Policy();
        Policy p = intersector.intersect(p1, p2);
        assertNotNull(p);
        // control.replay();
    }
    
}
