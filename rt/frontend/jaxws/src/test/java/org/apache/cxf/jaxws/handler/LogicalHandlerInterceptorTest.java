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

import javax.xml.ws.Binding;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerInInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.MessageObserver;
import org.apache.handlers.types.AddNumbersResponse;
import org.easymock.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createNiceControl;

public class LogicalHandlerInterceptorTest extends Assert {

    private IMocksControl control;
    private Binding binding;
    private HandlerChainInvoker invoker;
    private Message message;
    private Exchange exchange;

    @Before
    public void setUp() {
        control = createNiceControl();
        binding = control.createMock(Binding.class);
        invoker = control.createMock(HandlerChainInvoker.class);
        message = control.createMock(Message.class);
        exchange = control.createMock(Exchange.class);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testInterceptSuccess() {
        List<LogicalHandler> list = new ArrayList<LogicalHandler>();
        list.add(new LogicalHandler() {
            public void close(MessageContext arg0) {
            }

            public boolean handleFault(MessageContext arg0) {
                return true;
            }

            public boolean handleMessage(MessageContext arg0) {
                return true;
            }
        });
        expect(invoker.getLogicalHandlers()).andReturn(list);
        expect(message.getExchange()).andReturn(exchange).anyTimes();
        expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes();
        expect(message.keySet()).andReturn(new TreeSet<String>()).anyTimes();
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker);
        expect(exchange.getOutMessage()).andReturn(message);
        expect(invoker.invokeLogicalHandlers(eq(true), isA(LogicalMessageContext.class)))
            .andReturn(true);

        control.replay();
        LogicalHandlerInInterceptor<Message> li = new LogicalHandlerInInterceptor<Message>(binding);
        assertEquals("unexpected phase", "pre-protocol", li.getPhase());
        li.handleMessage(message);
        control.verify();
    }

    //JAX-WS spec: If handler returns false, for a request-response MEP, if the message 
    //direction is reversed during processing of a request message then the message
    //becomes a response message.
    //NOTE: commented out as this has been covered by other tests.
    public void xtestReturnFalseClientSide() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
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

        IMocksControl control1 = createNiceControl();
        Binding binding1 = control1.createMock(Binding.class);
        Exchange exchange1 = control1.createMock(Exchange.class);
        expect(exchange1.get(HandlerChainInvoker.class)).andReturn(invoker1).anyTimes();
        Message outMessage = new MessageImpl();
        outMessage.setExchange(exchange1);
        InterceptorChain chain = control1.createMock(InterceptorChain.class);
        outMessage.setInterceptorChain(chain);
        chain.abort();
        EasyMock.expectLastCall();
        MessageObserver observer = control1.createMock(MessageObserver.class);
        expect(exchange1.get(MessageObserver.class)).andReturn(observer).anyTimes();
        observer.onMessage(isA(Message.class));
        EasyMock.expectLastCall();

        control1.replay();

        LogicalHandlerInInterceptor<Message> li = new LogicalHandlerInInterceptor<Message>(binding1);
        li.handleMessage(outMessage);
        control1.verify();
    }
}
