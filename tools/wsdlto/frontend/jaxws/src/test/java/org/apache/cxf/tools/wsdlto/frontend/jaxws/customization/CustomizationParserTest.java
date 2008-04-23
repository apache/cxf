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
package org.apache.cxf.tools.wsdlto.frontend.jaxws.customization;

import java.io.File;
import java.io.FileOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.junit.Test;

public class CustomizationParserTest extends ProcessorTestBase {
    CustomizationParser parser = new CustomizationParser();
    CustomNodeSelector selector = new CustomNodeSelector();

    @Test
    public void testHasJaxbBindingDeclaration() throws Exception {
        Element doc = getDocumentElement("resources/embeded_jaxb.xjb");

        Node jaxwsBindingNode = selector.queryNode(doc, "//jaxws:bindings[@node]");

        assertNotNull(jaxwsBindingNode);

        assertTrue(parser.hasJaxbBindingDeclaration(jaxwsBindingNode));

        doc = getDocumentElement("resources/external_jaxws.xml");
        jaxwsBindingNode = selector.queryNode(doc, "//jaxws:bindings[@node]");
        assertNotNull(jaxwsBindingNode);
        assertFalse(parser.hasJaxbBindingDeclaration(jaxwsBindingNode));
    }

    @Test
    public void testCopyAllJaxbDeclarations() throws Exception {
        Element schema = getDocumentElement("resources/test.xsd");
        Element binding = getDocumentElement("resources/embeded_jaxb.xjb");

        String checkingPoint = "//xsd:annotation/xsd:appinfo/jaxb:schemaBindings/jaxb:package[@name]";
        assertNull(selector.queryNode(schema, checkingPoint));

        Node jaxwsBindingNode = selector.queryNode(binding, "//jaxws:bindings[@node]");
        Node schemaNode = selector.queryNode(schema, "//xsd:schema");

        parser.copyAllJaxbDeclarations(schemaNode, (Element)jaxwsBindingNode);

        File file = new File(output, "custom_test.xsd");
        XMLUtils.writeTo(schemaNode, new FileOutputStream(file));
        Document testNode = XMLUtils.parse(file);

        Node result = selector.queryNode(testNode, checkingPoint);
        assertNotNull(result);
    }

    @Test
    public void testInternalizeBinding1() throws Exception {
        Element wsdlDoc = getDocumentElement("resources/test.wsdl");
        Element jaxwsBinding = getDocumentElement("resources/external_jaxws.xml");

        parser.setWSDLNode(wsdlDoc);
        parser.internalizeBinding(jaxwsBinding, wsdlDoc, "");

        File file = new File(output, "custom_test.wsdl");
        XMLUtils.writeTo(wsdlDoc, new FileOutputStream(file));
        Document testNode = XMLUtils.parse(file);

        String[] checkingPoints =
            new String[]{"wsdl:definitions/wsdl:portType/jaxws:bindings/jaxws:class",
                         "wsdl:definitions/jaxws:bindings/jaxws:package"};
        checking(testNode, checkingPoints);
    }

    @Test
    public void testInternalizeBinding2() throws Exception {
        Element wsdlDoc = getDocumentElement("resources/test.wsdl");
        Element jaxwsBinding = getDocumentElement("resources/external_jaxws_embed_jaxb.xml");

        parser.setWSDLNode(wsdlDoc);
        parser.internalizeBinding(jaxwsBinding, wsdlDoc, "");

        String base = "wsdl:definitions/wsdl:types/xsd:schema/xsd:annotation/xsd:appinfo/";
        String[] checkingPoints =
            new String[]{base + "jaxb:schemaBindings/jaxb:package"};

        File file = new File(output, "custom_test.wsdl");
        XMLUtils.writeTo(wsdlDoc, new FileOutputStream(file));
        Document testNode = XMLUtils.parse(file);

        checking(testNode, checkingPoints);
    }

    @Test
    public void testInternalizeBinding3() throws Exception {
        Element wsdlDoc = getDocumentElement("resources/test.wsdl");
        Element jaxwsBinding = getDocumentElement("resources/external_jaxws_embed_jaxb_date.xml");
        parser.setWSDLNode(wsdlDoc);
        parser.internalizeBinding(jaxwsBinding, wsdlDoc, "");

        String base = "wsdl:definitions/wsdl:types/xsd:schema/xsd:annotation/xsd:appinfo/";
        String[] checkingPoints =
            new String[]{base + "jaxb:globalBindings/jaxb:javaType"};

        File file = new File(output, "custom_test.wsdl");
        XMLUtils.writeTo(wsdlDoc, new FileOutputStream(file));
        Document testNode = XMLUtils.parse(file);

        checking(testNode, checkingPoints);
    }

    @Test
    public void testInternalizeBinding4() throws Exception {
        Element wsdlDoc = getDocumentElement("resources/hello_world.wsdl");
        Element jaxwsBinding = getDocumentElement("resources/binding2.xml");
        parser.setWSDLNode(wsdlDoc);
        parser.internalizeBinding(jaxwsBinding, wsdlDoc, "");

        String checkingPoint = "wsdl:definitions/wsdl:types/xsd:schema";
        checkingPoint += "/xsd:element[@name='CreateProcess']/xsd:complexType/xsd:sequence";
        checkingPoint += "/xsd:element[@name='MyProcess']/xsd:simpleType/xsd:annotation/xsd:appinfo";
        checkingPoint += "/jaxb:typesafeEnumClass/jaxb:typesafeEnumMember[@name='BLUE']";

        String[] checkingPoints =
            new String[]{checkingPoint};

        File file = new File(output, "custom_test4.wsdl");
        XMLUtils.writeTo(wsdlDoc, new FileOutputStream(file));
        Document testNode = XMLUtils.parse(file);

        checking(testNode, checkingPoints);
    }

    private Element getDocumentElement(final String resource) throws Exception {
        return XMLUtils.parse(getClass().getResourceAsStream(resource)).getDocumentElement();
    }

    private void checking(Node node, String[] checkingPoints) {
        for (String checkingPoint : checkingPoints) {
            assertNotNull(selector.queryNode(node, checkingPoint));
        }
    }
}