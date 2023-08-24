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
import java.lang.annotation.Annotation;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BinaryDataProviderTest {

    @Test
    public void testIsWriteable() {
        MessageBodyWriter<Object> p = new BinaryDataProvider<>();
        assertTrue(p.isWriteable(byte[].class, null, null, null)
                   && p.isWriteable(InputStream.class, null, null, null)
                   && p.isWriteable(File.class, null, null, null)
                   && !p.isWriteable(int[].class, null, null, null));
    }

    @Test
    public void testIsReadable() {
        MessageBodyReader<Object> p = new BinaryDataProvider<>();
        assertTrue(p.isReadable(byte[].class, null, null, null)
                   && p.isReadable(InputStream.class, null, null, null)
                   && p.isReadable(File.class, null, null, null)
                   && p.isReadable(StreamingOutput.class, null, null, null)
                   && !p.isReadable(int[].class, null, null, null));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testReadFrom() throws Exception {
        MessageBodyReader p = new BinaryDataProvider();
        byte[] bytes = (byte[])p.readFrom(byte[].class, byte[].class, new Annotation[]{},
                                          MediaType.APPLICATION_OCTET_STREAM_TYPE,
                                          new MetadataMap<String, Object>(),
                                          new ByteArrayInputStream("hi".getBytes()));
        assertArrayEquals(new String("hi").getBytes(), bytes);

        InputStream is = (InputStream)p.readFrom(InputStream.class, InputStream.class, new Annotation[]{},
                                                 MediaType.APPLICATION_OCTET_STREAM_TYPE,
                                                 new MetadataMap<String, Object>(),
            new ByteArrayInputStream("hi".getBytes()));
        bytes = IOUtils.readBytesFromStream(is);
        assertArrayEquals(new String("hi").getBytes(), bytes);

        Reader r = (Reader)p.readFrom(Reader.class, Reader.class, new Annotation[]{},
                                      MediaType.APPLICATION_OCTET_STREAM_TYPE,
                                      new MetadataMap<String, Object>(),
                                      new ByteArrayInputStream("hi".getBytes()));
        assertEquals(IOUtils.toString(r), "hi");

        StreamingOutput so = (StreamingOutput)p.readFrom(StreamingOutput.class, StreamingOutput.class,
                                      new Annotation[]{},
                                      MediaType.APPLICATION_OCTET_STREAM_TYPE,
                                      new MetadataMap<String, Object>(),
                                      new ByteArrayInputStream("hi".getBytes()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        so.write(baos);
        bytes = baos.toByteArray();
        assertArrayEquals(new String("hi").getBytes(), bytes);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testReadBytesFromUtf8() throws Exception {
        MessageBodyReader p = new BinaryDataProvider();
        byte[] utf8Bytes = "世界ーファイル".getBytes("UTF-16");
        byte[] readBytes = (byte[])p.readFrom(byte[].class, byte[].class, new Annotation[]{},
                                          MediaType.APPLICATION_OCTET_STREAM_TYPE,
                                          new MetadataMap<String, Object>(),
                                          new ByteArrayInputStream(utf8Bytes));
        assertArrayEquals(utf8Bytes, readBytes);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testWriteTo() throws Exception {
        MessageBodyWriter p = new BinaryDataProvider();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        p.writeTo(new byte[]{'h', 'i'}, null, null, null, null, null, os);
        assertArrayEquals(new String("hi").getBytes(), os.toByteArray());
        ByteArrayInputStream is = new ByteArrayInputStream("hi".getBytes());
        os = new ByteArrayOutputStream();
        p.writeTo(is, null, null, null, null, null, os);
        assertArrayEquals(os.toByteArray(), new String("hi").getBytes());

        Reader r = new StringReader("hi");
        os = new ByteArrayOutputStream();
        p.writeTo(r, null, null, null, MediaType.valueOf("text/xml"), null, os);
        assertArrayEquals(os.toByteArray(), new String("hi").getBytes());

        os = new ByteArrayOutputStream();
        p.writeTo(new StreamingOutputImpl(), null, null, null,
                  MediaType.valueOf("text/xml"), null, os);
        assertArrayEquals(os.toByteArray(), new String("hi").getBytes());
    }


    private static final class StreamingOutputImpl implements StreamingOutput {

        public void write(OutputStream output) throws IOException {
            output.write("hi".getBytes());
        }

    }
}
