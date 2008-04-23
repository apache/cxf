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

import java.util.Collection;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.service.Hello2;
import org.apache.cxf.jaxws.service.Hello3;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.junit.Test;

public class CodeFirstWSDLTest extends AbstractJaxWsTest {
    String address = "local://localhost:9000/Hello";
    
    private Definition createService(Class clazz) throws Exception {
        
        JaxWsImplementorInfo info = new JaxWsImplementorInfo(clazz);
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean(info);

        Bus bus = getBus();
        bean.setBus(bus);
        
        Service service = bean.create();

        InterfaceInfo i = service.getServiceInfos().get(0).getInterface();
        assertEquals(4, i.getOperations().size());

        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setBus(bus);
        svrFactory.setServiceFactory(bean);
        svrFactory.setServiceBean(clazz.newInstance());
        svrFactory.setAddress(address);
        svrFactory.create();
        
        Collection<BindingInfo> bindings = service.getServiceInfos().get(0).getBindings();
        assertEquals(1, bindings.size());
        
        ServiceWSDLBuilder wsdlBuilder = 
            new ServiceWSDLBuilder(bus, service.getServiceInfos().get(0));
        return wsdlBuilder.build();
    }

    @Test
    public void testWSDL1() throws Exception {
        Definition d = createService(Hello2.class);

        QName serviceName = new QName("http://service.jaxws.cxf.apache.org/", "Hello2Service");

        javax.wsdl.Service service = d.getService(serviceName);

        assertNotNull(service);

        QName portName = new QName("http://service.jaxws.cxf.apache.org/", "Hello2Port");

        javax.wsdl.Port port = service.getPort(portName.getLocalPart());

        assertNotNull(port);

        QName portTypeName = new QName("http://service.jaxws.cxf.apache.org/", "HelloInterface");

        javax.wsdl.PortType portType = d.getPortType(portTypeName);

        assertNotNull(portType);
        assertEquals(4, portType.getOperations().size());
    }

    @Test
    public void testWSDL2() throws Exception {
        Definition d = createService(Hello3.class);

        QName serviceName = new QName("http://mynamespace.com/", "MyService");

        javax.wsdl.Service service = d.getService(serviceName);

        assertNotNull(service);

        QName portName = new QName("http://mynamespace.com/", "MyPort");

        javax.wsdl.Port port = service.getPort(portName.getLocalPart());

        assertNotNull(port);

        QName portTypeName = new QName("http://service.jaxws.cxf.apache.org/", "HelloInterface");

        javax.wsdl.PortType portType = d.getPortType(portTypeName);

        assertNotNull(portType);
        assertEquals(4, portType.getOperations().size());
    }

}
