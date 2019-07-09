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
package org.apache.cxf.aegis.type.basic;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.services.DataService;
import org.apache.cxf.wsdl.WSDLConstants;

import org.junit.Before;
import org.junit.Test;

public class ByteDataTest extends AbstractAegisTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createService(DataService.class);
    }

    @Test
    public void testBeanService() throws Exception {
        Node response = invoke("DataService", "GetData.xml");

        addNamespace("s", "http://services.aegis.cxf.apache.org");
        assertValid("//s:return/s:data", response);

        response = invoke("DataService", "EchoData.xml");

        assertValid("//s:return/s:data", response);

    }

    @Test
    public void testBeanServiceWSDL() throws Exception {
        Document doc = getWSDLDocument("DataService");
        addNamespace("wsdl", WSDLConstants.NS_WSDL11);
        addNamespace("wsdlsoap", WSDLConstants.NS_SOAP11);
        addNamespace("xsd", WSDLConstants.NS_SCHEMA_XSD);

        assertValid("//xsd:element[@name='data'][@type='xsd:base64Binary']", doc);
    }
}
