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

package org.apache.cxf.aegis.integration;

import java.io.StringWriter;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;

import org.w3c.dom.Node;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.services.AddNumbers;
import org.apache.cxf.aegis.services.ArrayService;
import org.junit.Before;
import org.junit.Test;

/**
 * Test inspired by CXF-962 ... do parameters actually work?
 */
public class ParameterMappingTest extends AbstractAegisTest {

    @Before 
    public void setUp() throws Exception {
        super.setUp();
        createService(AddNumbers.class, "AddNumbers", null);
        createService(ArrayService.class, "ArrayService", null);
    }

    @Test
    public void testParametersWSDL() throws Exception {
        Node doc = getWSDLDocument("AddNumbers");
        Definition def = getWSDLDefinition("AddNumbers");
        StringWriter sink = new StringWriter();
        WSDLFactory.newInstance().newWSDLWriter().writeWSDL(def, sink);
        assertValid("/wsdl:definitions/wsdl:types/"
                    + "xsd:schema[@targetNamespace='http://services.aegis.cxf.apache.org']"
                    + "/xsd:complexType[@name='add']" 
                    + "/xsd:sequence" 
                    + "/xsd:element[@name='value1']", doc);
        assertValid(
                    "/wsdl:definitions/wsdl:types/"
                        + "xsd:schema[@targetNamespace='http://services.aegis.cxf.apache.org']"
                        + "/xsd:complexType[@name='unmappedAdd']" + "/xsd:sequence"
                        + "/xsd:element[@name='one']", doc);
    }

    @Test
    public void testOccursWSDL() throws Exception {
        Node doc = getWSDLDocument("ArrayService");
        Definition def = getWSDLDefinition("ArrayService");
        StringWriter sink = new StringWriter();
        WSDLFactory.newInstance().newWSDLWriter().writeWSDL(def, sink);
        assertXPathEquals("/wsdl:definitions/wsdl:types/"
                    + "xsd:schema[@targetNamespace= 'http://services.aegis.cxf.apache.org']"
                    + "/xsd:complexType[@name='ArrayOfString-50-2']" 
                    + "/xsd:sequence" 
                    + "/xsd:element[@name='string']/@minOccurs", "2", doc);
    }
}
