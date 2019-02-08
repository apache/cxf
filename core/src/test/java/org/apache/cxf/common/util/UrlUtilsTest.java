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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class UrlUtilsTest {

    @Test
    public void testUrlDecode() {
        assertEquals("+ ", UrlUtils.urlDecode("%2B+"));
    }

    @Test
    public void testUrlDecodeSingleCharMultipleEscapes() {
        String s = "ß";
        String encoded = UrlUtils.urlEncode(s);
        assertEquals(s, UrlUtils.urlDecode(encoded));
    }

    @Test
    public void testUrlDecodeReserved() {
        assertEquals("!$&'()*,;=", UrlUtils.urlDecode("!$&'()*,;="));
    }

    @Test
    public void testUrlDecodeIncompleteEscapePatterns() {

        try {
            UrlUtils.urlDecode("%");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Invalid URL encoding"));
        }

        try {
            UrlUtils.urlDecode("a%%%%");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Invalid URL encoding"));
        }

        try {
            UrlUtils.urlDecode("a%2B%");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Invalid URL encoding"));
        }

        try {
            UrlUtils.urlDecode("%2");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Invalid URL encoding"));
        }
    }

    @Test
    public void testUrlDecodeInvalidEscapePattern() {
        try {
            UrlUtils.urlDecode("%2$");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Invalid URL encoding"));
        }
    }

    @Test
    public void testPathDecode() {
        assertEquals("+++", UrlUtils.pathDecode("+%2B+"));
    }
}