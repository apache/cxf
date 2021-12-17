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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MediaTypeHeaderProviderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullValue() throws Exception {
        MediaType.valueOf(null);
    }

    @Test
    public void testTypeWithExtendedParameters() {
        MediaType mt = MediaType.valueOf("multipart/related;type=application/dicom+xml");

        assertEquals("multipart", mt.getType());
        assertEquals("related", mt.getSubtype());
        Map<String, String> params2 = mt.getParameters();
        assertEquals(1, params2.size());
        assertEquals("application/dicom+xml", params2.get("type"));
    }

    @Test
    public void testTypeWithExtendedParametersQuote() {
        MediaType mt = MediaType.valueOf("multipart/related;type=\"application/dicom+xml\"");

        assertEquals("multipart", mt.getType());
        assertEquals("related", mt.getSubtype());
        Map<String, String> params2 = mt.getParameters();
        assertEquals(1, params2.size());
        assertEquals("\"application/dicom+xml\"", params2.get("type"));
    }

    @Test
    public void testTypeWithExtendedAndBoundaryParameter() {
        MediaType mt = MediaType.valueOf(
            "multipart/related; type=application/dicom+xml; boundary=\"uuid:b9aecb2a-ab37-48d6-a1cd-b2f4f7fa63cb\"");
        assertEquals("multipart", mt.getType());
        assertEquals("related", mt.getSubtype());
        Map<String, String> params2 = mt.getParameters();
        assertEquals(2, params2.size());
        assertEquals("\"uuid:b9aecb2a-ab37-48d6-a1cd-b2f4f7fa63cb\"", params2.get("boundary"));
        assertEquals("application/dicom+xml", params2.get("type"));
    }

    @Test
    public void testSimpleType() {
        MediaType m = MediaType.valueOf("text/html");
        assertEquals("Media type was not parsed correctly",
                     m, new MediaType("text", "html"));
        assertEquals("Media type was not parsed correctly",
                     MediaType.valueOf("text/html "), new MediaType("text", "html"));
    }

    @Test
    public void testShortWildcard() {
        MediaType m = MediaType.valueOf("*");
        assertEquals("Media type was not parsed correctly",
                     m, new MediaType("*", "*"));
    }

    @Test
    public void testShortWildcardWithParameters() {
        MediaType m = MediaType.valueOf("*;q=0.2");
        assertEquals("Media type was not parsed correctly",
                     m, new MediaType("*", "*", Collections.singletonMap("q", "0.2")));
    }

    @Test
    public void testShortWildcardWithParameters2() {
        MediaType m = MediaType.valueOf("* ;q=0.2");
        assertEquals("Media type was not parsed correctly",
                     m, new MediaType("*", "*", Collections.singletonMap("q", "0.2")));
    }

    @Test
    public void testShortWildcardWithParameters3() {
        MediaType m = MediaType.valueOf("*; q=.2");
        assertEquals("Media type was not parsed correctly",
                     m, new MediaType("*", "*", Collections.singletonMap("q", ".2")));
    }

    @Test
    public void testBadType() {
        try {
            new MediaTypeHeaderProvider().fromString("texthtml");
            fail("Parse exception must've been thrown");
        } catch (IllegalArgumentException pe) {
            // expected
        }

    }

    @Test
    public void testBadParameter() {
        try {
            new MediaTypeHeaderProvider().fromString("text/html;*");
            fail("Parse exception must've been thrown");
        } catch (IllegalArgumentException pe) {
            // expected
        }
    }

    @Test
    public void testIlleageMediaType() {
        try {
            new MediaTypeHeaderProvider().fromString("t//;*");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("s//t;type=a/b");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("s/b/t;type=a/b");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("/b;type=a/b");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("@pplication/json");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("application/<xml>");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("application/xml,json");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("t[ext]/plain");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("text/pla:n");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }

        try {
            new MediaTypeHeaderProvider().fromString("text/reg(ex)");
            fail("Parse exception expected");
        } catch (IllegalArgumentException pe) {
            // expected
        }
    }

    @Test
    public void testTypeWithParameters() {
        MediaType mt = MediaType.valueOf("text/html;q=1234;b=4321");

        assertEquals("text", mt.getType());
        assertEquals("html", mt.getSubtype());
        Map<String, String> params2 = mt.getParameters();
        assertEquals(2, params2.size());
        assertEquals("1234", params2.get("q"));
        assertEquals("4321", params2.get("b"));
    }

    @Test
    public void testSimpleToString() {
        MediaTypeHeaderProvider provider =
            new MediaTypeHeaderProvider();

        assertEquals("simple media type is not serialized", "text/plain",
                     provider.toString(new MediaType("text", "plain")));
    }

    @Test
    public void testHeaderFileName() {

        String fileName = "version_2006&#65288;3&#65289;.pdf";
        String header = "application/octet-stream; name=\"%s\"";
        String value = String.format(header, fileName);

        MediaTypeHeaderProvider provider = new MediaTypeHeaderProvider();
        MediaType mt = provider.fromString(value);
        assertEquals("application", mt.getType());
        assertEquals("octet-stream", mt.getSubtype());
        Map<String, String> params = mt.getParameters();
        assertEquals(1, params.size());
        assertEquals("\"version_2006&#65288;3&#65289;.pdf\"", params.get("name"));

    }

    @Test
    public void testComplexToString() {
        MediaTypeHeaderProvider provider =
            new MediaTypeHeaderProvider();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("foo", "bar");
        params.put("q", "0.2");

        assertEquals("complex media type is not serialized", "text/plain;foo=bar;q=0.2",
                     provider.toString(new MediaType("text", "plain", params)));

    }

}