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

import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.message.AbstractWrappedMessage;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createNiceControl;

public class AbstractProtocolHandlerInterceptorTest extends Assert {
    
    private IMocksControl control;
    private Binding binding;
    private HandlerChainInvoker invoker;
    private IIOPMessage message;
    private Exchange exchange;
    
    @Before
    public void setUp() {
        control = createNiceControl();
        invoker = control.createMock(HandlerChainInvoker.class);
        message = control.createMock(IIOPMessage.class);
        exchange = control.createMock(Exchange.class);
        binding = control.createMock(Binding.class);
        
        List<Handler> list = new ArrayList<Handler>();
        list.add(null);
        expect(binding.getHandlerChain()).andReturn(list).anyTimes();
    }
    
    @After
    public void tearDown() {
        control.verify();
    }

    @Test
    public void testInterceptSuccess() {
        expect(message.getExchange()).andReturn(exchange).anyTimes();
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        expect(invoker.invokeProtocolHandlers(eq(false),
                        isA(MessageContext.class))).andReturn(true);
        expect(exchange.getOutMessage()).andReturn(message);
        control.replay();
        IIOPHandlerInterceptor pi = new IIOPHandlerInterceptor(binding);
        assertEquals("unexpected phase", "user-protocol", pi.getPhase());
        pi.handleMessage(message);
    }

    @Test
    public void testInterceptFailure() {
        expect(message.getExchange()).andReturn(exchange).anyTimes();
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        expect(exchange.getOutMessage()).andReturn(message);
        expect(
                invoker.invokeProtocolHandlers(eq(false),
                        isA(MessageContext.class))).andReturn(false);
        control.replay();
        IIOPHandlerInterceptor pi = new IIOPHandlerInterceptor(binding);
        pi.handleMessage(message);
    }

    class IIOPMessage extends AbstractWrappedMessage {
        public IIOPMessage(Message m) {
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
