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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UriInfoImplTest extends Assert {
    
    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true);
    }
    
    @Test
    public void testGetAbsolutePath() {
        
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz/bar", 
                     u.getAbsolutePath().toString());
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/", "/bar"),
                                        null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz/bar", 
                     u.getAbsolutePath().toString());
        
    }
    
    @Test
    public void testGetAbsolutePathWithEncodedChars() {
        
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz%20foo", "/bar"),
                                        null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz%20foo/bar", 
                     u.getAbsolutePath().toString());
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/%20foo", "/bar%20foo"),
                                        null);
        assertEquals("Wrong absolute path", "http://localhost:8080/baz/%20foo/bar%20foo", 
                     u.getAbsolutePath().toString());
        
    }
    
    @Test
    public void testGetQueryParameters() {
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        null);
        assertEquals("unexpected queries", 0, u.getQueryParameters().size());
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar", "n=1%202"),
                            null);

        MultivaluedMap<String, String> qps = u.getQueryParameters(false);
        assertEquals("Number of queries is wrong", 1, qps.size());
        assertEquals("Wrong query value", qps.getFirst("n"), "1%202");
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar", 
                                        "N=0&n=1%202&n=3&b=2&a%2Eb=ab"),
                            null);

        qps = u.getQueryParameters();
        assertEquals("Number of queiries is wrong", 4, qps.size());
        assertEquals("Wrong query value", qps.get("N").get(0), "0");
        assertEquals("Wrong query value", qps.get("n").get(0), "1 2");
        assertEquals("Wrong query value", qps.get("n").get(1), "3");
        assertEquals("Wrong query value", qps.get("b").get(0), "2");
        assertEquals("Wrong query value", qps.get("a.b").get(0), "ab");
    }
    
    @Test
    public void testGetCaseinsensitiveQueryParameters() {
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        null);
        assertEquals("unexpected queries", 0, u.getQueryParameters().size());
        
        Message m = mockMessage("http://localhost:8080/baz", "/bar", 
                                "N=1%202&n=3&b=2&a%2Eb=ab");
        m.put("org.apache.cxf.http.case_insensitive_queries", "true");
                
        u = new UriInfoImpl(m, null);

        MultivaluedMap<String, String> qps = u.getQueryParameters();
        assertEquals("Number of queiries is wrong", 3, qps.size());
        assertEquals("Wrong query value", qps.get("n").get(0), "1 2");
        assertEquals("Wrong query value", qps.get("n").get(1), "3");
        assertEquals("Wrong query value", qps.get("b").get(0), "2");
        assertEquals("Wrong query value", qps.get("a.b").get(0), "ab");
    }
    
    @Test
    public void testGetRequestURI() {
        
        UriInfo u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/bar", "/foo", "n=1%202"),
                            null);

        assertEquals("Wrong request uri", "http://localhost:8080/baz/bar/foo?n=1%202",
                     u.getRequestUri().toString());
    }
    
    @Test
    public void testGetRequestURIWithEncodedChars() {
        
        UriInfo u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/bar", "/foo/%20bar", "n=1%202"),
                            null);

        assertEquals("Wrong request uri", "http://localhost:8080/baz/bar/foo/%20bar?n=1%202",
                     u.getRequestUri().toString());
    }
    
    @Test
    public void testGetTemplateParameters() {
       
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        new URITemplate("/bar").match("/baz", values);
        
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        values);
        assertEquals("unexpected templates", 0, u.getPathParameters().size());
        
        values.clear();
        new URITemplate("/{id}").match("/bar%201", values);
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar%201"),
                                        values);
        
        MultivaluedMap<String, String> tps = u.getPathParameters(false);
        assertEquals("Number of templates is wrong", 1, tps.size());
        assertEquals("Wrong template value", tps.getFirst("id"), "bar%201");
        
        values.clear();
        new URITemplate("/{id}/{baz}").match("/1%202/bar", values);
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/1%202/bar"),
                            values);

        tps = u.getPathParameters();
        assertEquals("Number of templates is wrong", 2, tps.size());
        assertEquals("Wrong template value", tps.getFirst("id"), "1 2");
        assertEquals("Wrong template value", tps.getFirst("baz"), "bar");
        
        // with suffix
        values.clear();
        new URITemplate("/bar").match("/bar", values);
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        values);
        assertEquals("unexpected templates", 0, u.getPathParameters().size());
    }
    
    @Test
    public void testGetBaseUri() {
        
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", null), null);
        assertEquals("Wrong base path", "http://localhost:8080/baz", 
                     u.getBaseUri().toString());
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/", null),
                                        null);
        assertEquals("Wrong base path", "http://localhost:8080/baz/", 
                     u.getBaseUri().toString());
    }
    
    @Test
    public void testGetPath() {
        
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/bar/baz", 
                                                    "/baz"),
                                        null);
        assertEquals("Wrong path", "baz", u.getPath());
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/bar/baz", 
                            "/bar/baz"), null);
        assertEquals("Wrong path", "/", u.getPath());
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/bar/baz/", 
                "/bar/baz/"), null);
        assertEquals("Wrong path", "/", u.getPath());
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/baz/bar%201"),
                                        null);
        assertEquals("Wrong path", "bar 1", u.getPath());
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/baz/bar%201"),
                            null);
        assertEquals("Wrong path", "bar%201", u.getPath(false));
      
        
    }
    
    private Message mockMessage(String baseAddress, String pathInfo) {
        return mockMessage(baseAddress, pathInfo, null, null);
    }
    
    private Message mockMessage(String baseAddress, String pathInfo, String query) {
        return mockMessage(baseAddress, pathInfo, query, null);
    }
    
    private Message mockMessage(String baseAddress, String pathInfo, 
                                String query, String fragment) {
        Message m = new MessageImpl();
        control.reset();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        ServletDestination d = control.createMock(ServletDestination.class);
        e.setDestination(d);
        EndpointInfo epr = new EndpointInfo(); 
        epr.setAddress(baseAddress);
        d.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(epr).anyTimes();
        m.put(Message.REQUEST_URI, pathInfo);
        m.put(Message.QUERY_STRING, query);
        control.replay();
        return m;
    }

}
