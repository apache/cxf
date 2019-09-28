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
package org.apache.cxf.transport.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class HeadersTest {

    @Test
    public void setHeadersTest() throws Exception {
        IMocksControl control = EasyMock.createNiceControl();
        String[] headerNames = {"Content-Type", "authorization", "soapAction"};
        String[] headerValues = {"text/xml", "Basic Zm9vOmJhcg==", "foo"};
        Map<String, List<String>> inmap = new HashMap<>();
        for (int i = 0; i < headerNames.length; i++) {
            inmap.put(headerNames[i], Arrays.asList(headerValues[i]));
        }

        HttpServletRequest req = control.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getHeaderNames()).andReturn(Collections.enumeration(inmap.keySet()));
        for (int i = 0; i < headerNames.length; i++) {
            EasyMock.expect(req.getHeaders(headerNames[i])).
                andReturn(Collections.enumeration(inmap.get(headerNames[i])));
        }
        EasyMock.expect(req.getContentType()).andReturn(headerValues[0]).anyTimes();

        control.replay();

        Message message = new MessageImpl();
        message.put(AbstractHTTPDestination.HTTP_REQUEST, req);

        Headers headers = new Headers(message);
        headers.copyFromRequest(req);

        Map<String, List<String>> protocolHeaders =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        assertTrue("unexpected size", protocolHeaders.size() == headerNames.length);

        assertEquals("unexpected header", protocolHeaders.get("Content-Type").get(0), headerValues[0]);
        assertEquals("unexpected header", protocolHeaders.get("content-type").get(0), headerValues[0]);
        assertEquals("unexpected header", protocolHeaders.get("CONTENT-TYPE").get(0), headerValues[0]);
        assertEquals("unexpected header", protocolHeaders.get("content-TYPE").get(0), headerValues[0]);

        assertEquals("unexpected header", protocolHeaders.get("Authorization").get(0), headerValues[1]);
        assertEquals("unexpected header", protocolHeaders.get("authorization").get(0), headerValues[1]);
        assertEquals("unexpected header", protocolHeaders.get("AUTHORIZATION").get(0), headerValues[1]);
        assertEquals("unexpected header", protocolHeaders.get("authoriZATION").get(0), headerValues[1]);

        assertEquals("unexpected header", protocolHeaders.get("SOAPAction").get(0), headerValues[2]);
        assertEquals("unexpected header", protocolHeaders.get("soapaction").get(0), headerValues[2]);
        assertEquals("unexpected header", protocolHeaders.get("SOAPACTION").get(0), headerValues[2]);
        assertEquals("unexpected header", protocolHeaders.get("soapAction").get(0), headerValues[2]);

        control.verify();
    }

    @Test
    public void sensitiveHeadersTest() {
        Map<String, List<Object>> headerMap = new HashMap<>();
        headerMap.put("Authorization", Arrays.asList("FAIL"));
        headerMap.put("Proxy-Authorization", Arrays.asList("FAIL"));
        headerMap.put("Content-Type", Arrays.asList("application/xml"));
        headerMap.put("Accept", Arrays.asList("text/plain"));

        String loggedString = Headers.toString(headerMap, false);
        assertFalse("The value of a sensitive header could be logged: " + loggedString, loggedString.contains("FAIL"));
        assertTrue("The value of a non-sensitive header would not be logged: " + loggedString,
                   loggedString.contains("application/xml") && loggedString.contains("text/plain"));
        assertTrue("Expected header keys were not logged: " + loggedString,
                   loggedString.contains("Authorization") && loggedString.contains("Proxy-Authorization")
                   && loggedString.contains("Accept") && loggedString.contains("Content-Type"));
    }

    @Test
    public void logProtocolHeadersTest() {
        Map<String, List<Object>> headerMap = new HashMap<>();
        headerMap.put("Normal-Header", Arrays.asList("normal"));
        headerMap.put("Multivalue-Header", Arrays.asList("first", "second"));
        headerMap.put("Authorization", Arrays.asList("myPassword"));
        headerMap.put("Null-Header", Arrays.asList((String)null));

        //Set up test logger
        Logger logger = Logger.getAnonymousLogger();
        // remove all "normal" handlers and just use our custom handler for testing
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                String msg = record.getMessage();
                if (msg.startsWith("Normal-Header")) {
                    assertTrue("Unexpected output for normal header - expected Normal-Header: normal, received " + msg,
                               "Normal-Header: normal".equals(msg));
                } else if (msg.startsWith("Multivalue-Header")) {
                    assertTrue("Unexpected output for multi-value header - expected Multivalue-Header: first or "
                        + "Multivalue-Header: second, received: " + msg,
                        "Multivalue-Header: first".equals(msg) || "Multivalue-Header: second".equals(msg));
                } else if (msg.startsWith("Authorization")) {
                    assertTrue("Unexpected output for sensitive header - expected Authorization: ***, received " + msg,
                               "Authorization: ***".equals(msg));
                } else if (msg.startsWith("Null-Header")) {
                    assertTrue("Unexpected output for null header - expected Null-Header: <null>, received " + msg,
                               "Null-Header: <null>".equals(msg));
                } else {
                    fail("Unexpected header logged: " + msg);
                }

            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() throws SecurityException {
                // no-op
            } });

        Headers.logProtocolHeaders(logger, Level.INFO, headerMap, false);
    }

    @Test
    public void nullContentTypeTest() {
        IMocksControl control = EasyMock.createNiceControl();

        Message message = new MessageImpl();

        // first check - content-type==null in message, nothing specified in request
        // expect that determineContentType will return the default value of text/xml
        message.put(Message.CONTENT_TYPE, null);
        Headers headers = new Headers(message);
        assertEquals("Unexpected content-type determined - expected text/xml", "text/xml",
                     headers.determineContentType());

        // second check - null specified in request, valid content-type specified in message
        // expect that determineContentType returns the content-type specified in the message
        HttpServletRequest req = control.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getHeaderNames()).andReturn(Collections.emptyEnumeration());
        EasyMock.expect(req.getContentType()).andReturn(null).anyTimes();
        control.replay();
        message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, "application/json");
        headers = new Headers(message);
        headers.copyFromRequest(req);
        assertEquals("Unexpected content-type determined - expected application/json", "application/json",
                     headers.determineContentType());

        control.verify();

        // third check - content-type==null in message, null in request
        // expect that determineContentType returns the default value of text/xml
        control = EasyMock.createNiceControl();
        req = control.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getHeaderNames()).andReturn(Collections.emptyEnumeration());
        EasyMock.expect(req.getContentType()).andReturn(null).anyTimes();
        control.replay();
        message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, null);
        headers = new Headers(message);
        headers.copyFromRequest(req);
        assertEquals("Unexpected content-type determined - expected text/xml", "text/xml",
                     headers.determineContentType());

        control.verify();
    }
    
    @Test
    public void httpLanguage() {
        Locale locale = new Locale("en", "US");
        assertEquals("en-US", Headers.toHttpLanguage(locale));

        locale = new Locale("de");
        assertEquals("de", Headers.toHttpLanguage(locale));

        locale = new Locale("aa", "ZZ");
        assertEquals("aa-ZZ", Headers.toHttpLanguage(locale));

        locale = new Locale("es", "");
        assertEquals("es", Headers.toHttpLanguage(locale));
    }
}