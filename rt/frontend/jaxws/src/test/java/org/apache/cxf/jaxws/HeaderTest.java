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
package org.apache.cxf.jaxws;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.binding.soap.JaxWsSoapBindingConfiguration;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.header_test.TestHeaderImpl;
import org.apache.header_test.types.TestHeader5;
import org.apache.header_test.types.TestHeader5ResponseBody;
import org.junit.Test;

public class HeaderTest extends AbstractJaxWsTest {
    
    @Test
    public void testInvocation() throws Exception {
        JaxWsServiceFactoryBean bean = new JaxWsServiceFactoryBean();

        Bus bus = getBus();
        bean.setBus(bus);
        bean.setServiceClass(TestHeaderImpl.class);
        
        Service service = bean.create();
        
        OperationInfo op = service.getServiceInfos().get(0).getInterface().getOperation(
            new QName(service.getName().getNamespaceURI(), "testHeader5"));
        assertNotNull(op);
        List<MessagePartInfo> parts = op.getInput().getMessageParts();
        assertEquals(1, parts.size());
        
        MessagePartInfo part = parts.get(0);
        assertNotNull(part.getTypeClass());
        assertEquals(TestHeader5.class, part.getTypeClass());
        
        parts = op.getOutput().getMessageParts();
        assertEquals(2, parts.size());
        
        part = parts.get(1);
        assertNotNull(part.getTypeClass());
        assertEquals(TestHeader5ResponseBody.class, part.getTypeClass());
        
        part = parts.get(0);
        assertNotNull(part.getTypeClass());
        assertEquals(TestHeader5.class, part.getTypeClass());
          
//        part = parts.get(1);
//        assertNotNull(part.getTypeClass());
        
        ServerFactoryBean svr = new ServerFactoryBean();
        svr.setBus(bus);
        svr.setServiceFactory(bean);
        svr.setServiceBean(new TestHeaderImpl());
        svr.setAddress("http://localhost:9104/SoapHeaderContext/SoapHeaderPort");
        svr.setBindingConfig(new JaxWsSoapBindingConfiguration(bean));

        
        svr.create();
        
        Node response = invoke("http://localhost:9104/SoapHeaderContext/SoapHeaderPort",
                               LocalTransportFactory.TRANSPORT_ID, 
                               "testHeader5.xml");

        assertNotNull(response);
        assertNoFault(response);

        addNamespace("t", "http://apache.org/header_test/types");
        assertValid("//s:Header/t:testHeader5", response);
    }
}
