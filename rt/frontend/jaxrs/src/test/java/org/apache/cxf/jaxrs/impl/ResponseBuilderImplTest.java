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

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.junit.Assert;
import org.junit.Test;


public class ResponseBuilderImplTest extends Assert {

    @Test
    public void testValidStatus() {
        assertEquals(100, Response.status(100).build().getStatus());
        assertEquals(101, Response.status(101).build().getStatus());
        assertEquals(200, Response.status(200).build().getStatus());
        assertEquals(599, Response.status(599).build().getStatus());
        assertEquals(598, Response.status(598).build().getStatus());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalsStatus1() {
        Response.status(99).build();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalsStatus2() {
        Response.status(600).build();
    }
     
    @Test
    public void testAbsoluteLocation() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Location", "http://localhost/rest");
        checkBuild(Response.ok().location(URI.create("http://localhost/rest")).build(), 200, null, m);
    }
    
    @Test
    public void testLanguage() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Content-Language", "de");
        checkBuild(Response.ok().language("de").build(), 200, null, m);
    }
    
    @Test
    public void testLanguageReplace() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Content-Language", "en");
        checkBuild(Response.ok().language("de").language((Locale)null)
                   .language("en").build(), 200, null, m);
    }
    
    @Test
    public void testLinkStr() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Link", "<http://example.com/page3>;rel=\"next\"");
        checkBuild(Response.ok().link("http://example.com/page3", "next").build(), 200, null, m);
    }

    @Test
    public void testLinkStrMultiple() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Link", "<http://example.com/page1>;rel=\"previous\"");
        m.add("Link", "<http://example.com/page3>;rel=\"next\"");
        checkBuild(Response.ok().link("http://example.com/page1", "previous")
                       .link("http://example.com/page3", "next").build(), 200, null, m);
    }
    
    @Test
    public void testLinkStrMultipleSameRel() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Link", "<http://example.com/page2.pdf>;rel=\"alternate\"");
        m.add("Link", "<http://example.com/page2.txt>;rel=\"alternate\"");
        checkBuild(Response.ok().link("http://example.com/page2.pdf", "alternate")
                       .link("http://example.com/page2.txt", "alternate").build(), 200, null, m);
    }
    
    @Test
    public void testLinkURI() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        URI uri = URI.create("http://example.com/page3");
        m.putSingle("Link", "<http://example.com/page3>;rel=\"next\"");
        checkBuild(Response.ok().link(uri, "next").build(), 200, null, m);
    }

    @Test
    public void testLinks() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Link", "<http://example.com/page1>;rel=\"previous\"");
        m.add("Link", "<http://example.com/page3>;rel=\"next\"");
        RuntimeDelegateImpl delegate = new RuntimeDelegateImpl();
        Link.Builder linkBuilder = delegate.createLinkBuilder();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        // Reset linkbuilder
        linkBuilder = delegate.createLinkBuilder();
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").build();
        checkBuild(Response.ok().links(prevLink, nextLink).build(), 200, null, m);
    }

    @Test
    public void testLinksNoReset() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Link", "<http://example.com/page1>;rel=\"previous\"");
        m.add("Link", "<http://example.com/page3>;rel=\"next\"");
        RuntimeDelegateImpl delegate = new RuntimeDelegateImpl();
        Link.Builder linkBuilder = delegate.createLinkBuilder();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        linkBuilder = delegate.createLinkBuilder();
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").build();
        checkBuild(Response.ok().links(prevLink).links(nextLink).build(), 200, null, m);
    }

    @Test
    public void testLinksWithReset() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Link", "<http://example.com/page3>;rel=\"next\"");
        RuntimeDelegateImpl delegate = new RuntimeDelegateImpl();
        Link.Builder linkBuilder = delegate.createLinkBuilder();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        linkBuilder = delegate.createLinkBuilder();
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").build();
        // CHECK: Should .links() do a reset? Undocumented feature; so we'll
        // test with the awkward <code>(Link[])null</code> instead..
        // Note: .cookie() has same behavior.
        checkBuild(Response.ok().links(prevLink).links((Link[])null).links(nextLink).build(), 200, null, m);
    }
    
    @Test
    public void testAddHeader() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Content-Language", "en");
        checkBuild(Response.ok().header(HttpHeaders.CONTENT_LANGUAGE, "de")
                                .header(HttpHeaders.CONTENT_LANGUAGE, null)
                                .header(HttpHeaders.CONTENT_LANGUAGE, "en").build(), 
                  200, null, m);
    }
    
    @Test
    public void testAddCookie() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Set-Cookie", "a=b;Version=1");
        m.add("Set-Cookie", "c=d;Version=1");
        checkBuild(Response.ok().cookie(new NewCookie("a", "b"))
                                .cookie(new NewCookie("c", "d")).build(), 
                  200, null, m);
    }
    
    @Test
    public void testTagString() {
        Response r = Response.ok().tag("foo").build();
        String eTag = r.getMetadata().getFirst("ETag").toString();
        assertEquals("\"foo\"", eTag);
    }
    
    @Test
    public void testTagStringWithQuotes() {
        Response r = Response.ok().tag("\"foo\"").build();
        String eTag = r.getMetadata().getFirst("ETag").toString();
        assertEquals("\"foo\"", eTag);
    }
    
    @Test
    public void testEntityTag() {
        Response r = Response.ok().tag(new EntityTag("foo")).build();
        String eTag = r.getMetadata().getFirst("ETag").toString();
        assertEquals("\"foo\"", eTag);
    }
    
    @Test
    public void testEntityTag2() {
        Response r = Response.ok().tag(new EntityTag("\"foo\"")).build();
        String eTag = r.getMetadata().getFirst("ETag").toString();
        assertEquals("\"foo\"", eTag);
    }
    
    @Test
    public void testExpires() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Expires", "Tue, 21 Oct 2008 17:00:00 GMT");
        SimpleDateFormat format = HttpUtils.getHttpDateFormat();
        Date date = format.parse("Tue, 21 Oct 2008 17:00:00 GMT");
        checkBuild(Response.ok().expires(date).build(), 200, null, m);
        checkBuild(Response.ok().expires(date)
                   .header(HttpHeaders.EXPIRES, date).build(), 200, null, m);
        checkBuild(Response.ok().header(HttpHeaders.EXPIRES, date).build(), 200, null, m);
    }
    
    @Test
    public void testOkBuild() {
      
        checkBuild(Response.ok().build(),
                          200, null, new MetadataMap<String, Object>());
        
    }
    
    @Test
    public void testVariant() throws Exception {
        
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Content-Type", "text/xml");
        m.putSingle("Content-Language", "en");
        m.putSingle("Content-Encoding", "gzip");
        Variant v = new Variant(MediaType.TEXT_XML_TYPE, new Locale("en"), "gzip");
        
        checkBuild(Response.ok().variant(v).build(),
                   200, null, m);
    }
    

    @Test
    public void testCreatedNoEntity() throws Exception {
        
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Location", "http://foo");
        
        checkBuild(Response.created(new URI("http://foo")).build(),
                   201, null, m);
        
        
    }
    
    
    private void checkBuild(Response r, int status, Object entity, 
                            MetadataMap<String, Object> meta) {
        ResponseImpl ri = (ResponseImpl)r;
        assertEquals("Wrong status", status, ri.getStatus());
        assertSame("Wrong entity", entity, ri.getEntity());
        assertEquals("Wrong meta", meta, ri.getMetadata());
    }
    
    @Test
    public void testVariants() throws Exception {
        
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Accept", "text/xml");
        m.add("Accept", "application/xml");
        m.add("Accept-Language", "en_UK");
        m.add("Accept-Language", "en_GB");
        m.add("Accept-Encoding", "compress");
        m.add("Accept-Encoding", "gzip");
        m.add("Vary", "Accept");
        m.add("Vary", "Accept-Language");
        m.add("Vary", "Accept-Encoding");
        List<Variant> vts = Variant.VariantListBuilder.newInstance()
            .mediaTypes(MediaType.TEXT_XML_TYPE, MediaType.APPLICATION_XML_TYPE).
            languages(new Locale("en", "UK"), new Locale("en", "GB")).encodings("compress", "gzip").
                      add().build();

        checkBuild(Response.ok().variants(vts).build(),
                   200, null, m);
    }
    
    
}
