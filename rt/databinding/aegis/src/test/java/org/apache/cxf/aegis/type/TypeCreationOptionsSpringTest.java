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

package org.apache.cxf.aegis.type;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.test.AbstractCXFSpringTest;
import org.apache.cxf.test.TestUtilities;
import org.junit.Test;

/**
 * 
 */
public class TypeCreationOptionsSpringTest extends AbstractCXFSpringTest {

    @Override
    protected String[] getConfigLocations() {
        setConfigContextClass(TypeCreationOptionsSpringTest.class);
        return new String[] {"/org/apache/cxf/aegis/type/aegisOptionsTestBeans.xml"};
    }
    
    @Test 
    public void testMinOccurs() throws Exception {
        TestUtilities testUtilities = new TestUtilities(TypeCreationOptionsSpringTest.class);
        testUtilities.setBus(getBean(Bus.class, "cxf"));
        testUtilities.addDefaultNamespaces();
        testUtilities.addNamespace("ts", "http://cxf.org.apache/service");
        //{urn:org.apache.cxf.aegis}arrayService
        Server s = testUtilities.getServerForService(new QName("urn:org.apache.cxf.aegis", 
                                                               "arrayService"));
        Document wsdl = testUtilities.getWSDLDocument(s);
        assertXPathEquals("//xsd:complexType[@name='ArrayOfInt']/" 
                          + "xsd:sequence/xsd:element[@name='int']/@minOccurs", 
                          "3", wsdl);

    }
    
}
