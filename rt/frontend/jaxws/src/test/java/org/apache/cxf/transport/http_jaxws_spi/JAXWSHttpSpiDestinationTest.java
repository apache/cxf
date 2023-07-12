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
package org.apache.cxf.transport.http_jaxws_spi;

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.ws.spi.http.HttpContext;
import jakarta.xml.ws.spi.http.HttpExchange;
import org.apache.cxf.Bus;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.DestinationRegistryImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JAXWSHttpSpiDestinationTest {

    private static final String ADDRESS = "http://localhost:80/foo/bar";
    private static final String CONTEXT_PATH = "/foo";
    private Bus bus;
    private HttpContext context;
    private MessageObserver observer;
    private EndpointInfo endpoint;

    @Before
    public void setUp() {
        bus = mock(Bus.class);
        when(bus.getExtension(org.apache.cxf.policy.PolicyDataEngine.class)).thenReturn(null);
        observer = mock(MessageObserver.class);
        context = mock(HttpContext.class);
        endpoint = new EndpointInfo();
        endpoint.setAddress(ADDRESS);
    }

    @After
    public void tearDown() {
        context = null;
        bus = null;
        observer = null;
    }

    @Test
    public void testCtor() throws Exception {
        JAXWSHttpSpiDestination destination =
            new JAXWSHttpSpiDestination(bus, new DestinationRegistryImpl(), endpoint);

        assertNull(destination.getMessageObserver());
        assertNotNull(destination.getAddress());
        assertNotNull(destination.getAddress().getAddress());
        assertEquals(ADDRESS,
                     destination.getAddress().getAddress().getValue());
    }

    @Test
    public void testMessage() throws Exception {
        HttpExchange exchange = setUpExchange();

        JAXWSHttpSpiDestination destination =
            new JAXWSHttpSpiDestination(bus, new DestinationRegistryImpl(), endpoint);
        destination.setMessageObserver(observer);

        destination.doService(new HttpServletRequestAdapter(exchange),
                              new HttpServletResponseAdapter(exchange));
    }


    private HttpExchange setUpExchange() throws Exception {
        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getHttpContext()).thenReturn(context);
        when(exchange.getQueryString()).thenReturn(null);
        when(exchange.getPathInfo()).thenReturn(null);
        when(exchange.getRequestURI()).thenReturn(CONTEXT_PATH);
        when(exchange.getContextPath()).thenReturn(CONTEXT_PATH);
        Map<String, List<String>> reqHeaders = new HashMap<>();
        reqHeaders.put("Content-Type", Collections.singletonList("text/xml"));
        when(exchange.getRequestHeaders()).thenReturn(reqHeaders);
        OutputStream responseBody = mock(OutputStream.class);
        doNothing().when(responseBody).flush();
        when(exchange.getResponseBody()).thenReturn(responseBody);
        doNothing().when(observer).onMessage(isA(Message.class));

        return exchange;
    }


}