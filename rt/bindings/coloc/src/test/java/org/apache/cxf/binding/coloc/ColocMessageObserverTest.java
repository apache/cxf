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
package org.apache.cxf.binding.coloc;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ColocMessageObserverTest extends Assert {
    private IMocksControl control = EasyMock.createNiceControl();
    private ColocMessageObserver observer;
    private Message msg;
    private Exchange ex;
    private Service srv;
    private Endpoint ep;
    private Bus bus;

    @Before
    public void setUp() throws Exception {
        ep = control.createMock(Endpoint.class);
        bus = control.createMock(Bus.class);
        srv = control.createMock(Service.class);
        BusFactory.setDefaultBus(bus);        
        msg = new MessageImpl();
        ex = new ExchangeImpl();
        //msg.setExchange(ex);
    }

    @After
    public void tearDown() throws Exception {
        BusFactory.setDefaultBus(null);
    }

    @Test
    public void testSetExchangeProperties() throws Exception {
        QName opName = new QName("A", "B");
        msg.put(Message.WSDL_OPERATION, opName);    
        EasyMock.expect(ep.getService()).andReturn(srv);
        Binding binding = control.createMock(Binding.class);
        EasyMock.expect(ep.getBinding()).andReturn(binding);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(ep.getEndpointInfo()).andReturn(ei);
        BindingInfo bi = control.createMock(BindingInfo.class);
        EasyMock.expect(ei.getBinding()).andReturn(bi);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(bi.getOperation(opName)).andReturn(boi);
        EasyMock.expect(bus.getExtension(ClassLoader.class)).andReturn(this.getClass().getClassLoader());
        control.replay();
        observer = new ColocMessageObserver(ep, bus);
        observer.setExchangeProperties(ex, msg);
        control.verify();

        assertNotNull("Bus should be set",
                      ex.getBus());
        assertNotNull("Endpoint should be set",
                      ex.getEndpoint());
        assertNotNull("Binding should be set",
                      ex.getBinding());
        assertNotNull("Service should be set",
                      ex.getService());
        assertNotNull("BindingOperationInfo should be set",
                      ex.getBindingOperationInfo());
    }

    @Test
    public void testObserverOnMessage() throws Exception {
        msg.setExchange(ex);
        
        Binding binding = control.createMock(Binding.class);
        EasyMock.expect(ep.getBinding()).andReturn(binding);
        
        Message inMsg = new MessageImpl();
        EasyMock.expect(binding.createMessage()).andReturn(inMsg);

        EasyMock.expect(ep.getService()).andReturn(srv).anyTimes();
        EasyMock.expect(
            bus.getExtension(PhaseManager.class)).andReturn(
                                      new PhaseManagerImpl()).times(2);
        EasyMock.expect(bus.getInInterceptors()).andReturn(new ArrayList<Interceptor<? extends Message>>());
        EasyMock.expect(ep.getInInterceptors()).andReturn(new ArrayList<Interceptor<? extends Message>>());
        EasyMock.expect(srv.getInInterceptors()).andReturn(new ArrayList<Interceptor<? extends Message>>());
        EasyMock.expect(bus.getExtension(ClassLoader.class)).andReturn(this.getClass().getClassLoader());
        control.replay();
        observer = new TestColocMessageObserver(ep, bus);
        observer.onMessage(msg);
        control.verify();

        Exchange inEx = inMsg.getExchange();
        assertNotNull("Should Have a valid Exchange", inEx);
        assertEquals("Message.REQUESTOR_ROLE should be false",
                     Boolean.FALSE,
                     inMsg.get(Message.REQUESTOR_ROLE));
        assertEquals("Message.INBOUND_MESSAGE should be true",
                     Boolean.TRUE,
                     inMsg.get(Message.INBOUND_MESSAGE));
        assertNotNull("Chain should be set", inMsg.getInterceptorChain());
        Exchange ex1 = msg.getExchange();
        assertNotNull("Exchange should be set", ex1);
    }
    
    class TestColocMessageObserver extends ColocMessageObserver {
        public TestColocMessageObserver(Endpoint endpoint, Bus bus) {
            super(endpoint, bus);
        }
        
        public void setExchangeProperties(Exchange exchange, Message m) {
            exchange.put(Bus.class, bus);
            exchange.put(Endpoint.class, ep);
            exchange.put(Service.class, srv);
        }
        
        protected List<Interceptor<? extends Message>> addColocInterceptors() {
            return new ArrayList<Interceptor<? extends Message>>();
        }
    }
}
