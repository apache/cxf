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

import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.DataFormatException;

import org.apache.cxf.helpers.IOUtils;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CompressionUtilsTest {

    @Test
    public void testDeflateInflateRoundTrip() throws Exception {
        byte[] original = "the quick brown fox jumps over the lazy dog".getBytes();
        byte[] deflated = CompressionUtils.deflate(original, true);

        InputStream inflated = CompressionUtils.inflate(deflated, true);
        assertArrayEquals(original, IOUtils.readBytesFromStream(inflated, original.length));
    }

    @Test
    public void testInflateWithinExplicitCapSucceeds() throws Exception {
        byte[] original = new byte[1024];
        Arrays.fill(original, (byte) 'a');
        byte[] deflated = CompressionUtils.deflate(original, true);

        InputStream inflated = CompressionUtils.inflate(deflated, true, original.length);
        assertArrayEquals(original, IOUtils.readBytesFromStream(inflated, original.length));
    }

    @Test
    public void testInflateRejectsOutputExceedingExplicitCap() {
        // Highly compressible payload: deflates to a tiny stream but expands well past the cap
        byte[] original = new byte[1024 * 1024];
        Arrays.fill(original, (byte) 0);
        byte[] deflated = CompressionUtils.deflate(original, true);

        try {
            CompressionUtils.inflate(deflated, true, 1024);
            fail("Expected a DataFormatException as the inflated size exceeds the cap");
        } catch (DataFormatException e) {
            assertEquals("Inflated data exceeds the maximum allowed size of 1024 bytes", e.getMessage());
        }
    }

    @Test
    public void testDefaultInflateRejectsDecompressionBomb() {
        // Simulate a decompression bomb: a large run of zeros compresses to a very small stream
        byte[] original = new byte[(int) CompressionUtils.DEFAULT_MAX_INFLATED_SIZE + (1024 * 1024)];
        Arrays.fill(original, (byte) 0);
        byte[] deflated = CompressionUtils.deflate(original, true);

        try {
            CompressionUtils.inflate(deflated, true);
            fail("Expected a DataFormatException as the inflated size exceeds the default cap");
        } catch (DataFormatException e) {
            assertEquals("Inflated data exceeds the maximum allowed size of "
                + CompressionUtils.DEFAULT_MAX_INFLATED_SIZE + " bytes", e.getMessage());
        }
    }
}
