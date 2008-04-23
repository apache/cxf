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
package org.apache.cxf.aegis.inheritance;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class InheritancePOJOTest extends AbstractAegisTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        ServerFactoryBean sf = createServiceFactory(InheritanceService.class,
                                                    null, "InheritanceService",
                                                    new QName("urn:xfire:inheritance",
                                                              "InheritanceService"),
                                                    null);
        AegisContext globalContext = new AegisContext();
        globalContext.setWriteXsiTypes(true);

        Set<String> l = new HashSet<String>();
        l.add(Employee.class.getName());
        globalContext.setRootClassNames(l);
        AegisDatabinding binding = new AegisDatabinding();
        binding.setAegisContext(globalContext);

        sf.getServiceFactory().setDataBinding(binding);
        sf.create();
    }

    @Test
    public void testGenerateWsdl() throws Exception {
        Document d = getWSDLDocument("InheritanceService");

        String types = "//wsdl:types/xsd:schema/";

        // check for Employee as extension
        String employeeType = types + "xsd:complexType[@name='Employee']";
        assertValid(employeeType, d);
        String extension = "/xsd:complexContent/xsd:extension[@base='tns:AbstractUser']";
        assertValid(employeeType + extension, d);
        assertValid(employeeType + extension + "/xsd:sequence/xsd:element[@name='division']", d);
        // assertValid("count(" + employeeType + extension +
        // "/xsd:sequence/*)=1", d);

        // check for BaseUser as abstract
        String baseUserType = types + "xsd:complexType[(@name='AbstractUser') and (@abstract='true')]";
        assertValid(baseUserType, d);
        assertValid(baseUserType + "/xsd:sequence/xsd:element[@name='name']", d);
        // assertValid("count(" + baseUserType + "/xsd:sequence/*)=1", d);
    }

    @Test
    public void testLocalReceiveEmployee() throws Exception {
        Node response = invoke("InheritanceService", "ReceiveEmployee.xml");
        addNamespace("w", "urn:xfire:inheritance");
        assertValid("//s:Body/w:receiveUserResponse", response);
    }

    @Test
    public void testLocalGetEmployee() throws Exception {
        Node response = invoke("InheritanceService", "GetEmployee.xml");
        addNamespace("xsi", SOAPConstants.XSI_NS);
        addNamespace("w", "urn:xfire:inheritance");
        addNamespace("p", "http://inheritance.aegis.cxf.apache.org");
        assertValid("//s:Body/w:getEmployeeResponse/w:return/p:division", response);
        assertValid("//s:Body/w:getEmployeeResponse/w:return[@xsi:type]", response);
    }
}
