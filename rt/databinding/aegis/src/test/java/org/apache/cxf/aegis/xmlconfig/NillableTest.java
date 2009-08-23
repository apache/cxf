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
package org.apache.cxf.aegis.xmlconfig;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.services.NillableService;

import org.junit.Before;
import org.junit.Test;

public class NillableTest extends AbstractAegisTest {

    private Document arrayWsdlDoc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createService(NillableService.class, new NillableService(), "Nillable", new QName("urn:nillable",
                                                                                          "Nillable"));
        arrayWsdlDoc = getWSDLDocument("Nillable");
    }

    @Test
    public void testXmlConfigurationOfParameterTypeSchema() throws Exception {
        NodeList typeList = assertValid(
                                        "/wsdl:definitions/wsdl:types"
                                            + "/xsd:schema[@targetNamespace='urn:nillable']"
                                            + "/xsd:complexType[@name=\"submitStringArray\"]"
                                            + "/xsd:sequence/xsd:element"
                                            + "[@name='array']", arrayWsdlDoc);
        Element typeElement = (Element)typeList.item(0);
        String nillableValue = typeElement.getAttribute("nillable");
        assertTrue(nillableValue == null || "".equals(nillableValue) || "false".equals("nillableValue"));

        typeList = assertValid("/wsdl:definitions/wsdl:types"
                               + "/xsd:schema[@targetNamespace='urn:nillable']"
                               + "/xsd:complexType[@name=\"takeNotNillableString\"]"
                               + "/xsd:sequence/xsd:element[@name='string']", arrayWsdlDoc);
        typeElement = (Element)typeList.item(0);
        nillableValue = typeElement.getAttribute("nillable");
        assertTrue(nillableValue == null || "".equals(nillableValue) || "false".equals("nillableValue"));

    }
}
