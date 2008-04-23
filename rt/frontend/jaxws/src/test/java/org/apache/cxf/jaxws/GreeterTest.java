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

import java.net.URL;

import org.w3c.dom.Node;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.Test;

public class GreeterTest extends AbstractJaxWsTest {


    @Test
    public void testEndpoint() throws Exception {
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        URL resource = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(resource);        
        bean.setWsdlURL(resource.toString());
        bean.setBus(bus);
        bean.setServiceClass(GreeterImpl.class);
        GreeterImpl greeter = new GreeterImpl();
        BeanInvoker invoker = new BeanInvoker(greeter);
        
        
        Service service = bean.create();

        assertEquals("SOAPService", service.getName().getLocalPart());
        assertEquals("http://apache.org/hello_world_soap_http", service.getName().getNamespaceURI());

        ServerFactoryBean svr = new ServerFactoryBean();
        svr.setBus(bus);
        svr.setServiceFactory(bean);
        svr.setInvoker(invoker);
        
        svr.create();

        Node response = invoke("http://localhost:9000/SoapContext/SoapPort",
                           LocalTransportFactory.TRANSPORT_ID,
                           "GreeterMessage.xml");
        
        assertEquals(1, greeter.getInvocationCount());
        
        assertNotNull(response);
        
        addNamespace("h", "http://apache.org/hello_world_soap_http/types");
        
        assertValid("/s:Envelope/s:Body", response);
        assertValid("//h:sayHiResponse", response);
    }
}
