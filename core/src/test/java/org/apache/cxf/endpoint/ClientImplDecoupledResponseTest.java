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

package org.apache.cxf.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The response of a decoupled call and other messages for its exchange (an HTTP 202 acknowledgement,
 * a partial response) can arrive in any order: the response must survive the others.
 */
public class ClientImplDecoupledResponseTest {

    private static final List<Object> RESULT = List.of("Hello Paul!");

    private Bus bus;
    private ConduitSelector selector;
    private ClientImpl client;
    private Exchange exchange;
    private Message outMessage;

    @Before
    public void setUp() {
        bus = new ExtensionManagerBus();
        Binding binding = mock(Binding.class);
        when(binding.createMessage(any(Message.class))).thenAnswer(i -> i.getArgument(0));
        when(binding.getInInterceptors()).thenReturn(new ArrayList<>());
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getBinding()).thenReturn(binding);
        when(endpoint.getService()).thenReturn(mock(Service.class));
        when(endpoint.getInInterceptors()).thenReturn(new ArrayList<>());
        selector = mock(ConduitSelector.class);
        client = new ClientImpl(bus, endpoint, selector);

        exchange = new ExchangeImpl();
        exchange.put(Endpoint.class, endpoint);
        outMessage = new MessageImpl();
        outMessage.put(Message.INBOUND_MESSAGE, Boolean.FALSE);
        Map<String, Object> invocationContext = new HashMap<>();
        invocationContext.put(Client.RESPONSE_CONTEXT, new HashMap<String, Object>());
        outMessage.put(Message.INVOCATION_CONTEXT, invocationContext);
        exchange.setOutMessage(outMessage);
        outMessage.setExchange(exchange);
    }

    @After
    public void tearDown() {
        bus.shutdown(true);
    }

    @Test
    public void testPartialResponseAfterReply() throws Exception {
        Message reply = reply();
        client.onMessage(reply);
        client.onMessage(partialResponse());

        assertReplyKept(reply);
    }

    @Test
    public void testFaultPartialResponseAfterReply() throws Exception {
        Message reply = reply();
        client.onMessage(reply);
        Message partial = partialResponse();
        partial.setContent(Exception.class, new IllegalStateException("late"));
        client.onMessage(partial);

        assertReplyKept(reply);
    }

    @Test
    public void testAcknowledgementAfterReply() throws Exception {
        // with propagate202Response the transport passes the HTTP 202 on to ClientImpl
        Message reply = reply();
        client.onMessage(reply);
        client.onMessage(acknowledgement());

        assertReplyKept(reply);
    }

    @Test
    public void testAcknowledgementWhileAsyncReplyIsProcessed() throws Exception {
        ClientCallback callback = new ClientCallback();
        exchange.put(ClientCallback.class, callback);
        // the transport publishes an HTTP 202 for the exchange while the reply is being processed,
        // before the reply has finished it
        AtomicBoolean acknowledged = new AtomicBoolean();
        doAnswer(i -> {
            if (acknowledged.compareAndSet(false, true)) {
                exchange.setInMessage(acknowledgement());
            }
            return null;
        }).when(selector).complete(exchange);

        Message reply = reply();
        client.onMessage(reply);

        assertArrayEquals(RESULT.toArray(), callback.get(5, TimeUnit.SECONDS));
        assertReplyKept(reply);
    }

    @Test
    public void testPartialFaultWhileAsyncReplyIsProcessed() throws Exception {
        ClientCallback callback = new ClientCallback();
        exchange.put(ClientCallback.class, callback);
        AtomicBoolean replaced = new AtomicBoolean();
        doAnswer(i -> {
            if (replaced.compareAndSet(false, true)) {
                Message partial = partialResponse();
                partial.setContent(Exception.class, new IllegalStateException("late partial fault"));
                exchange.setInMessage(partial);
            }
            return null;
        }).when(selector).complete(exchange);
        client.onMessage(reply());
        assertArrayEquals(RESULT.toArray(), callback.get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testAsyncResponseFaultIsReported() throws Exception {
        ClientCallback callback = new ClientCallback();
        exchange.put(ClientCallback.class, callback);
        Message reply = reply();
        IllegalStateException failure = new IllegalStateException("response fault");
        // Install the exception during completion so the test exercises processResult's
        // failure selection after the normal inbound chain has run.
        doAnswer(i -> {
            reply.setContent(Exception.class, failure);
            return null;
        }).when(selector).complete(exchange);
        client.onMessage(reply);
        ExecutionException ex = assertThrows(ExecutionException.class,
            () -> callback.get(5, TimeUnit.SECONDS));
        assertSame(failure, ex.getCause());
    }

    private void assertReplyKept(Message reply) throws Exception {
        assertSame(reply, exchange.getInMessage());
        assertEquals(Boolean.TRUE, exchange.get(ClientImpl.FINISHED));
        // what a synchronous call returns once the reply has finished the exchange
        assertArrayEquals(RESULT.toArray(), client.processResult(outMessage, exchange, null, null));
    }

    private Message reply() {
        Message reply = inbound();
        reply.setContent(List.class, RESULT);
        return reply;
    }

    private Message partialResponse() {
        Message partial = inbound();
        partial.put(Message.PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
        return partial;
    }

    private Message acknowledgement() {
        Message ack = inbound();
        ack.put(Message.RESPONSE_CODE, 202);
        return ack;
    }

    private Message inbound() {
        Message m = new MessageImpl();
        m.setExchange(exchange);
        return m;
    }
}
