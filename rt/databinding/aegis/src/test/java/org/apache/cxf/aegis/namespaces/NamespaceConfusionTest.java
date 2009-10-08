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
package org.apache.cxf.aegis.namespaces;

import java.io.StringWriter;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.namespaces.data.Name;
import org.apache.cxf.aegis.namespaces.impl.NameServiceImpl;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.service.Service;

import org.junit.Before;
import org.junit.Test;


/** 
 * Regression test for CXF-959. This is a point test for consistent 
 * use of namespace prefixes in generated WSDL/XMLSchema. This test could 
 * be made into a more comprehensive functional test by exercising
 * cases such as multiple schema.
 */
public class NamespaceConfusionTest extends AbstractAegisTest {
    
    private TypeMapping tm;
    private Service service;
    private AegisDatabinding databinding;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        Server s = createService(NameServiceImpl.class);
        service = s.getEndpoint().getService();
        databinding = (AegisDatabinding)service.getDataBinding();
        tm = databinding.getAegisContext().getTypeMapping();
    }
    
    private String getNamespaceForPrefix(Element rootElement, 
                                         Element typeElement, 
                                         String prefix) throws Exception {
        Element schemaElement = (Element)assertValid("ancestor::xsd:schema", typeElement).item(0);

        NamedNodeMap attributes = schemaElement.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++) {
            Attr attr = (Attr)attributes.item(x);
            if (attr.getName().startsWith("xmlns:")) {
                String attrPrefix = attr.getName().split(":")[1];
                if (attrPrefix.equals(prefix)) {
                    return attr.getValue();
                }
            }
        }

        attributes = rootElement.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++) {
            Attr attr = (Attr)attributes.item(x);
            if (attr.getName().startsWith("xmlns:")) {
                String attrPrefix = attr.getName().split(":")[1];
                if (attrPrefix.equals(prefix)) {
                    return attr.getValue();
                }
            }
        }

        return null;
    }

    
    @Test
    public void testNameNamespace() throws Exception {
        
        org.w3c.dom.Document doc = getWSDLDocument("NameServiceImpl");
        Element rootElement = doc.getDocumentElement();

        Definition def = getWSDLDefinition("NameServiceImpl");
        StringWriter sink = new StringWriter();
        WSDLFactory.newInstance().newWSDLWriter().writeWSDL(def, sink);
        NodeList aonNodes = 
            assertValid("//xsd:complexType[@name='ArrayOfName']/xsd:sequence/xsd:element", doc);
        Element arrayOfNameElement = (Element)aonNodes.item(0);
        
        String typename = arrayOfNameElement.getAttribute("type");
        String prefix = typename.split(":")[0];

        String uri = getNamespaceForPrefix(rootElement, arrayOfNameElement, prefix);
        assertNotNull(uri);
        Type nameType = tm.getTypeCreator().createType(Name.class);
        QName tmQname = nameType.getSchemaType();
        assertEquals(tmQname.getNamespaceURI(), uri);
        
    }
    
    
    

}
