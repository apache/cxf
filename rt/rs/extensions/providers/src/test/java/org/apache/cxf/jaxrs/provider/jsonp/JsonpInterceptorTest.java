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

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        assertEquals("myCallback();", bos.toString());

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
            assertEquals("myCallback();", bos.toString());
        } finally {
            in.setCallbackParam("_jsonp");
        }

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
        assertEquals("callback();", bos.toString());
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