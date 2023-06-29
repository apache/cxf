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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DomainExpressionBuilderRegistryTest {
    @Test
    public void testNoBuilder() {
        DomainExpressionBuilderRegistry reg = new DomainExpressionBuilderRegistry();
        assertEquals(DomainExpressionBuilderRegistry.class, reg.getRegistrationType());

        Element e = mock(Element.class);
        when(e.getNamespaceURI()).thenReturn("http://a.b.c");
        when(e.getLocalName()).thenReturn("x");

        try {
            reg.build(e);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
    }

    @Test
    public void testBuild() {
        DomainExpressionBuilder builder = mock(DomainExpressionBuilder.class);
        Map<QName, DomainExpressionBuilder> builders = new HashMap<>();
        QName qn = new QName("http://a.b.c", "x");
        builders.put(qn, builder);
        DomainExpressionBuilderRegistry reg = new DomainExpressionBuilderRegistry(builders);

        Element e = mock(Element.class);
        when(e.getNamespaceURI()).thenReturn("http://a.b.c");
        when(e.getLocalName()).thenReturn("x");
        DomainExpression de = mock(DomainExpression.class);
        when(builder.build(e)).thenReturn(de);

        assertSame(de, reg.build(e));
    }
}