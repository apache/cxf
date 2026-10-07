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

package org.apache.cxf.ext.logging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.DelayedCachedOutputStreamCleaner;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThrows;

public class LoggingOutInterceptorTest {
    private Bus bus;
    private DelayedCachedOutputStreamCleaner cleaner;
    private TestEventSender sender;
    private LoggingOutInterceptor interceptor; 
    private Message message;
    
    @Before
    public void setUp() {
        bus = BusFactory.getDefaultBus(true);
        cleaner = bus.getExtension(DelayedCachedOutputStreamCleaner.class);
        sender = new TestEventSender();
        interceptor = new LoggingOutInterceptor(sender);
        message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
    }
    
    @After
    public void tearDown() {
        assertThat(cleaner.size(), equalTo(0));
    }

    @Test
    public void shouldLogMultipartPayload() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length);
        cached.close();
        os.close();

        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }
    
    @Test
    public void shouldLogMultipartPayloadOnClean() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length);
        os.close();

        // We did not close the cached stream, should be subject of cleanup
        assertThat(cleaner.size(), equalTo(1));

        cleaner.forceClean();
        assertThat(cleaner.size(), equalTo(0));

        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }

    @Test
    public void shouldLogMultipartPayloadCachedOutputStream() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final CachedOutputStream os = new CachedOutputStream();
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length);
        cached.close();
        os.close();

        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }

    @Test
    public void shouldLogMultipartPayloadWithExceptionOnClose() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new ByteArrayOutputStream() {
            public void close() throws IOException {
                throw new IOException("Simulated");
            }
        };
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length);
        assertThrows(IOException.class, () -> cached.close());

        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }
    
    @Test
    public void shouldLogMultipartPayloadWithExceptionOnFlush() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new ByteArrayOutputStream() {
            public void flush() throws IOException {
                throw new IOException("Simulated");
            }
        };
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length);
        assertThrows(IOException.class, () -> cached.flush());
        cached.close();

        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }
    
    @Test
    public void shouldLogMultipartPayloadClosedTwice() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length);

        cached.close();
        cached.close();

        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }
    
    @Test
    public void shouldLogMultipartPayloadWithExceptionOnWrite() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new ByteArrayOutputStream() {
            @Override
            public synchronized void write(byte[] b, int off, int len) {
                if (len == 1) {
                    throw new UncheckedIOException(new IOException("Simulated"));
                } else {
                    super.write(bytes, off, len);
                }
            }
        };
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length - 1);
        assertThrows(UncheckedIOException.class, () -> cached.write(bytes, bytes.length - 1, 1));

        // We did not close the cached stream, should be subject of cleanup
        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        buf.setLength(buf.length() - 1);
        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }

    @Test
    public void shouldDeleteTempFileWhenWrittenAfterClose() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length);
        cached.close();
        assertThat(sender.getEvents(), hasSize(1));
        assertThat(cleaner.size(), equalTo(0));

        // A late writer still holding the old stream reference, with a flow-through stream that
        // accepts writes after close: it must not be cached (and spilled to a new temp file) again
        cached.write(bytes, 0, bytes.length);
        assertThat(((ByteArrayOutputStream) os).size(), equalTo(2 * bytes.length));
        final File tempFile = ((CachedOutputStream) cached).getTempFile();

        cleaner.forceClean();
        assertThat(cleaner.size(), equalTo(0));
        assertThat(sender.getEvents(), hasSize(1));
        assertThat("temp file leaked: " + tempFile, tempFile != null && tempFile.exists(), equalTo(false));
    }

    @Test
    public void shouldDeleteTempFileWhenWrittenAfterFailedWrite() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder buf = content();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final AtomicBoolean failNextWrite = new AtomicBoolean();
        final OutputStream os = new ByteArrayOutputStream() {
            @Override
            public synchronized void write(byte[] b, int off, int len) {
                if (failNextWrite.compareAndSet(true, false)) {
                    throw new UncheckedIOException(new IOException("Simulated"));
                }
                super.write(b, off, len);
            }
        };
        message.setContent(OutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.setInMemThreshold(1);
        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);

        final OutputStream cached = message.getContent(OutputStream.class);
        cached.write(bytes, 0, bytes.length);
        failNextWrite.set(true);
        assertThrows(UncheckedIOException.class, () -> cached.write(bytes, 0, bytes.length));
        assertThat(sender.getEvents(), hasSize(1));
        assertThat(cleaner.size(), equalTo(0));

        // e.g. the fault chain (SoapOutEndingInterceptor) writing the closing tags through the
        // XMLStreamWriter that still wraps this stream, with a flow-through stream that accepts writes
        // after close (as Tomcat 10.1 does): it must not be cached (and spilled to a new temp file) again
        cached.write(bytes, 0, bytes.length);
        assertThat(((ByteArrayOutputStream) os).size(), equalTo(2 * bytes.length));
        final File tempFile = ((CachedOutputStream) cached).getTempFile();

        cleaner.forceClean();
        assertThat(cleaner.size(), equalTo(0));
        assertThat(sender.getEvents(), hasSize(1));
        assertThat("temp file leaked: " + tempFile, tempFile != null && tempFile.exists(), equalTo(false));
    }

    private static StringBuilder content() {
        StringBuilder buf = new StringBuilder(512);
        buf.append("------=_Part_0_2180223.1203118300920\n");
        buf.append("Content-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\"\n");
        buf.append("Content-Transfer-Encoding: 8bit\n");
        buf.append("Content-ID: <soap.xml@xfire.codehaus.org>\n");
        buf.append('\n');
        buf.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                   + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                   + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                   + "<soap:Body><getNextMessage xmlns=\"http://foo.bar\" /></soap:Body>"
                   + "</soap:Envelope>\n");
        buf.append("------=_Part_0_2180223.1203118300920--\n");
        return buf;
    }
}
