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
package org.apache.cxf.frontend.soap;

import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.factory.AbstractSimpleFrontendTest;
import org.apache.cxf.service.factory.HelloService;
import org.apache.cxf.service.factory.HelloServiceImpl;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultipleEndpointObserver;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.junit.Test;

public class SoapBindingSelectionTest extends AbstractSimpleFrontendTest {

    boolean service1Invoked;
    boolean service2Invoked;
    
    @Test    
    public void testMultipleSoapBindings() throws Exception {
        ServerFactoryBean svrBean1 = new ServerFactoryBean();
        svrBean1.setAddress("http://localhost/Hello");
        svrBean1.setServiceClass(HelloService.class);
        svrBean1.setServiceBean(new HelloServiceImpl());
        svrBean1.setBus(getBus());
        svrBean1.getInInterceptors().add(new AbstractPhaseInterceptor<Message>(Phase.USER_LOGICAL) {
            public void handleMessage(Message message) throws Fault {
                service1Invoked = true;
            }
        });
        svrBean1.create();
        
        ServerFactoryBean svrBean2 = new ServerFactoryBean();
        svrBean2.setAddress("http://localhost/Hello");
        svrBean2.setServiceClass(HelloService.class);
        svrBean2.setServiceBean(new HelloServiceImpl());
        svrBean2.setBus(getBus());
        svrBean2.getInInterceptors().add(new AbstractPhaseInterceptor<Message>(Phase.USER_LOGICAL) {
            public void handleMessage(Message message) throws Fault {
                service2Invoked = true;
            }
        });
        
        SoapBindingConfiguration config = new SoapBindingConfiguration();
        config.setVersion(Soap12.getInstance());
        svrBean2.setBindingConfig(config);
        
        ServerImpl server2 = (ServerImpl)svrBean2.create();
        
        Destination d = server2.getDestination();
        MessageObserver mo = d.getMessageObserver();
        assertTrue(mo instanceof MultipleEndpointObserver);
        
        MultipleEndpointObserver meo = (MultipleEndpointObserver) mo;
        assertEquals(2, meo.getEndpoints().size());
        
        invoke("http://localhost/Hello", LocalTransportFactory.TRANSPORT_ID, "soap11.xml");
        
        assertTrue(service1Invoked);
        assertFalse(service2Invoked);
        
        service1Invoked = false;
        
        invoke("http://localhost/Hello", LocalTransportFactory.TRANSPORT_ID, "soap12.xml");
        
        assertFalse(service1Invoked);
        assertTrue(service2Invoked);
    }

}
