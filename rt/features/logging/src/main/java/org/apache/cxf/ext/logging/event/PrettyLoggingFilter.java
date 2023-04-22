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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formats the log message with indents.
 * If the log message was truncated then this filter might additionally cut incomplete tags
 */
public class PrettyLoggingFilter implements LogEventSender {
    private static final Logger LOG = LoggerFactory.getLogger(PrettyLoggingFilter.class);
    private LogEventSender next;
    private boolean prettyLogging;

    public PrettyLoggingFilter(LogEventSender next) {
        this.next = next;
        this.prettyLogging = false;
    }

    @Override
    public void send(LogEvent event) {
        if (shouldPrettyPrint(event)) {
            event.setPayload(getPrettyMessage(event));
        }
        next.send(event);
    }

    private boolean shouldPrettyPrint(LogEvent event) {
        String contentType = event.getContentType(); 
        return prettyLogging 
            && contentType != null 
            && contentType.indexOf("xml") >= 0
            && contentType.toLowerCase().indexOf("multipart/related") < 0
            && event.getPayload() != null
            && event.getPayload().length() > 0;
    }

    /**
     * Pretty-print {@linkplain LogEvent} XML payload.
     * 
     * @param event the log event containing an XML payload which is to be pretty-printed.
     * @return pretty-printed XML or original payload in case of an unexpected exception.
     */
    
    private String getPrettyMessage(LogEvent event) {
        String payload = event.getPayload();
        StringWriter swriter = new StringWriter(estimatePrettySize(payload));
        
        XMLStreamWriter xwriter = new PrettyPrintXMLStreamWriter(StaxUtils.createXMLStreamWriter(swriter), 2);
        XMLStreamReader xreader = StaxUtils.createXMLStreamReader(new StringReader(payload));
        try {
            StaxUtils.copy(xreader, xwriter);
            xwriter.flush();
        } catch (XMLStreamException xse) {
            return getPrettyMessageAfterExceptionHandling(xse, event, payload, swriter, xwriter);
        } catch (RuntimeException ex) {
            if (!(ex.getCause() instanceof XMLStreamException)) {
                throw ex;
            }

            XMLStreamException xse = (XMLStreamException) ex.getCause();
            return getPrettyMessageAfterExceptionHandling(xse, event, payload, swriter, xwriter);
        } finally {
            close(xwriter);
            close(xreader);
        } 
        return swriter.toString();
    }

    private String getPrettyMessageAfterExceptionHandling(XMLStreamException xse,
                                                          LogEvent event,
                                                          String payload,
                                                          StringWriter swriter,
                                                          XMLStreamWriter xwriter) {
        if (!event.isTruncated()) {
            LOG.debug("Error while pretty printing cxf message, returning raw message.", xse);
            return payload;
        }

        // Expected behavior for truncated payloads - keep what is already written.
        // This might effectively result in additional truncation,
        // as the truncated XML document might result in partially parsed XML nodes,
        // for example an open start tag. As long as a truncated payload is not
        // mistaken for a non-truncated payload, we're good.
        flush(xwriter);
        return swriter.toString();
    }

    private void flush(XMLStreamWriter xwriter) {
        try {
            xwriter.flush();
        } catch (XMLStreamException xse2) {
            //ignore
        }
    }
    
    private void close(XMLStreamWriter xwriter) {
        try {
            xwriter.close();
        } catch (XMLStreamException xse2) {
            //ignore
        }
    }

    private void close(XMLStreamReader xreader) {
        try {
            xreader.close();
        } catch (XMLStreamException xse2) {
            //ignore
        }
    }

    private int estimatePrettySize(String payload) {
        return payload.length() * 2;
    }

    public void setNext(LogEventSender next) {
        this.next = next;
    }
    
    public void setPrettyLogging(boolean prettyLogging) {
        this.prettyLogging = prettyLogging;
    }

}