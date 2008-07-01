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
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
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
                                        "n=1%202&n=3&b=2"),
                            null);

        qps = u.getQueryParameters();
        assertEquals("Number of queiries is wrong", 2, qps.size());
        assertEquals("Wrong query value", qps.get("n").get(0), "1 2");
        assertEquals("Wrong query value", qps.get("n").get(1), "3");
        assertEquals("Wrong query value", qps.get("b").get(0), "2");
    }
    
    @Test
    public void testGetRequestURI() {
        
        UriInfo u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar", "n=1%202"),
                            null);

        assertEquals("Wrong request uri", "http://localhost:8080/baz/bar?n=1%202",
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
        new URITemplate("/bar", URITemplate.UNLIMITED_REGEX_SUFFIX).match("/bar", values);
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        values);
        assertEquals("unexpected templates", 0, u.getPathParameters().size());
    }
    
    @Test
    public void testGetBaseUri() {
        
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        null);
        assertEquals("Wrong base path", "http://localhost:8080/baz", 
                     u.getBaseUri().toString());
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz/", "/bar"),
                                        null);
        assertEquals("Wrong base path", "http://localhost:8080/baz/", 
                     u.getBaseUri().toString());
    }
    
    @Test
    public void testGetPath() {
        
        UriInfoImpl u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar"),
                                        null);
        assertEquals("Wrong path", "/bar", u.getPath());
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar%201"),
                                        null);
        assertEquals("Wrong path", "/bar 1", u.getPath());
        
        u = new UriInfoImpl(mockMessage("http://localhost:8080/baz", "/bar%201"),
                            null);
        assertEquals("Wrong path", "/bar%201", u.getPath(false));
    }
    
    private Message mockMessage(String baseAddress, String pathInfo) {
        return mockMessage(baseAddress, pathInfo, null, null);
    }
    
    private Message mockMessage(String baseAddress, String pathInfo, String query) {
        return mockMessage(baseAddress, pathInfo, query, null);
    }
    
    private Message mockMessage(String baseAddress, String pathInfo, 
                                String query, String fragment) {
        control.reset();
        Message m = control.createMock(Message.class);
        Exchange e = control.createMock(Exchange.class);
        m.getExchange();
        EasyMock.expectLastCall().andReturn(e);
        Destination d = control.createMock(Destination.class);
        e.getDestination();
        EasyMock.expectLastCall().andReturn(d);
        EndpointReferenceType epr = new EndpointReferenceType(); 
        epr.setAddress(new AttributedURIType());
        epr.getAddress().setValue(baseAddress);
        d.getAddress();
        EasyMock.expectLastCall().andReturn(epr);
        m.get(Message.PATH_INFO);
        EasyMock.expectLastCall().andReturn(pathInfo);
        m.get(Message.QUERY_STRING);
        EasyMock.expectLastCall().andReturn(query);
        control.replay();
        return m;
    }

}
