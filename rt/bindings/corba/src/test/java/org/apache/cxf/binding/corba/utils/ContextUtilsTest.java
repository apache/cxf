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


package org.apache.cxf.binding.corba.utils;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Holder for utility methods relating to contexts.
 */
public final class ContextUtilsTest {


    @Test
    public void testIsRequestor() throws Exception {
        Message message = new MessageImpl();
        message.put("org.apache.cxf.client", "org.apache.cxf.client");
        assertTrue(ContextUtils.isRequestor(message));
    }

    @Test
    public void testIsOutbound() throws Exception {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setOutMessage(message);
        message.setExchange(exchange);

        assertTrue(ContextUtils.isOutbound(message));
    }

}
