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

package org.apache.cxf.transport.http_jetty.continuations;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class JettyContinuationProviderTest extends Assert {
    
    @Test
    public void testGetContinuation() {
        HttpServletRequest httpRequest = EasyMock.createMock(HttpServletRequest.class);
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        JettyContinuationProvider provider = new JettyContinuationProvider(httpRequest, m);
        JettyContinuationWrapper c = (JettyContinuationWrapper)provider.getContinuation();
        assertSame(m, c.getMessage());
        assertTrue(c.getContinuation().isNew());
    }
    
    @Test
    public void testNoContinuationForOneWay() {
        Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(true);
        Message m = new MessageImpl();
        m.setExchange(exchange);
        JettyContinuationProvider provider = new JettyContinuationProvider(null, m);
        assertNull(provider.getContinuation());
    }
    
}
