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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class MaskSensitiveHelperTest {
    private static final String SENSITIVE_LOGGING_CONTENT_XML =
            "<user>testUser</user><password>my secret password</password>";
    private static final String MASKED_LOGGING_CONTENT_XML =
            "<user>testUser</user><password>XXX</password>";

    private static final String SENSITIVE_LOGGING_CONTENT_JSON =
            "\"user\":\"testUser\", \"password\": \"my secret password\"";
    private static final String MASKED_LOGGING_CONTENT_JSON =
            "\"user\":\"testUser\", \"password\": \"XXX\"";

    private static final Set<String> SENSITIVE_ELEMENTS = new HashSet(Arrays.asList("password"));
    private static final String APPLICATION_XML = "application/xml";
    private static final String APPLICATION_JSON = "application/json";

    private final String loggingContent;
    private final String maskedContent;
    private final String contentType;
    private LogEventSenderMock logEventSender = new LogEventSenderMock();
    public MaskSensitiveHelperTest(String loggingContent, String maskedContent, String contentType) {
        this.loggingContent = loggingContent;
        this.maskedContent = maskedContent;
        this.contentType = contentType;
    }

    @Parameterized.Parameters
    public static Collection primeNumbers() {
        return Arrays.asList(new Object[][] {
            {SENSITIVE_LOGGING_CONTENT_XML, MASKED_LOGGING_CONTENT_XML, APPLICATION_XML},
            {SENSITIVE_LOGGING_CONTENT_JSON, MASKED_LOGGING_CONTENT_JSON, APPLICATION_JSON}
        });
    }

    @Test
    public void shouldReplaceSensitiveDataIn() {
        // Arrange
        final LoggingInInterceptor inInterceptor = new LoggingInInterceptor(logEventSender);
        inInterceptor.addSensitiveElementNames(SENSITIVE_ELEMENTS);

        final Message message = prepareInMessage();

        // Act
        Collection<PhaseInterceptor<? extends Message>> interceptors = inInterceptor.getAdditionalInterceptors();
        for (PhaseInterceptor intercept : interceptors) {
            intercept.handleMessage(message);
        }
        inInterceptor.handleMessage(message);

        // Verify
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        assertEquals(maskedContent, event.getPayload());
    }

    @Test
    public void shouldReplaceSensitiveDataOut() throws IOException {
        // Arrange
        final LoggingOutInterceptor outInterceptor = new LoggingOutInterceptor(logEventSender);
        outInterceptor.addSensitiveElementNames(SENSITIVE_ELEMENTS);

        final Message message = prepareOutMessage();

        // Act
        outInterceptor.handleMessage(message);
        byte[] payload = loggingContent.getBytes(StandardCharsets.UTF_8);
        OutputStream out = message.getContent(OutputStream.class);
        out.write(payload);
        out.close();

        // Verify
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        assertEquals(maskedContent, event.getPayload());
    }

    @Test
    public void shouldNotReplaceSensitiveDataEmptyExpression() throws IOException {
        // Arrange
        final LoggingOutInterceptor outInterceptor = new LoggingOutInterceptor(logEventSender);
        final Message message = prepareOutMessage();

        // Act
        outInterceptor.handleMessage(message);
        byte[] payload = loggingContent.getBytes(StandardCharsets.UTF_8);
        OutputStream out = message.getContent(OutputStream.class);
        out.write(payload);
        out.close();

        // Verify
        LogEvent event = logEventSender.getLogEvent();
        assertNotNull(event);
        assertEquals(loggingContent, event.getPayload());
    }

    private Message prepareInMessage() {
        Message message = new MessageImpl();
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(loggingContent.getBytes(StandardCharsets.UTF_8));
        message.put(Message.CONTENT_TYPE, contentType);
        message.setContent(InputStream.class, inputStream);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        return message;
    }

    private Message prepareOutMessage() {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, contentType);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, outputStream);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        return message;
    }

}
