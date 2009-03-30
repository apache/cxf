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
import org.easymock.classextension.EasyMock;
import org.junit.Test;

public class ResponseTimeMessageOutInterceptorTest extends AbstractMessageResponseTestBase {
    private ResponseTimeMessageOutInterceptor rtmoi = new ResponseTimeMessageOutInterceptor();
    
    @Test
    public void testServerMessageOut() {
        // need to increase the counter and is not a client
        setupCounterRepository(true, false);
        setupExchangeForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        EasyMock.expect(exchange.get(FaultMode.class)).andReturn(null);
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        mhtr.setFaultMode(null);
        EasyMock.expectLastCall();
        mhtr.endHandling();
        EasyMock.expectLastCall();              
         
        EasyMock.replay(mhtr);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(mhtr);        
        EasyMock.replay(exchange);
        EasyMock.replay(message);
        
        rtmoi.handleMessage(message);
        EasyMock.verify(message);
        EasyMock.verify(bus);
        EasyMock.verify(exchange);
        EasyMock.verify(mhtr);
        EasyMock.verify(cRepository);
        
    }

    @Test
    public void testServerCheckedApplicationFaultMessageOut() {
        testServerFaultMessageOut(FaultMode.CHECKED_APPLICATION_FAULT);
    }

    @Test
    public void testServerLogicalRuntimeFaultMessageOut() {
        testServerFaultMessageOut(FaultMode.LOGICAL_RUNTIME_FAULT);
    }
    
    @Test
    public void testServerRuntimeFaultMessageOut() {
        testServerFaultMessageOut(FaultMode.RUNTIME_FAULT);
    }
    
    @Test
    public void testServerUncheckedApplicationFaultMessageOut() {
        testServerFaultMessageOut(FaultMode.UNCHECKED_APPLICATION_FAULT);
    }
    
    public void testServerFaultMessageOut(FaultMode faultMode) {
        // need to increase the counter and is not a client
        setupCounterRepository(true, false);
        setupExchangeForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE).anyTimes();
        EasyMock.expect(message.get(FaultMode.class)).andReturn(faultMode).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        exchange.put(FaultMode.class, faultMode);
        EasyMock.expectLastCall();
        EasyMock.expect(exchange.isOneWay()).andReturn(false);
        EasyMock.expect(exchange.get(FaultMode.class)).andReturn(faultMode).anyTimes();
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        mhtr.setFaultMode(faultMode);
        EasyMock.expectLastCall();
        mhtr.endHandling();
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
        EasyMock.verify(cRepository);
        
    }

    @Test
    public void testClientOneWayMessageOut() {
        //need to increase the counter and is a client
        setupCounterRepository(true, true);
        setupExchangeForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes(); 
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        //MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        //mhtr.setOneWay(true);
        //EasyMock.expectLastCall();
         
        //EasyMock.replay(mhtr);
        EasyMock.expect(exchange.isOneWay()).andReturn(true);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(null);        
        exchange.put(EasyMock.eq(MessageHandlingTimeRecorder.class), 
                     EasyMock.isA(MessageHandlingTimeRecorder.class));
        EasyMock.expectLastCall();
        EasyMock.replay(exchange);
        EasyMock.replay(message);
        
        rtmoi.handleMessage(message);
        EasyMock.verify(message);
        EasyMock.verify(bus);
        EasyMock.verify(exchange);
        //EasyMock.verify(mhtr);
        EasyMock.verify(cRepository);
    }
    
    @Test
    public void testClientMessageOut() {
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE);
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(exchange.isOneWay()).andReturn(false);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(null);     
        exchange.put(EasyMock.eq(MessageHandlingTimeRecorder.class), 
                     EasyMock.isA(MessageHandlingTimeRecorder.class));
        EasyMock.replay(exchange);
        EasyMock.replay(message);
        rtmoi.handleMessage(message);        
        EasyMock.verify(message);        
        EasyMock.verify(exchange);
        
    }
}
