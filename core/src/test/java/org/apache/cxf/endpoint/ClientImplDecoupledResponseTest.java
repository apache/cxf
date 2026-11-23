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
import org.apache.cxf.interceptor.InFaultChainInitiatorObserver;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    private Endpoint endpoint;
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
        endpoint = mock(Endpoint.class);
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

    @Test
    public void testAcknowledgementBeforeReply() throws Exception {
        // with propagate202Response the HTTP 202 usually arrives first, on the calling thread
        decoupled();
        client.onMessage(acknowledgement());
        assertNull("the acknowledgement must not finish the exchange", exchange.get(ClientImpl.FINISHED));

        Message reply = reply();
        client.onMessage(reply);
        assertReplyKept(reply);
    }

    @Test
    public void testAcknowledgementBeforeAsyncReply() throws Exception {
        decoupled();
        ClientCallback callback = new ClientCallback();
        exchange.put(ClientCallback.class, callback);
        client.onMessage(acknowledgement());
        assertFalse("the acknowledgement must not complete the callback", callback.isDone());

        client.onMessage(reply());
        assertArrayEquals(RESULT.toArray(), callback.get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testAcknowledgementWithoutDecoupledDestination() throws Exception {
        // without a decoupled endpoint, the acknowledgement is the response
        Message ack = acknowledgement();
        client.onMessage(ack);
        assertEquals(Boolean.TRUE, exchange.get(ClientImpl.FINISHED));
        assertSame(ack, exchange.getInMessage());
    }

    @Test
    public void testAcknowledgementOfAsyncOneWayCall() throws Exception {
        // a one-way call gets no reply on the decoupled endpoint: the acknowledgement completes it
        decoupled();
        exchange.setOneWay(true);
        ClientCallback callback = new ClientCallback();
        exchange.put(ClientCallback.class, callback);
        client.onMessage(acknowledgement());
        assertEquals(Boolean.TRUE, exchange.get(ClientImpl.FINISHED));
        assertNull(callback.get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testSoapFaultArrivingWhileAsyncFailureIsResolved() throws Exception {
        // a partial response with a SOAP fault becomes the exchange's in fault message after the reply
        // was found to be the exchange's in message, but before getException(Exchange) reads the exchange
        AtomicBoolean arrived = new AtomicBoolean();
        client = new ClientImpl(bus, endpoint, selector) {
            @Override
            protected Exception getException(Exchange e) {
                if (arrived.compareAndSet(false, true)) {
                    Message partial = partialResponse();
                    partial.setContent(Exception.class, new IllegalStateException("late partial fault"));
                    e.setInMessage(null);
                    e.setInFaultMessage(partial);
                }
                return super.getException(e);
            }
        };
        ClientCallback callback = new ClientCallback();
        exchange.put(ClientCallback.class, callback);
        client.onMessage(reply());
        assertArrayEquals(RESULT.toArray(), callback.get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testSoapFaultPartialResponseAfterReply() throws Exception {
        // as CheckFaultInterceptor does for a SOAP fault: the in fault chain makes the message the
        // in fault message of the exchange, and clears its in message
        IllegalStateException lateFault = new IllegalStateException("late partial fault");
        client.getInInterceptors().add(new AbstractPhaseInterceptor<Message>(Phase.POST_PROTOCOL) {
            public void handleMessage(Message message) {
                if (Boolean.TRUE.equals(message.get(Message.PARTIAL_RESPONSE_MESSAGE))) {
                    message.getInterceptorChain().abort();
                    message.setContent(Exception.class, lateFault);
                    new InFaultChainInitiatorObserver(bus).onMessage(message);
                }
            }
        });
        Message reply = reply();
        client.onMessage(reply);
        client.onMessage(partialResponse());

        // what a synchronous call returns once the reply has finished the exchange
        assertArrayEquals(RESULT.toArray(), client.processResult(outMessage, exchange, null, null));
    }

    @Test
    public void testOverriddenGetExceptionForAsyncCalls() throws Exception {
        IllegalStateException failure = new IllegalStateException("from the subclass");
        client = new ClientImpl(bus, endpoint, selector) {
            @Override
            protected Exception getException(Exchange e) {
                return failure;
            }
        };
        ClientCallback callback = new ClientCallback();
        exchange.put(ClientCallback.class, callback);
        client.onMessage(reply());
        ExecutionException ex = assertThrows(ExecutionException.class,
            () -> callback.get(5, TimeUnit.SECONDS));
        assertSame(failure, ex.getCause());
    }

    private void decoupled() {
        EndpointInfo info = new EndpointInfo();
        info.setProperty("org.apache.cxf.ws.addressing.MAPAggregator.decoupledDestination", new Object());
        when(endpoint.getEndpointInfo()).thenReturn(info);
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
