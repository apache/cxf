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
import java.io.IOException;
import java.io.PushbackInputStream;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MimeBodyPartInputStreamTest {

    private byte[] messageBytes;
    private PushbackInputStream pushbackStream;

    @Before
    public void setUp() {
        messageBytes = ("------=_Part_1\n\nJJJJ\n------=_Part_1\n\n"
                        + "Content-Transfer-Encoding: binary\n\n=3D=3D=3D\n------=_Part_1\n").getBytes();

        pushbackStream = new PushbackInputStream(new ByteArrayInputStream(messageBytes), 2048);
    }

    @Test
    public void readCloseStream() throws Exception {
        MimeBodyPartInputStream mimeBodyInputStream = new MimeBodyPartInputStream(pushbackStream,
                                                                                  "------=_Part_1".getBytes(),
                                                                                  2048);
        mimeBodyInputStream.close();

        assertEquals(-1, mimeBodyInputStream.read(new byte[1000], 0, 1000));

    }

    @Test
    public void readZeroStreamLength() throws Exception {
        MimeBodyPartInputStream mimeBodyInputStream = new MimeBodyPartInputStream(pushbackStream,
                                                                                  "------=_Part_1".getBytes(),
                                                                                  2048);

        assertEquals(0, mimeBodyInputStream.read(new byte[1000], 0, 0));

    }

    @Test
    public void readLengtEqualBoundaryLength() throws Exception {
        byte[] boundaryParam = "------=_Part_1".getBytes();
        MimeBodyPartInputStream mimeBodyInputStream = new MimeBodyPartInputStream(pushbackStream,
                                                                                  boundaryParam, 2048);
        int len = boundaryParam.length;
        assertEquals(-1, mimeBodyInputStream.read(new byte[1000], 0, len));

    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testOutOfBound() throws IOException {
        MimeBodyPartInputStream m = new MimeBodyPartInputStream(null, "------=_Part_1".getBytes(), 2048);
        m.read(new byte[100], 101, 100);
        m.close();

    }

}
