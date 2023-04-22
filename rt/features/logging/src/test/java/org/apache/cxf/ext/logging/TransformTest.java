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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransformTest {

    private static final String ORIG_LOGGING_CONTENT = "myOrigString";
    private static final String TRANSFORMED_LOGGING_CONTENT = "myNewString";

    private static class TransformLoggingOutInterceptor extends LoggingOutInterceptor {
        TransformLoggingOutInterceptor(final LogEventSender sender) {
            super(sender);
        }

        @Override
        protected String transform(final Message message, final String content) {
            return content.replace(ORIG_LOGGING_CONTENT, TRANSFORMED_LOGGING_CONTENT);
        }
    }

    private static class TransformLoggingInInterceptor extends LoggingInInterceptor {
        TransformLoggingInInterceptor(final LogEventSender sender) {
            super(sender);
        }

        @Override
        protected String transform(final Message message, final String content) {
            return content.replace(ORIG_LOGGING_CONTENT, TRANSFORMED_LOGGING_CONTENT);
        }
    }

    @Test
    public void transformOutboundInterceptorOutputStream() throws IOException {
        // Arrange
        Message message = new MessageImpl();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, outputStream);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEventSenderMock logEventSender = new LogEventSenderMock();
        LoggingOutInterceptor interceptor = new TransformLoggingOutInterceptor(logEventSender);

        // Act
        interceptor.handleMessage(message);
        byte[] payload = ORIG_LOGGING_CONTENT.getBytes(StandardCharsets.UTF_8);
        OutputStream out = message.getContent(OutputStream.class);
        out.write(payload);
        out.close();

        // Verify
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        assertEquals(TRANSFORMED_LOGGING_CONTENT, event.getPayload());
    }

    @Test
    public void transformOutboundInterceptorWriter() throws IOException {
        // Arrange
        Message message = new MessageImpl();
        StringWriter stringWriter = new StringWriter();
        message.setContent(Writer.class, stringWriter);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEventSenderMock logEventSender = new LogEventSenderMock();
        LoggingOutInterceptor interceptor = new TransformLoggingOutInterceptor(logEventSender);

        // Act
        interceptor.handleMessage(message);
        Writer out = message.getContent(Writer.class);
        out.write(ORIG_LOGGING_CONTENT);
        out.close();

        // Assert
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        assertEquals(TRANSFORMED_LOGGING_CONTENT, event.getPayload()); // only the first byte is read!
    }

    @Test
    public void transformInboundInterceptorInputStream() {
        // Arrange
        Message message = new MessageImpl();
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(ORIG_LOGGING_CONTENT.getBytes(StandardCharsets.UTF_8));
        message.setContent(InputStream.class, inputStream);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEventSenderMock logEventSender = new LogEventSenderMock();
        LoggingInInterceptor interceptor = new TransformLoggingInInterceptor(logEventSender);

        // Act
        Collection<PhaseInterceptor<? extends Message>> interceptors = interceptor.getAdditionalInterceptors();
        for (PhaseInterceptor intercept : interceptors) {
            intercept.handleMessage(message);
        }
        interceptor.handleMessage(message);

        // Verify
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        assertEquals(TRANSFORMED_LOGGING_CONTENT, event.getPayload()); // only the first byte is read!
    }

    @Test
    public void transformInboundInterceptorReader() {
        // Arrange
        Message message = new MessageImpl();
        StringReader stringReader = new StringReader(ORIG_LOGGING_CONTENT);
        message.setContent(Reader.class, stringReader);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEventSenderMock logEventSender = new LogEventSenderMock();
        LoggingInInterceptor interceptor = new TransformLoggingInInterceptor(logEventSender);

        // Act
        Collection<PhaseInterceptor<? extends Message>> interceptors = interceptor.getAdditionalInterceptors();
        for (PhaseInterceptor intercept : interceptors) {
            intercept.handleMessage(message);
        }
        interceptor.handleMessage(message);

        // Verify
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        assertEquals(TRANSFORMED_LOGGING_CONTENT, event.getPayload()); // only the first byte is read!
    }
}
