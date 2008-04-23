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

import java.io.InputStream;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.policy.builder.xml.XMLPrimitiveAssertionBuilder;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PolicyBuilderTest extends Assert {
    
    private PolicyBuilderImpl builder;
    
    @Before
    public void setUp() {
        builder = new PolicyBuilderImpl();
        AssertionBuilderRegistry abr = new AssertionBuilderRegistryImpl();
        builder.setAssertionBuilderRegistry(abr);
        AssertionBuilder ab = new XMLPrimitiveAssertionBuilder();
        abr.register(new QName("http://sample.org/Assertions", "A"), ab);
        abr.register(new QName("http://sample.org/Assertions", "B"), ab);
        abr.register(new QName("http://sample.org/Assertions", "C"), ab);
    }
    
    @Test
    public void testGetPolicy() throws Exception {
        String name = "/samples/test25.xml";
        InputStream is = PolicyBuilderTest.class.getResourceAsStream(name);        
        
        Policy p = builder.getPolicy(is);
        assertNotNull(p);
        List<PolicyComponent> a = CastUtils.cast(p.getAssertions(), PolicyComponent.class);
        assertEquals(3, a.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(Constants.TYPE_ASSERTION, a.get(i).getType());
        }
    }
    
    @Test
    public void testGetPolicyReference() throws Exception {
        String name = "/samples/test26.xml";
        InputStream is = PolicyBuilderTest.class.getResourceAsStream(name);        
        
        PolicyReference pr = builder.getPolicyReference(is);
        assertEquals("#PolicyA", pr.getURI());
        
        name = "/samples/test27.xml";
        is = PolicyBuilderTest.class.getResourceAsStream(name);        
        
        pr = builder.getPolicyReference(is);
        assertEquals("http://sample.org/test.wsdl#PolicyA", pr.getURI());   
    }
    
    @Test
    public void testGetPolicyWithAttributes() throws Exception {
        String name = "/samples/test28.xml";
        InputStream is = PolicyBuilderTest.class.getResourceAsStream(name);        
        
        Policy p = builder.getPolicy(is);
        assertNotNull(p);
        assertTrue(p.getAttributes().size() >= 2);
        QName n1 = new QName("nonsattr");
        Object v1 = p.getAttribute(n1);
        assertNotNull(v1);
        QName n2 = new QName("http://x.y.z", "nsattr");
        Object v2 = p.getAttribute(n2);
        assertNotNull(v2);
    }
}
