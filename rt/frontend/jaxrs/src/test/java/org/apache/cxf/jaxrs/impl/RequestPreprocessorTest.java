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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class RequestPreprocessorTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true);
    }
    
    @Test
    public void testMethodQuery() {
        Message m = mockMessage("http://localhost:8080", "/bar", "_method=GET", "POST");
        RequestPreprocessor sqh = new RequestPreprocessor();
        sqh.preprocess(m, new UriInfoImpl(m, null));
        assertEquals("GET", m.get(Message.HTTP_REQUEST_METHOD));
    }
    
    @Test
    public void testMethodOverride() {
        Message m = mockMessage("http://localhost:8080", "/bar", "bar", "POST", "GET");
        RequestPreprocessor sqh = new RequestPreprocessor();
        sqh.preprocess(m, new UriInfoImpl(m, null));
        assertEquals("GET", m.get(Message.HTTP_REQUEST_METHOD));
    }
    
    @Test
    public void testTypeQuery() {
        Message m = mockMessage("http://localhost:8080", "/bar", "_type=xml", "POST");
        RequestPreprocessor sqh = new RequestPreprocessor();
        sqh.preprocess(m, new UriInfoImpl(m, null));
        assertEquals("POST", m.get(Message.HTTP_REQUEST_METHOD));
        assertEquals("application/xml", m.get(Message.ACCEPT_CONTENT_TYPE));
    }
    
    private Message mockMessage(String baseAddress, 
                                String pathInfo, 
                                String query,
                                String method) {
        return mockMessage(baseAddress, pathInfo, query, method, null);
    }
    
    private Message mockMessage(String baseAddress, 
                                String pathInfo, 
                                String query,
                                String method,
                                String methodHeader) {
        Message m = new MessageImpl();
        control.reset();
        Exchange e = control.createMock(Exchange.class);
        m.setExchange(e);
        ServletDestination d = control.createMock(ServletDestination.class);
        e.getDestination();
        EasyMock.expectLastCall().andReturn(d).anyTimes();
        EndpointInfo epr = new EndpointInfo(); 
        epr.setAddress(baseAddress);
        d.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(epr).anyTimes();
        m.put(Message.REQUEST_URI, pathInfo);
        m.put(Message.QUERY_STRING, query);
        m.put(Message.HTTP_REQUEST_METHOD, method);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        if (methodHeader != null) {
            headers.put("X-HTTP-Method-Override", Collections.singletonList(methodHeader));   
        }
        m.put(Message.PROTOCOL_HEADERS, headers);
        control.replay();
        return m;
    }
}
