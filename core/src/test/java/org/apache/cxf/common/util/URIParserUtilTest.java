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

package org.apache.cxf.common.util;

import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class URIParserUtilTest {

    @Test
    public void testRelativize() throws URISyntaxException {
        assertNull(URIParserUtil.relativize(null, "foo"));
        assertNull(URIParserUtil.relativize("foo", null));
        assertEquals("", URIParserUtil.relativize("", ""));
        assertEquals("", URIParserUtil.relativize("fds", ""));
        assertEquals("../", URIParserUtil.relativize("fds/", ""));
        assertEquals("fdsfs", URIParserUtil.relativize("", "fdsfs"));
        assertEquals("fdsfs/a", URIParserUtil.relativize("", "fdsfs/a"));
        assertEquals("../de", URIParserUtil.relativize("ab/cd", "de"));
        assertEquals("../de/fe/gh", URIParserUtil.relativize("ab/cd", "de/fe/gh"));
        assertEquals("../../../de/fe/gh", URIParserUtil.relativize("/abc/def/", "de/fe/gh"));
        assertNull(URIParserUtil.relativize("file:/c:/abc/def/", "de/fe/gh")); // null as the URI obtained by
                                                                               // the 2 strings are not both
                                                                               // absolute or not absolute
        assertEquals("pippo2.xsd", URIParserUtil.relativize("/abc/def/pippo1.xsd", "/abc/def/pippo2.xsd"));
        assertEquals("../default/pippo2.xsd",
                     URIParserUtil.relativize("/abc/def/pippo1.xsd", "/abc/default/pippo2.xsd"));
        assertEquals("def/pippo2.xsd", URIParserUtil.relativize("/abc/def", "/abc/def/pippo2.xsd"));
        assertEquals("hello_world_schema2.xsd",
                     URIParserUtil.relativize("jar:file:/home/a.jar!/wsdl/others/",
                                              "jar:file:/home/a.jar!/wsdl/others/hello_world_schema2.xsd"));
    }

    @Test
    public void testNormalize() throws Exception {
        String uri = "wsdl/hello_world.wsdl";
        assertEquals("file:wsdl/hello_world.wsdl", URIParserUtil.normalize(uri));

        uri = "\\src\\wsdl/hello_world.wsdl";
        assertEquals("file:/src/wsdl/hello_world.wsdl", URIParserUtil.normalize(uri));

        uri = "wsdl\\hello_world.wsdl";
        assertEquals("file:wsdl/hello_world.wsdl", URIParserUtil.normalize(uri));

        uri = "http://hello.com";
        assertEquals("http://hello.com", URIParserUtil.normalize(uri));

        uri = "file:///c:\\hello.wsdl";
        assertEquals("file:/c:/hello.wsdl", URIParserUtil.normalize(uri));

        uri = "c:\\hello.wsdl";
        assertEquals("file:/c:/hello.wsdl", URIParserUtil.normalize(uri));

        uri = "/c:\\hello.wsdl";
        assertEquals("file:/c:/hello.wsdl", URIParserUtil.normalize(uri));

        uri = "file:/home/john/test/all/../../alltest";
        assertEquals("file:/home/john/alltest", URIParserUtil.normalize(uri));
    }

    @Test
    public void testGetAbsoluteURI() throws Exception {
        String uri = "wsdl/hello_world.wsdl";
        String uri2 = URIParserUtil.getAbsoluteURI(uri);
        assertNotNull(uri2);
        assertTrue(uri2.startsWith("file"));
        assertTrue(uri2.contains(uri));
        assertTrue(uri2.contains(new java.io.File("").toString()));

        uri = getClass().getResource("/schemas/echoSchema.xsd").toString();
        uri2 = URIParserUtil.getAbsoluteURI(uri);
        assertNotNull(uri2);
        assertTrue(uri2.startsWith("file"));
        assertTrue(uri2.contains(uri));
        assertTrue(uri2.contains(new java.io.File("").toString()));

        uri = "c:\\wsdl\\hello_world.wsdl";
        uri2 = URIParserUtil.getAbsoluteURI(uri);
        assertNotNull(uri2);
        assertEquals("file:/c:/wsdl/hello_world.wsdl", uri2);

        uri = "/c:\\wsdl\\hello_world.wsdl";
        uri2 = URIParserUtil.getAbsoluteURI(uri);
        assertNotNull(uri2);
        assertEquals("file:/c:/wsdl/hello_world.wsdl", uri2);

        uri = "http://hello/world.wsdl";
        assertEquals(uri, URIParserUtil.getAbsoluteURI(uri));

        uri = "file:/home/john/test/all/../../alltest";
        assertEquals("file:/home/john/alltest", URIParserUtil.getAbsoluteURI(uri));
    }
    @Test
    public void testCXF3855() throws Exception {
        String orig = new String(new byte[] {-47, -122}, StandardCharsets.UTF_8);
        orig = "/foo" + orig + ".txt";
        String s = URIParserUtil.escapeChars(orig);
        assertEquals(orig, URLDecoder.decode(s, StandardCharsets.UTF_8.name()));
    }

}