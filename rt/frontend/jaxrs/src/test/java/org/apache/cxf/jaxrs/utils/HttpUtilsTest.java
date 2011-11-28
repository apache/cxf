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

package org.apache.cxf.jaxrs.utils;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class HttpUtilsTest extends Assert {

    @Test
    public void testUrlDecode() {
        assertEquals("+ ", HttpUtils.urlDecode("%2B+"));
    }
    
    @Test
    public void testIsDateHeader() {
        assertFalse(HttpUtils.isDateRelatedHeader(HttpHeaders.ETAG));
        assertTrue(HttpUtils.isDateRelatedHeader(HttpHeaders.EXPIRES));
        assertTrue(HttpUtils.isDateRelatedHeader(HttpHeaders.IF_MODIFIED_SINCE));
        assertTrue(HttpUtils.isDateRelatedHeader(HttpHeaders.IF_UNMODIFIED_SINCE));
        assertTrue(HttpUtils.isDateRelatedHeader(HttpHeaders.DATE));
        assertTrue(HttpUtils.isDateRelatedHeader(HttpHeaders.LAST_MODIFIED));
    }
    
    @Test
    public void testUrlEncode() {
        assertEquals("%2B+", HttpUtils.urlEncode("+ "));
    }
     
    @Test
    public void testPathEncode() {
        assertEquals("+%20", HttpUtils.pathEncode("+ "));
    }
    
    @Test
    public void testURLEncode() {
        assertEquals("%2B+", HttpUtils.urlEncode("+ "));
    }
    
    @Test
    public void testUrlDecodeReserved() {
        assertEquals("!$&'()*,;=", HttpUtils.urlDecode("!$&'()*,;="));
    }
    
    @Test
    public void testPathDecode() {
        assertEquals("+++", HttpUtils.pathDecode("+%2B+"));
    }
    
    @Test
    public void testPathToMatch() {
        assertEquals("/", HttpUtils.getPathToMatch("/", "/", true));
        assertEquals("/", HttpUtils.getPathToMatch("/", "/bar", true));
        assertEquals("/", HttpUtils.getPathToMatch("/bar", "/bar/", true));
        assertEquals("/bar", HttpUtils.getPathToMatch("/bar", "/", true));
        
        assertEquals("/", HttpUtils.getPathToMatch("/bar", "/bar", true));
        assertEquals("/bar", HttpUtils.getPathToMatch("/baz/bar", "/baz", true));
        assertEquals("/baz/bar/foo/", HttpUtils.getPathToMatch("/baz/bar/foo/", "/bar", true));
        
    }
    
    @Test
    public void testUpdatePath() {
        
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        m.put(Message.ENDPOINT_ADDRESS, "http://localhost/");
        HttpUtils.updatePath(m, "/bar");
        assertEquals("/bar", m.get(Message.REQUEST_URI));
        HttpUtils.updatePath(m, "bar");
        assertEquals("/bar", m.get(Message.REQUEST_URI));
        HttpUtils.updatePath(m, "bar/");
        assertEquals("/bar/", m.get(Message.REQUEST_URI));
        m.put(Message.ENDPOINT_ADDRESS, "http://localhost");
        HttpUtils.updatePath(m, "bar/");
        assertEquals("/bar/", m.get(Message.REQUEST_URI));
    }
    
    
    @Test
    public void testParameterErrorStatus() {
        assertEquals(Response.Status.NOT_FOUND,
                     HttpUtils.getParameterFailureStatus(ParameterType.PATH));
        assertEquals(Response.Status.NOT_FOUND,
                     HttpUtils.getParameterFailureStatus(ParameterType.QUERY));
        assertEquals(Response.Status.NOT_FOUND,
                     HttpUtils.getParameterFailureStatus(ParameterType.MATRIX));
        assertEquals(Response.Status.BAD_REQUEST,
                     HttpUtils.getParameterFailureStatus(ParameterType.HEADER));
        assertEquals(Response.Status.BAD_REQUEST,
                     HttpUtils.getParameterFailureStatus(ParameterType.FORM));
        assertEquals(Response.Status.BAD_REQUEST,
                     HttpUtils.getParameterFailureStatus(ParameterType.COOKIE));
    }

    @Test
    public void testGetBaseAddressHttpUri() {
        doTestGetBaseAddress("http://localhost:8080/store?query", "/store");
    }
    
    @Test
    public void testGetBaseAddressHttpEncodedUri() {
        doTestGetBaseAddress("http://localhost:8080/store%20?query", "/store%20");
    }
    
    @Test
    public void testGetBaseAddressJmsUri() {
        doTestGetBaseAddress("jms://topic", "/");
    }
    
    @Test
    public void testGetBaseAddressWithoutScheme() {
        doTestGetBaseAddress("/s", "/s");
    }
    
    @Test
    public void testReplaceAnyIPAddress() {
        Message m = new MessageImpl();
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, req);
        req.getScheme();
        EasyMock.expectLastCall().andReturn("http");
        req.getServerName();
        EasyMock.expectLastCall().andReturn("localhost");
        req.getLocalPort();
        EasyMock.expectLastCall().andReturn(8080);
        EasyMock.replay(req);
        URI u = HttpUtils.toAbsoluteUri(URI.create("http://0.0.0.0/bar/foo"), m);
        assertEquals("http://localhost:8080/bar/foo", u.toString());
    }
    
    private void doTestGetBaseAddress(String baseURI, String expected) {
        Message m = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        m.setExchange(exchange);
        Destination dest = EasyMock.createMock(Destination.class);
        exchange.setDestination(dest);
        m.put(Message.BASE_PATH, baseURI);
        String address = HttpUtils.getBaseAddress(m);
        assertEquals(expected, address);
    }
}
