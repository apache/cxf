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

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;


public class ResponseImplTest extends Assert {
    
    @Test
    public void testReadBufferedStaxUtils() throws Exception {
        ResponseImpl r = new ResponseImpl(200);
        Source responseSource = readResponseSource(r);
        Document doc = StaxUtils.read(responseSource);
        assertEquals("Response", doc.getDocumentElement().getLocalName());
    }
    
    @Test
    public void testReadBufferedStaxSource() throws Exception {
        ResponseImpl r = new ResponseImpl(200);
        Source responseSource = readResponseSource(r);
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        DOMResult res = new DOMResult();
        trans.transform(responseSource, res);
        Document doc = (Document)res.getNode();
        assertEquals("Response", doc.getDocumentElement().getLocalName());
    }
    
    private Source readResponseSource(ResponseImpl r) {
        String content = "<Response " 
            + " xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\"" 
            + " xmlns:ns2=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\">"
            + "<Result><Decision>Permit</Decision><Status><StatusCode" 
            + " Value=\"urn:oasis:names:tc:xacml:1.0:status:ok\"/></Status></Result></Response>";
        
        
        MultivaluedMap<String, Object> headers = new MetadataMap<String, Object>();
        headers.putSingle("Content-Type", "text/xml");
        r.addMetadata(headers);
        r.setEntity(new ByteArrayInputStream(content.getBytes()), null);
        r.setMessage(createMessage());
        r.bufferEntity();
        return r.readEntity(Source.class);
    }
    
