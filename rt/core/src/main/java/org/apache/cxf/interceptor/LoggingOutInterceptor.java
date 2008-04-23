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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
public class LoggingOutInterceptor extends AbstractPhaseInterceptor {
   
    private static final Logger LOG = LogUtils.getL7dLogger(LoggingOutInterceptor.class); 

    private int limit = 100 * 1024;
    private PrintWriter writer;
    
    public LoggingOutInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(StaxOutInterceptor.class.getName());
    }
    public LoggingOutInterceptor(int lim) {
        this();
        limit = lim;
    }

    public LoggingOutInterceptor(PrintWriter w) {
        this();
        this.writer = w;
    }
    
    public void setLimit(int lim) {
        limit = lim;
    }
    
    public int getLimit() {
        return limit;
    }    

    public void handleMessage(Message message) throws Fault {
        final OutputStream os = message.getContent(OutputStream.class);
        if (os == null) {
            return;
        }

        if (LOG.isLoggable(Level.INFO) || writer != null) {
            // Write the output while caching it for the log message
            final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
            message.setContent(OutputStream.class, newOut);
            newOut.registerCallback(new LoggingCallback(message));
        }
    }

    class LoggingCallback implements CachedOutputStreamCallback {
        
        private final Message message;
        
        public LoggingCallback(final Message msg) {
            this.message = msg;
        }

        public void onFlush(CachedOutputStream cos) {  
            
        }
        
        public void onClose(CachedOutputStream cos) {
            final LoggingMessage buffer = new LoggingMessage("Outbound Message\n---------------------------");
            
            String encoding = (String)message.get(Message.ENCODING);

            if (encoding != null) {
                buffer.getEncoding().append(encoding);
            }            
            
            Object headers = message.get(Message.PROTOCOL_HEADERS);

            if (headers != null) {
                buffer.getHeader().append(headers);
            }

            if (cos.getTempFile() == null) {
                //buffer.append("Outbound Message:\n");
                if (cos.size() > limit) {
                    buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
                }
            } else {
                buffer.getMessage().append("Outbound Message (saved to tmp file):\n");
                buffer.getMessage().append("Filename: " + cos.getTempFile().getAbsolutePath() + "\n");
                if (cos.size() > limit) {
                    buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
                }
            }
            try {
                cos.writeCacheTo(buffer.getPayload(), limit);
            } catch (Exception ex) {
                //ignore
            }

            if (writer != null) {
                writer.println(buffer.toString());
            } else if (LOG.isLoggable(Level.INFO)) {
                LOG.info(buffer.toString());
            }
        }
    } 
}
