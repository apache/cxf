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

import org.apache.cxf.management.counters.MessageHandlingTimeRecorder;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResponseTimeMessageInInterceptorTest extends AbstractMessageResponseTestBase {

    private ResponseTimeMessageInInterceptor rtmii = new ResponseTimeMessageInInterceptor();
    private ResponseTimeMessageOutInterceptor rtmoi = new ResponseTimeMessageOutInterceptor();

    @Test
    public void testClientMessageIn() {
        // need to increase the counter and is a client
        setupCounterRepository(true, true);
        setupOperationForMessage();
        setupExchangeForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(message).anyTimes();
        EasyMock.expect(exchange.get("org.apache.cxf.management.counter.enabled")).andReturn(null);

        EasyMock.expect(exchange.get(FaultMode.class)).andReturn(null);
        EasyMock.expect(exchange.isOneWay()).andReturn(false);
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        mhtr.endHandling();
        EasyMock.expectLastCall();
        mhtr.setFaultMode(null);
        EasyMock.expectLastCall();

        EasyMock.replay(mhtr);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(mhtr);
        EasyMock.replay(exchange);
        EasyMock.replay(message);

        rtmii.handleMessage(message);
        EasyMock.verify(message);
        EasyMock.verify(bus);
        EasyMock.verify(exchange);
        EasyMock.verify(mhtr);
        EasyMock.verify(cRepository);

    }

    @Test
    public void testClientCheckedApplicationFaultMessageIn() {
        testClientFaultMessageIn(FaultMode.CHECKED_APPLICATION_FAULT);
    }

    @Test
    public void testClientLogicalFaultMessageIn() {
        testClientFaultMessageIn(FaultMode.LOGICAL_RUNTIME_FAULT);
    }

    @Test
    public void testClientRuntimeFaultMessageIn() {
        testClientFaultMessageIn(FaultMode.RUNTIME_FAULT);
    }

    @Test
    public void testClientUncheckedApplicationFaultMessageIn() {
        testClientFaultMessageIn(FaultMode.UNCHECKED_APPLICATION_FAULT);
    }

    public void testClientFaultMessageIn(FaultMode faultMode) {
        // need to increase the counter and is a client
        setupCounterRepository(true, true);
        setupExchangeForMessage();
        setupOperationForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes();
        EasyMock.expect(exchange.isOneWay()).andReturn(false);
        EasyMock.expect(exchange.get("org.apache.cxf.management.counter.enabled")).andReturn(true);
        exchange.put(FaultMode.class, faultMode);
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(FaultMode.class)).andReturn(faultMode).anyTimes();
        EasyMock.expect(exchange.get(FaultMode.class)).andReturn(faultMode).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        mhtr.endHandling();
        EasyMock.expectLastCall();
        mhtr.setFaultMode(faultMode);
        EasyMock.expectLastCall();

        EasyMock.replay(mhtr);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(mhtr);
        EasyMock.replay(exchange);
        EasyMock.replay(message);

        rtmoi.handleFault(message);
        EasyMock.verify(message);
        EasyMock.verify(bus);
        EasyMock.verify(exchange);
        EasyMock.verify(mhtr);
    }


    // it would not fire the counter increase action now
    @Test
    public void testServerOneWayMessageIn() {
        // need to increase the counter and is not a client
        setupCounterRepository(true, false);
        setupExchangeForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE).anyTimes();
        EasyMock.expect(exchange.get("org.apache.cxf.management.counter.enabled")).andReturn(null);
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        mhtr.beginHandling();
        EasyMock.expectLastCall();

        EasyMock.replay(mhtr);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(mhtr);


        EasyMock.replay(exchange);
        EasyMock.replay(message);

        rtmii.handleMessage(message);
        EasyMock.verify(message);
        EasyMock.verify(exchange);
        EasyMock.verify(mhtr);
    }

    @Test
    public void testServiceMessageIn() {
        setupCounterRepository(true, false);
        setupExchangeForMessage();
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE).anyTimes();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(exchange.get("org.apache.cxf.management.counter.enabled")).andReturn(null);
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(null);
        exchange.put(EasyMock.eq(MessageHandlingTimeRecorder.class),
                     EasyMock.isA(MessageHandlingTimeRecorder.class));
        EasyMock.replay(exchange);
        EasyMock.replay(message);
        rtmii.handleMessage(message);
        EasyMock.verify(message);
        EasyMock.verify(exchange);

    }

    @Test
    public void testIsClient() {
        Message message1 = null;
        Message message2 = new MessageImpl();
        Message message3 = new MessageImpl();
        message3.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        assertFalse("the message should not be client", rtmii.isClient(message1));
        assertFalse("the message should not be client", rtmii.isClient(message2));
        assertTrue("the message should be client", rtmii.isClient(message3));
    }

}
