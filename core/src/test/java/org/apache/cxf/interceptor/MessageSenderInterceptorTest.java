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


package org.apache.cxf.interceptor;

import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.PreexistingConduitSelector;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class MessageSenderInterceptorTest {

    private Exchange exchange;
    private Message inMessage;
    private Message outMessage;
    private PhaseInterceptorChain chain;

    @Before
    public void setUp() {
        exchange = new ExchangeImpl();
        inMessage = new MessageImpl();
        outMessage = new MessageImpl();
        exchange.setInMessage(inMessage);
        exchange.setOutMessage(outMessage);
        chain = mock(PhaseInterceptorChain.class);
        outMessage.setInterceptorChain(chain);
    }

    @Test
    public void testConduitIsPrepared() throws Exception {
        Conduit conduit = mock(Conduit.class);
        exchange.put(ConduitSelector.class, new PreexistingConduitSelector(conduit));

        new MessageSenderInterceptor().handleMessage(outMessage);

        verify(conduit).prepare(outMessage);
        verify(chain).add(any(
            MessageSenderInterceptor.MessageSenderEndingInterceptor.class));
    }

    @Test
    public void testNoConduitOrDestination() {
        assertNoConduitFault();
    }

    @Test
    public void testNullBackChannel() throws Exception {
        Destination destination = mock(Destination.class);
        when(destination.getBackChannel(inMessage)).thenReturn(null);
        exchange.setDestination(destination);

        assertNoConduitFault();
        verify(destination).getBackChannel(inMessage);
    }

    private void assertNoConduitFault() {
        try {
            new MessageSenderInterceptor().handleMessage(outMessage);
            fail("Expected a Fault when no conduit is available");
        } catch (Fault f) {
            assertFalse(f.getCause() instanceof NullPointerException);
            assertTrue(f.getMessage().contains("no conduit"));
            verifyNoInteractions(chain);
        }
    }
}
