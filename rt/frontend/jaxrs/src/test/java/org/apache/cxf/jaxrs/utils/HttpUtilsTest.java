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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpUtilsTest {

    @Test
    public void testEncodePartiallyEncoded() {
        assertEquals("/address", HttpUtils.encodePartiallyEncoded("/address", false));
    }
    @Test
    public void testEncodePartiallyEncoded2() {
        assertEquals("/add%20ress", HttpUtils.encodePartiallyEncoded("/add ress", false));
    }
    @Test
    public void testEncodePartiallyEncoded3() {
        assertEquals("/add%20ress", HttpUtils.encodePartiallyEncoded("/add%20ress", false));
    }
    @Test
    public void testEncodePartiallyEncoded4() {
        assertEquals("http://localhost:8080/", 
                     HttpUtils.encodePartiallyEncoded("http://localhost:8080/", false));
    }
    @Test
    public void testEncodePartiallyEncoded5() {
        assertEquals("http://localhost:8080/1/%202", 
                     HttpUtils.encodePartiallyEncoded("http://localhost:8080/1/ 2", false));
    }
    
    @Test
    public void testUrlDecode() {
        assertEquals("+ ", HttpUtils.urlDecode("%2B+"));
    }

    @Test
    public void testCommaInQuery() {
        assertEquals("a+,b", HttpUtils.queryEncode("a ,b"));
    }

    @Test
    public void testRelativize() throws Exception {
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6226081
        URI a = new URI("file:/c:/abc/def/myDocument/doc.xml");
        URI b = new URI("file:/c:/abc/def/images/subdir/image.png");

        URI c = HttpUtils.relativize(a, b);

        assertEquals("../images/subdir/image.png", c.toString());
    }

    @Test
    public void testMediaTypeWithUTF8() {
        assertEquals("UTF-8",
                     HttpUtils.getEncoding(MediaType.valueOf("application/json;charset=UTF-8"), "UTF-16"));
    }
    @Test
    public void testMediaTypeWithUTF8WithQuotes() {
        assertEquals("UTF-8",
                     HttpUtils.getEncoding(MediaType.valueOf("application/json;charset=\"UTF-8\""), "UTF-16"));
    }
    @Test
    public void testMediaTypeWithNoCharset() {
        assertEquals("UTF-16",
                     HttpUtils.getEncoding(MediaType.valueOf("application/json"), "UTF-16"));
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
        // rfc3986.txt 3.3
        //segment-nz = 1*pchar
        //pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
        // sub-delims = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
        // unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"

        // '&' has to be represented as &amp; in WADL

        String pathChars = ":@!$&'()*+,;=-._~";
        String str = HttpUtils.pathEncode(pathChars);
        assertEquals(str, pathChars);
    }

    @Test
    public void testPathEncodeWithPlusAndSpace() {
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
    public void testGetBaseAddressHttpUriMixedCase() {
        final String baseURI = "HTTP://LoCALHoST:8080/STORE";
        
        Message m = new MessageImpl();
        HttpServletRequest req = mock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, req);
        Exchange exchange = new ExchangeImpl();
        
        when(req.getRequestURL()).thenReturn(new StringBuffer(baseURI));
        when(req.getPathInfo()).thenReturn("/STORE");
        when(req.getContextPath()).thenReturn("/");
        when(req.getServletPath()).thenReturn("/");

        m.setExchange(exchange);
        Destination dest = mock(Destination.class);
        exchange.setDestination(dest);
        m.put(Message.BASE_PATH, baseURI);
        String address = HttpUtils.getBaseAddress(m);
        assertEquals("/STORE", address);
    }

    @Test
    public void testReplaceAnyIPAddress() {
        Message m = new MessageImpl();
        HttpServletRequest req = mock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, req);
        when(req.getScheme()).thenReturn("http");
        when(req.getServerName()).thenReturn("localhost");
        when(req.getServerPort()).thenReturn(8080);
        URI u = HttpUtils.toAbsoluteUri(URI.create("http://0.0.0.0/bar/foo"), m);
        assertEquals("http://localhost:8080/bar/foo", u.toString());
    }

    @Test
    public void testReplaceAnyIPAddressWithPort() {
        doTestReplaceAnyIPAddressWithPort(true);
    }
    @Test
    public void testReplaceLHostIPAddressWithPort() {
        doTestReplaceAnyIPAddressWithPort(false);
    }

    private void doTestReplaceAnyIPAddressWithPort(boolean anyIp) {
        Message m = new MessageImpl();
        HttpServletRequest req = mock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, req);
        when(req.getScheme()).thenReturn("http");
        when(req.getServerName()).thenReturn("localhost");
        when(req.getServerPort()).thenReturn(8080);
        String host = anyIp ? "0.0.0.0" : "127.0.0.1";
        URI u = HttpUtils.toAbsoluteUri(URI.create("http://" + host + ":8080/bar/foo"), m);
        assertEquals("http://localhost:8080/bar/foo", u.toString());
    }

    @Test
    public void testReplaceLocalHostWithPort() {
        Message m = new MessageImpl();
        URI u = HttpUtils.toAbsoluteUri(URI.create("http://localhost:8080/bar/foo"), m);
        assertEquals("http://localhost:8080/bar/foo", u.toString());
    }

    private void doTestGetBaseAddress(String baseURI, String expected) {
        Message m = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        m.setExchange(exchange);
        Destination dest = mock(Destination.class);
        exchange.setDestination(dest);
        m.put(Message.BASE_PATH, baseURI);
        String address = HttpUtils.getBaseAddress(m);
        assertEquals(expected, address);
    }
}