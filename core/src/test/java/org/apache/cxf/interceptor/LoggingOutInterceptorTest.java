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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
public class LoggingOutInterceptorTest {

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
    public void testFormatting() throws Exception {
        control.replay();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);

        LoggingOutInterceptor p = new LoggingOutInterceptor(pw);
        //p.setPrettyLogging(true);
        CachedOutputStream cos = new CachedOutputStream();
        String s = "<today><is><the><twenty> <second> <of> <january> <two> <thousand> <and> <nine></nine> "
            + "</and></thousand></two></january></of></second></twenty></the></is></today>";
        cos.write(s.getBytes());
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.CONTENT_TYPE, "application/xml");
        Logger logger = LogUtils.getL7dLogger(this.getClass());
        LoggingOutInterceptor.LoggingCallback l = p.new LoggingCallback(logger, message, cos);
        l.onClose(cos);
        String str = baos.toString();
        //format has changed
        assertFalse(str.matches(s));
        assertTrue(str.contains("<today>"));
    }

    @Test
    public void testPrettyLoggingWithoutEncoding() throws Exception {
        control.replay();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        
        LoggingOutInterceptor p = new LoggingOutInterceptor(pw);
        p.setPrettyLogging(true);
        CachedOutputStream cos = new CachedOutputStream();
        String s = "<today><is><the><twenty> <second> <of> <january> <two> <thousand> <and> <nine></nine> "
            + "</and></thousand></two></january></of></second></twenty></the></is></today>";
        cos.write(s.getBytes());
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.CONTENT_TYPE, "application/xml");
        Logger logger = LogUtils.getL7dLogger(this.getClass());
        LoggingOutInterceptor.LoggingCallback l = p.new LoggingCallback(logger, message, cos);
        l.onClose(cos);
        String str = baos.toString();
        //format has changed
        assertFalse(str.matches(s));
        assertTrue(str.contains("<today>"));
    }

    @Test
    public void testPrettyLoggingWithEncoding() throws Exception {
        control.replay();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        
        LoggingOutInterceptor p = new LoggingOutInterceptor(pw);
        p.setPrettyLogging(true);
        CachedOutputStream cos = new CachedOutputStream();
        String s = "<today><is><the><twenty> <second> <of> <january> <two> <thousand> <and> <nine></nine> "
            + "</and></thousand></two></january></of></second></twenty></the></is></today>";
        cos.write(s.getBytes());
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.CONTENT_TYPE, "application/xml");
        message.put(Message.ENCODING, "UTF-8");
        Logger logger = LogUtils.getL7dLogger(this.getClass());
        LoggingOutInterceptor.LoggingCallback l = p.new LoggingCallback(logger, message, cos);
        l.onClose(cos);
        String str = baos.toString();
        //format has changed
        assertFalse(str.matches(s));
        assertTrue(str.contains("<today>"));

    }

    @Test
    public void testFormattingOverride() throws Exception {
        control.replay();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // create a custom logging interceptor that overrides how formatting is done
        LoggingOutInterceptor p = new CustomFormatLoggingOutInterceptor(new PrintWriter(baos));
        CachedOutputStream cos = new CachedOutputStream();
        String s = "<today><is><the><twenty> <second> <of> <january> <two> <thousand> <and> <nine></nine> "
            + "</and></thousand></two></january></of></second></twenty></the></is></today>";
        cos.write(s.getBytes());

        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.CONTENT_TYPE, "application/xml");
        Logger logger = LogUtils.getL7dLogger(this.getClass());
        LoggingOutInterceptor.LoggingCallback l = p.new LoggingCallback(logger, message, cos);
        l.onClose(cos);

        String str = baos.toString();
        assertTrue(str.contains("<tomorrow/>"));

    }

    @Test
    public void testFormattingOverrideLogWriter() throws Exception {
        // create a custom logging interceptor that overrides how formatting is done
        LoggingOutInterceptor p = new CustomFormatLoggingOutInterceptor();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        p.setPrintWriter(new PrintWriter(baos));

        StringWriter sw = new StringWriter();
        sw.append("<today/>");

        Endpoint endpoint = control.createMock(Endpoint.class);
        EndpointInfo endpointInfo = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(endpointInfo).anyTimes();
        control.replay();

        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.CONTENT_TYPE, "application/xml");
        message.setContent(Writer.class, sw);

        p.handleMessage(message);

        Writer w = message.getContent(Writer.class);
        w.close();

        String str = baos.toString();
        assertTrue(str.contains("<tomorrow/>"));
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
        BindingInfo bindingInfo = control.createMock(BindingInfo.class);
        EasyMock.expect(endpointInfo.getBinding()).andReturn(bindingInfo).anyTimes();
        EasyMock.expect(endpointInfo.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
        EasyMock.expect(bindingInfo.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
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

    private class CustomFormatLoggingOutInterceptor extends LoggingOutInterceptor {
        CustomFormatLoggingOutInterceptor() {
            super();
        }

        CustomFormatLoggingOutInterceptor(PrintWriter w) {
            super(w);
        }

        @Override
        protected String formatLoggingMessage(LoggingMessage loggingMessage) {
            loggingMessage.getPayload().append("<tomorrow/>");
            return super.formatLoggingMessage(loggingMessage);
        }

    }
}