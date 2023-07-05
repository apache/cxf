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
package org.apache.cxf.jaxrs.lifecycle;

import jakarta.ws.rs.core.Application;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.Customer;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PerRequestResourceProviderTest {

    @Test
    public void testGetInstance() {
        PerRequestResourceProvider rp = new PerRequestResourceProvider(Customer.class);
        Message message = createMessage();
        message.put(Message.QUERY_STRING, "a=aValue");
        Customer c = (Customer)rp.getInstance(message);
        assertNotNull(c.getUriInfo());
        assertEquals("aValue", c.getQueryParam());
        assertTrue(c.isPostConstuctCalled());
        rp.releaseInstance(message, c);
        assertTrue(c.isPreDestroyCalled());
    }

    private Message createMessage() {
        ProviderFactory factory = ServerProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(null);
        when(endpoint.get(Application.class.getName())).thenReturn(null);
        when(endpoint.size()).thenReturn(0);
        when(endpoint.isEmpty()).thenReturn(true);
        when(endpoint.get(ServerProviderFactory.class.getName())).thenReturn(factory);
        e.put(Endpoint.class, endpoint);
        return m;
    }
}

