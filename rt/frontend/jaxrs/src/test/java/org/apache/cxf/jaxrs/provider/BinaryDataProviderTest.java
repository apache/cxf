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

package org.apache.cxf.jaxrs.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.helpers.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class BinaryDataProviderTest extends Assert {
    
    @Test
    public void testIsWriteable() {
        MessageBodyWriter<Object> p = new BinaryDataProvider();
        assertTrue(p.isWriteable(byte[].class, null, null)
                   && p.isWriteable(InputStream.class, null, null)
                   && p.isWriteable(File.class, null, null)
                   && !p.isWriteable(int[].class, null, null));
    }
    
    @Test
    public void testIsReadable() {
        MessageBodyReader<Object> p = new BinaryDataProvider();
        assertTrue(p.isReadable(byte[].class, null, null)
                   && p.isReadable(InputStream.class, null, null)
                   && !p.isReadable(File.class, null, null)
                   && !p.isReadable(int[].class, null, null));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadFrom() throws Exception {
        MessageBodyReader p = new BinaryDataProvider();
        byte[] bytes = (byte[])p.readFrom(byte[].class, null, null, 
                                          null, null, new ByteArrayInputStream("hi".getBytes()));
        assertTrue(Arrays.equals(new String("hi").getBytes(), bytes));
        
        InputStream is = (InputStream)p.readFrom(InputStream.class, null, null, null, null, 
            new ByteArrayInputStream("hi".getBytes()));
        bytes = IOUtils.readBytesFromStream(is);
        assertTrue(Arrays.equals(new String("hi").getBytes(), bytes));
        
        Reader r = (Reader)p.readFrom(Reader.class, null, null, 
                       MediaType.valueOf("text/xml"), null, new ByteArrayInputStream("hi".getBytes()));
        assertEquals(IOUtils.toString(r), "hi");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testWriteTo() throws Exception {
        MessageBodyWriter p = new BinaryDataProvider();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        p.writeTo(new byte[]{'h', 'i'}, null, null, null, null, null, os);
        assertTrue(Arrays.equals(new String("hi").getBytes(), os.toByteArray()));
        ByteArrayInputStream is = new ByteArrayInputStream("hi".getBytes());
        os = new ByteArrayOutputStream();
        p.writeTo(is, null, null, null, null, null, os);
        assertTrue(Arrays.equals(os.toByteArray(), new String("hi").getBytes()));
        
        Reader r = new StringReader("hi");
        os = new ByteArrayOutputStream();
        p.writeTo(r, null, null, null, MediaType.valueOf("text/xml"), null, os);
        assertTrue(Arrays.equals(os.toByteArray(), new String("hi").getBytes()));
        
        os = new ByteArrayOutputStream();
        p.writeTo(new StreamingOutputImpl(), null, null, null, 
                  MediaType.valueOf("text/xml"), null, os);
        assertTrue(Arrays.equals(os.toByteArray(), new String("hi").getBytes()));
    }

    
    private static class StreamingOutputImpl implements StreamingOutput {

        public void write(OutputStream output) throws IOException {
            output.write("hi".getBytes());
        }
        
    }
}
