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

package org.apache.cxf.ws.policy.attachment.external;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.ws.policy.PolicyException;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class DomainExpressionBuilderRegistryTest extends Assert {

    private IMocksControl control;
    
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();        
    } 
    
    @Test
    public void testNoBuilder() {
        DomainExpressionBuilderRegistry reg = new DomainExpressionBuilderRegistry();
        assertEquals(DomainExpressionBuilderRegistry.class, reg.getRegistrationType());
        
        Element e = control.createMock(Element.class); 
        EasyMock.expect(e.getNamespaceURI()).andReturn("http://a.b.c");
        EasyMock.expect(e.getLocalName()).andReturn("x");
        
        control.replay();
        try {
            reg.build(e);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        control.verify();
        
    }
    
    @Test
    public void testBuild() {
        DomainExpressionBuilder builder = control.createMock(DomainExpressionBuilder.class);
        Map<QName, DomainExpressionBuilder> builders = new HashMap<QName, DomainExpressionBuilder>();
        QName qn = new QName("http://a.b.c", "x");
        builders.put(qn, builder);
        DomainExpressionBuilderRegistry reg = new DomainExpressionBuilderRegistry(builders);
        
        Element e = control.createMock(Element.class); 
        EasyMock.expect(e.getNamespaceURI()).andReturn("http://a.b.c");
        EasyMock.expect(e.getLocalName()).andReturn("x");
        DomainExpression de = control.createMock(DomainExpression.class); 
        EasyMock.expect(builder.build(e)).andReturn(de);
        
        control.replay();
        assertSame(de, reg.build(e));
        control.verify();     
    }
}
