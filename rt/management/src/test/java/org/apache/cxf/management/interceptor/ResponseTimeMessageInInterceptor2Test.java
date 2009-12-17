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
import org.apache.cxf.service.model.OperationInfo;
import org.easymock.classextension.EasyMock;

import org.junit.Test;

public class ResponseTimeMessageInInterceptor2Test extends AbstractMessageResponseTestBase {
   
    private ResponseTimeMessageInInterceptor rtmii = new ResponseTimeMessageInInterceptor();
    
    @Test
    public void testClientMessageIn() {
        // need to increase the counter and is a client
        setupCounterRepository(true, true);
        setupExchangeForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
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
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes();
        EasyMock.expect(message.get(FaultMode.class)).andReturn(faultMode).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        exchange.put(FaultMode.class, faultMode);
        EasyMock.expectLastCall();
        EasyMock.expect(exchange.get(FaultMode.class)).andReturn(faultMode).anyTimes();
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        mhtr.endHandling();
        EasyMock.expectLastCall();        
        mhtr.setFaultMode(faultMode);
        EasyMock.expectLastCall();
         
        EasyMock.replay(mhtr);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(mhtr);        
        EasyMock.replay(exchange);
        EasyMock.replay(message);
        
        rtmii.handleFault(message);
        EasyMock.verify(message);
        EasyMock.verify(bus);
        EasyMock.verify(exchange);
        EasyMock.verify(mhtr);
        EasyMock.verify(cRepository);
        
    }

    
    // it would not fire the counter increase action now
    @Test
    public void testServerOneWayMessageIn() {
        // need to increase the counter and is not a client
        //setupCounterRepository(false, false);
        //setupExchangeForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE).anyTimes();
        //EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        mhtr.beginHandling();
        EasyMock.expectLastCall();
         
        EasyMock.replay(mhtr);
        //EasyMock.expect(exchange.isOneWay()).andReturn(true);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(mhtr);        
        EasyMock.replay(exchange);
        EasyMock.replay(message);
        
        rtmii.handleMessage(message);
        EasyMock.verify(message);
        //EasyMock.verify(bus);
        EasyMock.verify(exchange);
        EasyMock.verify(mhtr);
        //EasyMock.verify(cRepository);
    }
    
    @Test
    public void testServiceMessageIn() {
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE);
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        //EasyMock.expect(exchange.isOneWay()).andReturn(false);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(null);     
        exchange.put(EasyMock.eq(MessageHandlingTimeRecorder.class), 
                     EasyMock.isA(MessageHandlingTimeRecorder.class));
        EasyMock.replay(exchange);
        EasyMock.replay(message);
        rtmii.handleMessage(message);        
        EasyMock.verify(message);        
        EasyMock.verify(exchange);
        
    }
    
    public void testIsClient() {
        Message message1 = null;
        Message message2 = new MessageImpl();
        Message message3 = new MessageImpl();
        message3.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        assertTrue("the message should not be client", !rtmii.isClient(message1));
        assertTrue("the message should not be client", !rtmii.isClient(message2));
        assertTrue("the message should be client", rtmii.isClient(message3));
    }

    @Override
    protected void setupOperationForMessage() {
        EasyMock.expect(exchange.get(OperationInfo.class)).andReturn(null);
        EasyMock.expect(exchange.get("org.apache.cxf.resource.operation.name"))
            .andReturn(OPERATION_NAME.getLocalPart());
    }
}
