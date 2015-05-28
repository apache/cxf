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

package org.apache.cxf.interceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LoggingOutInterceptorTest extends Assert {

    private static final String XML_UNFORMATTED = "<Envelope><Body>Text</Body></Envelope>";
    private static final String XML_UNFORMATTED_INVALID = "<Envelope><Body>Text</Body>";
    private static final String XML_FORMATTED = "<Envelope>\n  <Body>Text</Body>\n</Envelope>";
    
    protected IMocksControl control;

    @Before
    public void setUp() throws Exception {
        control = EasyMock.createNiceControl();
    }

    @After
    public void tearDown() throws Exception {
        control.verify();
    }
    
    @Test
    public void testValidXML() throws Exception {
        validateLogging(XML_UNFORMATTED, false, XML_UNFORMATTED);
    }

    @Test
    public void testPrettyPrintValidXML() throws Exception {
        validateLogging(XML_UNFORMATTED, true, XML_FORMATTED);
    }

    @Test
    public void testPrettyPrintInvalidXML() throws Exception {
        validateLogging(XML_UNFORMATTED_INVALID, true, XML_UNFORMATTED_INVALID);
    }

    private void validateLogging(String payload, boolean prettyPrint, String loggedPayload) throws IOException {
        control.replay();
        
        // short-circuit the logging by providing a writer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        
        LoggingOutInterceptor p = new LoggingOutInterceptor(pw);
        p.setPrettyLogging(prettyPrint);
        CachedOutputStream cos = new CachedOutputStream();
        cos.write(payload.getBytes());
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.CONTENT_TYPE, "application/xml");
        Logger logger = LogUtils.getL7dLogger(this.getClass());
        LoggingOutInterceptor.LoggingCallback l = p.new LoggingCallback(logger, message, cos);
        l.onClose(cos);
        
        String str = baos.toString();
        assertTrue(str.contains(loggedPayload));
    }
    
    @Test
    public void testCachedOutputStreamThreshold() throws Exception {
        byte[] mex = "<test><threshold/></test>".getBytes();

        LoggingOutInterceptor p = new LoggingOutInterceptor();
        p.setInMemThreshold(mex.length);

        CachedOutputStream cos = handleAndGetCachedOutputStream(p);
        cos.write(mex);
        assertNull(cos.getTempFile());
        cos.write("a".getBytes());
        assertNotNull(cos.getTempFile());
    }

    private CachedOutputStream handleAndGetCachedOutputStream(LoggingOutInterceptor interceptor) {
        interceptor.setPrintWriter(new PrintWriter(new ByteArrayOutputStream()));

        Endpoint endpoint = control.createMock(Endpoint.class);
        EndpointInfo endpointInfo = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(endpointInfo).anyTimes();
        control.replay();

        Message message = new MessageImpl();
        ExchangeImpl exchange = new ExchangeImpl();
        message.setExchange(exchange);
        exchange.put(Endpoint.class, endpoint);

        message.put(Message.CONTENT_TYPE, "application/xml");
        message.setContent(OutputStream.class, new ByteArrayOutputStream());
        interceptor.handleMessage(message);
        OutputStream os = message.getContent(OutputStream.class);
        assertTrue(os instanceof CachedOutputStream);
        return (CachedOutputStream)os;
    }

}
