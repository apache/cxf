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
package org.apache.cxf.jaxrs.impl;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ContainerRequestContextImplTest {

    @Test
    public void testSetRequestUriReplacesQuery() {
        Message m = createMessage("/echo", "a=1&b=2");
        new ContainerRequestContextImpl(m, true, false).setRequestUri(URI.create("/echo?a=3"));

        MultivaluedMap<String, String> query = new UriInfoImpl(m).getQueryParameters();
        assertEquals(1, query.size());
        assertEquals("3", query.getFirst("a"));
    }

    @Test
    public void testSetRequestUriWithoutQueryRemovesQuery() {
        Message m = createMessage("/echo", "a=1&b=2");
        new ContainerRequestContextImpl(m, true, false).setRequestUri(URI.create("/echo"));

        assertNull(m.get(Message.QUERY_STRING));
        assertTrue(new UriInfoImpl(m).getQueryParameters().isEmpty());
    }

    @Test
    public void testSetRequestUriWithEmptyQueryRemovesQuery() {
        Message m = createMessage("/echo", "a=1&b=2");
        new ContainerRequestContextImpl(m, true, false).setRequestUri(URI.create("/echo?"));

        assertNull(m.get(Message.QUERY_STRING));
        assertTrue(new UriInfoImpl(m).getQueryParameters().isEmpty());
    }

    private static Message createMessage(String requestUri, String query) {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        m.put(Message.REQUEST_URI, requestUri);
        m.put(Message.QUERY_STRING, query);
        return m;
    }
}
