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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class PrettyLoggingFilter implements LogEventSender {
    private LogEventSender next;
    private boolean prettyLogging;
    private TransformerFactory transformerFactory;

    public PrettyLoggingFilter(LogEventSender next) {
        this.next = next;
        this.prettyLogging = false;
        transformerFactory = TransformerFactory.newInstance();
    }

    @Override
    public void send(LogEvent event) {
        if (shouldPrettyPrint(event)) {
            event.setPayload(getPrettyMessage(event.getPayload()));
        }
        next.send(event);
    }

    private boolean shouldPrettyPrint(LogEvent event) {
        return prettyLogging 
            && event.getContentType() != null 
            && event.getContentType().indexOf("xml") >= 0 
            && event.getPayload().length() > 0;
    }

    public String getPrettyMessage(String message) {
        try {
            Transformer serializer = transformerFactory.newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter swriter = new StringWriter();
            serializer.transform(new StreamSource(new StringReader(message)), new StreamResult(swriter));
            return swriter.toString();
        } catch (TransformerException e) {
            return message;
        }
    }

    public void setNext(LogEventSender next) {
        this.next = next;
    }
    
    public void setPrettyLogging(boolean prettyLogging) {
        this.prettyLogging = prettyLogging;
    }

}
