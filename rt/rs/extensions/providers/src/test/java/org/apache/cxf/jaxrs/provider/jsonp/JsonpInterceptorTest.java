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

package org.apache.cxf.jaxrs.provider.jsonp;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonpInterceptorTest {

    public static final String JSON = "{}";

    JsonpInInterceptor in;
    JsonpPreStreamInterceptor preStream;
    JsonpPostStreamInterceptor postStream;

    @Before
    public void setUp() throws Exception {
        // Create the interceptors
        in = new JsonpInInterceptor();
        preStream = new JsonpPreStreamInterceptor();
        postStream = new JsonpPostStreamInterceptor();
    }

    @Test
    public void testJsonWithPadding() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, JsonpInInterceptor.CALLBACK_PARAM + "=" + "myCallback");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertEquals("/**/myCallback();", bos.toString());
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS);
        assertEquals(Collections.singletonList("nosniff"), headers.get("X-Content-Type-Options"));

    }

    @Test
    public void testJsonWithPaddingCustomCallbackParam() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, "_customjsonp=myCallback");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        try {
            in.setCallbackParam("_customjsonp");
            in.handleMessage(message);
            preStream.handleMessage(message);
            postStream.handleMessage(message);
            assertEquals("/**/myCallback();", bos.toString());
        } finally {
            in.setCallbackParam("_jsonp");
        }

    }

    @Test
    public void testJsonWithoutPaddingStart() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, JsonpInInterceptor.CALLBACK_PARAM + "=" + "myCallback");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        preStream.setPaddingStart("");
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertEquals("myCallback();", bos.toString());
    }

    @Test
    public void testJsonWithoutNosniffHeader() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, JsonpInInterceptor.CALLBACK_PARAM + "=" + "myCallback");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        preStream.setAddNosniffHeader(false);
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertEquals("/**/myCallback();", bos.toString());
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS);
        assertNull(headers.get("X-Content-Type-Options"));
    }

    @Test
    public void testNullCallbackPatternRestoresDefault() throws Exception {
        in.setCallbackPattern("[a-z]+");
        in.setCallbackPattern(null);
        assertEquals(JsonpInInterceptor.DEFAULT_CALLBACK_PATTERN, in.getCallbackPattern());
    }

    @Test
    public void testJsonWithDottedCallback() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, JsonpInInterceptor.CALLBACK_PARAM + "=" + "angular.callbacks._0");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertEquals("/**/angular.callbacks._0();", bos.toString());
    }

    @Test
    public void testJsonWithCustomCallbackPattern() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, JsonpInInterceptor.CALLBACK_PARAM + "=" + "angular.callbacks._0");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        try {
            in.setCallbackPattern("[A-Za-z0-9_]{1,32}");
            in.handleMessage(message);
            preStream.handleMessage(message);
            postStream.handleMessage(message);
            assertNull(message.getExchange().get(JsonpInInterceptor.CALLBACK_KEY));
            assertEquals("", bos.toString());
        } finally {
            in.setCallbackPattern(JsonpInInterceptor.DEFAULT_CALLBACK_PATTERN);
        }
    }

    @Test
    public void testJsonWithInvalidCallback() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, JsonpInInterceptor.CALLBACK_PARAM + "=" + "alert(document.domain)//");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertNull(message.getExchange().get(JsonpInInterceptor.CALLBACK_KEY));
        assertEquals("", bos.toString());
    }

    @Test
    public void testJsonWithInvalidCallbackAndJsonpAccept() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.ACCEPT_CONTENT_TYPE, JsonpInInterceptor.JSONP_TYPE);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, JsonpInInterceptor.CALLBACK_PARAM + "=" + "alert(document.domain)//");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message: a rejected callback must not fall back to the default one
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertNull(message.getExchange().get(JsonpInInterceptor.CALLBACK_KEY));
        assertEquals("", bos.toString());
    }

    @Test
    public void testJsonWithOverlongCallback() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        message.put(Message.QUERY_STRING, JsonpInInterceptor.CALLBACK_PARAM + "=" + "a".repeat(129));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertNull(message.getExchange().get(JsonpInInterceptor.CALLBACK_KEY));
        assertEquals("", bos.toString());
    }

    @Test
    public void testJsonWithDefaultPadding() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.ACCEPT_CONTENT_TYPE, JsonpInInterceptor.JSONP_TYPE);
        message.setExchange(new ExchangeImpl());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertEquals("/**/callback();", bos.toString());
    }

    @Test
    public void testJsonWithoutPadding() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        message.setExchange(new ExchangeImpl());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.setContent(OutputStream.class, bos);

        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);
        assertEquals("", bos.toString());
    }

}