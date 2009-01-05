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

package org.apache.cxf.tools.validator.internal.model;

import org.apache.cxf.common.WSDLConstants;

import org.junit.Assert;
import org.junit.Test;

public class XNodeTest extends Assert {
    @Test
    public void testWSDLDefinition() {
        XDef def = new XDef();
        assertEquals("/wsdl:definitions", def.toString());
    }

    @Test
    public void testGetXPath() {
        XNode node = new XNode();
        node.setQName(WSDLConstants.QNAME_BINDING);
        node.setPrefix("wsdl");
        node.setAttributeName("name");
        node.setAttributeValue("SOAPBinding");
        assertEquals("/wsdl:binding[@name='SOAPBinding']", node.toString());
        assertEquals("[binding:SOAPBinding]", node.getPlainText());
    }

    @Test
    public void testParentNode() {
        XDef definition = new XDef();
        String ns = "{http://apache.org/hello_world/messages}";
        definition.setTargetNamespace("http://apache.org/hello_world/messages");
        assertEquals(ns, definition.getPlainText());
        
        XPortType portType = new XPortType();
        portType.setName("Greeter");
        portType.setParentNode(definition);

        String portTypeText = ns + "[portType:Greeter]";
        assertEquals(portTypeText, portType.getPlainText());

        XOperation op = new XOperation();
        op.setName("sayHi");
        op.setParentNode(portType);
        assertEquals(portTypeText + "[operation:sayHi]", op.getPlainText());

        String expected = "/wsdl:definitions[@targetNamespace='http://apache.org/hello_world/messages']";
        expected += "/wsdl:portType[@name='Greeter']/wsdl:operation[@name='sayHi']";
        assertEquals(expected, op.toString());
    }
}
