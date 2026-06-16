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
package org.apache.cxf.attachment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class QuotedPrintableDecoderStreamTest {

    private static byte[] decode(byte[] encoded) throws IOException {
        InputStream in = new QuotedPrintableDecoderStream(new ByteArrayInputStream(encoded));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toByteArray();
    }

    @Test
    public void testDecodesHexPair() throws Exception {
        assertArrayEquals("hello world".getBytes("US-ASCII"),
                          decode("hello=20world".getBytes("US-ASCII")));
    }

    @Test
    public void testDecodesLowerCaseHex() throws Exception {
        // a lower case hex pair must decode to the same byte as the upper case form
        assertArrayEquals(new byte[] {(byte)0xe2}, decode("=e2".getBytes("US-ASCII")));
        assertArrayEquals(new byte[] {(byte)0xe2}, decode("=E2".getBytes("US-ASCII")));
    }

    @Test
    public void testRejectsNonHexCharacters() {
        assertThrows(IOException.class, () -> decode("=GG".getBytes("US-ASCII")));
    }

    @Test
    public void testRejectsHighBitByteAfterMarker() {
        // a byte >= 0x80 following the '=' marker must not index the decode table negatively
        assertThrows(IOException.class, () -> decode(new byte[] {'=', (byte)0xff, 'A'}));
    }
}
