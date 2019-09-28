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
import org.apache.cxf.message.Message;

import org.easymock.EasyMock;
import org.junit.Test;

public class ResponseTimeMessageInvokerInterceptorTest extends AbstractMessageResponseTestBase {

    private ResponseTimeMessageInvokerInterceptor invokerInterceptor =
        new ResponseTimeMessageInvokerInterceptor();
    private ResponseTimeMessageInvokerInterceptor.ResponseTimeMessageInvokerEndingInteceptor rtmii =
        invokerInterceptor.new ResponseTimeMessageInvokerEndingInteceptor();

    @Test
    public void testServerOneWayMessageIn() {
        // need to increase the counter and is not a client
        setupCounterRepository(true, false);
        setupExchangeForMessage();
        setupOperationForMessage();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(message);
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        mhtr.setOneWay(true);
        EasyMock.expectLastCall();
        mhtr.endHandling();
        EasyMock.expectLastCall();

        EasyMock.replay(mhtr);
        EasyMock.expect(exchange.isOneWay()).andReturn(true);
        EasyMock.expect(exchange.get(MessageHandlingTimeRecorder.class)).andReturn(mhtr);
        EasyMock.replay(exchange);
        EasyMock.replay(message);

        rtmii.handleMessage(message);
        EasyMock.verify(message);
        EasyMock.verify(bus);
        EasyMock.verify(exchange);
        EasyMock.verify(mhtr);
    }
}
