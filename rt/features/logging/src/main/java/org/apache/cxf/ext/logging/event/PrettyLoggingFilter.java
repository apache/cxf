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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            event.setPayload(getPrettyMessage(event.getPayload(), event.getEncoding()));
        }
        next.send(event);
    }

    private boolean shouldPrettyPrint(LogEvent event) {
        String contentType = event.getContentType(); 
        return prettyLogging 
            && contentType != null 
            && contentType.indexOf("xml") >= 0
            && contentType.toLowerCase().indexOf("multipart/related") < 0
            && event.getPayload().length() > 0;
    }

    public String getPrettyMessage(String message, String encoding) {
        StringWriter swriter = new StringWriter();
        try {
            // Using XMLStreamWriter instead of Transformer as it works with non well formed xml
            // that can occur when we set a limit and cur the rest off
            XMLStreamWriter xwriter = StaxUtils.createXMLStreamWriter(swriter);
            xwriter = new PrettyPrintXMLStreamWriter(xwriter, 2);
            encoding = encoding == null ? "UTF-8" : encoding;
            InputStream in = new ByteArrayInputStream(message.getBytes(encoding));
            try {
                StaxUtils.copy(new StreamSource(in), xwriter);
            } catch (XMLStreamException xse) {
                //ignore
            } finally {
                try {
                    xwriter.flush();
                    xwriter.close();
                } catch (XMLStreamException xse2) {
                    //ignore
                }
                in.close();
            }
        } catch (IOException e) {
            LOG.debug("Error while pretty printing cxf message, returning what we got till now.", e);
        }
        return swriter.toString();
    }

    public void setNext(LogEventSender next) {
        this.next = next;
    }
    
    public void setPrettyLogging(boolean prettyLogging) {
        this.prettyLogging = prettyLogging;
    }

}
