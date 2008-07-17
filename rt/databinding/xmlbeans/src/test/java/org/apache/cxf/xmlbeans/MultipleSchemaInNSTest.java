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
package org.apache.cxf.xmlbeans;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.helpers.XMLUtils;
import org.junit.Before;
import org.junit.Test;
  
/**
 * Tests that we can handle multiple schemas within the same namespace.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class MultipleSchemaInNSTest extends AbstractXmlBeansTest {
    String ns = "urn:xfire:xmlbeans:nstest";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        createService(MultipleSchemaService.class,
                      new MultipleSchemaService(),
                      "MultipleSchemaService", 
                      new QName("http://xmlbeans.cxf.apache.org/", "MultipleSchemaService"));
    }

    @Test
    public void testWSDL() throws Exception {
        Node wsdl = getWSDLDocument("MultipleSchemaService");

        addNamespace("xsd", SOAPConstants.XSD);
        NodeList list = assertValid("//xsd:schema[@targetNamespace='" + ns + "']", wsdl);
        assertEquals(XMLUtils.toString(wsdl), 3, list.getLength());
        assertValid("//xsd:import[@namespace='" + ns + "']",
                    list.item(0));
        assertValid("//xsd:import[@namespace='" + ns + "']", list.item(0));
        
        assertValid("//xsd:import[@namespace='" + ns + "']",
                      list.item(1));
        assertValid("//xsd:import[@namespace='" + ns + "']",
                    list.item(2));
        assertInvalid("//xsd:import[@namespace='" + ns + "']/@schemaLocation",
                    list.item(1));
        assertInvalid("//xsd:import[@namespace='" + ns + "']/@schemaLocation",
                    list.item(2));
        /*
        endpoint.setProperty(AbstractWSDL.REMOVE_ALL_IMPORTS, "True");

        wsdl = getWSDLDocument("MultipleSchemaService");

        assertValid("//xsd:schema[@targetNamespace='" + ns + "'][1]", wsdl);
        assertInvalid("//xsd:schema[@targetNamespace='" + ns + "'][1]" + "/xsd:import[@namespace='" + ns
                      + "']", wsdl);
        assertValid("//xsd:schema[@targetNamespace='" + ns + "'][3]", wsdl);
        assertInvalid("//xsd:schema[@targetNamespace='" + ns + "'][3]" + "/xsd:import[@namespace='" + ns
                      + "']", wsdl);
                      */
    }
}
