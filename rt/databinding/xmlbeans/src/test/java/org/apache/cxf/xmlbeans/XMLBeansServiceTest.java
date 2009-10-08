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

import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.wsdl.WSDLConstants;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class XMLBeansServiceTest extends AbstractXmlBeansTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        createService(WeatherService.class, new WeatherService(), 
                     "WeatherService", new QName("http://www.webservicex.net", "WeatherService"));
        createService(TestService.class, new TestService(), "TestService",
                      new QName("urn:TestService", "TestService"));
    }

    @Test
    public void testAnyService() throws Exception {
        try {
            getWSDLDocument("TestService");
            assertTrue("Generating WSDL above should not throw an NPE", true);
        } catch (NullPointerException e) {
            fail("Shouldn't be throwing an NPE here");
        }
    }

    @Test
    public void testService() throws Exception {
        Node response = invoke("WeatherService", "GetWeatherByZip.xml");

        addNamespace("w", "http://www.webservicex.net");
        assertValid("//w:GetWeatherByZipCodeResponse", response);
    }

    @Test
    public void testWSDL() throws Exception {
        Node wsdl = getWSDLDocument("WeatherService");
        // printNode(wsdl);
        addNamespace("wsdl", WSDLConstants.NS_WSDL11);
        addNamespace("wsdlsoap", WSDLConstants.NS_SOAP11);
        addNamespace("xsd", SOAPConstants.XSD);

        assertValid("//wsdl:types/xsd:schema[@targetNamespace='http://www.webservicex.net']", wsdl);
        assertValid("//xsd:schema[@targetNamespace='http://www.webservicex.net']"
                    + "/xsd:element[@name='WeatherForecasts']", wsdl);
        assertValidBoolean("count(//xsd:schema[@targetNamespace='http://www.webservicex.net']"
                    + "/xsd:element[@name='WeatherForecasts'])=1", wsdl);
        assertValid("//xsd:schema[@targetNamespace='http://www.webservicex.net']"
                    + "/xsd:complexType[@name='WeatherForecasts']", wsdl);
    }

    @Test
    public void testAnyWSDL() throws Exception {

        Node wsdl = getWSDLDocument("TestService");

        addNamespace("wsdl", WSDLConstants.NS_WSDL11);
        addNamespace("wsdlsoap", WSDLConstants.NS_SOAP11);
        addNamespace("xsd", SOAPConstants.XSD);

        assertValid("//wsdl:types/xsd:schema[@targetNamespace='http://cxf.apache.org/xmlbeans']"
                    + "/xsd:element[@name='request']", wsdl);
    }

    @Test
    public void testAnyWSDLNoDupRootRefElements() throws Exception {
        Node wsdl = getWSDLDocument("TestService");

        String xpathString = "/wsdl:definitions/wsdl:types//xsd:schema/xsd:element[@name='trouble']";

        addNamespace("wsdl", WSDLConstants.NS_WSDL11);
        addNamespace("wsdlsoap", WSDLConstants.NS_SOAP11);
        addNamespace("xsd", SOAPConstants.XSD);
        addNamespace("s", SOAPConstants.XSD);

        assertEquals(1, assertValid(xpathString, wsdl).getLength());
    }



}
