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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
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
    public void testWadlQuery() {
        Message m = mockMessage("http://localhost:8080/bar", "/bar", "_wadl", "GET");
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(TestResource.class, TestResource.class, true, true);
        m.getExchange().put(Service.class, new JAXRSServiceImpl(Collections.singletonList(cri)));
        RequestPreprocessor sqh = new RequestPreprocessor();
        sqh.preprocess(m, new UriInfoImpl(m, null));
        Response r = m.getExchange().get(Response.class);
        assertNotNull(r);
        assertEquals(WadlGenerator.WADL_TYPE.toString(),
                     r.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE));
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
        control.reset();
        Endpoint endp = control.createMock(Endpoint.class);
        e.put(Endpoint.class, endp);
        endp.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(ProviderFactory.getInstance()).anyTimes();
        ServletDestination d = control.createMock(ServletDestination.class);
        e.setDestination(d);
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
    
    @Path("/test")
    private static class TestResource {
        @GET
        public String get() {
            return "test";
        }
    }
}
