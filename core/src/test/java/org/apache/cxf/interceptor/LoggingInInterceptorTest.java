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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LoggingInInterceptorTest extends Assert {

    private static final String XML_UNFORMATTED = "<Envelope><Body>Text</Body></Envelope>";
    private static final String XML_FORMATTED = "<Envelope>\n  <Body>Text</Body>\n</Envelope>";
    
    /** A number of characters for pretty-printing to take effect" */
    private static final int XML_FORMATTING_RANGE = XML_FORMATTED.indexOf("Text");
    // invalid XMLs
    private static final String XML_INVALID_MISSING_END_ELEMENT = "<Envelope><Body>Text</Body>";
    private static final String XML_INVALID_START_ELEMENT = "<E n v e l o p e><Body>Text</Body></Envelope>";
    private static final String XML_INVALID_TRAILING_CHARACTERS = "<Envelope><Body>Text</Body></Envelope>tail";

    protected IMocksControl control;

    @Before
    public void setUp() throws Exception {
        control = EasyMock.createNiceControl();
    }

    @After
    public void tearDown() throws Exception {
        control.verify();
    }
    
    @Test
    public void testValidXML() throws Exception {
        validateLogging(XML_UNFORMATTED, false, XML_UNFORMATTED, -1);
    }

    @Test
    public void testValidXMLWithLimit() throws Exception {
        validateLogging(XML_UNFORMATTED, false, 
                XML_UNFORMATTED.substring(0, XML_FORMATTING_RANGE), XML_FORMATTING_RANGE);
    }

    @Test
    public void testPrettyPrintValidXML() throws Exception {
        validateLogging(XML_UNFORMATTED, true, XML_FORMATTED, -1);
    }

    @Test
    public void testPrettyPrintValidXMLWithLimit() throws Exception {
        validateLogging(XML_UNFORMATTED, true, 
                XML_UNFORMATTED.substring(0, XML_FORMATTING_RANGE), XML_FORMATTING_RANGE);
    }

    @Test
    public void testPrettyPrintInvalidXML1() throws Exception {
        validateLogging(XML_INVALID_MISSING_END_ELEMENT, true, XML_INVALID_MISSING_END_ELEMENT, -1);
    }

    @Test
    public void testPrettyPrintInvalidXML2() throws Exception {
        validateLogging(XML_INVALID_START_ELEMENT, true, XML_INVALID_START_ELEMENT, -1);
    }

    @Test
    public void testPrettyPrintInvalidXML3() throws Exception {
        validateLogging(XML_INVALID_TRAILING_CHARACTERS, true, XML_INVALID_TRAILING_CHARACTERS, -1);
    }
    
    @Test
    public void testPrettyPrintInvalidXMLWithLimit() throws Exception {
        validateLogging(XML_INVALID_MISSING_END_ELEMENT, true,
                XML_INVALID_MISSING_END_ELEMENT.substring(0, XML_FORMATTING_RANGE), XML_FORMATTING_RANGE);
    }


    private void validateLogging(String payload, boolean prettyPrint,
            String loggedPayload, int limit) throws IOException {
        control.replay();

        // short-circuit the logging by providing a writer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);

        LoggingInInterceptor interceptor = new LoggingInInterceptor(pw);
        interceptor.setPrettyLogging(prettyPrint);
        interceptor.setLimit(limit);
        
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.CONTENT_TYPE, "application/xml");
        message.setContent(InputStream.class, new ByteArrayInputStream(payload.getBytes()));

        interceptor.handleMessage(message);
        
        String str = baos.toString();
        
        assertTrue(loggedPayload, str.contains(loggedPayload));
    }
    

}
