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

package org.apache.cxf.ws.policy.builder.jaxb;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.test.assertions.foo.FooType;
import org.apache.cxf.ws.policy.builder.xml.XmlPrimitiveAssertion;
import org.apache.neethi.All;
import org.apache.neethi.Constants;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class JaxbAssertionTest extends Assert {

    @Test
    public void testBasic() {
        JaxbAssertion<FooType> assertion = new JaxbAssertion<FooType>();
        assertNull(assertion.getName());
        assertNull(assertion.getData());
        assertTrue(!assertion.isOptional());
        assertEquals(Constants.TYPE_ASSERTION, assertion.getType());
        FooType data = new FooType();
        data.setName("CXF");
        data.setNumber(2);
        QName qn = new QName("http://cxf.apache.org/test/assertions/foo", "FooType");
        assertion.setName(qn);
        assertion.setData(data);
        assertion.setOptional(true);
        assertSame(qn, assertion.getName());
        assertSame(data, assertion.getData());
        assertTrue(assertion.isOptional());
        assertEquals(Constants.TYPE_ASSERTION, assertion.getType());
    }
    
    @Test
    public void testEqual() {
        JaxbAssertion<FooType> assertion = new JaxbAssertion<FooType>();
        FooType data = new FooType();
        data.setName("CXF");
        data.setNumber(2);
        QName qn = new QName("http://cxf.apache.org/test/assertions/foo", "FooType");
        assertion.setName(qn);
        assertion.setData(data);
        
        PolicyComponent pc = new Policy();
        assertTrue(!assertion.equal(pc));
        pc = (PolicyComponent)new All();
        assertTrue(!assertion.equal(pc));
        pc = (PolicyComponent)new ExactlyOne();
        assertTrue(!assertion.equal(pc));
        
        IMocksControl ctrl = EasyMock.createNiceControl();
        XmlPrimitiveAssertion xpa = ctrl.createMock(XmlPrimitiveAssertion.class);
        QName oqn = new QName("http://cxf.apache.org/test/assertions/blah", "OtherType");
        EasyMock.expect(xpa.getName()).andReturn(oqn);
        EasyMock.expect(xpa.getType()).andReturn(Constants.TYPE_ASSERTION);
        
        ctrl.replay();
        assertTrue(!assertion.equal(xpa));
        ctrl.verify();
            
        FooType odata = new FooType();
        odata.setName(data.getName());
        odata.setNumber(data.getNumber());
        JaxbAssertion<FooType> oassertion = new JaxbAssertion<FooType>();
        oassertion.setData(odata);
        oassertion.setName(qn);
        assertTrue(!assertion.equal(oassertion));  
        oassertion.setData(data);
        assertTrue(assertion.equal(oassertion));
        assertTrue(assertion.equal(assertion));          
    }
    
    @Test
    public void testNormalise() {
        JaxbAssertion<FooType> assertion = new JaxbAssertion<FooType>();
        FooType data = new FooType();
        data.setName("CXF");
        data.setNumber(2);
        QName qn = new QName("http://cxf.apache.org/test/assertions/foo", "FooType");
        assertion.setName(qn);
        assertion.setData(data);

        JaxbAssertion normalised = (JaxbAssertion)assertion.normalize();
        assertTrue(normalised.equal(assertion));       
        assertSame(assertion.getData(), normalised.getData()); 
        
        assertion.setOptional(true);        
        PolicyComponent pc = assertion.normalize();
        assertEquals(Constants.TYPE_POLICY, pc.getType());
        Policy p = (Policy)pc; 
        Iterator alternatives = p.getAlternatives();

        int total = 0;
        for (int i = 0; i < 2; i++) {
            List<PolicyComponent> pcs = 
                CastUtils.cast((List<?>)(alternatives.next()), PolicyComponent.class);
            if (!pcs.isEmpty()) {
                assertTrue(assertion.equal(pcs.get(0)));
                total += pcs.size();
            }
        }
        assertTrue(!alternatives.hasNext());
        assertEquals(1, total);    
    }
}
