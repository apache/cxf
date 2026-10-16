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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.transport.common.gzip.GZIPOutInterceptor.UseGzip.FORCE;
import static org.apache.cxf.transport.common.gzip.GZIPOutInterceptor.UseGzip.YES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Test for the parsing of Accept-Encoding by the GZIPOutInterceptor. For
 * Accept-Encoding values that enable gzip we expect an extra interceptor to be
 * added to the out message, and the {@link GZIPOutInterceptor#USE_GZIP_KEY} to
 * be set correctly. For Accept-Encoding values that do not enable gzip the
 * interceptor should not be added.
 */
public class GZIPAcceptEncodingTest {

    private GZIPOutInterceptor interceptor;
    private Message inMessage;
    private Message outMessage;
    private InterceptorChain outInterceptors;

    @Before
    public void setUp() throws Exception {
        interceptor = new GZIPOutInterceptor();
        inMessage = new MessageImpl();
        outMessage = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(inMessage);
        inMessage.setExchange(exchange);
        inMessage.setContent(InputStream.class, new ByteArrayInputStream(new byte[0]));
        exchange.setOutMessage(outMessage);
        outMessage.setExchange(exchange);
        outMessage.setContent(OutputStream.class, new ByteArrayOutputStream());
        outInterceptors = mock(InterceptorChain.class);
        outMessage.setInterceptorChain(outInterceptors);
    }

    @Test
    public void testNoAcceptEncoding() throws Exception {
        interceptor.handleMessage(outMessage);
    }

    @Test
    public void testAcceptGzip() throws Exception {
        singleTest("gzip", YES, "gzip");
    }

    @Test
    public void testAcceptXGzip() throws Exception {
        singleTest("x-gzip, x-compress", YES, "x-gzip");
    }

    @Test
    public void testAcceptStar() throws Exception {
        singleTest("*", YES, "gzip");
    }

    @Test
    public void testAcceptOnlyGzip() throws Exception {
        singleTest("gzip, identity; q=0", FORCE, "gzip");
    }

    @Test
    public void testOnlyIdentitySupported() throws Exception {
        singleTest("deflate", null, null);
    }

    @Test
    public void testGzipExplicitlyDisabled() throws Exception {
        singleTest("gzip; q=0.00", null, null);
    }

    @Test(expected = Fault.class)
    public void testNoValidEncodings() throws Exception {
        setAcceptEncoding("*;q=0, deflate;q=0.5");
        interceptor.handleMessage(outMessage);
    }

    @Test
    public void testVaryWhenNotCompressed() throws Exception {
        // the response is not compressed, but would have been for a client accepting gzip
        setAcceptEncoding("identity");
        writeResponse(2048);
        assertNull(getOutHeaders().get("Content-Encoding"));
        assertVary("Accept-Encoding");
    }

    @Test
    public void testVaryWithoutAcceptEncoding() throws Exception {
        setRequestHeaders(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        writeResponse(2048);
        assertVary("Accept-Encoding");
    }

    @Test
    public void testVaryWithoutBody() throws Exception {
        setAcceptEncoding("identity");
        interceptor.handleMessage(outMessage);
        outMessage.getContent(OutputStream.class).close();
        assertVary("Accept-Encoding");
        assertNull(getOutHeaders().get("Content-Encoding"));
    }

    @Test
    public void testVaryWithoutBodyOrAcceptEncoding() throws Exception {
        setRequestHeaders(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        interceptor.handleMessage(outMessage);
        // JAX-RS can replace the response headers after this interceptor runs.
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("Vary", Collections.singletonList("Accept"));
        outMessage.put(Message.PROTOCOL_HEADERS, headers);
        outMessage.getContent(OutputStream.class).close();
        assertVary("Accept", "Accept-Encoding");
        assertNull(getOutHeaders().get("Content-Encoding"));
    }

    @Test
    public void testVaryWhenCompressed() throws Exception {
        setAcceptEncoding("gzip");
        writeResponse(2048);
        assertEquals(Collections.singletonList("gzip"), getOutHeaders().get("Content-Encoding"));
        assertVary("Accept-Encoding");
    }

    @Test
    public void testVaryBelowThreshold() throws Exception {
        setAcceptEncoding("gzip");
        writeResponse(16);
        assertNull(getOutHeaders().get("Content-Encoding"));
        assertVary("Accept-Encoding");
    }

    @Test
    public void testVaryAppended() throws Exception {
        Map<String, List<String>> outHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        outHeaders.put("Vary", Collections.singletonList("Accept"));
        outMessage.put(Message.PROTOCOL_HEADERS, outHeaders);
        setAcceptEncoding("identity");
        writeResponse(2048);
        assertVary("Accept", "Accept-Encoding");
    }

    @Test
    public void testNoVaryForUnsupportedContentType() throws Exception {
        // a response that is never compressed does not depend on Accept-Encoding
        interceptor.setSupportedPayloadContentTypes(Collections.singleton("text/xml"));
        outMessage.put(Message.CONTENT_TYPE, "image/png");
        setAcceptEncoding("gzip");
        writeResponse(2048);
        assertNull(getOutHeaders());
    }

    @Test
    public void testNoVaryForRequest() throws Exception {
        outMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        outMessage.put(GZIPOutInterceptor.USE_GZIP_KEY, YES);
        writeResponse(2048);
        assertEquals(Collections.singletonList("gzip"), getOutHeaders().get("Content-Encoding"));
        assertNull(getOutHeaders().get("Vary"));
    }

    private void writeResponse(int size) throws Exception {
        interceptor.handleMessage(outMessage);
        try (OutputStream os = outMessage.getContent(OutputStream.class)) {
            os.write(new byte[size]);
        }
    }

    private void assertVary(String... expected) {
        Map<String, List<String>> headers = getOutHeaders();
        assertNotNull("Response headers should include Vary", headers);
        assertEquals(Arrays.asList(expected), headers.get("Vary"));
    }

    private Map<String, List<String>> getOutHeaders() {
        return CastUtils.cast((Map<?, ?>)outMessage.get(Message.PROTOCOL_HEADERS));
    }

    private void singleTest(String encoding,
                            GZIPOutInterceptor.UseGzip expectedUseGzip, String expectedGzipEncoding)
        throws Exception {

        setAcceptEncoding(encoding);
        interceptor.handleMessage(outMessage);
        assertSame("Wrong value of " + GZIPOutInterceptor.USE_GZIP_KEY, expectedUseGzip, outMessage
            .get(GZIPOutInterceptor.USE_GZIP_KEY));
        assertEquals("Wrong value of " + GZIPOutInterceptor.GZIP_ENCODING_KEY, expectedGzipEncoding,
                     outMessage.get(GZIPOutInterceptor.GZIP_ENCODING_KEY));
    }

    private void setAcceptEncoding(String enc) {
        Map<String, List<String>> protocolHeaders
            = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        protocolHeaders.put(HttpHeaderHelper.getHeaderKey(HttpHeaderHelper.ACCEPT_ENCODING), Collections
            .singletonList(enc));
        setRequestHeaders(protocolHeaders);
    }

    private void setRequestHeaders(Map<String, List<String>> protocolHeaders) {
        inMessage.put(Message.PROTOCOL_HEADERS, protocolHeaders);
    }
}
