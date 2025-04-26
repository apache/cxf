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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.ext.logging.event.DefaultLogEventMapper.MASKED_HEADER_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
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
}
