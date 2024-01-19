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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HttpHeadersImplTest {

    @Test
    public void testNoRequestHeader() throws Exception {

        Message m = createMessage(createHeader("COMPLEX_HEADER",  "b=c; param=c, a=b;param=b"));

        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> values = h.getRequestHeader("HEADER");
        assertNull(values);
    }

    @Test
    public void testGetHeaderNameValue() throws Exception {

        Message m = createMessage(createHeader("COMPLEX_HEADER",  "b=c; param=c, a=b;param=b"));
        m.put(HttpHeadersImpl.HEADER_SPLIT_PROPERTY, "true");

        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> values = h.getRequestHeader("COMPLEX_HEADER");
        assertNotNull(values);
        assertEquals(2, values.size());
        assertEquals("b=c; param=c", values.get(0));
        assertEquals("a=b;param=b", values.get(1));
    }

    @Test
    public void testGetHeaderWithQuotes1() throws Exception {

        MetadataMap<String, String> headers = createHeader("COMPLEX_HEADER",
            "a1=\"a\", a2=\"a\";param, b, b;param, c1=\"c, d, e\", "
            + "c2=\"c, d, e\";param, a=b, a=b;p=p1, a2=\"a\";param=p,"
            + "a3=\"a\";param=\"p,b\"");
        Message m = createMessage(headers);
        m.put(HttpHeadersImpl.HEADER_SPLIT_PROPERTY, "true");

        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> values = h.getRequestHeader("COMPLEX_HEADER");
        assertNotNull(values);
        assertEquals(10, values.size());
        assertEquals("a1=\"a\"", values.get(0));
        assertEquals("a2=\"a\";param", values.get(1));
        assertEquals("b", values.get(2));
        assertEquals("b;param", values.get(3));
        assertEquals("c1=\"c, d, e\"", values.get(4));
        assertEquals("c2=\"c, d, e\";param", values.get(5));
        assertEquals("a=b", values.get(6));
        assertEquals("a=b;p=p1", values.get(7));
        assertEquals("a2=\"a\";param=p", values.get(8));
        assertEquals("a3=\"a\";param=\"p,b\"", values.get(9));
    }

    @Test
    public void testGetHeaderWithQuotes2() throws Exception {

        MetadataMap<String, String> headers =
            createHeader("X-WSSE", "UsernameToken Username=\"Foo\", Nonce=\"bar\"");
        Message m = createMessage(headers);
        m.put(HttpHeadersImpl.HEADER_SPLIT_PROPERTY, "true");

        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> values = h.getRequestHeader("X-WSSE");
        assertNotNull(values);
        assertEquals(3, values.size());
        assertEquals("UsernameToken", values.get(0));
        assertEquals("Username=\"Foo\"", values.get(1));
        assertEquals("Nonce=\"bar\"", values.get(2));
    }

    @Test
    public void testGetHeaderWithQuotes3() throws Exception {

        MetadataMap<String, String> headers =
            createHeader("COMPLEX_HEADER", "\"value with space\"");
        Message m = createMessage(headers);
        m.put(HttpHeadersImpl.HEADER_SPLIT_PROPERTY, "true");

        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> values = h.getRequestHeader("COMPLEX_HEADER");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("value with space", values.get(0));

    }


    @Test
    public void testGetHeaders() throws Exception {

        Message m = createMessage(createHeaders());
        m.put(HttpHeadersImpl.HEADER_SPLIT_PROPERTY, "true");

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

        Message m = createMessage(createHeaders());
        HttpHeaders h = new HttpHeadersImpl(m);
        assertEquals(MediaType.valueOf("*/*"), h.getMediaType());
    }

    @Test
    public void testGetMissingContentLength() throws Exception {

        Message m = createMessage(new MetadataMap<String, String>());
        HttpHeaders h = new HttpHeadersImpl(m);
        assertEquals(-1, h.getLength());
    }

    @Test
    public void testGetContentLength() throws Exception {

        Message m = createMessage(createHeaders());
        HttpHeaders h = new HttpHeadersImpl(m);
        assertEquals(10, h.getLength());
    }

    @Test
    public void testGetContentTypeLowCase() throws Exception {

        Message m = new MessageImpl();
        // this is what happens at runtime and is tested in the system tests
        Map<String, List<String>> headers =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("content-type", Collections.singletonList("text/plain"));
        m.put(Message.PROTOCOL_HEADERS, headers);
        HttpHeaders h = new HttpHeadersImpl(m);
        assertEquals("text/plain", h.getRequestHeaders().getFirst("Content-Type"));
    }

    @Test
    public void testGetEmptyHeader() throws Exception {

        Message m = new MessageImpl();
        // this is what happens at runtime and is tested in the system tests
        Map<String, List<String>> headers =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("A", Collections.<String>emptyList());
        m.put(Message.PROTOCOL_HEADERS, headers);
        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> values = h.getRequestHeader("A");
        assertTrue(values.isEmpty());
    }

    @Test
    public void testGetNullHeaderValue() throws Exception {

        Message m = new MessageImpl();
        // this is what happens at runtime and is tested in the system tests
        Map<String, List<String>> headers =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("A", Collections.<String>singletonList(null));
        m.put(Message.PROTOCOL_HEADERS, headers);
        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> values = h.getRequestHeader("A");
        assertThat(values, is(nullValue()));
    }

    @Test
    public void testGetNullHeader() throws Exception {

        Message m = new MessageImpl();
        // this is what happens at runtime and is tested in the system tests
        Map<String, List<String>> headers =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("A", null);
        m.put(Message.PROTOCOL_HEADERS, headers);
        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> values = h.getRequestHeader("A");
        assertThat(values, is(nullValue()));
    }

    @Test
    public void testGetDate() throws Exception {

        Message m = createMessage(createHeaders());
        HttpHeaders h = new HttpHeadersImpl(m);

        List<String> dateValues = h.getRequestHeader("Date");
        assertEquals(1, dateValues.size());
        assertEquals("Tue, 21 Oct 2008 17:00:00 GMT", dateValues.get(0));

        Date d = h.getDate();

        String theDateValue = HttpUtils.getHttpDateFormat().format(d);
        assertEquals(theDateValue, "Tue, 21 Oct 2008 17:00:00 GMT");
    }

    @Test
    public void testGetHeaderString() throws Exception {

        Message m = createMessage(createHeaders());
        HttpHeaders h = new HttpHeadersImpl(m);

        String date = h.getHeaderString("Date");
        assertEquals("Tue, 21 Oct 2008 17:00:00 GMT", date);
    }

    @Test
    public void testGetHeaderString2() throws Exception {

        Message m = createMessage(createHeaders());
        HttpHeaders h = new HttpHeadersImpl(m);

        String date = h.getHeaderString("a");
        assertEquals("1,2", date);
    }

    @Test
    public void testGetHeader2() throws Exception {

        Message m = createMessage(createHeaders());
        HttpHeaders h = new HttpHeadersImpl(m);

        List<String> values = h.getRequestHeader("a");
        assertEquals(2, values.size());
        assertEquals("1", values.get(0));
        assertEquals("2", values.get(1));
    }

    @Test
    public void testGetMediaTypes() throws Exception {

        Message m = createMessage(createHeaders());
        HttpHeaders h = new HttpHeadersImpl(m);
        List<MediaType> acceptValues = h.getAcceptableMediaTypes();
        assertEquals(3, acceptValues.size());
        assertEquals("text/*;q=1", acceptValues.get(0).toString());
        assertEquals("application/xml", acceptValues.get(1).toString());
        assertEquals("text/bar;q=0.6", acceptValues.get(2).toString());
    }

    @Test
    public void testGetNoMediaTypes() throws Exception {

        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, Collections.emptyMap());
        HttpHeaders h = new HttpHeadersImpl(m);
        List<MediaType> acceptValues = h.getAcceptableMediaTypes();
        assertEquals(1, acceptValues.size());
        assertEquals("*/*", acceptValues.get(0).toString());
    }

    @Test
    public void testGetNoLanguages() throws Exception {

        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, Collections.emptyMap());
        HttpHeaders h = new HttpHeadersImpl(m);
        List<Locale> locales = h.getAcceptableLanguages();
        assertEquals(1, locales.size());
        assertEquals("*", locales.get(0).toString());
    }

    @Test
    public void testGetHeader() throws Exception {

        Message m = createMessage(createHeaders());
        m.put(HttpHeadersImpl.HEADER_SPLIT_PROPERTY, "true");
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

        Message m = createMessage(createHeaders());
        HttpHeaders h = new HttpHeadersImpl(m);
        assertNull(h.getLanguage());
    }

    @Test
    public void testGetLanguage() throws Exception {

        Message m = createMessage(createHeader(HttpHeaders.CONTENT_LANGUAGE, "en-US"));
        HttpHeaders h = new HttpHeadersImpl(m);
        assertEquals("en_US", h.getLanguage().toString());
    }

    @Test
    public void testSingleAcceptableLanguages() throws Exception {

        Message m = createMessage(createHeader(HttpHeaders.ACCEPT_LANGUAGE, "en"));
        HttpHeaders h = new HttpHeadersImpl(m);
        List<Locale> languages = h.getAcceptableLanguages();
        assertEquals(1, languages.size());
        assertEquals(new Locale("en"), languages.get(0));
    }

    @Test
    public void testGetCookies() throws Exception {

        Message m = createMessage(createHeader(HttpHeaders.COOKIE, "a=$b;c=d"));
        HttpHeaders h = new HttpHeadersImpl(m);
        Map<String, Cookie> cookies = h.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("$b", cookies.get("a").getValue());
        assertEquals("d", cookies.get("c").getValue());
    }

    @Test
    public void testGetCookieWithAttributes() throws Exception {

        Message m = createMessage(createHeader(HttpHeaders.COOKIE, "$Version=1;a=b"));
        HttpHeaders h = new HttpHeadersImpl(m);
        Map<String, Cookie> cookies = h.getCookies();
        assertEquals(1, cookies.size());
        Cookie cookie = cookies.get("a");
        assertEquals("b", cookie.getValue());
        assertEquals(1, cookie.getVersion());
    }

    @Test
    public void testGetCookiesWithAttributes() throws Exception {

        Message m = createMessage(createHeader(HttpHeaders.COOKIE, "$Version=1;a=b, $Version=1;c=d"));
        HttpHeaders h = new HttpHeadersImpl(m);
        Map<String, Cookie> cookies = h.getCookies();
        assertEquals(2, cookies.size());
        Cookie cookieA = cookies.get("a");
        assertEquals("b", cookieA.getValue());
        assertEquals(1, cookieA.getVersion());
        Cookie cookieC = cookies.get("c");
        assertEquals("d", cookieC.getValue());
        assertEquals(1, cookieA.getVersion());
    }


    @Test
    public void testGetCookiesWithComma() throws Exception {

        Message m = createMessage(createHeader(HttpHeaders.COOKIE, "a=b,c=d"));
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(m);
        ex.put(HttpHeadersImpl.COOKIE_SEPARATOR_PROPERTY, ",");
        m.setExchange(ex);
        HttpHeaders h = new HttpHeadersImpl(m);
        Map<String, Cookie> cookies = h.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("b", cookies.get("a").getValue());
        assertEquals("d", cookies.get("c").getValue());
    }

    @Test
    public void testGetCookiesWithCRLF() throws Exception {

        Message m = createMessage(createHeader(HttpHeaders.COOKIE, "a=b\r\nc=d"));
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(m);
        ex.put(HttpHeadersImpl.COOKIE_SEPARATOR_PROPERTY, "crlf");
        m.setExchange(ex);
        HttpHeaders h = new HttpHeadersImpl(m);
        Map<String, Cookie> cookies = h.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("b", cookies.get("a").getValue());
        assertEquals("d", cookies.get("c").getValue());
    }

    @Test(expected = InternalServerErrorException.class)
    public void testInvalidCookieSeparator() throws Exception {

        Message m = createMessage(createHeader(HttpHeaders.COOKIE, "a=b,c=d"));
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(m);
        ex.put(HttpHeadersImpl.COOKIE_SEPARATOR_PROPERTY, "(e+)+");
        m.setExchange(ex);
        HttpHeaders h = new HttpHeadersImpl(m);
        h.getCookies();
    }

    @Test
    public void testMultipleAcceptableLanguages() throws Exception {

        MetadataMap<String, String> headers =
            createHeader(HttpHeaders.ACCEPT_LANGUAGE,
                         "en;q=0.7, en-gb;q=0.8, da, zh-Hans-SG;q=0.9");
        Message m = createMessage(headers);
        HttpHeaders h = new HttpHeadersImpl(m);
        List<Locale> languages = h.getAcceptableLanguages();
        assertEquals(4, languages.size());
        assertEquals(new Locale("da"), languages.get(0));
        assertEquals(new Locale("zh", "Hans-SG"), languages.get(1));
        assertEquals(new Locale("en", "GB"), languages.get(2));
        assertEquals(new Locale("en"), languages.get(3));
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableRequestHeaders() throws Exception {

        MetadataMap<String, String> headers =
            createHeader(HttpHeaders.ACCEPT_LANGUAGE,
                         "en;q=0.7, en-gb;q=0.8, da");
        Message m = createMessage(headers);
        m.put(HttpHeadersImpl.HEADER_SPLIT_PROPERTY, "true");
        HttpHeaders h = new HttpHeadersImpl(m);
        List<Locale> languages = h.getAcceptableLanguages();
        assertEquals(3, languages.size());
        languages.clear();
        languages = h.getAcceptableLanguages();
        assertEquals(3, languages.size());

        MultivaluedMap<String, String> rHeaders = h.getRequestHeaders();
        List<String> acceptL = rHeaders.get(HttpHeaders.ACCEPT_LANGUAGE);
        assertEquals(3, acceptL.size());

        rHeaders.clear();
    }

    private static MetadataMap<String, String> createHeaders() {
        MetadataMap<String, String> hs = new MetadataMap<>();
        hs.add("Accept", "text/bar;q=0.6");
        hs.add("Accept", "text/*;q=1,application/xml");
        hs.putSingle("Content-Type", "*/*");
        hs.putSingle("Date", "Tue, 21 Oct 2008 17:00:00 GMT");
        hs.putSingle("Content-Length", "10");
        hs.addAll("a", Arrays.asList("1", "2"));
        return hs;
    }

    private static MetadataMap<String, String> createHeader(String name, String... values) {
        MetadataMap<String, String> hs = new MetadataMap<>();
        hs.put(name, Arrays.asList(values));
        return hs;
    }

    private static Message createMessage(MetadataMap<String, String> headers) {
        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, headers);
        return m;
    }

}