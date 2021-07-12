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



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IOUtilsTest {


    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(IOUtils.isEmpty(new ByteArrayInputStream(new byte[]{})));
    }
    @Test
    public void testNonEmpty() throws Exception {
        InputStream is = new ByteArrayInputStream("Hello".getBytes());
        assertFalse(IOUtils.isEmpty(is));
        assertEquals("Hello", IOUtils.toString(is));
    }
    @Test
    public void testNonEmptyWithPushBack() throws Exception {
        InputStream is = new PushbackInputStream(
                             new ByteArrayInputStream("Hello".getBytes()));
        assertFalse(IOUtils.isEmpty(is));
        assertEquals("Hello", IOUtils.toString(is));
    }
    @Test
    public void testInputStreamWithNoMark() throws Exception {
        String data = "this is some data";
        InputStream is = new UnMarkedInputStream(data.getBytes());
        assertFalse(IOUtils.isEmpty(is));
        assertEquals(data, IOUtils.toString(is));
    }
    
    @Test
    public void testCopy() throws IOException {
        byte[] inBytes = "Foo".getBytes(IOUtils.UTF8_CHARSET);
        ByteArrayInputStream is = new ByteArrayInputStream(inBytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream(inBytes.length);
        IOUtils.copy(is, os);
        byte[] outBytes = os.toByteArray();
        String expectedString = new String(outBytes, IOUtils.UTF8_CHARSET);
        assertEquals("Foo", expectedString);
    }

    @Test(expected = IOException.class)
    public void testCopyAndCloseInput() throws IOException {
        InputStream is = getClass().getResourceAsStream("/wsdl/foo.wsdl");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copyAndCloseInput(is, os);
        is.read();
        fail("InputStream should be closed");
    }

    @Test
    public void testCopyAtLeastGreaterThanStreamSize() throws IOException {
        byte[] bytes = "Foo".getBytes(IOUtils.UTF8_CHARSET);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int atLeast = 5; // greater than inputStream length
        IOUtils.copyAtLeast(is, os, atLeast);
        assertEquals(3, os.toByteArray().length);

    }

    @Test
    public void testCopyAtLeastEqualStreamSize() throws IOException {
        byte[] bytes = "Foo".getBytes(IOUtils.UTF8_CHARSET);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int atLeast = 3; // equal than inputStream length
        IOUtils.copyAtLeast(is, os, atLeast);
        assertEquals(atLeast, os.toByteArray().length);

    }

    @Test
    public void testCopyAtLeastLessThanStreamSize() throws IOException {
        byte[] bytes = "Foo".getBytes(IOUtils.UTF8_CHARSET);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int atLeast = 2; // less than inpuStream length
        IOUtils.copyAtLeast(is, os, atLeast);
        assertEquals(atLeast, os.toByteArray().length);

    }
}