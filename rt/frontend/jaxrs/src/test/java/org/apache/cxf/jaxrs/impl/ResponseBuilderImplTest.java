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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Variant;
import org.apache.cxf.jaxrs.utils.HttpUtils;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


public class ResponseBuilderImplTest {

    @Test
    public void testStatusSet() throws Exception {
        assertEquals(200, Response.ok().build().getStatus());
        assertEquals(200, new ResponseBuilderImpl().status(200).build().getStatus());
    }

    @Test
    public void testStatusNotSetNoEntity() throws Exception {
        assertEquals(204, new ResponseBuilderImpl().build().getStatus());
    }

    @Test
    public void testStatusNotSetEntitySet() throws Exception {
        assertEquals(200, new ResponseBuilderImpl().entity("").build().getStatus());
    }

    @Test
    public void testAllow() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Allow", "HEAD");
        m.add("Allow", "GET");
        checkBuild(Response.ok().allow("HEAD").allow("GET").build(), 200, null, m);
    }

    @Test
    public void testEncoding() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Content-Encoding", "gzip");
        checkBuild(Response.ok().encoding("gzip").build(), 200, null, m);
    }

    @Test
    public void testEntity() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<>();
        checkBuild(Response.ok().entity("Hello").build(), 200, "Hello", m);
    }

    @Test
    public void testEntityAnnotations() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<>();
        Annotation[] annotations = new Annotation[1];
        Annotation produces = new Produces() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Produces.class;
            }
            @Override
            public String[] value() {
                return new String[] {
                    "text/turtle"
                };
            }
        };
        annotations[0] = produces;
        Response response = Response.ok().entity("<> a <#test>", annotations).build();
        checkBuild(response, 200, "<> a <#test>", m);
        assertArrayEquals(annotations, ((ResponseImpl)response).getEntityAnnotations());
    }

    @Test
    public void testReplaceAll() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Content-Type", "text/plain");
        checkBuild(Response.ok().type("image/png").tag("removeme").replaceAll(m).build(), 200, null, m);

    }

    @Test
    public void testAllowReset() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Allow", "POST");
        checkBuild(Response.ok().allow("HEAD").allow("GET").allow().allow("POST").build(), 200, null, m);
    }

    @Test
    public void testAllowSet() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Allow", "HEAD");
        m.add("Allow", "GET");
        // LinkedHashSet so we get a predictable order
        Set<String> methods = new LinkedHashSet<>();
        methods.add("HEAD");
        methods.add("GET");
        checkBuild(Response.ok().allow(methods).build(), 200, null, m);
    }

    @Test
    public void testAllowReSet() throws Exception {
        Response r = Response.ok().allow("GET").allow((Set<String>)null).build();
        assertNull(r.getMetadata().getFirst("Allow"));
    }

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
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("Location", URI.create("http://localhost/rest"));
        checkBuild(Response.ok().location(URI.create("http://localhost/rest")).build(), 200, null, m);
    }

    @Test
    public void testLanguage() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("Content-Language", "de");
        checkBuild(Response.ok().language("de").build(), 200, null, m);
    }

    @Test
    public void testLanguageReplace() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("Content-Language", "en");
        checkBuild(Response.ok().language("de").language((Locale)null)
                   .language("en").build(), 200, null, m);
    }

    @Test
    public void testLinkStr() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("Link", Link.valueOf("<http://example.com/page3>;rel=\"next\""));
        checkBuild(Response.ok().link("http://example.com/page3", "next").build(), 200, null, m);
    }

    @Test
    public void testLinkStrMultiple() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Link", Link.valueOf("<http://example.com/page1>;rel=\"previous\""));
        m.add("Link", Link.valueOf("<http://example.com/page3>;rel=\"next\""));
        checkBuild(Response.ok().link("http://example.com/page1", "previous")
                       .link("http://example.com/page3", "next").build(), 200, null, m);
    }

    @Test
    public void testLinkStrMultipleSameRel() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Link", Link.valueOf("<http://example.com/page2.pdf>;rel=\"alternate\""));
        m.add("Link", Link.valueOf("<http://example.com/page2.txt>;rel=\"alternate\""));
        checkBuild(Response.ok().link("http://example.com/page2.pdf", "alternate")
                       .link("http://example.com/page2.txt", "alternate").build(), 200, null, m);
    }

    @Test
    public void testLinkURI() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        URI uri = URI.create("http://example.com/page3");
        m.putSingle("Link", Link.valueOf("<http://example.com/page3>;rel=\"next\""));
        checkBuild(Response.ok().link(uri, "next").build(), 200, null, m);
    }

    @Test
    public void testLinks() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Link", Link.valueOf("<http://example.com/page1>;rel=\"previous\""));
        m.add("Link", Link.valueOf("<http://example.com/page3>;rel=\"next\""));
        RuntimeDelegateImpl delegate = new RuntimeDelegateImpl();
        Link.Builder linkBuilder = delegate.createLinkBuilder();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        // Reset linkbuilder
        linkBuilder = delegate.createLinkBuilder();
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").build();
        checkBuild(Response.ok().links(prevLink, nextLink).build(), 200, null, m);
    }

    @Test
    public void testLinks2() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Link", Link.valueOf("<http://example.com/page1>;rel=\"previous\""));
        m.add("Link", Link.valueOf("<http://example.com/page3>;rel=\"next\""));
        RuntimeDelegateImpl delegate = new RuntimeDelegateImpl();
        Link.Builder linkBuilder = delegate.createLinkBuilder();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        // Reset linkbuilder
        linkBuilder = delegate.createLinkBuilder();
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").build();
        Link[] links = new Link[2];
        links[0] = prevLink;
        links[1] = nextLink;
        checkBuild(Response.ok().links(links).build(), 200, null, m);
    }

    @Test
    public void testLinksNoReset() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Link", Link.valueOf("<http://example.com/page1>;rel=\"previous\""));
        m.add("Link", Link.valueOf("<http://example.com/page3>;rel=\"next\""));
        RuntimeDelegateImpl delegate = new RuntimeDelegateImpl();
        Link.Builder linkBuilder = delegate.createLinkBuilder();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        linkBuilder = delegate.createLinkBuilder();
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").build();
        checkBuild(Response.ok().links(prevLink).links(nextLink).build(), 200, null, m);
    }

    @Test
    public void testLinksWithReset() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Link", Link.valueOf("<http://example.com/page3>;rel=\"next\""));
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
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("Content-Language", "en");
        checkBuild(Response.ok().header(HttpHeaders.CONTENT_LANGUAGE, "de")
                                .header(HttpHeaders.CONTENT_LANGUAGE, null)
                                .header(HttpHeaders.CONTENT_LANGUAGE, "en").build(),
                  200, null, m);
    }

    @Test
    public void testAddCookie() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Set-Cookie", new NewCookie("a", "b"));
        m.add("Set-Cookie", new NewCookie("c", "d"));
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
        SimpleDateFormat format = HttpUtils.getHttpDateFormat();
        Date date = format.parse("Tue, 21 Oct 2008 17:00:00 GMT");

        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("Expires", date);
        checkBuild(Response.ok()
                   .expires(format.parse("Tue, 21 Oct 2008 17:00:00 GMT"))
                   .build(), 200, null, m);
        checkBuild(Response.ok()
                   .header(HttpHeaders.EXPIRES,
                           format.parse("Tue, 21 Oct 2008 17:00:00 GMT"))
                   .build(), 200, null, m);
    }

    @Test
    public void testOkBuild() {

        checkBuild(Response.ok().build(),
                          200, null, new MetadataMap<String, Object>());

    }

    @Test
    public void testVariant() throws Exception {

        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("Content-Type", MediaType.TEXT_XML_TYPE);
        m.putSingle("Content-Language", new Locale("en"));
        m.putSingle("Content-Encoding", "gzip");
        Variant v = new Variant(MediaType.TEXT_XML_TYPE, new Locale("en"), "gzip");

        checkBuild(Response.ok().variant(v).build(),
                   200, null, m);
    }

    @Test
    public void testVariant2() throws Exception {
        List<String> encoding = Arrays.asList("gzip", "compress");
        MediaType mt = MediaType.APPLICATION_JSON_TYPE;
        ResponseBuilder rb = Response.ok();
        rb = rb.variants(getVariantList(encoding, mt).toArray(new Variant[0]));
        Response response = rb.build();
        List<Object> enc = response.getHeaders().get(HttpHeaders.CONTENT_ENCODING);
        assertTrue(encoding.containsAll(enc));
        List<Object> ct = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        assertTrue(ct.contains(mt));
    }

    protected static List<Variant> getVariantList(List<String> encoding,
                                                  MediaType... mt) {
        return Variant.VariantListBuilder.newInstance()
            .mediaTypes(mt)
            .languages(new Locale("en", "US"), new Locale("en", "GB"), new Locale("zh", "CN"))
            .encodings(encoding.toArray(new String[]{}))
            .add()
            .build();
    }

    @Test
    public void testCreatedNoEntity() throws Exception {

        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("Location", URI.create("http://foo"));

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
    public void testVariantsArray() throws Exception {

        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Content-Type", MediaType.APPLICATION_JSON_TYPE);
        m.add("Content-Language", new Locale("en_uk"));
        m.add("Content-Language", new Locale("en_gb"));
        m.add("Vary", "Accept");
        m.add("Vary", "Accept-Language");

        Variant json = new Variant(MediaType.APPLICATION_JSON_TYPE, new Locale("en_uk"), null);
        Variant xml = new Variant(MediaType.APPLICATION_JSON_TYPE, new Locale("en_gb"), null);

        checkBuild(Response.ok().variants(json, xml).build(), 200, null, m);
    }

    @Test
    public void testVariantsList() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Content-Type", MediaType.TEXT_XML_TYPE);
        m.add("Content-Language", new Locale("en", "UK"));
        m.add("Content-Language", new Locale("en", "GB"));
        m.add("Content-Encoding", "compress");
        m.add("Content-Encoding", "gzip");
        m.add("Vary", "Accept");
        m.add("Vary", "Accept-Language");
        m.add("Vary", "Accept-Encoding");

        List<Variant> vts = Variant.VariantListBuilder.newInstance()
            .mediaTypes(MediaType.TEXT_XML_TYPE).
            languages(new Locale("en", "UK"), new Locale("en", "GB")).encodings("compress", "gzip").
                      add().build();

        checkBuild(Response.ok().variants(vts).build(),
                   200, null, m);
    }

}