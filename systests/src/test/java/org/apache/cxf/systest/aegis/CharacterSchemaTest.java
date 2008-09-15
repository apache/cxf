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

package org.apache.cxf.systest.aegis;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.type.basic.CharacterAsStringType;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.test.TestUtilities;
import org.junit.Test;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * 
 */
public class CharacterSchemaTest extends AbstractDependencyInjectionSpringContextTests {
    
    private TestUtilities testUtilities;
    
    public CharacterSchemaTest() {
        testUtilities = new TestUtilities(getClass());
    }
    
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:aegisSportsServiceBeans.xml"};
    }
    
    @Test
    public void testSchema() throws Exception {
        testUtilities.setBus((Bus)applicationContext.getBean("cxf"));
        testUtilities.addDefaultNamespaces();
        testUtilities.addNamespace("aegis", "http://cxf.apache.org/aegisTypes");
        Server s = testUtilities.
            getServerForService(new QName("http://aegis.systest.cxf.apache.org/", 
                                          "SportsService"));
        assertNotNull(s);
        Document wsdl = testUtilities.getWSDLDocument(s); 
        assertNotNull(wsdl);
        NodeList typeAttrList = 
            testUtilities.assertValid("//xsd:complexType[@name='BeanWithCharacter']/xsd:sequence"
                                      + "/xsd:element[@name='character']"
                                      + "/@type", 
                                      wsdl); 
        Attr typeAttr = (Attr)typeAttrList.item(0);
        String typeAttrValue = typeAttr.getValue();
        // now, this thing is a qname with a :, and we have to work out if it's correct.
        String[] pieces = typeAttrValue.split(":");
        assertEquals(CharacterAsStringType.CHARACTER_AS_STRING_TYPE_QNAME.getLocalPart(),
                     pieces[1]);
        Node elementNode = typeAttr.getOwnerElement();
        String url = testUtilities.resolveNamespacePrefix(pieces[0], elementNode);
        assertEquals(CharacterAsStringType.CHARACTER_AS_STRING_TYPE_QNAME.getNamespaceURI(),
                     url);
    }
}
