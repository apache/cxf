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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.neethi.Policy;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 */
public class NestedPrimitiveAssertionBuilderTest extends Assert {

    private static final String TEST_NAMESPACE = "http://www.w3.org/2007/01/addressing/metadata";
    private static final QName TEST_NAME1 = new QName(TEST_NAMESPACE, "Addressing");

    private NestedPrimitiveAssertionBuilder npab;
    private IMocksControl control;
    private PolicyBuilder builder;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        npab = new NestedPrimitiveAssertionBuilder();
        npab.setKnownElements(Collections.singletonList(TEST_NAME1));
        builder = control.createMock(PolicyBuilder.class);
        npab.setPolicyBuilder(builder);  
    }
    
    @Test
    public void testSimpleBuildOlderNs() throws Exception {
        String data = 
            "<wsam:Addressing wsp:Optional=\"true\""
            + " xmlns:wsp=\"http://www.w3.org/2006/07/ws-policy\""
            + " xmlns:wsam=\"http://www.w3.org/2007/01/addressing/metadata\" />";
        
        npab.build(getElement(data));
    }
    
    @Test
    public void testBuildDefaultNs() throws Exception {
        String data = 
            "<wsam:Addressing wsp:Optional=\"true\""
            + " xmlns:wsp=\"http://www.w3.org/ns/ws-policy\""
            + " xmlns:wsam=\"http://www.w3.org/2007/01/addressing/metadata\">"
            + "<wsp:Policy/></wsam:Addressing>";
 
        Policy nested = control.createMock(Policy.class);
        EasyMock.expect(builder.getPolicy(EasyMock.isA(Element.class))).andReturn(nested);
        control.replay();
        NestedPrimitiveAssertion npc = (NestedPrimitiveAssertion)npab.build(getElement(data));
        assertEquals(TEST_NAME1, npc.getName());
        assertSame(nested, npc.getPolicy());
        assertTrue(npc.isOptional());
        control.verify();
    }
    
    @Test
    public void testBuildOlderNs() throws Exception {
        String data = 
            "<wsam:Addressing wsp:Optional=\"true\""
            + " xmlns:wsp=\"http://www.w3.org/2006/07/ws-policy\""
            + " xmlns:wsam=\"http://www.w3.org/2007/01/addressing/metadata\">"
            + "<wsp:Policy/></wsam:Addressing>";
 
        Policy nested = control.createMock(Policy.class);
        EasyMock.expect(builder.getPolicy(EasyMock.isA(Element.class))).andReturn(nested);
        
        Bus bus = control.createMock(Bus.class);
        control.replay();
        
        npab.setBus(bus);
        NestedPrimitiveAssertion npc = (NestedPrimitiveAssertion)npab.build(getElement(data));
        assertEquals(TEST_NAME1, npc.getName());
        assertSame(nested, npc.getPolicy());
        assertTrue(npc.isOptional());
        control.verify();
    }
    
    Element getElement(String data) throws Exception {
        InputStream is = new ByteArrayInputStream(data.getBytes());
        return DOMUtils.readXml(is).getDocumentElement();
    }
}
