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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

/**
 * A simple logging handler which outputs the bytes of the message to the
 * Logger.
 */
public abstract class AbstractLoggingInterceptor extends AbstractPhaseInterceptor<Message> {

    protected int limit = 100 * 1024;
    protected PrintWriter writer;
    protected boolean prettyLogging;
    
    public AbstractLoggingInterceptor(String phase) {
        super(phase);
    }
    public AbstractLoggingInterceptor(String id, String phase) {
        super(id, phase);
    }
    
    protected abstract Logger getLogger();

    public void setOutputLocation(String s) {
        if (s == null || "<logger>".equals(s)) {
            writer = null;
        } else if ("<stdout>".equals(s)) {
            writer = new PrintWriter(System.out, true);
        } else if ("<stderr>".equals(s)) {
            writer = new PrintWriter(System.err, true);  
        } else {
            try {
                URI uri = new URI(s);
                File file = new File(uri);
                writer = new PrintWriter(new FileWriter(file, true), true);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "Error configuring log location " + s, ex);
            }
        }
    }
    
    public void setPrintWriter(PrintWriter w) {
        writer = w;
    }
    
    public PrintWriter getPrintWriter() {
        return writer;
    }
    
    public void setLimit(int lim) {
        limit = lim;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setPrettyLogging(boolean flag) {
        prettyLogging = flag;
    }
    
    public boolean isPrettyLogging() {
        return prettyLogging;
    }
    
    
    protected void writePayload(StringBuilder builder, CachedOutputStream cos,
                                String encoding, String contentType) 
        throws Exception {
        // Just transform the XML message when the cos has content
        if (isPrettyLogging() && (contentType != null && contentType.indexOf("xml") >= 0 
            && contentType.toLowerCase().indexOf("multipart/related") < 0) && cos.size() > 0) {
            Transformer serializer = XMLUtils.newTransformer(2);
            // Setup indenting to "pretty print"
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter swriter = new StringWriter();
            serializer.transform(new StreamSource(cos.getInputStream()), new StreamResult(swriter));
            String result = swriter.toString();
            if (result.length() < limit || limit == -1) {
                builder.append(swriter.toString());
            } else {
                builder.append(swriter.toString().substring(0, limit));
            }

        } else {
            if (StringUtils.isEmpty(encoding)) {
                cos.writeCacheTo(builder, limit);
            } else {
                cos.writeCacheTo(builder, encoding, limit);
            }

        }
    }


    /**
     * Transform the string before display. The implementation in this class 
     * does nothing. Override this method if you wish to change the contents of the 
     * logged message before it is delivered to the output. 
     * For example, you can use this to mask out sensitive information.
     * @param originalLogString the raw log message.
     * @return transformed data
     */
    protected String transform(String originalLogString) {
        return originalLogString;
    } 

    protected void log(String message) {
        message = transform(message);
        if (writer != null) {
            writer.println(message);
            // Flushing the writer to make sure the message is written 
            writer.flush();
        } else if (getLogger().isLoggable(Level.INFO)) {
            getLogger().info(message);
        }
    }
    
}
