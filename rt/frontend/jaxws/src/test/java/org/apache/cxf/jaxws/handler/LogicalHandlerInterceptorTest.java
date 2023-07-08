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

package org.apache.cxf.jaxws.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import jakarta.xml.ws.Binding;
import jakarta.xml.ws.LogicalMessage;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.LogicalHandler;
import jakarta.xml.ws.handler.LogicalMessageContext;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerInInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.MessageObserver;
import org.apache.handlers.types.AddNumbersResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogicalHandlerInterceptorTest {

    private Binding binding;
    private HandlerChainInvoker invoker;
    private Message message;
    private Exchange exchange;

    @Before
    public void setUp() {
        binding = mock(Binding.class);
        invoker = mock(HandlerChainInvoker.class);
        message = mock(Message.class);
        exchange = mock(Exchange.class);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testInterceptSuccess() {
        List<LogicalHandler<?>> list = new ArrayList<>();
        list.add(new LogicalHandler<LogicalMessageContext>() {
            public void close(MessageContext arg0) {
            }

            public boolean handleFault(LogicalMessageContext arg0) {
                return true;
            }

            public boolean handleMessage(LogicalMessageContext arg0) {
                return true;
            }
        });
        @SuppressWarnings("rawtypes")
        List<Handler> hList = CastUtils.cast(list);
        when(binding.getHandlerChain()).thenReturn(hList);
        when(invoker.getLogicalHandlers()).thenReturn(list);
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.TRUE);
        when(message.keySet()).thenReturn(new TreeSet<String>());
        when(exchange.get(HandlerChainInvoker.class)).thenReturn(invoker);
        when(exchange.getOutMessage()).thenReturn(message);
        when(invoker.invokeLogicalHandlers(eq(true), isA(LogicalMessageContext.class)))
            .thenReturn(true);

        LogicalHandlerInInterceptor li = new LogicalHandlerInInterceptor(binding);
        assertEquals("unwhened phase", "pre-protocol-frontend", li.getPhase());
        li.handleMessage(message);
    }

    //JAX-WS spec: If handler returns false, for a request-response MEP, if the message
    //direction is reversed during processing of a request message then the message
    //becomes a response message.
    //NOTE: commented out as this has been covered by other tests.
    @Test
    @org.junit.Ignore
    public void xtestReturnFalseClientSide() throws Exception {
        @SuppressWarnings("rawtypes")
        List<Handler> list = new ArrayList<>();
        list.add(new LogicalHandler<LogicalMessageContext>() {
            public void close(MessageContext arg0) {
            }

            public boolean handleFault(LogicalMessageContext messageContext) {
                return true;
            }

            public boolean handleMessage(LogicalMessageContext messageContext) {
                LogicalMessage msg = messageContext.getMessage();
                AddNumbersResponse resp = new AddNumbersResponse();
                resp.setReturn(11);
                msg.setPayload(resp, null);
                return false;
            }
        });
        HandlerChainInvoker invoker1 = new HandlerChainInvoker(list);

        Binding binding1 = mock(Binding.class);
        @SuppressWarnings("rawtypes")
        List<Handler> hList = CastUtils.cast(list);
        when(binding1.getHandlerChain()).thenReturn(hList);
        Exchange exchange1 = mock(Exchange.class);
        when(exchange1.get(HandlerChainInvoker.class)).thenReturn(invoker1);
        Message outMessage = new MessageImpl();
        outMessage.setExchange(exchange1);
        InterceptorChain chain = spy(InterceptorChain.class);
        outMessage.setInterceptorChain(chain);
        chain.abort();
        MessageObserver observer = spy(MessageObserver.class);
        when(exchange1.get(MessageObserver.class)).thenReturn(observer);
        observer.onMessage(isA(Message.class));

        LogicalHandlerInInterceptor li = new LogicalHandlerInInterceptor(binding1);
        li.handleMessage(outMessage);
        verify(chain, atLeastOnce()).abort();
        verify(observer, atLeastOnce()).onMessage(isA(Message.class));
    }
}