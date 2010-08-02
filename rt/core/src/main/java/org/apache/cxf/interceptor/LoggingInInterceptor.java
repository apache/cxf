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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * A simple logging handler which outputs the bytes of the message to the
 * Logger.
 */
public class LoggingInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(LoggingInInterceptor.class);

    private int limit = 100 * 1024;
    private PrintWriter writer;
    
    
    public LoggingInInterceptor() {
        super(Phase.RECEIVE);
    }
    
    public LoggingInInterceptor(String phase) {
        super(phase);
    }

    public LoggingInInterceptor(int lim) {
        this();
        limit = lim;
    }

    public LoggingInInterceptor(PrintWriter w) {
        this();
        this.writer = w;
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

    public void handleMessage(Message message) throws Fault {
        if (writer != null || LOG.isLoggable(Level.INFO)) {
            logging(message);
        }
    }

    /**
     * Transform the string before display. The implementation in this class 
     * does nothing. Override this method if you want to change the contents of the 
     * logged message before it is delivered to the output. 
     * For example, you can use this to mask out sensitive information.
     * @param originalLogString the raw log message.
     * @return transformed data
     */
    protected String transform(String originalLogString) {
        return originalLogString;
    } 

    private void logging(Message message) throws Fault {
        if (message.containsKey(LoggingMessage.ID_KEY)) {
            return;
        }
        String id = (String)message.getExchange().get(LoggingMessage.ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(LoggingMessage.ID_KEY, id);
        }
        message.put(LoggingMessage.ID_KEY, id);
        final LoggingMessage buffer 
            = new LoggingMessage("Inbound Message\n----------------------------", id);

        Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
        if (responseCode != null) {
            buffer.getResponseCode().append(responseCode);
        }

        String encoding = (String)message.get(Message.ENCODING);

        if (encoding != null) {
            buffer.getEncoding().append(encoding);
        }
        String ct = (String)message.get(Message.CONTENT_TYPE);
        if (ct != null) {
            buffer.getContentType().append(ct);
        }
        Object headers = message.get(Message.PROTOCOL_HEADERS);

        if (headers != null) {
            buffer.getHeader().append(headers);
        }
        String uri = (String)message.get(Message.REQUEST_URI);
        if (uri != null) {
            buffer.getAddress().append(uri);
        }
            
        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            CachedOutputStream bos = new CachedOutputStream();
            try {
                IOUtils.copy(is, bos);

                bos.flush();
                is.close();

                message.setContent(InputStream.class, bos.getInputStream());
                if (bos.getTempFile() != null) {
                    //large thing on disk...
                    buffer.getMessage().append("\nMessage (saved to tmp file):\n");
                    buffer.getMessage().append("Filename: " + bos.getTempFile().getAbsolutePath() + "\n");
                }
                if (bos.size() > limit) {
                    buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
                }
                if (StringUtils.isEmpty(encoding)) {
                    bos.writeCacheTo(buffer.getPayload(), limit);
                } else {
                    bos.writeCacheTo(buffer.getPayload(), encoding, limit);
                }
                    
                bos.close();
            } catch (IOException e) {
                throw new Fault(e);
            }
        }

        if (writer != null) {
            writer.println(transform(buffer.toString()));
        } else if (LOG.isLoggable(Level.INFO)) {
            LOG.info(transform(buffer.toString()));
        }
    }
}
