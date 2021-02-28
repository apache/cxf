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

import java.util.regex.Pattern;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.RegexLoggingFilter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 *
 * Test {@linkplain RegexLoggingFilter}} with well-formed and non-well-formed XML payloads.
 *
 */

public class TestRegexLoggingFilter {

    @Test
    public void testWellformedXMLMessage() {
        String message = "<parent><child>aa123456aa55555aa7777777</child></parent>";
        String expected = "<parent><child>aaaa55555aa7</child></parent>";
        Pattern regex = Pattern.compile("\\d{6}");
        filter(message, expected, regex, false);
    }

    @Test
    public void testInvalidXMLMessageUnexpectedEndTag() {
        String message = "<parentA><child>text</child></parentB>";
        Pattern regex = Pattern.compile("");
        filter(message, message, regex, false);
    }

    @Test
    public void testInvalidXMLMessageMissingEndTag() {
        String message = "<parentA><child>text</child>";
        Pattern regex = Pattern.compile("");
        filter(message, message, regex, false);
    }

    @Test
    public void testInvalidXMLMessageGarbageStartTag() {
        String message = "<a b c><child>text</child>";
        Pattern regex = Pattern.compile("");
        filter(message, message, regex, false);
    }

    @Test
    public void testInvalidButTruncatedXMLMessageWithMissingEndTag() {
        String message = "<parent><child>123text12</child>";
        String expected = "<parent><child>text12</child>";
        Pattern regex = Pattern.compile("\\d{3}");
        filter(message, expected, regex, true);
    }

    @Test
    public void testHtmlEntityTruncated() {
        String message = "<element>a&n";
        String expected = "<element>";
        Pattern regex = Pattern.compile("a&n");
        filter(message, expected, regex, true);
    }

    @Test
    public void testPatternSyntaxException() {
        String message = "<element>message</element>";
        Pattern regex = Pattern.compile("[");
        filter(message, null, regex, false);
    }

    private void filter(String payload, String expected, Pattern regex, boolean truncated) {
        LogEvent logEvent  = new LogEvent();
        logEvent.setPayload(payload);
        logEvent.setContentType("text/xml");
        logEvent.setTruncated(truncated);

        LogEventSender dummy = new LogEventSender() {
            public void send(LogEvent event) {
            }
        };

        RegexLoggingFilter regexFilter = new RegexLoggingFilter(dummy, regex);
        regexFilter.setRegexLogging(true);
        regexFilter.send(logEvent);
        assertEquals(expected, logEvent.getPayload());
    }

}
