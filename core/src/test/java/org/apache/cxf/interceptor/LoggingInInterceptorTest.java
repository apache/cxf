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

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
public class LoggingInInterceptorTest {
    static String encoding = "UTF-8";
    static String contentType = "text/xml";
    
    static String bufferContent = "<today><is><the><eighteenth><of><july><two><thousand><seventeen>"
            + "</seventeen></thousand></two></july></of></eighteenth></the></is></today>";
    static int bufferLength = bufferContent.getBytes().length;

    private Message message;
    private InputStream inputStream;
    private LoggingMessage loggingMessage;
    private LoggingInInterceptorAncestorTester classUnderTest;

    @Before
    public void setUp() throws Exception {
        loggingMessage = new LoggingMessage("", "");

        StringWriter sw = new StringWriter();
        sw.append("<today/>");
        message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.CONTENT_TYPE, "application/xml");
        message.setContent(Writer.class, sw);

        inputStream = mock(InputStream.class);
        when(inputStream.read(any(byte[].class), anyInt(), anyInt()))
                .then(invocation -> {
                    System.arraycopy(bufferContent.getBytes(), 0,
                            invocation.getArgument(0), 0,
                            bufferLength);
                    return bufferLength;
                })
                .thenReturn(-1);
    }

    @After
    public void tearDown() throws Exception {
        verify(inputStream, atLeastOnce()).read(any(byte[].class), anyInt(), anyInt());
    }

    @Test
    public void testLogInputStreamInLimit() throws Exception {
        //arrange
        classUnderTest = new LoggingInInterceptorAncestorTester(4098);
        //act
        classUnderTest.testLogInputStream(message, inputStream, loggingMessage, encoding, contentType);
        //assert
        assertFalse("The truncated status should be set to false",
                classUnderTest.isTruncated());
    }

    @Test
    public void testLogInputStreamOffLimit() throws Exception {
        //arrange
        classUnderTest = new LoggingInInterceptorAncestorTester(16);
        //act
        classUnderTest.testLogInputStream(message, inputStream, loggingMessage, encoding, contentType);
        //assert
        assertTrue("The truncated status should be set to true",
                classUnderTest.isTruncated());
    }

    class LoggingInInterceptorAncestorTester extends LoggingInInterceptor {
        private boolean truncated;

        LoggingInInterceptorAncestorTester(int limit) {
            super(limit);
        }

        public boolean isTruncated() {
            return truncated;
        }

        public void testLogInputStream(Message logMessage,
                                       InputStream is,
                                       LoggingMessage buffer,
                                       String contentEncoding,
                                       String logContentType) {
            this.logInputStream(logMessage, is, buffer, contentEncoding, logContentType);
        }

        @Override
        protected void writePayload(StringBuilder builder,
                                    CachedOutputStream cos,
                                    String contentEncoding,
                                    String logContentType,
                                    boolean truncatedStatus) {
            this.truncated = truncatedStatus;
        }
    }
}
