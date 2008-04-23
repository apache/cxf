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

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.Before;
import org.junit.Test;

public class SoapFaultTest extends AbstractJaxWsTest {

    private Service service;

    @Before
    public void setUp() throws Exception {
        super.setUpBus();

        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        URL resource = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(resource);
        bean.setWsdlURL(resource.toString());
        bean.setBus(bus);
        bean.setServiceClass(GreeterImpl.class);

        GreeterImpl greeter = new GreeterImpl();
        BeanInvoker invoker = new BeanInvoker(greeter);
        bean.setInvoker(invoker);

        service = bean.create();

        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setBus(bus);
        svrFactory.setServiceFactory(bean);
        
        svrFactory.create();
    }

    
    @Test
    public void testInterceptorThrowingSoapFault() throws Exception {
        service.getInInterceptors().add(new FaultThrowingInterceptor());

        Node response = invoke("http://localhost:9000/SoapContext/SoapPort",
                               LocalTransportFactory.TRANSPORT_ID, 
                               "GreeterMessage.xml");

        assertNotNull(response);

        assertValid("/s:Envelope/s:Body/s:Fault/faultstring[text()='I blame Hadrian.']", response);
    }

    /**
     * We need to get the jaxws fault -> soap fault conversion working for this
     * @throws Exception
     */
    @Test
    public void testWebServiceException() throws Exception {
        Node response = invoke("http://localhost:9000/SoapContext/SoapPort",
                               LocalTransportFactory.TRANSPORT_ID, "GreeterGetFaultMessage.xml");

        assertNotNull(response);

        assertValid("/s:Envelope/s:Body/s:Fault/faultstring[text()='TestBadRecordLit']", response);
        assertValid("/s:Envelope/s:Body/s:Fault/detail", response);
    }

    public class FaultThrowingInterceptor extends AbstractSoapInterceptor {
        public FaultThrowingInterceptor() {
            super(Phase.USER_LOGICAL);
        }

        public void handleMessage(SoapMessage message) throws Fault {
            throw new SoapFault("I blame Hadrian.", message.getVersion().getSender());
        }

    }
}
