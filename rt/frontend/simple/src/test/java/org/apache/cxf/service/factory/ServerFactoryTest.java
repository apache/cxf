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
package org.apache.cxf.service.factory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.hello_world.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeOneWay;
import org.junit.Test;

public class ServerFactoryTest extends AbstractSimpleFrontendTest {

    @Test
    public void testSetDF() throws Exception {
        ServerFactoryBean svrBean = new ServerFactoryBean();
        svrBean.setAddress("http://localhost/Hello");
        svrBean.setServiceClass(HelloService.class);
        svrBean.setServiceBean(new HelloServiceImpl());
        svrBean.setBus(getBus());
        svrBean.setDestinationFactory(new CustomDestinationFactory(getBus()));

        ServerImpl server = (ServerImpl)svrBean.create();
        assertTrue(server.getDestination() instanceof CustomDestination);
    }
    
        
    @SuppressWarnings("unchecked")
    @Test
    public void testJaxbExtraClass() throws Exception {
        ServerFactoryBean svrBean = new ServerFactoryBean();
        svrBean.setAddress("http://localhost/Hello");
        svrBean.setServiceClass(HelloServiceImpl.class);
        svrBean.setBus(getBus());

        Map props = svrBean.getProperties();
        if (props == null) {
            props = new HashMap<String, Object>();
        }
        props.put("jaxb.additionalContextClasses", 
                  new Class[] {GreetMe.class, GreetMeOneWay.class});
        svrBean.setProperties(props);
        svrBean.create();
        Class[] extraClass = ((JAXBDataBinding)svrBean.getServiceFactory().getDataBinding()).getExtraClass();
        assertEquals(extraClass.length, 2);
        assertEquals(extraClass[0], GreetMe.class);
        assertEquals(extraClass[1], GreetMeOneWay.class);
    }

    public class CustomDestinationFactory extends AbstractTransportFactory implements DestinationFactory {
        public CustomDestinationFactory(Bus b) {
            super(Arrays.asList("id"), b);
        }
        public Destination getDestination(EndpointInfo ei) throws IOException {
            return new CustomDestination();
        }

    }

    public static class CustomDestination implements Destination {

        public EndpointReferenceType getAddress() {
            // TODO Auto-generated method stub
            return null;
        }

        public Conduit getBackChannel(Message inMessage, 
                                      Message partialResponse, 
                                      EndpointReferenceType address) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public void shutdown() {
            // TODO Auto-generated method stub
            
        }

        public void setMessageObserver(MessageObserver observer) {
            // TODO Auto-generated method stub
            
        }

        public MessageObserver getMessageObserver() {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
