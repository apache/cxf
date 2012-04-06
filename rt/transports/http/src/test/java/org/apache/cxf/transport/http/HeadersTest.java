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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class HeadersTest extends Assert {
    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void setHeadersTest() throws Exception {
        String[] headerNames = {"Content-Type", "authorization", "soapAction"};
        String[] headerValues = {"text/xml", "Basic Zm9vOmJhcg==", "foo"};
        Map<String, List<String>> inmap = new HashMap<String, List<String>>();
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
            CastUtils.cast((Map)message.get(Message.PROTOCOL_HEADERS));
        
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
        
    }
}
