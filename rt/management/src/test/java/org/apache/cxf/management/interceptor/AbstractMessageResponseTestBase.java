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

import org.junit.Before;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        message = mock(Message.class);
        exchange = mock(Exchange.class);
        bus = mock(Bus.class);
        cRepository = mock(CounterRepository.class);
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
        when(bus.getExtension(CounterRepository.class)).thenReturn(cRepository);
        if (increase) {
            when(bus.getId()).thenReturn(Bus.DEFAULT_BUS_ID);
            doNothing().when(cRepository).increaseCounter(eq(serviceCounterOName),
                isA(MessageHandlingTimeRecorder.class));

            doNothing().when(cRepository).increaseCounter(eq(operationCounterOName),
                isA(MessageHandlingTimeRecorder.class));

            when(cRepository.getCounter(any(ObjectName.class))).thenReturn(null);
        }

        // increase the number
    }

    protected void setupExchangeForMessage() {
        when(exchange.getBus()).thenReturn(bus);

        Service service = mock(Service.class);
        when(service.getName()).thenReturn(SERVICE_NAME);
        when(exchange.getService()).thenReturn(service);

        Endpoint endpoint = mock(Endpoint.class);
        EndpointInfo endpointInfo = mock(EndpointInfo.class);
        when(endpointInfo.getName()).thenReturn(PORT_NAME);
        when(endpoint.getEndpointInfo()).thenReturn(endpointInfo);
        when(endpoint.get("javax.management.ObjectName")).thenReturn(null);
        when(endpoint.put(eq("javax.management.ObjectName"), any(ObjectName.class))).thenReturn(null);
        when(exchange.getEndpoint()).thenReturn(endpoint);

        //EasyMock.expect(exchange.getBus()).andReturn(bus);
        when(exchange.get("org.apache.cxf.management.service.counter.name")).thenReturn(null);
    }

    protected void setupOperationForMessage() {
        OperationInfo op = mock(OperationInfo.class);
        BindingOperationInfo bop = mock(BindingOperationInfo.class);
        when(exchange.getBindingOperationInfo()).thenReturn(bop);
        when(bop.getOperationInfo()).thenReturn(op);
        when(op.getName()).thenReturn(OPERATION_NAME);
        when(op.getProperty("javax.management.ObjectName", ObjectName.class)).thenReturn(null);
        doNothing().when(op).setProperty(eq("javax.management.ObjectName"),
                                       any(ObjectName.class));
    }

}
