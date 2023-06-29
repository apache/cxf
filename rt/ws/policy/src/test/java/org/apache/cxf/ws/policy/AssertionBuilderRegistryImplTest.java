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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.neethi.Assertion;
import org.apache.neethi.builders.PrimitiveAssertion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class AssertionBuilderRegistryImplTest {
    @Test
    public void testBuildUnknownAssertion() {
        Bus bus = mock(Bus.class);

        PolicyBuilder builder = mock(PolicyBuilder.class);
        when(bus.getExtension(PolicyBuilder.class)).thenReturn(builder);

        AssertionBuilderRegistryImpl reg = new AssertionBuilderRegistryImpl() {
            protected void loadDynamic() {
                //nothing
            }
        };
        reg.setIgnoreUnknownAssertions(false);
        Element[] elems = new Element[11];
        QName[] qnames = new QName[11];
        for (int i = 0; i < 11; i++) {
            qnames[i] = new QName("http://my.company.com", "type" + Integer.toString(i));
            elems[i] = mock(Element.class);
            when(elems[i].getNamespaceURI()).thenReturn(qnames[i].getNamespaceURI());
            when(elems[i].getLocalName()).thenReturn(qnames[i].getLocalPart());
        }

        reg.setBus(bus);

        assertFalse(reg.isIgnoreUnknownAssertions());
        try {
            reg.build(elems[0]);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            assertEquals("NO_ASSERTIONBUILDER_EXC", ex.getCode());
        }
        reg.setIgnoreUnknownAssertions(true);
        assertTrue(reg.isIgnoreUnknownAssertions());
        for (int i = 0; i < 10; i++) {
            Assertion assertion = reg.build(elems[i]);
            assertTrue("Not a PrimitiveAsertion: " + assertion.getClass().getName(),
                       assertion instanceof PrimitiveAssertion);
        }
        for (int i = 9; i >= 0; i--) {
            assertTrue(reg.build(elems[i]) instanceof PrimitiveAssertion);
        }
        assertTrue(reg.build(elems[10]) instanceof PrimitiveAssertion);
    }
}
