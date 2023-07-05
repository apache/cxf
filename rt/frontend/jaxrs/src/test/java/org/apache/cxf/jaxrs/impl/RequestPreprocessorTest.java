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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestPreprocessorTest {
    @Test
    public void testMethodQuery() {
        Message m = mockMessage("http://localhost:8080", "/bar", "_method=GET", "POST");
        RequestPreprocessor sqh = new RequestPreprocessor();

        // By default it should ignore _method
        sqh.preprocess(m, new UriInfoImpl(m, null));
        assertEquals("POST", m.get(Message.HTTP_REQUEST_METHOD));

        // Now allow HTTP method overriding
        m.put("org.apache.cxf.jaxrs.allow.http.method.override", true);
        sqh.preprocess(m, new UriInfoImpl(m, null));
        assertEquals("GET", m.get(Message.HTTP_REQUEST_METHOD));
    }

    @Test
    public void testMethodOverride() {
        Message m = mockMessage("http://localhost:8080", "/bar", "bar", "POST", "GET");
        RequestPreprocessor sqh = new RequestPreprocessor();

        // By default it should ignore the HTTP header
        sqh.preprocess(m, new UriInfoImpl(m, null));
        assertEquals("POST", m.get(Message.HTTP_REQUEST_METHOD));

        // Now allow HTTP method overriding
        m.put("org.apache.cxf.jaxrs.allow.http.method.override", true);
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
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        m.put("org.apache.cxf.endpoint.private", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);

        Endpoint endp = mock(Endpoint.class);
        e.put(Endpoint.class, endp);
        when(endp.isEmpty()).thenReturn(true);
        when(endp.get(ServerProviderFactory.class.getName())).thenReturn(ServerProviderFactory.getInstance());
        ServletDestination d = mock(ServletDestination.class);
        e.setDestination(d);
        EndpointInfo epr = new EndpointInfo();
        epr.setAddress(baseAddress);
        when(d.getEndpointInfo()).thenReturn(epr);
        when(endp.getEndpointInfo()).thenReturn(epr);
        m.put(Message.REQUEST_URI, pathInfo);
        m.put(Message.QUERY_STRING, query);
        m.put(Message.HTTP_REQUEST_METHOD, method);
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (methodHeader != null) {
            headers.put("X-HTTP-Method-Override", Collections.singletonList(methodHeader));
        }
        m.put(Message.PROTOCOL_HEADERS, headers);
        BindingInfo bi = mock(BindingInfo.class);
        epr.setBinding(bi);
        when(bi.getProperties()).thenReturn(Collections.emptyMap());

        return m;
    }

}