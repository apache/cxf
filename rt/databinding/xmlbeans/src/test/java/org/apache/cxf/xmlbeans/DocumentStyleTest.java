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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.endpoint.Server;

import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class DocumentStyleTest extends AbstractXmlBeansTest {
    String ns = "urn:TestService";
    Server server;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        server = createService(TestService.class, new TestService(), 
                               "TestService", 
                               new QName(ns, "TestService"));
    }

    @Test
    public void testInvoke() throws Exception {
        Node response = invoke("TestService", "/org/apache/cxf/xmlbeans/DocumentStyleRequest.xml");

        assertNotNull(response);

        addNamespace("x", "http://cxf.apache.org/xmlbeans");
        addNamespace("y", "urn:TestService");
        assertValid("//s:Body/y:mixedRequestResponse/x:response/x:form", response);
    }

    @Test
    public void testInvokeWithHack() throws Exception {
        server.getEndpoint().put(XmlBeansDataBinding.XMLBEANS_NAMESPACE_HACK, Boolean.TRUE);
        Node response = invoke("TestService", "/org/apache/cxf/xmlbeans/DocumentStyleRequest.xml");

        assertNotNull(response);

        addNamespace("x", "http://cxf.apache.org/xmlbeans");
        addNamespace("y", "urn:TestService");
        assertValid("//s:Body/y:mixedRequestResponse/x:response/x:form", response);
    }

    @Test
    public void testWSDL() throws Exception {
        Document wsdl = getWSDLDocument("TestService");

        addNamespace("xsd", SOAPConstants.XSD);
        assertValid("//xsd:schema[@targetNamespace='urn:TestService']"
                    + "/xsd:complexType[@name='mixedRequest']"
                    + "/xsd:sequence/xsd:element[@name='string'][@type='xsd:string']",
                    wsdl);
    }
}
