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

import java.util.Date;
import java.util.Locale;

import jakarta.ws.rs.core.NewCookie;
import org.apache.cxf.jaxrs.utils.HttpUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NewCookieHeaderProviderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullValue() throws Exception {
        NewCookie.valueOf(null);
    }

    @Test
    public void testFromSimpleString() {
        NewCookie c = NewCookie.valueOf("foo=bar");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName()));
    }

    @Test
    public void testFromSimpleStringWithExpires() {
        NewCookie c = NewCookie.valueOf("foo=bar;Expires=Wed, 09 Jun 2021 10:18:14 GMT");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName()));
    }

    @Test
    public void testNoValue() {
        NewCookie c = NewCookie.valueOf("foo=");
        assertTrue("".equals(c.getValue())
                   && "foo".equals(c.getName()));
    }

    @Test
    public void testFromComplexString() {
        NewCookie c = NewCookie.valueOf(
                      "foo=bar;Comment=comment;Path=path;Max-Age=10;Domain=domain;Secure;Version=1");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName())
                   && 1 == c.getVersion()
                   && "path".equals(c.getPath())
                   && "domain".equals(c.getDomain())
                   && "comment".equals(c.getComment())
                   && 10 == c.getMaxAge());
    }

    @Test
    public void testFromComplexStringWithExpiresAndHttpOnly() {
        NewCookie c = NewCookie.valueOf(
                      "foo=bar;Comment=comment;Path=path;Max-Age=10;Domain=domain;Secure;"
                      + "Expires=Wed, 09 Jun 2021 10:18:14 GMT;HttpOnly;Version=1");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName()));
        assertTrue(1 == c.getVersion()
                   && "path".equals(c.getPath())
                   && "domain".equals(c.getDomain())
                   && "comment".equals(c.getComment())
                   && c.isSecure()
                   && c.isHttpOnly()
                   && 10 == c.getMaxAge());
        Date d = c.getExpiry();
        assertNotNull(d);
        assertEquals("Wed, 09 Jun 2021 10:18:14 GMT", HttpUtils.toHttpDate(d));
    }

    @Test
    public void testFromComplexStringLowerCase() {
        NewCookie c = NewCookie.valueOf(
                      "foo=bar;comment=comment;path=path;max-age=10;domain=domain;secure;version=1");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName())
                   && 1 == c.getVersion()
                   && "path".equals(c.getPath())
                   && "domain".equals(c.getDomain())
                   && "comment".equals(c.getComment())
                   && 10 == c.getMaxAge());
    }


    @Test
    public void testFromStringWithSpaces() {
        NewCookie c = NewCookie.valueOf(
                      "foo=bar; Comment=comment; Path=path; Max-Age=10; Domain=domain; Secure; Version=1");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName())
                   && 1 == c.getVersion()
                   && "path".equals(c.getPath())
                   && "domain".equals(c.getDomain())
                   && "comment".equals(c.getComment())
                   && 10 == c.getMaxAge());
    }

    @Test
    public void testFromStringWithSpecialChar() {
        NewCookie c = NewCookie.valueOf(
                      "foo=\"bar (space)<>[]\"; Comment=\"comment@comment:,\"; Path=\"/path?path\"; Max-Age=10; "
                      + "Domain=\"domain.com\"; Secure; Version=1");
        assertTrue("bar (space)<>[]".equals(c.getValue())
                   && "foo".equals(c.getName())
                   && 1 == c.getVersion()
                   && "/path?path".equals(c.getPath())
                   && "domain.com".equals(c.getDomain())
                   && "comment@comment:,".equals(c.getComment())
                   && 10 == c.getMaxAge());
    }

    @Test
    public void testToString() {
        NewCookie c = new NewCookie("foo", "bar", "path", "domain", "comment", 2, true);
        assertEquals("foo=bar;Comment=comment;Domain=domain;Max-Age=2;Path=path;Secure;Version=1",
                     c.toString());
    }

    @Test
    public void testToStringWithSpecialChar() {
        NewCookie c = new NewCookie("foo", "bar (space)<>[]", "/path?path", "domain.com", "comment@comment:,", 2, true);
        assertEquals("foo=\"bar (space)<>[]\";Comment=\"comment@comment:,\";Domain=domain.com;Max-Age=2;"
                     + "Path=\"/path?path\";Secure;Version=1", c.toString());
    }
    @Test
    public void testToStringWithPathSlalshOnly() {
        NewCookie c = new NewCookie("foo", "bar (space)<>[]", "/path", "domain.com", "comment@comment:,", 2, true);
        assertEquals("foo=\"bar (space)<>[]\";Comment=\"comment@comment:,\";Domain=domain.com;Max-Age=2;"
                     + "Path=/path;Secure;Version=1", c.toString());
    }

    @Test
    public void testFromStringWithSameSite() {
        NewCookie c = NewCookie.valueOf("foo=bar;SameSite=Lax;Path=/");
        assertEquals("foo", c.getName());
        assertEquals("bar", c.getValue());
        assertEquals("/", c.getPath());
        assertEquals(NewCookie.SameSite.LAX, c.getSameSite());
    }

    @Test
    public void testFromStringWithSameSiteIsCaseInsensitive() {
        NewCookie c = NewCookie.valueOf("foo=bar; samesite=none; Secure");
        assertEquals("foo", c.getName());
        assertEquals("bar", c.getValue());
        assertTrue(c.isSecure());
        assertEquals(NewCookie.SameSite.NONE, c.getSameSite());
    }

    @Test
    public void testFromStringWithUnrecognizedSameSiteValue() {
        NewCookie c = NewCookie.valueOf("foo=bar;SameSite=Unknown");
        assertEquals("foo", c.getName());
        assertEquals("bar", c.getValue());
        assertNull(c.getSameSite());
    }

    @Test
    public void testFromStringIgnoresUnknownAttributes() {
        NewCookie c = NewCookie.valueOf("foo=bar;Priority=High;Path=path");
        assertEquals("foo", c.getName());
        assertEquals("bar", c.getValue());
        assertEquals("path", c.getPath());
    }

    @Test
    public void testFromStringWithCookieNamedAfterAnAttribute() {
        NewCookie c = NewCookie.valueOf("path=abc; Path=/");
        assertEquals("path", c.getName());
        assertEquals("abc", c.getValue());
        assertEquals("/", c.getPath());
    }

    @Test
    public void testFromStringWithCookieNamedSameSite() {
        NewCookie c = NewCookie.valueOf("SameSite=Lax");
        assertEquals("SameSite", c.getName());
        assertEquals("Lax", c.getValue());
        assertNull(c.getSameSite());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromStringWithoutNameValuePairIsMalformed() {
        NewCookie.valueOf("Secure; foo=bar");
    }

    @Test
    public void testFromStringSkipsEmptySegments() {
        NewCookie c = NewCookie.valueOf(" ; foo=bar;;Path=path");
        assertEquals("foo", c.getName());
        assertEquals("bar", c.getValue());
        assertEquals("path", c.getPath());
    }

    @Test
    public void testToStringWithSameSite() {
        NewCookie c = new NewCookie.Builder("foo").value("bar").path("path")
            .sameSite(NewCookie.SameSite.STRICT).build();
        assertEquals("foo=bar;Path=path;SameSite=Strict;Version=1", c.toString());
    }

    @Test
    public void testToStringWithSameSiteUnderTurkishLocale() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            NewCookie c = new NewCookie.Builder("foo").value("bar")
                .sameSite(NewCookie.SameSite.STRICT).build();
            assertEquals("foo=bar;SameSite=Strict;Version=1", c.toString());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testSameSiteRoundTrip() {
        NewCookie c = NewCookie.valueOf(
            new NewCookie.Builder("foo").value("bar").sameSite(NewCookie.SameSite.NONE).secure(true).build()
                .toString());
        assertEquals("foo", c.getName());
        assertEquals("bar", c.getValue());
        assertTrue(c.isSecure());
        assertEquals(NewCookie.SameSite.NONE, c.getSameSite());
    }

}