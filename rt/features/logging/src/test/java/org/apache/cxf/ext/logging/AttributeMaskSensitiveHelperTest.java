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
public class AttributeMaskSensitiveHelperTest {

    private static final String SENSITIVE_LOGGING_CONTENT_XML =
            "<user>testUser</user><password>my secret password</password>";
    private static final String MASKED_LOGGING_CONTENT_XML =
            "<user>testUser</user><password>XXX</password>";

    private static final String SENSITIVE_LOGGING_CONTENT_XML_WITH_ATTRIBUTE =
            "<user>testUser</user><password myAttribute=\"test\">my secret password</password>";
    private static final String MASKED_LOGGING_CONTENT_XML_WITH_ATTRIBUTE =
            "<user>testUser</user><password myAttribute=\"XXX\">XXX</password>";

    private static final String SENSITIVE_LOGGING_CONTENT_XML_WITH_MULTILINE =
        "<user>testUser</user><password>my \nsecret \npassword</password>";
    private static final String MASKED_LOGGING_CONTENT_XML_WITH_MULTILINE =
        "<user>testUser</user><password>XXX</password>";

    private static final String SENSITIVE_LOGGING_CONTENT_XML_WITH_WRAPPER =
        "<passwords><password>my secret password</password></passwords>";
    private static final String MASKED_LOGGING_CONTENT_XML_WITH_WITH_WRAPPER =
        "<passwords><password>XXX</password></passwords>";

    private static final String SENSITIVE_LOGGING_XML_EMPTY_TAG_REPEATED =
       "<user1><password/></user1><user2><password>VALUE</password></user2>";
    private static final String MASKED_LOGGING_XML_EMPTY_TAG_REPEATED =
        "<user1><password/></user1><user2><password>XXX</password></user2>";

    private static final String SENSITIVE_LOGGING_CONTENT_JSON =
            "\"user\":\"testUser\", \"password\": \"my secret password\"";
    private static final String MASKED_LOGGING_CONTENT_JSON =
            "\"user\":\"testUser\", \"password\": \"XXX\"";

    private static final String SENSITIVE_LOGGING_MULTIPLE_ELEMENT_XML =
        "<item><user>testUser1</user><password myAttribute=\"test\">my secret password 1</password></item>"
            + "<item><user>testUser2</user><password>my secret password 2</password></item>";
    private static final String MASKED_LOGGING_MULTIPLE_ELEMENT_XML =
        "<item><user>testUser1</user><password myAttribute=\"XXX\">XXX</password></item>"
            + "<item><user>testUser2</user><password>XXX</password></item>";

    private static final String SENSITIVE_LOGGING_CONTENT_XML_WITH_NAMESPACE =
            "<ns:user>testUser</ns:user><ns:password>my secret password</ns:password>";

    private static final String MASKED_LOGGING_CONTENT_XML_WITH_NAMESPACE =
            "<ns:user>testUser</ns:user><ns:password>XXX</ns:password>";

    private static final Set<String> SENSITIVE_ELEMENTS = new HashSet<>(Arrays.asList("password"));
    private static final Set<String> SENSITIVE_ATTRIBUTES = new HashSet<>(Arrays.asList("myAttribute"));
    private static final String APPLICATION_XML = "application/xml";
    private static final String APPLICATION_JSON = "application/json";

    private final String loggingContent;
    private final String maskedContent;
    private final String contentType;
    private LogEventSenderMock logEventSender = new LogEventSenderMock();
    public AttributeMaskSensitiveHelperTest(String loggingContent, String maskedContent, String contentType) {
        this.loggingContent = loggingContent;
        this.maskedContent = maskedContent;
        this.contentType = contentType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> primeNumbers() {
        return Arrays.asList(new Object[][] {
            {SENSITIVE_LOGGING_CONTENT_XML, MASKED_LOGGING_CONTENT_XML, APPLICATION_XML},
            {SENSITIVE_LOGGING_CONTENT_XML_WITH_ATTRIBUTE, MASKED_LOGGING_CONTENT_XML_WITH_ATTRIBUTE, APPLICATION_XML},
            {SENSITIVE_LOGGING_CONTENT_XML_WITH_MULTILINE, MASKED_LOGGING_CONTENT_XML_WITH_MULTILINE, APPLICATION_XML},
            {SENSITIVE_LOGGING_CONTENT_XML_WITH_WRAPPER, MASKED_LOGGING_CONTENT_XML_WITH_WITH_WRAPPER, APPLICATION_XML},
            {SENSITIVE_LOGGING_XML_EMPTY_TAG_REPEATED, MASKED_LOGGING_XML_EMPTY_TAG_REPEATED, APPLICATION_XML},
            {SENSITIVE_LOGGING_MULTIPLE_ELEMENT_XML, MASKED_LOGGING_MULTIPLE_ELEMENT_XML, APPLICATION_XML},
            {SENSITIVE_LOGGING_CONTENT_XML_WITH_NAMESPACE, MASKED_LOGGING_CONTENT_XML_WITH_NAMESPACE, APPLICATION_XML},
            {SENSITIVE_LOGGING_CONTENT_JSON, MASKED_LOGGING_CONTENT_JSON, APPLICATION_JSON}
        });
    }

    @Test
    public void shouldReplaceSensitiveDataInWithAdd() {
        // Arrange
        final LoggingInInterceptor inInterceptor = new LoggingInInterceptor(logEventSender);
        inInterceptor.setSensitiveAttributeNames(SENSITIVE_ATTRIBUTES);
        inInterceptor.setSensitiveElementNames(SENSITIVE_ELEMENTS);
        
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
    public void shouldReplaceSensitiveDataInWithSet() {
        // Arrange
        final LoggingInInterceptor inInterceptor = new LoggingInInterceptor(logEventSender);
        inInterceptor.setSensitiveAttributeNames(SENSITIVE_ATTRIBUTES);
        inInterceptor.setSensitiveElementNames(SENSITIVE_ELEMENTS);
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
    public void shouldReplaceSensitiveDataOutWithAdd() throws IOException {
        // Arrange
        final LoggingOutInterceptor outInterceptor = new LoggingOutInterceptor(logEventSender);
        outInterceptor.setSensitiveAttributeNames(SENSITIVE_ATTRIBUTES);
        outInterceptor.setSensitiveElementNames(SENSITIVE_ELEMENTS);

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
    public void shouldReplaceSensitiveDataOutWithSet() throws IOException {
        // Arrange
        final LoggingOutInterceptor outInterceptor = new LoggingOutInterceptor(logEventSender);
        outInterceptor.setSensitiveAttributeNames(SENSITIVE_ATTRIBUTES);
        outInterceptor.setSensitiveElementNames(SENSITIVE_ELEMENTS);

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
