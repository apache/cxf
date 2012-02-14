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

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JsonpInterceptorTest extends Assert {

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
        // The callback value included in the request
        String callback = "myCallback";

        // Mock up an output stream as a strict mock. We want to verify that its
        // being written to correctly.
        ServletOutputStream out = org.easymock.EasyMock
                .createMock(ServletOutputStream.class);
        out.write((byte[]) EasyMock.anyObject());
        // the interceptors write both "myCallback(" and ")"
        org.easymock.EasyMock.expectLastCall().times(2);
        org.easymock.EasyMock.replay(out);

        // Mock up an HTTP request
        HttpServletRequest request = EasyMock
                .createNiceMock(HttpServletRequest.class);
        EasyMock.expect(
                request.getParameter(JsonpInInterceptor.CALLBACK_PARAM))
                .andReturn(callback);
        EasyMock.replay(request);

        // Mock up an HTTP response
        HttpServletResponse response = EasyMock
                .createNiceMock(HttpServletResponse.class);
        EasyMock.expect(response.getOutputStream()).andReturn(out).anyTimes();
        EasyMock.replay(response);

        // Mock up an exchange
        Exchange exchange = EasyMock.createNiceMock(Exchange.class);
        EasyMock.expect(exchange.get(JsonpInInterceptor.CALLBACK_KEY))
                .andReturn(callback).anyTimes();
        EasyMock.replay(exchange);

        // Mock up a message
        Message message = EasyMock.createNiceMock(Message.class);
        EasyMock.expect(message.get("HTTP.REQUEST")).andReturn(request)
                .anyTimes();
        EasyMock.expect(message.get("HTTP.RESPONSE")).andReturn(response)
                .anyTimes();
        EasyMock.expect(message.get(Message.CONTENT_TYPE)).andReturn(
                MediaType.APPLICATION_JSON).anyTimes();
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.replay(message);

        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);

        // Verify that the mock response stream was written to as expected
        org.easymock.EasyMock.verify(out);
    }
    
    @Test
    public void testJsonWithDefaultPadding() throws Exception {
        // Mock up an output stream as a strict mock. We want to verify that its
        // being written to correctly.
        final TestServletOutputStream out = new TestServletOutputStream();

        // Mock up an HTTP request
        HttpServletRequest request = EasyMock
                .createNiceMock(HttpServletRequest.class);
        EasyMock.expect(
                request.getParameter(JsonpInInterceptor.CALLBACK_PARAM))
                .andReturn(null);
        EasyMock.replay(request);

        // Mock up an HTTP response
        HttpServletResponse response = EasyMock
                .createNiceMock(HttpServletResponse.class);
        EasyMock.expect(response.getOutputStream()).andReturn(out).times(2);
        EasyMock.replay(response);

        // Mock up a message
        Message message = new MessageImpl();
        message.put("HTTP.REQUEST", request);
        message.put("HTTP.RESPONSE", response);
        message.put(Message.ACCEPT_CONTENT_TYPE, JsonpInInterceptor.JSONP_TYPE);

        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        
        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);

        assertEquals("callback();", out.getValue());
    }

    @Test
    public void testJsonWithoutPadding() throws Exception {
        // Mock up an output stream as a strict mock. We want to verify that its
        // being written to correctly.
        ServletOutputStream out = org.easymock.EasyMock
                .createMock(ServletOutputStream.class);
        // the interceptors write nothing, so we expect no behaviors from the
        // mock
        org.easymock.EasyMock.replay(out);

        // Mock up an HTTP request
        HttpServletRequest request = EasyMock
                .createNiceMock(HttpServletRequest.class);
        EasyMock.expect(
                request.getParameter(JsonpInInterceptor.CALLBACK_PARAM))
                .andReturn(null);
        EasyMock.replay(request);

        // Mock up an HTTP response
        HttpServletResponse response = EasyMock
                .createNiceMock(HttpServletResponse.class);
        EasyMock.expect(response.getOutputStream()).andReturn(out).anyTimes();
        EasyMock.replay(response);

        // Mock up an exchange
        Exchange exchange = EasyMock.createNiceMock(Exchange.class);
        EasyMock.expect(exchange.get(JsonpInInterceptor.CALLBACK_KEY))
                .andReturn(null).anyTimes();
        EasyMock.replay(exchange);

        // Mock up a message
        Message message = EasyMock.createNiceMock(Message.class);
        EasyMock.expect(message.get("HTTP.REQUEST")).andReturn(request)
                .anyTimes();
        EasyMock.expect(message.get("HTTP.RESPONSE")).andReturn(response)
                .anyTimes();
        EasyMock.expect(message.get(Message.CONTENT_TYPE)).andReturn(
                MediaType.APPLICATION_JSON).anyTimes();
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.replay(message);

        // Process the message
        in.handleMessage(message);
        preStream.handleMessage(message);
        postStream.handleMessage(message);

        // Verify that the mock response stream was written to as expected
        org.easymock.EasyMock.verify(out);
    }

    private static class TestServletOutputStream extends ServletOutputStream {
        private StringBuilder sb = new StringBuilder();
        
        public void write(byte[] data) throws IOException {
            sb.append(new String(data, "UTF-8"));
        }
        public String getValue() {
            return sb.toString();
        }
        public void write(int b) throws IOException {
        }
    };
}