    private Message createMessage() {
        ProviderFactory factory = ProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        e.setOutMessage(new MessageImpl());
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        endpoint.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        endpoint.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        endpoint.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory).anyTimes();
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        return m;
    }
    
    @Test
    public void testResourceImpl() {
        String entity = "bar";
        ResponseImpl ri = new ResponseImpl(200, entity);
        assertEquals("Wrong status", ri.getStatus(), 200);
        assertSame("Wrong entity", entity, ri.getEntity());
        
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        ri.addMetadata(meta);
        ri.getMetadata();
        assertSame("Wrong metadata", meta, ri.getMetadata());
        assertSame("Wrong metadata", meta, ri.getHeaders());
    }
    
    @Test
    public void testHasEntity() {
        assertTrue(new ResponseImpl(200, "").hasEntity());
        assertFalse(new ResponseImpl(200).hasEntity());
    }
    
    @Test
    public void testStatuInfoForOKStatus() {
        StatusType si = new ResponseImpl(200, "").getStatusInfo();
        assertNotNull(si);
        assertEquals(200, si.getStatusCode());
        assertEquals(Status.Family.SUCCESSFUL, si.getFamily());
        assertEquals("OK", si.getReasonPhrase());
    }
    
    @Test
    public void testStatuInfoForClientErrorStatus() {
        StatusType si = new ResponseImpl(400, "").getStatusInfo();
        assertNotNull(si);
        assertEquals(400, si.getStatusCode());
        assertEquals(Status.Family.CLIENT_ERROR, si.getFamily());
        assertEquals("Bad Request", si.getReasonPhrase());
    }
    
    @Test
    public void testStatuInfoForClientErrorStatus2() {
        StatusType si = new ResponseImpl(499, "").getStatusInfo();
        assertNotNull(si);
        assertEquals(499, si.getStatusCode());
        assertEquals(Status.Family.CLIENT_ERROR, si.getFamily());
        assertEquals("", si.getReasonPhrase());
    }
    
    @Test
    public void testHasEntityAfterClose() {
        Response r = new ResponseImpl(200, new ByteArrayInputStream("data".getBytes())); 
        assertTrue(r.hasEntity());
        r.close();
        assertFalse(r.hasEntity());
    }
    
    
    @Test
    public void testBufferEntityNoEntity() {
        Response r = new ResponseImpl(200); 
        assertFalse(r.bufferEntity());
    }
    
    @Test
    public void testGetHeaderString() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        ri.addMetadata(meta);
        assertNull(ri.getHeaderString("a"));
        meta.putSingle("a", "aValue");
        assertEquals("aValue", ri.getHeaderString("a"));
        meta.add("a", "aValue2");
        assertEquals("aValue,aValue2", ri.getHeaderString("a"));
    }
    
    @Test
    public void testGetHeaderStrings() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        meta.add("Set-Cookie", NewCookie.valueOf("a=b"));
        ri.addMetadata(meta);
        MultivaluedMap<String, String> headers = ri.getStringHeaders();
        assertEquals(1, headers.size());
        assertEquals("a=b;Version=1", headers.getFirst("Set-Cookie"));
    }
    
    @Test
    public void testGetCookies() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        meta.add("Set-Cookie", NewCookie.valueOf("a=b"));
        meta.add("Set-Cookie", NewCookie.valueOf("c=d"));
        ri.addMetadata(meta);
        Map<String, NewCookie> cookies = ri.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("a=b;Version=1", cookies.get("a").toString());
        assertEquals("c=d;Version=1", cookies.get("c").toString());
    }
    
    @Test
    public void testGetContentLength() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        ri.addMetadata(meta);
        assertEquals(-1, ri.getLength());
        meta.add("Content-Length", "10");
        assertEquals(10, ri.getLength());
    }
    
    @Test
    public void testGetDate() {
        doTestDate(HttpHeaders.DATE);
    }
    
    @Test
    public void testLastModified() {
        doTestDate(HttpHeaders.LAST_MODIFIED);
    }
    
    public void doTestDate(String dateHeader) {
        boolean date = HttpHeaders.DATE.equals(dateHeader);
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        meta.add(dateHeader, "Tue, 21 Oct 2008 17:00:00 GMT");
        ri.addMetadata(meta);
        assertEquals(HttpUtils.getHttpDate("Tue, 21 Oct 2008 17:00:00 GMT"), 
                     date ? ri.getDate() : ri.getLastModified());
    }
    
    @Test
    public void testEntityTag() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        meta.add(HttpHeaders.ETAG, "1234");
        ri.addMetadata(meta);
        assertEquals("\"1234\"", ri.getEntityTag().toString());
    }
    
    @Test
    public void testLocation() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        meta.add(HttpHeaders.LOCATION, "http://localhost:8080");
        ri.addMetadata(meta);
        assertEquals("http://localhost:8080", ri.getLocation().toString());
    }
    
    @Test
    public void testGetLanguage() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        meta.add(HttpHeaders.CONTENT_LANGUAGE, "en-US");
        ri.addMetadata(meta);
        assertEquals("en_US", ri.getLanguage().toString());
    }

    @Test
    public void testGetMediaType() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        meta.add(HttpHeaders.CONTENT_TYPE, "text/xml");
        ri.addMetadata(meta);
        assertEquals("text/xml", ri.getMediaType().toString());
    }
    
    @Test
    public void testGetLinks() {
        ResponseImpl ri = new ResponseImpl(200);
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        ri.addMetadata(meta);
        assertFalse(ri.hasLink("next"));
        assertNull(ri.getLink("next"));
        assertFalse(ri.hasLink("prev"));
        assertNull(ri.getLink("prev"));
        
        meta.add(HttpHeaders.LINK, "<http://next>;rel=next");
        meta.add(HttpHeaders.LINK, "<http://prev>;rel=prev");
        
        assertTrue(ri.hasLink("next"));
        Link next = ri.getLink("next");
        assertNotNull(next);
        assertTrue(ri.hasLink("prev"));
        Link prev = ri.getLink("prev");
        assertNotNull(prev);
        
        Set<Link> links = ri.getLinks();
        assertTrue(links.contains(next));
        assertTrue(links.contains(prev));
        
        assertEquals("http://next", next.getUri().toString());
        assertEquals("next", next.getRel());
        assertEquals("http://prev", prev.getUri().toString());
        assertEquals("prev", prev.getRel());
    }
}
