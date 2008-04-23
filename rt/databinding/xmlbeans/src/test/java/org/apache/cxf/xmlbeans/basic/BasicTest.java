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

package org.apache.cxf.xmlbeans.basic;


import org.w3c.dom.Node;

import org.apache.cxf.xmlbeans.AbstractXmlBeansTest;
import org.junit.Before;
import org.junit.Test;

public class BasicTest extends AbstractXmlBeansTest {

    @Before 
    public void setUp() throws Exception {
        super.setUp();
        createService(TestService.class, null, "TestService", null);
    }
    
    @Test
    public void testBasicInvoke() throws Exception {
        Node response = invoke("TestService", "bean11.xml");
        addNamespace("ns1", "http://basic.xmlbeans.cxf.apache.org/");
        assertValid("/s:Envelope/s:Body/ns1:echoAddressResponse", response);
        assertValid("//ns1:echoAddressResponse/ns1:return", response);
        assertValid("//ns1:echoAddressResponse/ns1:return/country", response);
        assertValid("//ns1:echoAddressResponse/ns1:return/country[text()='Mars']",
                    response);
    }
    
    @Test
    public void testWSDL() throws Exception {
        Node doc = getWSDLDocument("TestService");
        assertValid("/wsdl:definitions/wsdl:types/xsd:schema"
                    + "[@targetNamespace='http://cxf.apache.org/databinding/xmlbeans/test']", 
                    doc);

        assertValid("/wsdl:definitions/wsdl:types/xsd:schema"
                    + "[@targetNamespace='http://cxf.apache.org/databinding/xmlbeans/test']"
                    + "/xsd:complexType[@name='Address']", 
                    doc);
    }
    
}
