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

import jakarta.xml.ws.Binding;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.message.AbstractWrappedMessage;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractProtocolHandlerInterceptorTest {

    private Binding binding;
    private HandlerChainInvoker invoker;
    private IIOPMessage message;
    private Exchange exchange;

    @Before
    public void setUp() {
        invoker = mock(HandlerChainInvoker.class);
        message = mock(IIOPMessage.class);
        exchange = mock(Exchange.class);
        binding = mock(Binding.class);

        @SuppressWarnings("rawtypes")
        List<Handler> list = new ArrayList<>();
        list.add(null);
        when(binding.getHandlerChain()).thenReturn(list);
    }

    @Test
    public void testInterceptSuccess() {
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.get(HandlerChainInvoker.class)).thenReturn(invoker);
        when(invoker.invokeProtocolHandlers(eq(false),
                        isA(MessageContext.class))).thenReturn(true);
        when(exchange.getOutMessage()).thenReturn(message);
        IIOPHandlerInterceptor pi = new IIOPHandlerInterceptor(binding);
        assertEquals("unexpected phase", "user-protocol", pi.getPhase());
        pi.handleMessage(message);
    }

    @Test
    public void testInterceptFailure() {
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.get(HandlerChainInvoker.class)).thenReturn(invoker);
        when(exchange.getOutMessage()).thenReturn(message);
        when(
                invoker.invokeProtocolHandlers(eq(false),
                        isA(MessageContext.class))).thenReturn(false);
        IIOPHandlerInterceptor pi = new IIOPHandlerInterceptor(binding);
        pi.handleMessage(message);
    }

    class IIOPMessage extends AbstractWrappedMessage {
        IIOPMessage(Message m) {
            super(m);
        }
    }

    interface IIOPMessageContext extends MessageContext {

    }

    interface IIOPHandler<T extends IIOPMessageContext> extends Handler<IIOPMessageContext> {

    }

    class IIOPHandlerInterceptor extends AbstractProtocolHandlerInterceptor<IIOPMessage> {
        IIOPHandlerInterceptor(Binding binding) {
            super(binding);
        }
    }


}