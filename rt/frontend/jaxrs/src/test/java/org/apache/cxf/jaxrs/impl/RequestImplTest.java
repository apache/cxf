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

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Variant;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class RequestImplTest {

    private Message m;
    private MultivaluedMap<String, String> metadata;

    @Before
    public void setUp() {
        m = new MessageImpl();
        metadata = new MetadataMap<>();
        m.put(Message.PROTOCOL_HEADERS, metadata);
        m.put(Message.HTTP_REQUEST_METHOD, "GET");
    }

    @After
    public void tearUp() {
        m = null;
        metadata.clear();
    }

    @Test
    public void testSingleMatchingVariant() {
        metadata.putSingle(HttpHeaders.CONTENT_TYPE, "application/xml");
        metadata.putSingle(HttpHeaders.CONTENT_LANGUAGE, "en");
        metadata.putSingle(HttpHeaders.CONTENT_ENCODING, "utf-8");

        assertSameVariant(MediaType.APPLICATION_XML_TYPE, new Locale("en"), StandardCharsets.UTF_8.name());
        assertSameVariant(null, new Locale("en"), StandardCharsets.UTF_8.name());
        assertSameVariant(MediaType.APPLICATION_XML_TYPE, null, StandardCharsets.UTF_8.name());
        assertSameVariant(MediaType.APPLICATION_XML_TYPE, new Locale("en"), null);

    }

    @Test
    public void testSingleMatchingVariantWithContentTypeOnly() {
        metadata.putSingle(HttpHeaders.CONTENT_TYPE, "application/xml");

        assertSameVariant(MediaType.APPLICATION_XML_TYPE, new Locale("en"), StandardCharsets.UTF_8.name());
        assertSameVariant(null, new Locale("en"), StandardCharsets.UTF_8.name());
        assertSameVariant(MediaType.APPLICATION_XML_TYPE, null, StandardCharsets.UTF_8.name());
        assertSameVariant(MediaType.APPLICATION_XML_TYPE, new Locale("en"), null);

    }

    @Test
    public void testSingleNonMatchingVariant() {
        metadata.putSingle(HttpHeaders.ACCEPT, "application/xml");
        metadata.putSingle(HttpHeaders.ACCEPT_LANGUAGE, "en");
        metadata.putSingle(HttpHeaders.ACCEPT_ENCODING, "utf-8");

        List<Variant> list = new ArrayList<>();
        list.add(new Variant(MediaType.APPLICATION_JSON_TYPE, new Locale("en"), "utf-8"));
        assertNull(new RequestImpl(m).selectVariant(list));

    }

    @Test
    public void testMultipleNonMatchingVariants() {
        metadata.putSingle(HttpHeaders.ACCEPT, "application/xml");
        metadata.putSingle(HttpHeaders.ACCEPT_LANGUAGE, "en");
        metadata.putSingle(HttpHeaders.ACCEPT_ENCODING, "utf-8");

        List<Variant> list = new ArrayList<>();
        list.add(new Variant(MediaType.APPLICATION_JSON_TYPE, new Locale("en"), "utf-8"));
        list.add(new Variant(MediaType.APPLICATION_XML_TYPE, new Locale("es"), "utf-8"));
        list.add(new Variant(MediaType.APPLICATION_XML_TYPE, new Locale("en"), "abc"));
        assertNull(new RequestImpl(m).selectVariant(list));

    }

    @Test
    public void testMultipleVariantsSingleMatch() {
        metadata.putSingle(HttpHeaders.ACCEPT, "application/xml");
        metadata.putSingle(HttpHeaders.ACCEPT_LANGUAGE, "en");
        metadata.putSingle(HttpHeaders.ACCEPT_ENCODING, "gzip");

        List<Variant> list = new ArrayList<>();
        list.add(new Variant(MediaType.APPLICATION_JSON_TYPE, new Locale("en"), "utf-8"));
        list.add(new Variant(MediaType.APPLICATION_XML_TYPE, new Locale("es"), "utf-8"));

        Variant var3 = new Variant(MediaType.APPLICATION_XML_TYPE, new Locale("en"), "gzip");
        list.add(var3);
        assertSame(var3, new RequestImpl(m).selectVariant(list));

    }

    @Test
    public void testMultipleVariantsBestMatch() {
        metadata.putSingle(HttpHeaders.ACCEPT, "application/xml");
        metadata.putSingle(HttpHeaders.ACCEPT_LANGUAGE, "en-us");
        metadata.putSingle(HttpHeaders.ACCEPT_ENCODING, "gzip;q=1.0, compress");

        List<Variant> list = new ArrayList<>();
        list.add(new Variant(MediaType.APPLICATION_JSON_TYPE, new Locale("en"), "gzip"));
        Variant var2 = new Variant(MediaType.APPLICATION_XML_TYPE, new Locale("en"), "gzip");
        list.add(var2);
        Variant var3 = new Variant(MediaType.APPLICATION_XML_TYPE, new Locale("en"), null);
        list.add(var3);
        assertSame(var2, new RequestImpl(m).selectVariant(list));
        list.clear();
        list.add(var3);
        assertSame(var3, new RequestImpl(m).selectVariant(list));
    }

    @Test
    public void testMultipleVariantsBestMatchMediaTypeQualityFactors() {
        metadata.putSingle(HttpHeaders.ACCEPT, "a/b;q=0.6, c/d;q=0.5, e/f+json");
        metadata.putSingle(HttpHeaders.ACCEPT_LANGUAGE, "en-us");
        metadata.putSingle(HttpHeaders.ACCEPT_ENCODING, "gzip;q=1.0, compress");

        List<Variant> list = new ArrayList<>();
        Variant var1 = new Variant(MediaType.valueOf("a/b"), new Locale("en"), "gzip");
        Variant var2 = new Variant(MediaType.valueOf("x/z"), new Locale("en"), "gzip");
        Variant var3 = new Variant(MediaType.valueOf("e/f+json"), new Locale("en"), "gzip");
        Variant var4 = new Variant(MediaType.valueOf("c/d"), new Locale("en"), "gzip");
        list.add(var1);
        list.add(var2);
        list.add(var3);
        list.add(var4);
        assertSame(var3, new RequestImpl(m).selectVariant(list));

        list.clear();
        list.add(var1);
        list.add(var4);
        assertSame(var1, new RequestImpl(m).selectVariant(list));

        list.clear();
        list.add(var2);
        list.add(var4);
        assertSame(var4, new RequestImpl(m).selectVariant(list));
    }

    private void assertSameVariant(MediaType mt, Locale lang, String enc) {
        Variant var = new Variant(mt, lang, enc);
        List<Variant> list = new ArrayList<>();
        list.add(var);
        assertSame(var, new RequestImpl(m).selectVariant(list));
    }

    @Test
    public void testWeakEtags() {
        metadata.putSingle("If-Match", new EntityTag("123", true).toString());

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertNotNull("Strict compararison is required", rb);
        Response r = rb.build();
        assertEquals("If-Match precondition was not met", 412, r.getStatus());
        assertEquals("Response should include ETag",
                     "\"123\"", r.getMetadata().getFirst("ETag").toString());
    }

    @Test
    public void testGetMethod() {
        assertEquals("Wrong method",
                   "GET", new RequestImpl(m).getMethod());

    }


    @Test
    public void testStrictEtagsPreconditionMet() {
        metadata.putSingle("If-Match", new EntityTag("123").toString());

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertNull("Precondition must be met", rb);
    }

    @Test
    public void testStrictEtagsPreconditionNotMet() {
        metadata.putSingle("If-Match", new EntityTag("123", true).toString());


        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertEquals("Precondition must not be met, strict comparison is required",
                     412, rb.build().getStatus());
    }

    @Test
    public void testStarEtags() {
        metadata.putSingle("If-Match", "*");

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertNull("Precondition must be met", rb);
    }

    @Test
    public void testStarEtagsIfNotMatch() {
        metadata.putSingle(HttpHeaders.IF_NONE_MATCH, "*");

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertEquals("Precondition must not be met",
                     304, rb.build().getStatus());
    }

    @Test
    public void testIfNotMatchAndLastModified() {
        metadata.putSingle(HttpHeaders.IF_NONE_MATCH, "1");

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(new Date(), new EntityTag("1"));
        assertEquals("Precondition must not be met",
                     304, rb.build().getStatus());
    }

    @Test
    public void testEtagsIfNotMatch() {
        metadata.putSingle(HttpHeaders.IF_NONE_MATCH, "\"123\"");

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertEquals("Precondition must not be met",
                     304, rb.build().getStatus());
    }

    @Test
    public void testStarEtagsIfNotMatchPut() {
        metadata.putSingle(HttpHeaders.IF_NONE_MATCH, "*");
        m.put(Message.HTTP_REQUEST_METHOD, "PUT");
        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertEquals("Precondition must not be met",
                     412, rb.build().getStatus());
    }

    @Test
    public void testBeforeDate() throws Exception {
        metadata.putSingle("If-Modified-Since", "Tue, 21 Oct 2008 14:00:00 GMT");
        Date serverDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
            .parse("Tue, 21 Oct 2008 17:00:00 GMT");

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(serverDate);
        assertNull("Precondition must be met", rb);
    }

    @Test
    public void testBeforeDateIfNotModified() throws Exception {
        metadata.putSingle(HttpHeaders.IF_UNMODIFIED_SINCE, "Mon, 20 Oct 2008 14:00:00 GMT");
        Date serverDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
            .parse("Tue, 21 Oct 2008 14:00:00 GMT");

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(serverDate);
        assertEquals("Precondition must not be met", 412, rb.build().getStatus());
    }

    @Test
    public void testAfterDate() throws Exception {
        metadata.putSingle("If-Modified-Since", "Tue, 21 Oct 2008 14:00:00 GMT");
        Date lastModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
            .parse("Mon, 20 Oct 2008 14:00:00 GMT");


        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(lastModified);
        assertNotNull("Precondition is not met", rb);

        Response r = rb.build();
        assertEquals("If-Modified-Since precondition was not met", 304, r.getStatus());
    }

    @Test
    public void testIfNoneMatchAndDateWithMatchingTags() throws Exception {
        metadata.putSingle(HttpHeaders.IF_NONE_MATCH, "\"123\"");
        metadata.putSingle("If-Modified-Since", "Tue, 21 Oct 2008 14:00:00 GMT");

        Date lastModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
            .parse("Mon, 22 Oct 2008 14:00:00 GMT");

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(lastModified, new EntityTag("\"123\""));
        assertNull("Precondition is not met", rb);
    }

    @Test
    public void testIfNoneMatchAndDateWithNonMatchingTags() throws Exception {
        metadata.putSingle(HttpHeaders.IF_NONE_MATCH, "\"123\"");
        metadata.putSingle("If-Modified-Since", "Tue, 20 Oct 2008 14:00:00 GMT");

        Date lastModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
            .parse("Mon, 21 Oct 2008 14:00:00 GMT");

        ResponseBuilder rb =
            new RequestImpl(m).evaluatePreconditions(lastModified, new EntityTag("124"));
        assertNull("Dates must not be checked if tags do not match", rb);
    }

}