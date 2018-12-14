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

package org.apache.cxf.helpers;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class HttpHeaderHelperTest {

    @Test
    public void testMapCharset() {
        final String charset = StandardCharsets.UTF_8.name();

        String cs = HttpHeaderHelper.mapCharset(charset);
        assertEquals(charset, cs);

        cs = HttpHeaderHelper.mapCharset("\"" + charset + "\"");
        assertEquals(charset, cs);

        cs = HttpHeaderHelper.mapCharset("'" + charset + "'");
        assertEquals(charset, cs);

        cs = HttpHeaderHelper.mapCharset(null);
        assertEquals(HttpHeaderHelper.ISO88591, cs);

        cs = HttpHeaderHelper.mapCharset("''");
        assertEquals(HttpHeaderHelper.ISO88591, cs);

        cs = HttpHeaderHelper.mapCharset("wrong-charset-name");
        assertNull(cs);
    }

    @Test
    public void testEmptyCharset() {
        String cs = HttpHeaderHelper.mapCharset(HttpHeaderHelper.findCharset("foo/bar; charset="));
        assertEquals(HttpHeaderHelper.ISO88591, cs);
    }

    @Test
    public void testEmptyCharset2() {
        String cs = HttpHeaderHelper.mapCharset(HttpHeaderHelper.findCharset("foo/bar; charset=;"));
        assertEquals(HttpHeaderHelper.ISO88591, cs);
    }

    @Test
    public void testNoCharset() {
        String cs = HttpHeaderHelper.mapCharset(HttpHeaderHelper.findCharset("foo/bar"));
        assertEquals(HttpHeaderHelper.ISO88591, cs);
    }

    @Test
    public void testFindCharset() {
        final String charset = StandardCharsets.UTF_8.name();
        String cs = HttpHeaderHelper.findCharset("foo/bar; charset=" + charset);
        assertEquals(charset, cs);
    }

}
