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

import org.junit.Test;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.FALSE);
        when(exchange.getOutMessage()).thenReturn(message);
        MessageHandlingTimeRecorder mhtr = mock(MessageHandlingTimeRecorder.class);
        doNothing().when(mhtr).setOneWay(true);
        doNothing().when(mhtr).endHandling();

        when(exchange.isOneWay()).thenReturn(true);
        when(exchange.get(MessageHandlingTimeRecorder.class)).thenReturn(mhtr);

        rtmii.handleMessage(message);
        verify(mhtr, atLeastOnce()).setOneWay(true);
        verify(mhtr, atLeastOnce()).endHandling();
    }
}
