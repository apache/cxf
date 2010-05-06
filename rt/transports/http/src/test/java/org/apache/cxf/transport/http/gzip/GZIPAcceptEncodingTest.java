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
package org.apache.cxf.transport.http.gzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.transport.http.gzip.GZIPOutInterceptor.UseGzip.*;

/**
 * Test for the parsing of Accept-Encoding by the GZIPOutInterceptor. For
 * Accept-Encoding values that enable gzip we expect an extra interceptor to be
 * added to the out message, and the {@link GZIPOutInterceptor#USE_GZIP_KEY} to
 * be set correctly. For Accept-Encoding values that do not enable gzip the
 * interceptor should not be added.
 */
public class GZIPAcceptEncodingTest extends Assert {

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
        outInterceptors = EasyMock.createMock(InterceptorChain.class);
        outMessage.setInterceptorChain(outInterceptors);
    }

    @Test
    public void testNoAcceptEncoding() throws Exception {
        EasyMock.replay(outInterceptors);
        interceptor.handleMessage(outMessage);
    }

    @Test
    public void testAcceptGzip() throws Exception {
        singleTest("gzip", true, YES, "gzip");
    }

    @Test
    public void testAcceptXGzip() throws Exception {
        singleTest("x-gzip, x-compress", true, YES, "x-gzip");
    }

    @Test
    public void testAcceptStar() throws Exception {
        singleTest("*", true, YES, "gzip");
    }

    @Test
    public void testAcceptOnlyGzip() throws Exception {
        singleTest("gzip, identity; q=0", true, FORCE, "gzip");
    }

    @Test
    public void testOnlyIdentitySupported() throws Exception {
        singleTest("deflate", false, null, null);
    }

    @Test
    public void testGzipExplicitlyDisabled() throws Exception {
        singleTest("gzip; q=0.00", false, null, null);
    }

    @Test(expected = Fault.class)
    public void testNoValidEncodings() throws Exception {
        EasyMock.replay();
        setAcceptEncoding("*;q=0, deflate;q=0.5");
        interceptor.handleMessage(outMessage);
    }

    private void singleTest(String encoding, boolean expectEndingInterceptor,
                            GZIPOutInterceptor.UseGzip expectedUseGzip, String expectedGzipEncoding)
        throws Exception {

        EasyMock.replay(outInterceptors);
        setAcceptEncoding(encoding);
        interceptor.handleMessage(outMessage);
        assertSame("Wrong value of " + GZIPOutInterceptor.USE_GZIP_KEY, expectedUseGzip, outMessage
            .get(GZIPOutInterceptor.USE_GZIP_KEY));
        assertEquals("Wrong value of " + GZIPOutInterceptor.GZIP_ENCODING_KEY, expectedGzipEncoding,
                     outMessage.get(GZIPOutInterceptor.GZIP_ENCODING_KEY));
    }

    private void setAcceptEncoding(String enc) {
        Map<String, List<String>> protocolHeaders = new HashMap<String, List<String>>();
        protocolHeaders.put(HttpHeaderHelper.getHeaderKey(HttpHeaderHelper.ACCEPT_ENCODING), Collections
            .singletonList(enc));
        inMessage.put(Message.PROTOCOL_HEADERS, protocolHeaders);
    }
}
