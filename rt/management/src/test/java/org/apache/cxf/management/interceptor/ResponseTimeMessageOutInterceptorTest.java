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

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.management.counters.MessageHandlingTimeRecorder;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResponseTimeMessageOutInterceptorTest extends AbstractMessageResponseTestBase {
    private ResponseTimeMessageOutInterceptor rtmoi = new ResponseTimeMessageOutInterceptor();

    @Test
    public void testServerMessageOut() {
        // need to increase the counter and is not a client
        setupCounterRepository(true, false);
        setupExchangeForMessage();
        setupOperationForMessage();
        when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(Boolean.FALSE);
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.FALSE);
        when(exchange.getOutMessage()).thenReturn(message);
        when(exchange.get("org.apache.cxf.management.counter.enabled")).thenReturn(null);
        when(exchange.get(Exception.class)).thenReturn(null);
        when(exchange.get(FaultMode.class)).thenReturn(null);
        MessageHandlingTimeRecorder mhtr = mock(MessageHandlingTimeRecorder.class);
        doNothing().when(mhtr).endHandling();
        doNothing().when(mhtr).setFaultMode(null);

        when(exchange.get(MessageHandlingTimeRecorder.class)).thenReturn(mhtr);

        rtmoi.handleMessage(message);
        verify(mhtr, atLeastOnce()).endHandling();
        verify(mhtr, atLeastOnce()).setFaultMode(null);
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
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.FALSE);
        when(message.get(FaultMode.class)).thenReturn(faultMode);
        when(exchange.get("org.apache.cxf.management.counter.enabled")).thenReturn(null);
        when(exchange.get(FaultMode.class)).thenReturn(faultMode);

        rtmoi.handleFault(message);
    }

    @Test
    public void testClientOneWayMessageOut() {
        //need to increase the counter and is a client
        setupCounterRepository(true, true);
        setupExchangeForMessage();
        setupOperationForMessage();
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(Boolean.FALSE);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.TRUE);
        when(exchange.getOutMessage()).thenReturn(message);
        when(exchange.get(FaultMode.class)).thenReturn(null);
        when(exchange.get(Exception.class)).thenReturn(null);
        when(exchange.isOneWay()).thenReturn(true);
        MessageHandlingTimeRecorder mhtr = mock(MessageHandlingTimeRecorder.class);
        when(exchange.get(MessageHandlingTimeRecorder.class)).thenReturn(mhtr);
        when(exchange.get("org.apache.cxf.management.counter.enabled")).thenReturn(null);

        InterceptorChain chain = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(chain);
        doNothing().when(chain).add(isA(ResponseTimeMessageOutInterceptor.EndingInterceptor.class));

        rtmoi.handleMessage(message);
        rtmoi.getEndingInterceptor().handleMessage(message);

        verify(chain, atLeastOnce()).add(isA(ResponseTimeMessageOutInterceptor.EndingInterceptor.class));
    }

    @Test
    public void testClientMessageOut() {
        when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(Boolean.FALSE);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getBus()).thenReturn(bus);
        when(exchange.get("org.apache.cxf.management.counter.enabled")).thenReturn(null);

        rtmoi.handleMessage(message);
    }
}
