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
package org.apache.cxf.transport.common.gzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class GZIPInInterceptorTest {

    @Test
    public void testDecompress() throws Exception {
        byte[] data = "<hello>world</hello>".getBytes();
        Message message = createMessage(gzip(data));
        new GZIPInInterceptor().handleMessage(message);
        assertArrayEquals(data, IOUtils.readBytesFromStream(message.getContent(InputStream.class)));
    }

    @Test
    public void testDecompressedSizeAtLimit() throws Exception {
        byte[] data = new byte[1024 * 1024];
        Message message = createMessage(gzip(data));
        message.put(GZIPInInterceptor.GZIP_MAX_DECOMPRESSED_SIZE, data.length);
        new GZIPInInterceptor().handleMessage(message);
        assertEquals(data.length, IOUtils.readBytesFromStream(message.getContent(InputStream.class)).length);
    }

    @Test
    public void testDecompressedSizeExceeded() throws Exception {
        // 10 MB of zeros compresses to about 10 KB
        Message message = createMessage(gzip(new byte[10 * 1024 * 1024]));
        message.put(GZIPInInterceptor.GZIP_MAX_DECOMPRESSED_SIZE, "1048576");
        new GZIPInInterceptor().handleMessage(message);
        InputStream in = message.getContent(InputStream.class);
        assertThrows(IOException.class, () -> IOUtils.readBytesFromStream(in));
    }

    @Test
    public void testStreamClosedWhenSizeExceeded() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        InputStream body = new ByteArrayInputStream(gzip(new byte[10 * 1024 * 1024])) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        Message message = createMessage(body);
        message.put(GZIPInInterceptor.GZIP_MAX_DECOMPRESSED_SIZE, 1024L * 1024);
        new GZIPInInterceptor().handleMessage(message);
        InputStream in = message.getContent(InputStream.class);

        IOException ex = assertThrows(IOException.class, () -> IOUtils.readBytesFromStream(in));
        assertTrue(closed.get());
        // any further read reports the same error rather than reading from the closed stream
        assertEquals(ex.getMessage(), assertThrows(IOException.class, () -> in.read()).getMessage());
        assertEquals(ex.getMessage(), assertThrows(IOException.class, () -> in.read(new byte[16])).getMessage());
        assertEquals(ex.getMessage(), assertThrows(IOException.class, () -> in.skip(16)).getMessage());
    }

    @Test
    public void testDecompressedSizeExceededSingleByteReads() throws Exception {
        Message message = createMessage(gzip(new byte[2048]));
        message.put(GZIPInInterceptor.GZIP_MAX_DECOMPRESSED_SIZE, 1024L);
        new GZIPInInterceptor().handleMessage(message);
        InputStream in = message.getContent(InputStream.class);
        assertThrows(IOException.class, () -> {
            while (in.read() != -1) {
                // keep reading
            }
        });
    }

    @Test
    public void testDecompressedSizeUnlimited() throws Exception {
        byte[] data = new byte[2 * 1024 * 1024];
        Message message = createMessage(gzip(data));
        message.put(GZIPInInterceptor.GZIP_MAX_DECOMPRESSED_SIZE, -1);
        new GZIPInInterceptor().handleMessage(message);
        assertEquals(data.length, IOUtils.readBytesFromStream(message.getContent(InputStream.class)).length);
    }

    @Test
    public void testDefaultMaxDecompressedSize() {
        assertEquals(100L * 1024 * 1024, GZIPInInterceptor.DEFAULT_GZIP_MAX_DECOMPRESSED_SIZE);
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bos)) {
            out.write(data);
        }
        return bos.toByteArray();
    }

    private static Message createMessage(byte[] body) {
        return createMessage(new ByteArrayInputStream(body));
    }

    private static Message createMessage(InputStream body) {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);
        message.setExchange(exchange);
        message.put(Message.HTTP_REQUEST_METHOD, "POST");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("Content-Encoding", Collections.singletonList("gzip"));
        message.put(Message.PROTOCOL_HEADERS, headers);
        message.setContent(InputStream.class, body);
        return message;
    }
}
