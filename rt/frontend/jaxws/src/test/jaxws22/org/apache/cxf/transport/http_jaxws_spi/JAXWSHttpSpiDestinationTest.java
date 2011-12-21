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

import javax.xml.ws.spi.http.HttpContext;
import javax.xml.ws.spi.http.HttpExchange;

import org.apache.cxf.Bus;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

public class JAXWSHttpSpiDestinationTest extends Assert {
    
    private static final String ADDRESS = "http://localhost:80/foo/bar";
    private static final String CONTEXT_PATH = "/foo";
    private IMocksControl control; 
    private Bus bus;
    private HttpContext context;
    private MessageObserver observer;
    private EndpointInfo endpoint;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        observer = control.createMock(MessageObserver.class);
        context = control.createMock(HttpContext.class);
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
        expect(bus.getExtension(QueryHandlerRegistry.class)).andReturn(null);
        HttpExchange exchange = setUpExchange();
        control.replay();

        JAXWSHttpSpiDestination destination = 
            new JAXWSHttpSpiDestination(bus, new DestinationRegistryImpl(), endpoint);
        destination.setMessageObserver(observer);

        destination.doService(new HttpServletRequestAdapter(exchange),
                              new HttpServletResponseAdapter(exchange));

        control.verify();
    }
    

    private HttpExchange setUpExchange() throws Exception {
        HttpExchange exchange = control.createMock(HttpExchange.class);
        expect(exchange.getHttpContext()).andReturn(context).anyTimes();
        expect(exchange.getQueryString()).andReturn(null);
        expect(exchange.getPathInfo()).andReturn(null);
        expect(exchange.getRequestURI()).andReturn(CONTEXT_PATH);
        expect(exchange.getContextPath()).andReturn(CONTEXT_PATH);
        Map<String, List<String>> reqHeaders = new HashMap<String, List<String>>();
        reqHeaders.put("Content-Type", Collections.singletonList("text/xml"));
        expect(exchange.getRequestHeaders()).andReturn(reqHeaders).anyTimes();
        OutputStream responseBody = control.createMock(OutputStream.class);
        responseBody.flush();
        EasyMock.expectLastCall();
        expect(exchange.getResponseBody()).andReturn(responseBody).anyTimes();
        observer.onMessage(isA(Message.class));
        EasyMock.expectLastCall();
        
        return exchange;
    }
    
    
}
