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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.ext.logging.event.DefaultLogEventMapper.MASKED_HEADER_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

public class LoggingInInterceptorTest {
    private static final String TEST_HEADER_VALUE = "TestValue";
    private static final String TEST_HEADER_NAME = "TestHeader";

    private TestEventSender sender;
    private LoggingInInterceptor interceptor; 
    private Message message;
    
    @Before
    public void setUp() {
        sender = new TestEventSender();
        interceptor = new LoggingInInterceptor(sender);
        message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
    }

    @Test
    public void testRest() {
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put(Message.REQUEST_URI, "test");

        interceptor.handleMessage(message);

        assertThat(sender.getEvents(), hasSize(1));
        LogEvent event = sender.getEvents().get(0);

        assertEquals("GET[test]", event.getOperationName());
    }

    @Test
    public void shouldMaskHeaders() {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(TEST_HEADER_NAME, Arrays.asList(TEST_HEADER_VALUE));
        final Set<String> sensitiveHeaders = new HashSet<>();
        sensitiveHeaders.add(TEST_HEADER_NAME);
        message.put(Message.PROTOCOL_HEADERS, headers);

        interceptor.setSensitiveProtocolHeaderNames(Collections.singleton(TEST_HEADER_NAME));
        interceptor.handleMessage(message);
        
        assertThat(sender.getEvents(), hasSize(1));
        LogEvent event = sender.getEvents().get(0);

        assertEquals(MASKED_HEADER_VALUE, event.getHeaders().get(TEST_HEADER_NAME));
    }
    
    @Test
    public void shouldMaskUsingCustomMaskSensitiveHelper() {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final MaskSensitiveHelper helper = new MaskSensitiveHelper() {
            public void maskHeaders(Map<String, String> headerMap, Set<String> sensitiveHeaderNames) {
                // Do nothing
            }
        };
        
        final Map<String, Object> headers = new HashMap<>();
        headers.put(TEST_HEADER_NAME, Arrays.asList(TEST_HEADER_VALUE));
        final Set<String> sensitiveHeaders = new HashSet<>();
        sensitiveHeaders.add(TEST_HEADER_NAME);
        message.put(Message.PROTOCOL_HEADERS, headers);

        interceptor.setSensitiveDataHelper(helper);
        interceptor.setSensitiveProtocolHeaderNames(Collections.singleton(TEST_HEADER_NAME));
        interceptor.handleMessage(message);
        
        assertThat(sender.getEvents(), hasSize(1));
        LogEvent event = sender.getEvents().get(0);

        assertEquals(TEST_HEADER_VALUE, event.getHeaders().get(TEST_HEADER_NAME));
    }

    @Test
    public void shouldLogMultipartPayload() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

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

        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new CachedOutputStream();
        os.write(bytes, 0, bytes.length);
        message.setContent(CachedOutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);
        
        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }

    @Test
    public void shouldLogMultipartHeadersOnly() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        final StringBuilder headers = new StringBuilder(512);
        headers.append("------=_Part_0_2180223.1203118300920\n");
        headers.append("Content-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\"\n");
        headers.append("Content-Transfer-Encoding: 8bit\n");
        headers.append("Content-ID: <soap.xml@xfire.codehaus.org>\n");

        final StringBuilder buf = new StringBuilder(512);
        buf.append(headers);
        buf.append('\n');
        buf.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                   + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                   + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                   + "<soap:Body><getNextMessage xmlns=\"http://foo.bar\" /></soap:Body>"
                   + "</soap:Envelope>\n");
        buf.append("------=_Part_0_2180223.1203118300920--\n");

        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new CachedOutputStream();
        os.write(bytes, 0, bytes.length);
        message.setContent(CachedOutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(false);
        interceptor.handleMessage(message);
        
        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload().replaceAll("\r\n", "\n"), equalToIgnoringCase(headers.toString()
            + "--- Content suppressed ---\n--\n------=_Part_0_2180223.1203118300920"));
    }

    @Test
    public void shouldLogMultipartPayloadNoHeaders() throws IOException {
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");

        StringBuilder buf = new StringBuilder(512);
        buf.append("------=_Part_0_2180223.1203118300920\n");
        buf.append('\n');
        buf.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                   + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                   + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                   + "<soap:Body><getNextMessage xmlns=\"http://foo.bar\" /></soap:Body>"
                   + "</soap:Envelope>\n");
        buf.append("------=_Part_0_2180223.1203118300920--\n");

        String ct = "multipart/related; type=\"application/xop+xml\"; "
                + "boundary=\"----=_Part_0_2180223.1203118300920\"";

        final byte[] bytes = buf.toString().getBytes(StandardCharsets.UTF_8);
        final OutputStream os = new CachedOutputStream();
        os.write(bytes, 0, bytes.length);
        message.setContent(CachedOutputStream.class, os);
        message.put(Message.CONTENT_TYPE, ct);

        interceptor.addBinaryContentMediaTypes("application/xop+xml");
        interceptor.setLogMultipart(true);
        interceptor.setLogBinary(true);
        interceptor.handleMessage(message);
        
        assertThat(sender.getEvents(), hasSize(1));
        final LogEvent event = sender.getEvents().get(0);

        assertThat(event.getPayload(), equalToIgnoringCase(buf.toString()));
    }
}
