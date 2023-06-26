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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResponseTimeMessageInInterceptorTest extends AbstractMessageResponseTestBase {

    private ResponseTimeMessageInInterceptor rtmii = new ResponseTimeMessageInInterceptor();
    private ResponseTimeMessageOutInterceptor rtmoi = new ResponseTimeMessageOutInterceptor();

    @Test
    public void testClientMessageIn() {
        // need to increase the counter and is a client
        setupCounterRepository(true, true);
        setupOperationForMessage();
        setupExchangeForMessage();
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.TRUE);
        when(exchange.getOutMessage()).thenReturn(message);
        when(exchange.get("org.apache.cxf.management.counter.enabled")).thenReturn(null);

        when(exchange.get(FaultMode.class)).thenReturn(null);
        when(exchange.isOneWay()).thenReturn(false);
        MessageHandlingTimeRecorder mhtr = mock(MessageHandlingTimeRecorder.class);
        doNothing().when(mhtr).endHandling();
        doNothing().when(mhtr).setFaultMode(null);

        when(exchange.get(MessageHandlingTimeRecorder.class)).thenReturn(mhtr);

        rtmii.handleMessage(message);
        verify(mhtr, atLeastOnce()).endHandling();
        verify(mhtr, atLeastOnce()).setFaultMode(null);
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
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.TRUE);
        when(exchange.isOneWay()).thenReturn(false);
        when(exchange.get("org.apache.cxf.management.counter.enabled")).thenReturn(true);
        doNothing().when(exchange).put(FaultMode.class, faultMode);
        when(message.get(FaultMode.class)).thenReturn(faultMode);
        when(exchange.get(FaultMode.class)).thenReturn(faultMode);
        when(exchange.getOutMessage()).thenReturn(message);
        MessageHandlingTimeRecorder mhtr = mock(MessageHandlingTimeRecorder.class);
        doNothing().when(mhtr).endHandling();
        doNothing().when(mhtr).setFaultMode(faultMode);

        when(exchange.get(MessageHandlingTimeRecorder.class)).thenReturn(mhtr);

        rtmoi.handleFault(message);
        verify(exchange, atLeastOnce()).put(FaultMode.class, faultMode);
        verify(mhtr, atLeastOnce()).endHandling();
        verify(mhtr, atLeastOnce()).setFaultMode(faultMode);
    }


    // it would not fire the counter increase action now
    @Test
    public void testServerOneWayMessageIn() {
        // need to increase the counter and is not a client
        setupCounterRepository(true, false);
        setupExchangeForMessage();
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.FALSE);
        when(exchange.get("org.apache.cxf.management.counter.enabled")).thenReturn(null);
        when(exchange.getOutMessage()).thenReturn(message);
        MessageHandlingTimeRecorder mhtr = mock(MessageHandlingTimeRecorder.class);
        doNothing().when(mhtr).beginHandling();

        when(exchange.get(MessageHandlingTimeRecorder.class)).thenReturn(mhtr);
        rtmii.handleMessage(message);

        verify(mhtr, atLeastOnce()).beginHandling();
    }

    @Test
    public void testServiceMessageIn() {
        setupCounterRepository(true, false);
        setupExchangeForMessage();
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.FALSE);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.get("org.apache.cxf.management.counter.enabled")).thenReturn(null);
        when(exchange.getOutMessage()).thenReturn(message);
        when(exchange.get(MessageHandlingTimeRecorder.class)).thenReturn(null);

        doNothing().when(exchange).put(eq(MessageHandlingTimeRecorder.class),
                     isA(MessageHandlingTimeRecorder.class));

        rtmii.handleMessage(message);

        verify(exchange, atLeastOnce()).put(eq(MessageHandlingTimeRecorder.class),
            isA(MessageHandlingTimeRecorder.class));
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
