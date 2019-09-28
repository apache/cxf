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

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.services.ArrayService;
import org.apache.cxf.aegis.services.BeanService;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WrappedTest extends AbstractAegisTest {

    private ArrayService arrayService;
    private Document arrayWsdlDoc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setEnableJDOM(true);
        arrayService = new ArrayService();
        createService(ArrayService.class, arrayService, "Array", new QName("urn:Array", "Array"));
        createService(BeanService.class, "BeanService");
        arrayWsdlDoc = getWSDLDocument("Array");
    }

    @Test
    public void testBeanService() throws Exception {
        Node response = invoke("BeanService", "bean11.xml");

        addNamespace("sb", "http://services.aegis.cxf.apache.org");
        addNamespace("beanz", "urn:beanz");
        assertValid("/s:Envelope/s:Body/sb:getSimpleBeanResponse", response);
        assertValid("//sb:getSimpleBeanResponse/sb:return", response);
        assertValid("//sb:getSimpleBeanResponse/sb:return/beanz:howdy[text()=\"howdy\"]", response);
        assertValid("//sb:getSimpleBeanResponse/sb:return/beanz:bleh[text()=\"bleh\"]", response);
    }

    @Test
    public void testArrayWsdl() throws Exception {
        NodeList stuff = assertValid("//xsd:complexType[@name='ArrayOfString-2-50']", arrayWsdlDoc);
        assertEquals(1, stuff.getLength());
    }

    @Test
    public void testXmlConfigurationOfParameterTypeSchema() throws Exception {
        assertValid(
                    "/wsdl:definitions/wsdl:types"
                        + "/xsd:schema[@targetNamespace='urn:Array']"
                        + "/xsd:complexType[@name=\"takeNumber\"]/xsd:sequence/xsd:element"
                        + "[@type=\"xsd:long\"]",
                    arrayWsdlDoc);
    }

    @Test
    public void testXmlConfigurationOfParameterType() throws Exception {
        invoke("Array", "takeNumber.xml");
        assertEquals(Long.valueOf(123456789), arrayService.getNumberValue());
    }

    @Test
    public void testBeanServiceWSDL() throws Exception {
        Node doc = getWSDLDocument("BeanService");

        assertValid("/wsdl:definitions/wsdl:types", doc);
        assertValid("/wsdl:definitions/wsdl:types/xsd:schema", doc);
        assertValid("/wsdl:definitions/wsdl:types/"
                    + "xsd:schema[@targetNamespace='http://services.aegis.cxf.apache.org']",
                    doc);
        assertValid("//xsd:schema[@targetNamespace='http://services.aegis.cxf.apache.org']/"
                    + "xsd:element[@name='getSubmitBean']",
                    doc);
        assertValid("//xsd:complexType[@name='getSubmitBean']/xsd:sequence"
                    + "/xsd:element[@name='bleh'][@type='xsd:string'][@minOccurs='0']", doc);
        assertValid("//xsd:complexType[@name='getSubmitBean']/xsd:sequence"
                    + "/xsd:element[@name='bean'][@type='ns0:SimpleBean'][@minOccurs='0']", doc);

        assertValid("/wsdl:definitions/wsdl:types"
                    + "/xsd:schema[@targetNamespace='http://services.aegis.cxf.apache.org']", doc);
        assertValid("/wsdl:definitions/wsdl:types"
                    + "/xsd:schema[@targetNamespace='urn:beanz']"
                    + "/xsd:complexType[@name=\"SimpleBean\"]", doc);
        assertValid(
                    "/wsdl:definitions/wsdl:types"
                        + "/xsd:schema[@targetNamespace='urn:beanz']"
                        + "/xsd:complexType[@name=\"SimpleBean\"]/xsd:sequence/xsd:element"
                        + "[@name=\"bleh\"][@minOccurs='0']",
                    doc);
        assertValid(
                    "/wsdl:definitions/wsdl:types"
                        + "/xsd:schema[@targetNamespace='urn:beanz']"
                        + "/xsd:complexType[@name=\"SimpleBean\"]/xsd:sequence/xsd:element"
                        + "[@name=\"howdy\"][@minOccurs='0']",
                    doc);
        assertValid(
                    "/wsdl:definitions/wsdl:types"
                        + "/xsd:schema[@targetNamespace='urn:beanz']"
                        + "/xsd:complexType[@name=\"SimpleBean\"]/xsd:sequence/xsd:element"
                        + "[@type=\"xsd:string\"]",
                    doc);
    }

    @Test
    public void testSubmitW3CArray() throws Exception {
        addNamespace("a", "urn:Array");
        addNamespace("iam", "uri:iam");
        addNamespace("linux", "uri:linux");
        addNamespace("planets", "uri:planets");

        invoke("Array", "/org/apache/cxf/aegis/integration/anyTypeArrayW3C.xml");
        assertEquals("before items", arrayService.getBeforeValue());
        assertEquals(3, arrayService.getW3cArray().length);
        org.w3c.dom.Document e = arrayService.getW3cArray()[0];
        assertValid("/iam:walrus", e);
        assertEquals("after items", arrayService.getAfterValue());
    }

}