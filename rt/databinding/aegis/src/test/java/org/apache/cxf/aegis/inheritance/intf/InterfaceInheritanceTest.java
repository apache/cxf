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
package org.apache.cxf.aegis.inheritance.intf;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.junit.Before;
import org.junit.Test;

/**
 * This test ensures that we're handling inheritance of interfaces correctly.
 * Since we can't do multiple parent inheritance in XML schema, which interfaces
 * require, we just don't allow interface inheritance period.
 * 
 * @author Dan Diephouse
 */
public class InterfaceInheritanceTest extends AbstractAegisTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Server server = createService(IInterfaceService.class);
        Service service = server.getEndpoint().getService();
        service.setInvoker(new BeanInvoker(new InterfaceService()));
    }

    @Test
    public void testClient() throws Exception {
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setAddress("local://IInterfaceService");
        proxyFac.setServiceClass(IInterfaceService.class);
        proxyFac.setBus(getBus());
        setupAegis(proxyFac.getClientFactoryBean());

        IInterfaceService client = (IInterfaceService)proxyFac.create();

        IChild child = client.getChild();
        assertNotNull(child);
        assertEquals("child", child.getChildName());
        assertEquals("parent", child.getParentName());

        IParent parent = client.getChildViaParent();
        assertEquals("parent", parent.getParentName());
        assertFalse(parent instanceof IChild);

        IGrandChild grandChild = client.getGrandChild();
        assertEquals("parent", grandChild.getParentName());

        Document wsdl = getWSDLDocument("IInterfaceService");
        assertValid("//xsd:complexType[@name='IGrandChild']", wsdl);
        assertValid("//xsd:complexType[@name='IGrandChild']//xsd:element[@name='grandChildName']", wsdl);
        assertValid("//xsd:complexType[@name='IGrandChild']//xsd:element[@name='childName'][1]", wsdl);
        assertInvalid("//xsd:complexType[@name='IGrandChild']//xsd:element[@name='childName'][2]", wsdl);
        assertValid("//xsd:complexType[@name='IChild']", wsdl);
        assertValid("//xsd:complexType[@name='IParent']", wsdl);
        assertInvalid("//xsd:complexType[@name='IChild'][@abstract='true']", wsdl);
    }
}
