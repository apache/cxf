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

package org.apache.cxf.tools.util;

import org.junit.Assert;
import org.junit.Test;


public class URIParserUtilTest extends Assert {

    @Test
    public void testGetPackageName() {
        
        String packageName = URIParserUtil.getPackageName("http://www.cxf.iona.com");
        assertEquals(packageName, "com.iona.cxf");
        packageName = URIParserUtil.getPackageName("urn://www.class.iona.com");
        assertEquals(packageName, "com.iona._class");
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
    }

    @Test
    public void testGetAbsoluteURI() throws Exception {
        String uri = "wsdl/hello_world.wsdl";
        String uri2 = URIParserUtil.getAbsoluteURI(uri);
        assertNotNull(uri2);
        assertTrue(uri2.startsWith("file"));
        assertTrue(uri2.contains(uri));
        assertTrue(uri2.contains(new java.io.File("").toString()));

        uri = getClass().getResource("/schemas/wsdl/test.xsd").toString();
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
    }
}
