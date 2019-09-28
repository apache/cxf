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

package org.apache.cxf.management.interceptor;

import javax.management.ObjectName;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.counters.MessageHandlingTimeRecorder;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;

import org.easymock.EasyMock;
import org.junit.Before;


public class AbstractMessageResponseTestBase {
    protected static final QName SERVICE_NAME = new QName("http://org.apache.cxf", "hello");
    protected static final QName OPERATION_NAME = new QName("http://org.apache.cxf", "world");
    protected static final QName PORT_NAME = new QName("http://org.apache.cxf", "port");

    protected static final String CLIENT_SERVICE_ONAME =
        "org.apache.cxf:type=Performance.Counter.Client,bus.id=cxf,service=\""
        + SERVICE_NAME.toString() + "\",port=\""
        + PORT_NAME.getLocalPart() + "\"";
    protected static final String SERVER_SERVICE_ONAME =
        "org.apache.cxf:type=Performance.Counter.Server,bus.id=cxf,service=\""
        + SERVICE_NAME.toString() + "\",port=\""
        + PORT_NAME.getLocalPart() + "\"";
    protected ObjectName clientServiceCounterOName;
    protected ObjectName serverServiceCounterOName;
    protected ObjectName clientOperationCounterOName;
    protected ObjectName serverOperationCounterOName;

    protected Bus bus;
    protected Message message;
    protected Exchange exchange;
    protected CounterRepository cRepository;

    @Before
    public void setUp() throws Exception {
        message = EasyMock.createMock(Message.class);
        exchange = EasyMock.createMock(Exchange.class);
        bus = EasyMock.createMock(Bus.class);
        cRepository = EasyMock.createMock(CounterRepository.class);
        clientServiceCounterOName = new ObjectName(CLIENT_SERVICE_ONAME);
        serverServiceCounterOName = new ObjectName(SERVER_SERVICE_ONAME);
        clientOperationCounterOName = new ObjectName(CLIENT_SERVICE_ONAME
            + ",operation=\"" + OPERATION_NAME.getLocalPart() + "\"");
        serverOperationCounterOName = new ObjectName(SERVER_SERVICE_ONAME
            + ",operation=\"" + OPERATION_NAME.getLocalPart() + "\"");
    }

    protected void setupCounterRepository(boolean increase, boolean isClient) {
        ObjectName serviceCounterOName;
        ObjectName operationCounterOName;
        if (isClient) {
            serviceCounterOName = clientServiceCounterOName;
            operationCounterOName = clientOperationCounterOName;
        } else {
            serviceCounterOName = serverServiceCounterOName;
            operationCounterOName = serverOperationCounterOName;
        }
        BusFactory.setDefaultBus(bus);
        bus.getExtension(CounterRepository.class);
        EasyMock.expectLastCall().andReturn(cRepository).anyTimes();
        if (increase) {
            EasyMock.expect(bus.getId()).andReturn(Bus.DEFAULT_BUS_ID).anyTimes();
            cRepository.increaseCounter(EasyMock.eq(serviceCounterOName),
                EasyMock.isA(MessageHandlingTimeRecorder.class));
            EasyMock.expectLastCall();
            cRepository.increaseCounter(EasyMock.eq(operationCounterOName),
                EasyMock.isA(MessageHandlingTimeRecorder.class));
            EasyMock.expectLastCall();
            EasyMock.expect(cRepository.getCounter(EasyMock.isA(ObjectName.class))).andReturn(null);
            EasyMock.replay(cRepository);
        }

        EasyMock.replay(bus);
        // increase the number
    }

    protected void setupExchangeForMessage() {
        EasyMock.expect(exchange.getBus()).andReturn(bus).anyTimes();

        Service service = EasyMock.createMock(Service.class);
        EasyMock.expect(service.getName()).andReturn(SERVICE_NAME).anyTimes();
        EasyMock.expect(exchange.getService()).andReturn(service).anyTimes();
        EasyMock.replay(service);

        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        EndpointInfo endpointInfo = EasyMock.createMock(EndpointInfo.class);
        EasyMock.expect(endpointInfo.getName()).andReturn(PORT_NAME).anyTimes();
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(endpointInfo).anyTimes();
        EasyMock.expect(endpoint.get("javax.management.ObjectName")).andReturn(null).anyTimes();
        EasyMock.expect(endpoint.put(EasyMock.eq("javax.management.ObjectName"), EasyMock.anyObject(ObjectName.class)))
            .andReturn(null).anyTimes();
        EasyMock.expect(exchange.getEndpoint()).andReturn(endpoint).anyTimes();
        EasyMock.replay(endpointInfo);
        EasyMock.replay(endpoint);


        //EasyMock.expect(exchange.getBus()).andReturn(bus);
        EasyMock.expect(exchange.get("org.apache.cxf.management.service.counter.name")).andReturn(null).anyTimes();
    }

    protected void setupOperationForMessage() {
        OperationInfo op = EasyMock.createMock(OperationInfo.class);
        BindingOperationInfo bop = EasyMock.createMock(BindingOperationInfo.class);
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(bop);
        EasyMock.expect(bop.getOperationInfo()).andReturn(op);
        EasyMock.expect(op.getName()).andReturn(OPERATION_NAME);
        EasyMock.expect(op.getProperty("javax.management.ObjectName", ObjectName.class)).andReturn(null).anyTimes();
        op.setProperty(EasyMock.eq("javax.management.ObjectName"),
                                       EasyMock.anyObject(ObjectName.class));
        EasyMock.expectLastCall();
        EasyMock.replay(bop, op);
    }

}
