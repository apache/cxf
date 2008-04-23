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

package org.apache.cxf.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.phase.PhaseManagerImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OutgoingChainInterceptorTest extends Assert {

    private IMocksControl control;
    private Bus bus;
    private Service service;
    private Endpoint endpoint;
    private BindingOperationInfo bopInfo;
    private OperationInfo opInfo;
    private BindingMessageInfo bmInfo;
    private MessageInfo mInfo;
    private List<Phase> phases;
    private List<Interceptor> empty;
    private Binding binding;

    @Before
    public void setUp() throws Exception {

        control = EasyMock.createNiceControl();

        phases = new ArrayList<Phase>();
        phases.add(new Phase(Phase.SEND, 1000));
        empty = new ArrayList<Interceptor>();

        bus = control.createMock(Bus.class);
        PhaseManager pm = new PhaseManagerImpl();
        EasyMock.expect(bus.getExtension(PhaseManager.class)).andReturn(pm).anyTimes();

        service = control.createMock(Service.class);
        endpoint = control.createMock(Endpoint.class);
        binding = control.createMock(Binding.class);
        EasyMock.expect(endpoint.getBinding()).andStubReturn(binding);
        MessageImpl m = new MessageImpl();
        EasyMock.expect(binding.createMessage()).andStubReturn(m);

        EasyMock.expect(endpoint.getService()).andReturn(service).anyTimes();
        EasyMock.expect(endpoint.getOutInterceptors()).andReturn(empty);
        EasyMock.expect(service.getOutInterceptors()).andReturn(empty);
        EasyMock.expect(bus.getOutInterceptors()).andReturn(empty);

        bopInfo = control.createMock(BindingOperationInfo.class);
        opInfo = control.createMock(OperationInfo.class);
        mInfo = control.createMock(MessageInfo.class);
        bmInfo = control.createMock(BindingMessageInfo.class);
        EasyMock.expect(bopInfo.getOperationInfo()).andReturn(opInfo).times(3);
        EasyMock.expect(opInfo.getOutput()).andReturn(mInfo);
        EasyMock.expect(opInfo.isOneWay()).andReturn(false);
        EasyMock.expect(bopInfo.getOutput()).andReturn(bmInfo);

        control.replay();

    }

    @After
    public void tearDown() {
        control.verify();
    }

    @Test
    public void testInterceptor() throws Exception {
        OutgoingChainInterceptor intc = new OutgoingChainInterceptor();

        MessageImpl m = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        m.setExchange(exchange);
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);
        exchange.put(Binding.class, binding);
        exchange.put(BindingOperationInfo.class, bopInfo);
        exchange.setOutMessage(m);
        intc.handleMessage(m);
    }

}
