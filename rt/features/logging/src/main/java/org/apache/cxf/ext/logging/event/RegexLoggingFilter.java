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
package org.apache.cxf.ext.logging.event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes regex matches from the payload.
 * Matches the complete payload, including element tags.
 */
public class RegexLoggingFilter implements LogEventSender {
    private LogEventSender next;
    private boolean filterLogging;
    private Pattern regexPattern;
    private String replacer;

    public RegexLoggingFilter(LogEventSender next, Pattern regexPattern, String replacer) {
        this.next = next;
        this.replacer = replacer;
        this.regexPattern = regexPattern;
    }

    public RegexLoggingFilter(LogEventSender next, Pattern regexPattern) {
        this.next = next;
        this.regexPattern = regexPattern;
        this.replacer = "";
    }

    @Override
    public void send(LogEvent event) {
        if (shouldFilter(event)) {
            event.setPayload(getRegexFilteredMessage(event));
        }
        next.send(event);
    }

    private boolean shouldFilter(LogEvent event) {
        String contentType = event.getContentType();
        return filterLogging
            && regexPattern != null
            && contentType != null
            && contentType.indexOf("xml") >= 0
            && contentType.toLowerCase().indexOf("multipart/related") < 0
            && event.getPayload() != null
            && event.getPayload().length() > 0;
    }

    public String getRegexFilteredMessage(LogEvent event) {
        String payload = event.getPayload();
        Matcher regexMatcher = regexPattern.matcher(payload);

        while (regexMatcher.find()) {
            String matchString = regexMatcher.group();
            payload = payload.replace(matchString, replacer);
        }
        return payload;
    }

    public void setNext(LogEventSender next) {
        this.next = next;
    }

    public void setRegexFilterLogging(boolean filterLoggingEnabled) {
        this.filterLogging = filterLoggingEnabled;
    }
}
