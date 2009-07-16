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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

//import org.w3c.dom.Node;
//import org.w3c.dom.Text;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.services.BeanWithDOM;
import org.apache.cxf.aegis.services.DocumentService;
import org.apache.cxf.aegis.services.IDocumentService;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;

import org.junit.Before;
import org.junit.Test;

/**
 * Test mapping DOM.
 * Commented out code for the case of an embedded Node object,
 *  which doesn't work at all, and perhaps isn't supposed to.
 */
public class DOMMappingTest extends AbstractAegisTest {

    private IDocumentService docClient;

    @Before 
    public void setUp() throws Exception {
        super.setUp();
        createService(DocumentService.class, "DocService");
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        ReflectionServiceFactoryBean factory = new ReflectionServiceFactoryBean();
        factory.getServiceConfigurations()
            .add(0, new org.apache.cxf.aegis.databinding.XFireCompatibilityServiceConfiguration());
        proxyFac.setServiceFactory(factory);
        proxyFac.setDataBinding(new AegisDatabinding());

        proxyFac.setAddress("local://DocService");
        proxyFac.setServiceClass(IDocumentService.class);
        proxyFac.setBus(getBus());

        Object proxyObj = proxyFac.create();
        docClient = (IDocumentService)proxyObj;
        Client client = ClientProxy.getClient(proxyObj);
        ClientImpl clientImpl = (ClientImpl)client;
        clientImpl.setSynchronousTimeout(1000000000);
    }
    
    @Test
    public void testSimpleString() throws Exception {
        String s = docClient.simpleStringReturn();
        assertEquals("simple", s);
    }
    
    @Test
    public void testDocService() throws Exception {
        Document doc = docClient.returnDocument();
        Element rootElement = doc.getDocumentElement();
        assertEquals("carrot", rootElement.getNodeName());
    }
    
    @Test
    public void testBeanCases() throws Exception {
        BeanWithDOM bwd = docClient.getBeanWithDOM();
        Element rootElement = bwd.getDocument().getDocumentElement();
        assertEquals("carrot", rootElement.getNodeName());
        /*
        Node shouldBeText = bwd.getNode();
        assertTrue(shouldBeText instanceof Text);
        Text text = (Text) shouldBeText;
        assertEquals("Is a root vegetable.", text);
        */
    }
}
