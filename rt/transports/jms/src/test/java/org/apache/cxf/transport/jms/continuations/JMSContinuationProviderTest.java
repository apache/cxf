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

package org.apache.cxf.transport.jms.continuations;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.Test;


public class JMSContinuationProviderTest extends Assert {

    @Test
    public void testNoContinuationForOneWay() {
        Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(true);
        Message m = new MessageImpl();
        m.setExchange(exchange);
        JMSContinuationProvider provider = 
            new JMSContinuationProvider(null, m, null, null, null, null);
        assertNull(provider.getContinuation());
    }
    
    @Test
    public void testGetNewContinuation() {
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        JMSContinuationProvider provider = 
            new JMSContinuationProvider(null, m, null, null, null, null);
        Continuation cw = provider.getContinuation(); 
        assertTrue(cw.isNew());
        assertSame(cw, m.get(JMSContinuation.class));
    }
    
    @Test
    public void testGetExistingContinuation() {
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        JMSContinuation cw = new JMSContinuation(null, m, null, null, null, null);
        m.put(JMSContinuation.class, cw);
        JMSContinuationProvider provider = new JMSContinuationProvider(null, m, null, null, null, null);
        assertSame(cw, provider.getContinuation());
        assertSame(cw, m.get(JMSContinuation.class));
    }
}
