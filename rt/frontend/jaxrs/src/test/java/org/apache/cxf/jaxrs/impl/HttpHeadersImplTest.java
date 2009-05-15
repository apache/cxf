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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HttpHeadersImplTest extends Assert {
    
    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
    @Test
    public void testGetHeaders() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        EasyMock.expectLastCall().andReturn(createHeaders());
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        MultivaluedMap<String, String> hs = h.getRequestHeaders();
        List<String> acceptValues = hs.get("Accept");
        assertEquals(3, acceptValues.size());
        assertEquals("text/bar;q=0.6", acceptValues.get(0));
        assertEquals("text/*;q=1", acceptValues.get(1));
        assertEquals("application/xml", acceptValues.get(2));
        assertEquals(hs.getFirst("Content-Type"), "*/*");
    }
    
    @Test
    public void testMediaType() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        EasyMock.expectLastCall().andReturn(createHeaders());
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        assertEquals(MediaType.valueOf("*/*"), h.getMediaType());
    }
    
    @Test
    public void testGetMediaTypes() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        EasyMock.expectLastCall().andReturn(createHeaders());
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        List<MediaType> acceptValues = h.getAcceptableMediaTypes();
        assertEquals(3, acceptValues.size());
        assertEquals("text/*;q=1", acceptValues.get(0).toString());
        assertEquals("application/xml", acceptValues.get(1).toString());
        assertEquals("text/bar;q=0.6", acceptValues.get(2).toString());
    }
    
    @Test
    public void testGetHeader() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        EasyMock.expectLastCall().andReturn(createHeaders());
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> acceptValues = h.getRequestHeader("Accept");
        assertEquals(3, acceptValues.size());
        assertEquals("text/bar;q=0.6", acceptValues.get(0));
        assertEquals("text/*;q=1", acceptValues.get(1));
        assertEquals("application/xml", acceptValues.get(2));
        List<String> contentValues = h.getRequestHeader("Content-Type");
        assertEquals(1, contentValues.size());
        assertEquals("*/*", contentValues.get(0));
        
        List<String> dateValues = h.getRequestHeader("Date");
        assertEquals(1, dateValues.size());
        assertEquals("Tue, 21 Oct 2008 17:00:00 GMT", dateValues.get(0));
    }
    
    @Test
    public void testGetNullLanguage() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        EasyMock.expectLastCall().andReturn(createHeaders());
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        assertNull(h.getLanguage());
    }
    
    @Test
    public void testGetLanguage() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        MetadataMap<String, String> headers = createHeaders();
        headers.putSingle(HttpHeaders.CONTENT_LANGUAGE, "en-US");
        EasyMock.expectLastCall().andReturn(headers);
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        assertEquals("en_US", h.getLanguage().toString());
    }
    
    @Test
    public void testSingleAcceptableLanguages() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        MetadataMap<String, String> headers = createHeaders();
        headers.putSingle(HttpHeaders.ACCEPT_LANGUAGE, "en");
        EasyMock.expectLastCall().andReturn(headers);
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        List<Locale> languages = h.getAcceptableLanguages();
        assertEquals(1, languages.size());
        assertEquals(new Locale("en"), languages.get(0));
    }
    
    @Test
    public void testGetCookies() throws Exception {
        
        Message m = new MessageImpl();
        MetadataMap<String, String> headers = createHeaders();
        headers.putSingle(HttpHeaders.COOKIE, "a=b,c=d");
        m.put(Message.PROTOCOL_HEADERS, headers);
        HttpHeaders h = new HttpHeadersImpl(m);
        Map<String, Cookie> cookies = h.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("b", cookies.get("a").getValue());
        assertEquals("d", cookies.get("c").getValue());
    }
    
    @Test
    public void testMultipleAcceptableLanguages() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        MetadataMap<String, String> headers = 
            createHeader(HttpHeaders.ACCEPT_LANGUAGE, 
                         "en;q=0.7, en-gb;q=0.8, da");
        EasyMock.expectLastCall().andReturn(headers);
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        List<Locale> languages = h.getAcceptableLanguages();
        assertEquals(3, languages.size());
        assertEquals(new Locale("da"), languages.get(0));
        assertEquals(new Locale("en", "GB"), languages.get(1));
        assertEquals(new Locale("en"), languages.get(2));
    }
    
        
    private MetadataMap<String, String> createHeaders() {
        MetadataMap<String, String> hs = new MetadataMap<String, String>();
        hs.putSingle("Accept", "text/bar;q=0.6,text/*;q=1,application/xml");
        hs.putSingle("Content-Type", "*/*");
        hs.putSingle("Date", "Tue, 21 Oct 2008 17:00:00 GMT");
        return hs;
    }
    
    private MetadataMap<String, String> createHeader(String name, String... values) {
        MetadataMap<String, String> hs = new MetadataMap<String, String>();
        List<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(values));
        hs.put(name, list);
        return hs;
    }
    
    @Test
    public void testUnmodifiableRequestHeaders() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        MetadataMap<String, String> headers = 
            createHeader(HttpHeaders.ACCEPT_LANGUAGE, 
                         "en;q=0.7, en-gb;q=0.8, da");
        EasyMock.expectLastCall().andReturn(headers);
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        List<Locale> languages = h.getAcceptableLanguages();
        assertEquals(3, languages.size());
        languages.clear();
        languages = h.getAcceptableLanguages();
        assertEquals(3, languages.size());
        
        MultivaluedMap<String, String> rHeaders  = h.getRequestHeaders();
        List<String> acceptL = rHeaders.get(HttpHeaders.ACCEPT_LANGUAGE);
        assertEquals(3, acceptL.size());
        try {
            rHeaders.clear();
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }
}
