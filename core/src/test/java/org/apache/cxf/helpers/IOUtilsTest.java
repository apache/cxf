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
import java.io.File;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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

    /**
     * add a new test when destinationFile is null
     */
    @Test
    public void testTransferToWhenEmptyFile() throws Exception {
        InputStream is = new ByteArrayInputStream("Hello".getBytes());
        File destinationFile = null;
        Exception exception = assertThrows(Exception.class, () -> { 
            IOUtils.transferTo(is, destinationFile);
        });
        String expectedMessage = "The destinationFile is required";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}