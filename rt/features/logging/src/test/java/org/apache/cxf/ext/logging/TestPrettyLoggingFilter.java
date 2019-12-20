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

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.PrettyLoggingFilter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 
 * Test {@linkplain PrettyLoggingFilter}} with well-formed and non-well-formed XML payloads.
 * 
 */

public class TestPrettyLoggingFilter {

    @Test
    public void testWellformedXMLMessage() {
        String message = "<parent><child>text</child></parent>";
        String expected = "<parent>\n  <child>text</child>\n</parent>\n";
        filter(message, expected, false);
    }

    @Test
    public void testInvalidXMLMessageUnexpectedEndTag() {
        String message = "<parentA><child>text</child></parentB>";
        filter(message, message, false);
    }
    
    @Test
    public void testInvalidXMLMessageMissingEndTag() {
        String message = "<parentA><child>text</child>";
        filter(message, message, false);
    }

    @Test
    public void testInvalidXMLMessageGarbageStartTag() {
        String message = "<a b c><child>text</child>";
        filter(message, message, false);
    }
    
    /**
     * In case of a truncated message we do not want the pretty print to auto close the tags 
     * giving a false impression of how to message looks like.
     */
    @Test
    public void testInvalidButTruncatedXMLMessageWithMissingEndTag() {
        String message = "<parent><child>text</child>";
        String expected = "<parent>\n  <child>text</child>";
        filter(message, expected, true);
    }

    /**
     * If truncation happens in the middle of an html entity, com.ctc.wstx.exc.WstxLazyException can be thrown.
     * This test ensures that WstxLazyException is properly handled (ignored) just like the XMLStreamException.
     * See CXF-8008.
     */
    @Test
    public void testHtmlEntityTruncated() {
        String message = "<element>a&n";
        String expected = "<element";
        filter(message, expected, true);
    }

    private void filter(String payload, String expected, boolean truncated) {
        LogEvent logEvent  = new LogEvent();
        logEvent.setPayload(payload);
        logEvent.setContentType("text/xml");
        logEvent.setTruncated(truncated);
        
        LogEventSender dummy = new LogEventSender() {
            public void send(LogEvent event) {
            }
        };
        
        PrettyLoggingFilter prettyFilter = new PrettyLoggingFilter(dummy);
        prettyFilter.setPrettyLogging(true);
        prettyFilter.send(logEvent);
        
        assertEquals(expected, logEvent.getPayload());
    }
    
}